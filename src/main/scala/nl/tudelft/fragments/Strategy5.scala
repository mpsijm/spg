package nl.tudelft.fragments

import javax.inject.Singleton

import nl.tudelft.fragments.spoofax.Signatures.Decl
import nl.tudelft.fragments.spoofax.{Printer, Signatures, Specification}
import org.metaborg.core.project.{IProjectService, SimpleProjectService}
import org.metaborg.spoofax.core.{Spoofax, SpoofaxModule}

import scala.annotation.tailrec

object Strategy5 {
  val spoofax = new Spoofax(new SpoofaxModule() {
    override def bindProject() {
      bind(classOf[SimpleProjectService]).in(classOf[Singleton])
      bind(classOf[IProjectService]).to(classOf[SimpleProjectService])
    }
  })

  implicit val signatures = Signatures.read(
    strategoPath = "zip:/Users/martijn/Projects/spoofax-releng/stratego/org.metaborg.meta.lang.stratego/target/org.metaborg.meta.lang.stratego-2.0.0-SNAPSHOT.spoofax-language!/",
    signaturePath = "/Users/martijn/Projects/scopes-frames/L3/src-gen/signatures/L3-sig.str"
  )

  implicit val specification = Specification.read(
    nablPath = "zip:/Users/martijn/Projects/nabl/org.metaborg.meta.nabl2.lang/target/org.metaborg.meta.nabl2.lang-2.0.0-SNAPSHOT.spoofax-language!/",
    specPath = "/Users/martijn/Projects/scopes-frames/L3/trans/analysis/l3.nabl2"
  )

  implicit val rules: List[Rule] = specification.rules

  def main(args: Array[String]): Unit = {
    val kb = repeat(gen, 1000)(rules)

    for (i <- 1 to 100) {
      val rule = kb.filter(_.sort == SortAppl("Start")).random

      val term = complete(kb, rule)

      println(term)

      if (term.isDefined) {
        val solutions = Solver
          .solve(term.get.state)
          .map(state => term.get.copy(state = state))

        if (solutions.nonEmpty) {
          println("Solved all constraints in the following " + solutions.length + " ways:")

          for (solution <- solutions) {
            println(solution)
          }
        } else {
          println("Unable to solve the constraints")
        }
      }

      /*
      if (term.isDefined) {
        val concrete = Concretor.concretize(term.get, term.get.state.facts)

        val aterm = Converter.toTerm(concrete)

        println(print(aterm))
      }
      */
    }
  }

  def gen(rules: List[Rule])(implicit signatures: List[Decl]): List[Rule] = {
    // Pick a random rule
    val rule = rules.random

    // Pick a random recurse constraint
    val recurseOpt = rule.recurse.safeRandom

    // Lazily merge a random other rule $r \in rules$ into $rule$, solving $recurse$
    val mergedOpt = recurseOpt.flatMap(recurse =>
      rules.shuffle.view
        .flatMap(rule.merge(recurse, _))
        .find(_.pattern.size < 10)
    )

    // Solve any constraints, create new rules out of all possible solvings
    val newRules = mergedOpt.map(merged =>
      Solver
        .solveAny(merged.state)
        .map(state => merged.copy(state = state))
    )

    // Check the new rules for consistency
    val validNewRules = newRules.map(rules =>
      rules.filter(rule => Consistency.check(rule))
    )

    val result = validNewRules
      .map(_ ++ rules)
      .getOrElse(rules)

    result
  }

  // Complete the given rule by solving resolution & recurse constraints
  def complete(rules: List[Rule], rule: Rule, limit: Int = 10)(implicit signatures: List[Decl]): Option[Rule] =
    if (limit == 0) {
      None
    } else {
      println(rule)

      rule.recurse match {
        case Nil =>
          Some(rule)
        case _ =>
          for (recurse <- rule.recurse) {
            val choices = rules
              .flatMap(rule.merge(recurse, _))
              .filter(choice => choice.pattern.size + choice.recurse.size <= 20)

            if (choices.isEmpty) {
              return None
            } else {
              for (choice <- choices) {
                val deeper = complete(rules, choice, limit-1)

                if (deeper.isDefined) {
                  return deeper
                }
              }
            }
          }

          None
      }
    }
}
