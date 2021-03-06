package org.metaborg.spg.sentence.antlr.generator;

import org.metaborg.spg.sentence.antlr.grammar.*;
import org.metaborg.spg.sentence.antlr.grammar.Character;
import org.metaborg.spg.sentence.antlr.term.Appl;
import org.metaborg.spg.sentence.antlr.term.Term;
import org.metaborg.spg.sentence.antlr.term.TermList;
import org.metaborg.spg.sentence.antlr.term.Text;

import java.util.Optional;
import java.util.Random;

import static java.util.Optional.empty;
import static java.util.Optional.of;

public class Generator {
    private final Random random;
    private final Grammar grammar;

    public Generator(Random random, Grammar grammar) {
        this.random = random;
        this.grammar = grammar;
    }

    public Optional<Term> generate(String startSymbol, int size) {
        Nonterminal start = new Nonterminal(startSymbol);

        return generateNonterminal(start, size);
    }

    public Optional<Term> generateNonterminal(Nonterminal nonterminal, int size) {
        if (size <= 0) {
            return empty();
        }

        Rule rule = grammar.getRule(nonterminal.getName());

        return forRule(rule, size - 1).map(term -> node(nonterminal, term));

    }

    public Optional<Term> forRule(Rule rule, int size) {
        Optional<Term> treeOpt = forElement(rule.getEmptyElement(), size);

        if (rule.isLexical()) {
            return treeOpt.map(this::join);
        } else {
            return treeOpt;
        }
    }

    public Optional<Term> forElement(EmptyElement emptyElement, int size) {
        if (size <= 0) {
            return empty();
        }

        if (emptyElement instanceof Empty) {
            return generateEmpty((Empty) emptyElement, size);
        } else if (emptyElement instanceof Conc) {
            return generateConc((Conc) emptyElement, size);
        } else if (emptyElement instanceof Alt) {
            return generateAlt((Alt) emptyElement, size);
        } else if (emptyElement instanceof Star) {
            return generateStar((Star) emptyElement, size);
        } else if (emptyElement instanceof Opt) {
            return generateOpt((Opt) emptyElement, size);
        } else if (emptyElement instanceof Not) {
            return generateNot((Not) emptyElement, size);
        } else if (emptyElement instanceof Plus) {
            return generatePlus((Plus) emptyElement, size);
        } else if (emptyElement instanceof Nonterminal) {
            return generateNonterminal((Nonterminal) emptyElement, size);
        } else if (emptyElement instanceof Literal) {
            return generateLiteral((Literal) emptyElement);
        } else if (emptyElement instanceof DottedRange) {
            return generateDottedRange((DottedRange) emptyElement);
        } else if (emptyElement instanceof CharClass) {
            return generateCharacterClass((CharClass) emptyElement, size);
        } else if (emptyElement instanceof NegClass) {
            return generateNegatedClass((NegClass) emptyElement, size);
        } else if (emptyElement instanceof Wildcard) {
            return generateWildcard();
        } else if (emptyElement instanceof EOF) {
            return generateEof();
        }

        throw new IllegalStateException("Unknown emptyElement: " + emptyElement);
    }

    private Optional<Term> generateEmpty(Empty element, int size) {
        return of(Text.EMPTY);
    }

    private Optional<Term> generateLiteral(Literal element) {
        return of(leaf(element.getText()));
    }

    private Optional<Term> generateConc(Conc element, int size) {
        int remainingSize = size - 1;
        int headSize = element.divideSize(remainingSize);
        int tailSize = remainingSize - headSize;

        Optional<Term> headOpt = forElement(element.getFirst(), headSize);

        if (headOpt.isPresent()) {
            Optional<Term> tailOpt = forElement(element.getSecond(), tailSize);

            if (tailOpt.isPresent()) {
                return of(node(element, headOpt.get(), tailOpt.get()));
            }
        }

        return empty();
    }

