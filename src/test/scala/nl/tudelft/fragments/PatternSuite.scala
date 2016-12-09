package nl.tudelft.fragments

import nl.tudelft.fragments.spoofax.models.SortVar
import org.scalatest.FunSuite

class PatternSuite extends FunSuite {
  test("collect on pattern") {
    val rule = Rule("Init", SortVar("y"), Some(TermAppl("UNIT", List())), List(ScopeAppl("s424595")), State(TermAppl("Mod", List(TermAppl("Let", List(TermAppl("Cons", List(TermAppl("TypeDecs", List(TermAppl("Conss", List(TermAppl("TypeDec", List(Var("x424785"), TermAppl("RecordTy", List(TermAppl("Nil", List()))))), TermAppl("Nil", List()))))), TermAppl("Nil", List()))), TermAppl("Nil", List()))))),List(CDistinct(Declarations(ScopeAppl("s424986"), "All")), CDistinct(Declarations(ScopeAppl("s424985"), "Field"))),List(CGDecl(ScopeAppl("s424595"),ConcreteName("Type", "int", -1)), CGDecl(ScopeAppl("s424595"),ConcreteName("Type", "string", -1)), CGDecl(ScopeAppl("s424595"),ConcreteName("Var", "print", -1)), CGDecl(ScopeAppl("s424595"),ConcreteName("Var", "flush", -1)), CGDecl(ScopeAppl("s424595"),ConcreteName("Var", "getchar", -1)), CGDecl(ScopeAppl("s424595"),ConcreteName("Var", "ord", -1)), CGDecl(ScopeAppl("s424595"),ConcreteName("Var", "chr", -1)), CGDecl(ScopeAppl("s424595"),ConcreteName("Var", "size", -1)), CGDecl(ScopeAppl("s424595"),ConcreteName("Var", "substring", -1)), CGDecl(ScopeAppl("s424595"),ConcreteName("Var", "concat", -1)), CGDecl(ScopeAppl("s424595"),ConcreteName("Var", "not", -1)), CGDecl(ScopeAppl("s424595"),ConcreteName("Var", "exit", -1)), CGDirectEdge(ScopeAppl("s424986"),Label('P'),ScopeAppl("s424595")), CGDecl(ScopeAppl("s424986"),SymbolicName("Type", "x424785"))),TypeEnv(Map(Binding(ConcreteName("Var", "concat", -1), TermAppl("FUN", List(TermAppl("Cons", List(TermAppl("STRING", List()), TermAppl("Cons", List(TermAppl("STRING", List()), TermAppl("Nil", List()))))), TermAppl("STRING", List())))), Binding(ConcreteName("Var", "size", -1), TermAppl("FUN", List(TermAppl("Cons", List(TermAppl("STRING", List()), TermAppl("Nil", List()))), TermAppl("INT", List())))), Binding(ConcreteName("Var", "print", -1), TermAppl("FUN", List(TermAppl("Cons", List(TermAppl("STRING", List()), TermAppl("Nil", List()))), TermAppl("UNIT", List())))), Binding(ConcreteName("Var", "ord", -1), TermAppl("FUN", List(TermAppl("Cons", List(TermAppl("STRING", List()), TermAppl("Nil", List()))), TermAppl("INT", List())))), Binding(ConcreteName("Var", "chr", -1), TermAppl("FUN", List(TermAppl("Cons", List(TermAppl("INT", List()), TermAppl("Nil", List()))), TermAppl("STRING", List())))), Binding(ConcreteName("Type", "string", -1), TermAppl("STRING", List())), Binding(ConcreteName("Var", "substring", -1), TermAppl("FUN", List(TermAppl("Cons", List(TermAppl("INT", List()), TermAppl("Cons", List(TermAppl("INT", List()), TermAppl("Cons", List(TermAppl("STRING", List()), TermAppl("Nil", List()))))))), TermAppl("STRING", List())))), Binding(ConcreteName("Var", "not", -1), TermAppl("FUN", List(TermAppl("Cons", List(TermAppl("INT", List()), TermAppl("Nil", List()))), TermAppl("INT", List())))), Binding(ConcreteName("Var", "exit", -1), TermAppl("FUN", List(TermAppl("Cons", List(TermAppl("INT", List()), TermAppl("Nil", List()))), TermAppl("UNIT", List())))), Binding(ConcreteName("Var", "getchar", -1), TermAppl("FUN", List(TermAppl("Nil", List()), TermAppl("STRING", List())))), Binding(ConcreteName("Var", "flush", -1), TermAppl("FUN", List(TermAppl("Nil", List()), TermAppl("UNIT", List())))), Binding(ConcreteName("Type", "int", -1), TermAppl("INT", List())), Binding(SymbolicName("Type", "x424785"), TermAppl("RECORD", List(TermAppl("s_rec", List())))))),Resolution(Map()),SubtypeRelation(List(Binding(TermAppl("NIL", List()), TermAppl("RECORD", List(TermAppl("s_rec", List())))))),List()))

    val typeDecs = rule.pattern.collect {
      case x@TermAppl("TypeDecs", _) =>
        List(x)
      case _ =>
        Nil
    }

    val varDecs = rule.pattern.collect {
      case x@TermAppl("VarDec", _) =>
        List(x)
      case _ =>
        Nil
    }

    println(typeDecs)
    println(varDecs)
  }

  test("alpha-equivalent patterns") {
    val p1 = TermAppl("X", List(Var("a")))
    val p2 = TermAppl("X", List(Var("b")))

    assert(Pattern.equivalence(p1, p2))
  }

  test("more general pattern is not alpha-equivalent to more specific pattern") {
    val p1 = TermAppl("X", List(Var("a"), Var("b")))
    val p2 = TermAppl("X", List(Var("c"), Var("c")))

    assert(!Pattern.equivalence(p1, p2))
  }
}
