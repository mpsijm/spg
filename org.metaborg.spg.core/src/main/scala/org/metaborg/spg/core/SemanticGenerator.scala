package org.metaborg.spg.core

import com.google.inject.Inject
import com.typesafe.scalalogging.LazyLogging
import org.metaborg.core.language.ILanguageService
import org.metaborg.spg.core.resolution.{Graph, Occurrence}
import org.metaborg.spg.core.resolution.OccurrenceImplicits._
import org.metaborg.spg.core.solver._
import org.metaborg.spg.core.stratego.Strategy.{attempt, topdown}
import org.metaborg.spg.core.spoofax.{Converter, Language, LanguageService}
import org.metaborg.spg.core.stratego.Strategy
import org.metaborg.spg.core.terms.{Pattern, Var}

import scala.util.Random

/**
  * The semantics generator generates semantically valid terms.
  *
  * @param languageService
  * @param baseLanguageService
  * @param random
  */
class SemanticGenerator @Inject()(languageService: LanguageService, baseLanguageService: ILanguageService)(implicit val random: Random) extends AbstractGenerator(languageService, baseLanguageService) with LazyLogging {
  /**
    * Generate a single term by repeatedly invoking generateTry until it
    * returns a semantically valid term.
    *
    * @param language
    * @param config
    * @return
    */
  override def generateSingle(language: Language, config: Config): String = {
    Iterator
      .continually(generateTry(language, config))
      .dropWhile(_.isEmpty)
      .next
      .get
  }

  /**
    * Try to generate a semantically valid term by invoking generateFueled with
    * a fuel parameter taken from the config object.
    *
    * If a term can be generated, returns Some with the term. Otherwise,
    * returns None.
    *
    * @param language
    * @param config
    * @return
    */
  private def generateTry(implicit language: Language, config: Config): Option[String] = {
    nameProvider.reset()

    val init = language.initRule.instantiate()
    val start = language.startRules.random
    val recurse = CGenRecurse(start.name, init.pattern, init.scopes, init.typ, start.sort, config.sizeLimit)
    val program = Program.fromRule(init) + recurse

    try {
      val termOpt = generateTop(program)(language, config)

      termOpt.map(term => {
        val concretePattern = Concretor(language).concretize(term)
        val strategoTerm = Converter.toTerm(concretePattern)

        language.printer(strategoTerm)
      })
    } catch {
      case OutOfFuelException(rule) =>
        logger.debug("Out of fuel: {}", rule)

        None
      case PatternSizeException(rule) =>
        logger.debug("Rule pattern too large: {}", rule)

        None
      case InconsistencyException(rule) =>
        logger.debug("Inconsistency observed in program: {}", rule)

        None
    }
  }

  def generateTop(program: Program)(implicit language: Language, config: Config): Option[Program] = {
    generateFueled(language, config)(program.recurse.head, config.sizeLimit, program.constraints).flatMap {
      case (program, _) =>
        Solver.solveAll(program).randomOption
    }.randomOption
  }

  /**
    * Wraps generateRecursive in a function that limits backtracking by the
    * given fuel parameter.
    *
    * @param language
    * @param config
    * @return
    */
  def generateFueled(language: Language, config: Config): (CGenRecurse, Int, List[Constraint]) => List[(Program, TermBinding)] = {
    var mutableFuel = config.fuel

    lazy val self: (CGenRecurse, Int, List[Constraint]) => List[(Program, TermBinding)] = (r: CGenRecurse, s: Int, c: List[Constraint]) => mutableFuel match {
      case 0 =>
        Nil
      case _ =>
        mutableFuel = mutableFuel - 1; generateRecursive(self)(r, s, c)(language, config)
    }

    self
  }

  /**
    * Given a recurse constraint, generate a program.
    *
    * For the given recurse: pick a random rule, generate a program for a
    * random child, merge it back into the rule, solve unsolved "trivial"
    * constraints, and continue with the next child.
    *
    * Afterwards, there may still be unsolved constraints (e.g. resolve
    * constraints). Let the caller solve remaining constraints (see
    * generateTop).
    *
    * @param generateRecursive Continuation
    * @param recurse
    * @param size
    * @param language
    * @param config
    * @return
    */
  def generateRecursive(generateRecursive: (CGenRecurse, Int, List[Constraint]) => List[(Program, TermBinding)])(recurse: CGenRecurse, size: Int, context: List[Constraint])(implicit language: Language, config: Config): List[(Program, TermBinding)] = {
    if (size > 0) {
      for (rule <- language.rules(recurse).shuffle) {
        val program = Program.fromRule(rule.instantiate().freshen())
        val mergedPrograms = generateProgram(generateRecursive)(program, size, context)

        for (mergedProgram <- mergedPrograms) {
          val cleaned = clean(mergedProgram, context)

          if (cleaned.nonEmpty) {
            return cleaned
          }
        }
      }
    }

    Nil
  }

