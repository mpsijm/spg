package org.metaborg.spg.sentence.signature;

import org.metaborg.sdf2table.grammar.Sort;
import org.spoofax.interpreter.terms.IStrategoConstructor;

import java.util.List;

public class Constructor {
    private final String name;
    private final List<Sort> arguments;
    private final Sort result;

    public Constructor(String name, List<Sort> arguments, Sort result) {
        this.name = name;
        this.arguments = arguments;
        this.result = result;
    }

    public String getName() {
        return name;
    }

    public int getArity() {
        return arguments.size();
    }

    public Sort getArgument(int i) {
        return arguments.get(i);
    }

    public Sort getResult() {
        return result;
    }

    public boolean matches(IStrategoConstructor constructor) {
        return name.equals(constructor.getName()) && getArity() == constructor.getArity();
    }
}
