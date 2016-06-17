package nl.tudelft.fragments

object Builder {
  import Graph._

//  type Point = (Pattern, Sort, List[Scope])
//
//  // Complete the given rule
//  def build(rules: List[Rule], current: Rule, size: Int, up: Map[Sort, Int], down: Map[Sort, Int]): Option[Rule] = {
//    val holes = current.pattern.vars
//
//    // If we have a complete program, return
//    if (holes.isEmpty && current.pattern.asInstanceOf[TermAppl].cons == "Program") {
//      return Some(current)
//    }
//
//    // TODO: consult the inverse signature. If we have a class and we are at size = 17, then we will never get a program,
//    // TODO:   since this requires Program(_, Cons(x, _)) which is size 21 and hence exceeds limit 20. So no need to go over the 5 possibilities
//    // TODO: another example: if we have a method at size = 11, then to get a program we need Program(_, Cons(Class(_, _, _, Cons(x, _)), _))
//    // TODO:   which has size = 21 and hence exceeds limit 20. So no need to go over the 8 possibilities here.
//    // TODO: and we can do the same thing for going down: given a
//
//    // TODO: Handle root cleanly. Is it logical to force generating the root first?
//    if (current.pattern.asInstanceOf[TermAppl].cons != "Program") {
//      // Consistency check (TODO: handle lists uniformly)
//      if (up.contains(current.sort)) {
//        if (up(current.sort) + current.pattern.size > size) {
//          return None
//        }
//      }
//
//      // Merge this rule into another rule
//      val applicable = rules
//        .filter(rule => rule.pattern.size + current.pattern.size + rule.pattern.vars.length + current.pattern.vars.length <= size)
//
//      for (randomRule <- applicable.randomSubset(20)) {
//        val randomHole = randomRule.pattern.vars.random
//        val merged = randomRule.merge(randomHole, current)
//
//        if (merged.isDefined) {
//          val result = build(rules, merged.get, size, up, down)
//
//          if (result.isDefined) {
//            return result
//          }
//        }
//      }
//    } else {
//      // Consistency check
//      val minimal = holes.map(hole => down.getOrElse(hole.sort, 0)).sum
//      if (minimal + current.pattern.size > size) {
//        return None
//      }
//
//      val hole: TermVar = holes.random
//
//      // Filter rules that are a) syntactically valid and b) balance the size
//      val applicable = rules
//        .filter(rule => rule.sort.unify(hole.sort).isDefined)
//        .filter(rule => rule.pattern.size+rule.pattern.vars.length <= (2*size-current.pattern.size-current.pattern.vars.length)/current.pattern.vars.length)
//
//      for (randomRule <- applicable.randomSubset(20)) {
//        val merged = current.merge(hole, randomRule)
//
//        if (merged.isDefined) {
//          val result = build(rules, merged.get, size, up, down)
//
//          if (result.isDefined) {
//            return result
//          }
//        }
//      }
//    }
//
//    None
//  }

  // Work towards the root
  def buildToRoot(rules: List[Rule], rule: Rule): List[Rule] = {
    val choices = for (other <- rules; recurse <- other.recurse) yield {
      other.merge(recurse, rule)
    }

    choices.flatten
  }

  // Close given hole in rule
  def buildToClose(rules: List[Rule], rule: Rule, recurse: Recurse): List[Rule] = {
    val choices = for (other <- rules) yield {
      rule.merge(recurse, other)
    }

    choices.flatten
  }

  // Close a hole in rule
  def buildToClose(rules: List[Rule], rule: Rule): List[Rule] = {
    val choices = for (recurse <- rule.recurse; other <- rules) yield {
      rule.merge(recurse, other)
    }

    choices.flatten
  }

  // Combine a rule with its resolution constraints
  def withRess(rule: Rule): List[(Rule, Res)] = {
    rulesWithRes(List(rule)).flatMap { case (rule, ress) =>
      ress.map(res =>
        (rule, res)
      )
    }
  }

