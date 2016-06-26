package nl.tudelft.fragments

import org.scalatest.FunSuite

class GraphSuite extends FunSuite {

  test("scope") {
    val constraints = List(
      Ref(SymbolicName("", "n190"), ScopeVar("s192")),
      Res(SymbolicName("", "n190"), NameVar("n193")),
      TypeOf(SymbolicName("", "n190"), TypeVar("t191")),
      TypeEquals(TypeVar("t176"), TypeAppl("Fun", List(TypeVar("t191"), TypeVar("t177")))),
      DirectEdge(ScopeVar("s192"), ScopeVar("s172")),
      Dec(ScopeVar("s192"), SymbolicName("", "n170")),
      TypeOf(SymbolicName("", "n170"), TypeVar("t191"))
    )

    assert(Graph(constraints).scope(SymbolicName("", "n190")) == List(ScopeVar("s192")))
  }

  test("associated import") {
    val constraints = List(
      DirectEdge(ScopeVar("s1"), ScopeVar("s")),
      DirectEdge(ScopeVar("s2"), ScopeVar("s")),
      Dec(ScopeVar("s"), SymbolicName("C", "n1")),
      Dec(ScopeVar("s"), SymbolicName("C", "n2")),
      AssocFact(SymbolicName("C", "n1"), ScopeVar("s1")),
      AssocFact(SymbolicName("C", "n2"), ScopeVar("s2")),
      Ref(SymbolicName("C", "n3"), ScopeVar("s2")),
      AssociatedImport(ScopeVar("s2"), SymbolicName("C", "n3"))
    )

    assert(Graph(constraints).resolves(Nil, SymbolicName("C", "n3"), Nil) == List((
      List(SymbolicName("C", "n3")),
      List(Parent()),
      SymbolicName("C", "n1"),
      List(Eq(SymbolicName("C", "n3"), SymbolicName("C", "n1")))
      ), (
      List(SymbolicName("C", "n3")),
      List(Parent()),
      SymbolicName("C", "n2"),
      List(Eq(SymbolicName("C", "n3"), SymbolicName("C", "n2")))
      )))
  }

  test("chained associated import") {
    val constraints = List(
      DirectEdge(ScopeVar("s1"), ScopeVar("s")),
      DirectEdge(ScopeVar("s2"), ScopeVar("s")),
      DirectEdge(ScopeVar("s3"), ScopeVar("s")),
      Dec(ScopeVar("s"), SymbolicName("C", "n1")),
      Dec(ScopeVar("s"), SymbolicName("C", "n2")),
      Dec(ScopeVar("s"), SymbolicName("C", "n3")),
      AssocFact(SymbolicName("C", "n1"), ScopeVar("s1")),
      AssocFact(SymbolicName("C", "n2"), ScopeVar("s2")),
      AssocFact(SymbolicName("C", "n3"), ScopeVar("s3")),
      Ref(SymbolicName("C", "n4"), ScopeVar("s")),
      Ref(SymbolicName("C", "n5"), ScopeVar("s")),
      AssociatedImport(ScopeVar("s1"), SymbolicName("C", "n4")),
      AssociatedImport(ScopeVar("s3"), SymbolicName("C", "n5"))
    )

    assert(Graph(constraints).resolves(Nil, SymbolicName("C", "n4"), Nil) == List((
      List(SymbolicName("C", "n4")),
      List(),
      SymbolicName("C", "n1"),
      List(Eq(SymbolicName("C", "n4"), SymbolicName("C", "n1")))
      ), (
      List(SymbolicName("C", "n4")),
      List(),
      SymbolicName("C", "n2"),
      List(Eq(SymbolicName("C", "n4"), SymbolicName("C", "n2")))
      ), (
      List(SymbolicName("C", "n4")),
      List(),
      SymbolicName("C", "n3"),
      List(Eq(SymbolicName("C", "n4"), SymbolicName("C", "n3")))
      )))
  }

  test("dependent resolution") {
    val constraints = List(
      Ref(SymbolicName("C", "n0"), ScopeVar("s")),
      Dec(ScopeVar("s"), SymbolicName("C", "n1")),
      Dec(ScopeVar("s"), SymbolicName("C", "n2")),
      AssocFact(SymbolicName("C", "n1"), ScopeVar("s2")),
      AssocFact(SymbolicName("C", "n2"), ScopeVar("s3")),
      Dec(ScopeVar("s2"), SymbolicName("V", "n3")),
      Dec(ScopeVar("s3"), SymbolicName("V", "n4")),
      DirectEdge(ScopeVar("s4"), ScopeVar("s")),
      Ref(SymbolicName("V", "n5"), ScopeVar("s4")),
      AssociatedImport(ScopeVar("s4"), SymbolicName("C", "n0"))
    )

    assert(Graph(constraints).resolves(Nil, SymbolicName("V", "n5"), Nil).length == 2)
  }

  test("dependent resolution with naming conditions") {
    val constraints = List(
      Ref(SymbolicName("C", "n0"), ScopeVar("s")),
      Dec(ScopeVar("s"), SymbolicName("C", "n1")),
      Dec(ScopeVar("s"), SymbolicName("C", "n2")),
      AssocFact(SymbolicName("C", "n1"), ScopeVar("s2")),
      AssocFact(SymbolicName("C", "n2"), ScopeVar("s3")),
      Dec(ScopeVar("s2"), SymbolicName("V", "n3")),
      Dec(ScopeVar("s3"), SymbolicName("V", "n4")),
      DirectEdge(ScopeVar("s4"), ScopeVar("s")),
      Ref(SymbolicName("V", "n5"), ScopeVar("s4")),
      AssociatedImport(ScopeVar("s4"), SymbolicName("C", "n0"))
    )

    val conditions = List(
      Eq(SymbolicName("C", "n0"), SymbolicName("C", "n1")),
      Diseq(SymbolicName("C", "n0"), SymbolicName("C", "n2"))
    )

    assert(Graph(constraints).resolves(Nil, SymbolicName("V", "n5"), conditions).length == 1)
  }

}