    private Optional<Term> generateAlt(Alt element, int size) {
        if (random.nextInt(element.size()) == 0) {
            Optional<Term> firstResultOpt = forElement(element.getFirst(), size);

            if (!firstResultOpt.isPresent()) {
                return forElement(element.getSecond(), size);
            } else {
                return firstResultOpt;
            }
        } else {
            Optional<Term> secondResultOpt = forElement(element.getSecond(), size);

            if (!secondResultOpt.isPresent()) {
                return forElement(element.getFirst(), size);
            } else {
                return secondResultOpt;
            }
        }
    }

    private Optional<Term> generateOpt(Opt element, int size) {
        if (random.nextInt(2) == 0) {
            return of(Text.EMPTY);
        } else {
            return forElement(element.getElement(), size);
        }
    }

    private Optional<Term> generateNot(Not element, int size) {
        return of(leaf("x")); // TODO
    }

    private Optional<Term> generateStar(Star element, int size) {
        if (random.nextInt(2) == 0) {
            return of(list(element));
        } else {
            int headSize = size / 4;
            int tailSize = size - headSize;

            Optional<Term> tailOpt = forElement(element, tailSize);

            return tailOpt.map(tail -> prepend(element, element.getElement(), tail, headSize));
        }
    }

    // TODO: This is wrong; it may return empty lists!
    private Optional<Term> generatePlus(Plus element, int size) {
        int headSize = size / 4;
        int tailSize = size - headSize;

        Optional<Term> tailOpt = generateStar(new Star(element.getElement()), tailSize);

        return tailOpt.map(tail -> prepend(element, element.getElement(), tail, headSize));
    }

    private Term prepend(Element operation, Element element, Term tail, int size) {
        Optional<Term> headOpt = forElement(element, size);

        if (headOpt.isPresent()) {
            TermList termList = (TermList) tail;

            return list(operation, headOpt.get(), termList);
        } else {
            return tail;
        }
    }

    private Optional<Term> generateDottedRange(DottedRange element) {
        int size = element.size();
        int rand = random.nextInt(size);

        return of(leaf(String.valueOf(element.get(rand))));
    }

    private Optional<Term> generateCharacterClass(CharClass element, int size) {
        return generateRanges(element.getRanges(), size);
    }

    private Optional<Term> generateNegatedClass(NegClass negatedClass, int size) {
        return of(leaf("x")); // TODO
    }

    private Optional<Term> generateWildcard() {
        return of(leaf("a")); // TODO: A single token (in parser rule) or a single character (in lexer rule)
    }

    private Optional<Term> generateEof() {
        return of(Text.EMPTY);
    }

    private Optional<Term> generateRanges(Ranges ranges, int size) {
        if (ranges instanceof RangeConc) {
            return generateRangesConc((RangeConc) ranges);
        } else if (ranges instanceof Range) {
            return generateRange((Range) ranges, size);
        }

        throw new IllegalStateException("Unknown ranges: " + ranges);
    }

    private Optional<Term> generateRangesConc(RangeConc ranges) {
        int size = ranges.size();
        int rand = random.nextInt(size);

        return of(leaf(String.valueOf(ranges.get(rand))));
    }

    private Optional<Term> generateRange(Range range, int size) {
        if (range instanceof CharRange) {
            return generateCharRange((CharRange) range);
        } else if (range instanceof Character) {
            return generateChar((Character) range, size);
        }

        throw new IllegalStateException("Unknown range: " + range);
    }

    private Optional<Term> generateCharRange(CharRange range) {
        int size = range.size();
        int rand = random.nextInt(size);

        return of(leaf(String.valueOf(range.get(rand))));
    }

    private Optional<Term> generateChar(Character c, int size) {
        if (c instanceof Single) {
            return generateSingle((Single) c, size);
        }

        throw new IllegalStateException("Unknown char: " + c);
    }

    private Optional<Term> generateSingle(Single s, int size) {
        return of(leaf(s.getText()));
    }

    private Term join(Term term) {
        return leaf(term.toString(false));
    }

    private TermList list(Element element, Term term, TermList tail) {
        return new TermList(element, term, tail);
    }

    private TermList list(Star element) {
        return new TermList(element);
    }

    private Term node(EmptyElement elementOpt, Term... children) {
        return new Appl(elementOpt, children);
    }

    private Term leaf(String text) {
        return new Text(text);
    }
}