  // Combine a (Rule, Res) with the scopes reachable from the reference in the resolution constraint
  def withScopes(r: (Rule, Res)): List[(Rule, Res, Scope)] = {
    r match { case (rule, res@Res(ref, _)) =>
      path(Nil, scope(ref, rule.state.facts).head, rule.state.facts, Nil).map(_._3).map(scope =>
        (rule, res, scope)
      )
    }
  }

  // Combine a (Rule, Res, Scope) with the extension points, i.e. holes (TODO: also consider root as extension point)
  // TODO: the situation is more complex with direct imports. E.g. n -> (s2) -> (s3), there is no hole with scope s3. There is one with s2,
  def withPoints(r: (Rule, Res, Scope)): List[(Rule, Res, Scope, Recurse)] = {
    r match { case (rule, res, scope) =>
      rule.recurse.filter(_.scopes.contains(scope)).map { point =>
        (rule, res, scope, point)
      }
    }
  }

  // Combine a (Rule, Res, Scope, Recurse) with another rule
  def withOther(r: (Rule, Res, Scope, Recurse), rules: List[Rule]): List[(Rule, Res, Scope, Recurse, Rule)] = {
    r match {
      case (rule, res, scope, recurse) =>
        rules.map(other =>
          (rule, res, scope, recurse, other)
        )
    }
  }

  // Combine a (Rule, Res, Scope, Recurse, Rule) with reachable declarations in the last rule
  def withDecs(r: (Rule, Res, Scope, Recurse, Rule)): List[(Rule, Res, Scope, Recurse, Rule, Name)] = {
    r match {
      case (rule, res, scope, recurse, other) =>
        // TODO: You're search for `recurse.scopes` in `other`. This won't work if you do not first merge the scopes?!
        val reachableDeclarations = recurse.scopes.flatMap(s =>
          decls(other, s)
            .filter {
              case (_, _, SymbolicName(ns, _), _) =>
                ns == res.n1.namespace
              case (_, _, ConcreteName(ns, name, pos), _) =>
                ns == res.n1.namespace && res.n1.isInstanceOf[ConcreteName] && res.n1.name == name // TODO: We should also be able to resolve symbolic names to concrete names..
              case _ =>
                throw new Exception("Not expected")
            }
        )

        reachableDeclarations.map { case (_, _, dec, _) =>
          (rule, res, scope, recurse, other, dec)
        }
    }
  }

  // Combine a (Rule, Res, Scope, Recurse, Rule, Name) with the solution after merging & resolving
  def withResolved(r: (Rule, Res, Scope, Recurse, Rule, Name)): List[(Rule, Res, Scope, Recurse, Rule, Name, Rule)] = {
    r match {
      case (rule, res, scope, recurse, other, dec) =>
        val mergeResult = rule.mergex(recurse, other)

        mergeResult.map { case (merged, nameBinding) =>
          // The merge may have changed the name of the Res-constraint that we are trying to fix, so apply same name substitution
          val newRes = res.substitute(nameBinding)

          // Resolve the reference to the declaration and solve additional constraints
          val resolvedOpt = resolve(merged, newRes, dec)

          resolvedOpt
            .map(resolved => List((rule, res, scope, recurse, other, dec, resolved)))
            .getOrElse(Nil)
        }.getOrElse(Nil)
    }
  }

