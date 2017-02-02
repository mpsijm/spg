package org.metaborg.spg.core

import org.metaborg.spg.core.resolution.LabelImplicits._
import org.metaborg.spg.core.resolution.{Label, LabelOrdering}
import org.metaborg.spg.core.solver._
import org.metaborg.spg.core.spoofax.{Language, ResolutionParams, Specification}
import org.scalatest.FunSuite

class ConcretorSuite extends FunSuite {
  val resolutionParams = new ResolutionParams(
    labels = List(
      Label('P'),
      Label('I')
    ),
    order = LabelOrdering(
      (Label('D'), Label('P')),
      (Label('D'), Label('I')),
      (Label('I'), Label('P'))
    ),
    wf = (Label('P') *) ~ (Label('I') *)
  )

  val language: Language = new Language(Nil, null, new Specification(resolutionParams, Nil), null, Set(), null)

  test("fails to concretize on inconsistent resolution") {
    val state = Program(
      pattern =
        TermAppl("x"),
      constraints = List(
        CGRef(SymbolicName("Class", "a"), TermAppl("s")),
        CGRef(SymbolicName("Class", "b"), TermAppl("s")),
        CGDirectEdge(TermAppl("s"), Label('P'), TermAppl("s'")),
        CGDecl(TermAppl("s'"), SymbolicName("Class", "b")),
        CGDirectEdge(TermAppl("s'"), Label('P'), TermAppl("s''")),
        CGDecl(TermAppl("s''"), SymbolicName("Class", "c"))
      ),
      typeEnv =
        TypeEnv(),
      resolution = Resolution(Map(
        SymbolicName("Class", "a") -> SymbolicName("Class", "b"),
        SymbolicName("Class", "b") -> SymbolicName("Class", "c")
      )),
      subtypes =
        Subtypes(),
      inequalities =
        Nil
    )

    println(Concretor(language).concretize(state)(language))
  }
}