  /**
    * Given a program, complete it by filling in all holes.
    *
    * @param generateRecursive
    * @param program
    * @param size
    * @param context
    * @return
    */
  def generateProgram(generateRecursive: (CGenRecurse, Int, List[Constraint]) => List[(Program, TermBinding)])(program: Program, size: Int, context: List[Constraint])(implicit language: Language): List[(Program, Map[Var, Pattern])] = {
    val childSize = (size - 1) / (program.recurse.size max 1)

    // TODO: Propagate unifier instead of going over (1 to recurse.size) and picking a random recurse

    (1 to program.recurse.size).toList.foldLeftMap((program, Map.empty[Var, Pattern])) {
      case ((program, substitution), _) =>
        val recurse = program.recurse.random
        val newContext = (context ++ program.constraints).substitute(substitution)
        val options = generateRecursive(recurse, childSize, newContext)

        options.flatMap {
          case (subProgram, newSubstitution) =>
            program.merge(recurse, subProgram).map(program => {
              val substitutedProgram = program.substitute(newSubstitution)

              // Due to the merge, we may need to propagate knowledge again
              val solvedProgram = Solver.solveFixpoint(substitutedProgram)

              // Return new program and propagate all substitutions upward
              (solvedProgram, substitution ++ newSubstitution)
            })
        }
    }
  }

  /**
    * Cleanup a program by solving all its CResolve constraints.
    *
    * TODO: We should also require all CSubtype constraints to be satisfied, as these may also lead to excessive backtracking.
    *
    * @param pu
    * @param context
    * @param language
    * @return
    */
  def clean(pu: (Program, Map[Var, Pattern]), context: List[Constraint])(implicit language: Language): List[(Program, Map[Var, Pattern])] = {
    pu match {
      case (program, substitution) =>
        // First, solveFixpoint to cleanup any remaining constraints
        val a = Solver.solveFixpoint(program)

        // Second, resolve every reference. We don't want unresolved references, because this kills performance
        val b = solveResolves(a, context)

        // Third, solveFixpoint on each option to cleanup any remaining constraints (again)
        val c = b.map {
          case (program, newSubstitution) =>
            (Solver.solveFixpoint(program), newSubstitution)
        }

        // Forth, remove any programs that are now inconsistent. We don't want inconsistencies.
        val d = c.filter {
          case (program, _) =>
            Consistency.constraintsCheck(program)
        }

        // If there is at least one valid program
        if (d.nonEmpty) {
          // Fifth, for all remaining programs, combine the substitution
          return d.map {
            case (program, newSubstitution) =>
              (program, substitution ++ newSubstitution)
          }
        }
    }

    Nil
  }

  /**
    * Solve all CResolve constraints in the given program.
    *
    * If a reference cannot be resolved, return None. If a reference can be
    * resolved to multiple declarations, fork on each choice.
    *
    * TODO: The order of resolving references may be important (when references depend upon each other)
    */
  def solveResolves(program: Program, context: List[Constraint])(implicit language: Language): List[(Program, Map[Var, Pattern])] = {
    program.resolve.foldLeftMap((program, Map.empty[Var, Pattern])) {
      case ((program, substitution), resolve) =>
        solveResolve(program, resolve, context).map {
          case (program, newSubstitution) =>
            (program, substitution ++ newSubstitution)
        }
    }
  }

  /**
    * Solve the given CResolve constraint in the given program.
    *
    * If the reference cannot be resolved, return None. Otherwise, fork on each
    * declaration that the reference may resolve to and return the new program.
    *
    * @return
    */
  def solveResolve(program: Program, resolve: CResolve, context: List[Constraint])(implicit language: Language): List[(Program, TermBinding)] = {
    val declarations = Graph(program.constraints ++ context).res(Resolution())(resolve.n1)

    declarations.toList.flatMap(declaration => {
      applyResolution(program, resolve, declaration)
    })
  }

  /**
    * Solve the given CResolve constraint by resolving to the given declaration.
    *
    * TODO: We use a rewrite rule to replace the reference (occurrence) by the declaration (occurrence) to ensure they get the same name. Hacky..
    * TODO: Can't we move this method to the Program class? I.e. program.resolve(reference, declaration)?
    *
    * @param program
    * @param resolve
    * @param declaration
    * @return
    */
  def applyResolution(program: Program, resolve: CResolve, declaration: Occurrence): Option[(Program, TermBinding)] = {
    val substitutionOpt = resolve.n2.unify(declaration)

    substitutionOpt.map(substitution => {
      val newProgram = (program - resolve)
        .substitute(substitution)
        .addResolution(resolve.n1 -> declaration)
        .rewrite(topdown(attempt(new Strategy {
          override def apply(p: Pattern): Option[Pattern] = {
            if (p == resolve.n1.occurrence.name) {
              Some(declaration.name)
            } else {
              None
            }
          }
        })))

      (newProgram, substitution)
    })
  }
}