  // Build to resolve on a single rule
  def buildToResolve(rules: List[Rule], rule: Rule): List[(Rule, Res, Scope, Recurse, Rule, Name, Rule)] = {
    // TODO: Currently, we only resolve a reference by merging. We should also consider resolving a reference within the fragment itself.
    // TODO: Currently, we only close holes, and do not consider merging the root into another fragment

    val ruleWithRess: List[(Rule, Res)] =
      withRess(rule)

    val ruleWithRessWithScopes: List[(Rule, Res, Scope)] =
      ruleWithRess.flatMap(withScopes)

    val ruleWithRefsWithScopesWithPoints =
      ruleWithRessWithScopes.flatMap(withPoints)

    val ruleWithRefsWithScopesWithPointsWithRules =
      ruleWithRefsWithScopesWithPoints.flatMap(withOther(_, rules))

    val ruleWithRefsWithScopesWithPointsWithRulesWithDecs =
      ruleWithRefsWithScopesWithPointsWithRules.flatMap(withDecs)

    val ruleWithRefsWithScopesWithPointsWithRulesWithDecsWithResolved =
      ruleWithRefsWithScopesWithPointsWithRulesWithDecs.flatMap(withResolved)

    // Return the choices
    ruleWithRefsWithScopesWithPointsWithRulesWithDecsWithResolved
  }

  def withDecsInternal(r: (Rule, Res)): List[(Rule, Res, Name)] = r match {
    case (rule, res@Res(ref, delta)) =>
      Graph
        .resolves(Nil, ref, rule.state.facts, rule.state.nameConstraints)
        .map { case (_, _, dec, _) =>
          (rule, res, dec)
        }
  }

  def withResolvedInternal(r: (Rule, Res, Name)): List[(Rule, Res, Name, Rule)] = r match {
    case (rule, res, dec) =>
      val resolvedOpt = resolve(rule, res, dec)

      resolvedOpt
        .map(resolved => List((rule, res, dec, resolved)))
        .getOrElse(Nil)
  }

  def buildToResolveInternal(rules: List[Rule], rule: Rule): List[(Rule, Res, Name, Rule)] = {
    val ruleWithRess: List[(Rule, Res)] =
      withRess(rule)

    val ruleWithRessWithDec: List[(Rule, Res, Name)] =
      ruleWithRess.flatMap(withDecsInternal)

    val ruleWithRessWithDecWithResolved =
      ruleWithRessWithDec.flatMap(withResolvedInternal)

    ruleWithRessWithDecWithResolved
  }

  // Merge fragments in such a way that we close resolution constraints
  def buildToResolve(rules: List[Rule]): List[Rule] = {
    val generated1 = buildToResolve(rules, rulesWithRes(rules).random._1)
    val generated2 = buildToResolveInternal(rules, rulesWithRes(rules).random._1)

    if (generated1.nonEmpty && generated2.nonEmpty) {
      generated1.random._7 :: generated2.random._4 :: rules
    } else if (generated1.nonEmpty) {
      generated1.random._7 :: rules
    } else if (generated2.nonEmpty) {
      generated2.random._4 :: rules
    } else {
      rules
    }
  }

  // Get the declarations that are reachable from given scope (TODO: "visible from given scope" ignores that we are looking for resolutions of a name, which has a namespace. We don't need DisEq constraints if the namespaces don't match anyway.)
  def decls(rule: Rule, scope: Scope) =
    visible(Nil, scope, rule.state.facts, Nil)

  // Get [Rule, [Res]]
  def rulesWithRes(rules: List[Rule]) = rules
    .filter(_.constraints.exists {
      case Res(n1, n2) => true
      case _ => false
    })
    .map(rule =>
      (rule, rule.constraints.flatMap {
        case res@Res(_, _) => Some(res)
        case _ => None
      })
    )

  // Attempts to resolve the reference from the given resolution constraint
  def resolve(rule: Rule, res: Res, dec: Name): Option[Rule] = res match {
    case Res(n, d@NameVar(_)) =>
      Solver
        // Rewrite the resolution constraint
        .rewrite(res, rule.state.copy(constraints = rule.state.constraints - res))
        // Propagate changes to other constraints
        .flatMap(Solver.solvePartial)
        // Create new rule with new state
        .map(state => rule.copy(state = state))
        // Filter on consistency
        .filter(rule => Consistency.check(rule.state.constraints))
    case _ =>
      throw new Exception("Could not match " + res)
  }
}
