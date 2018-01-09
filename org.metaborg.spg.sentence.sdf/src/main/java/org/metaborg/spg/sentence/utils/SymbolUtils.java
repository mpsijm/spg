package org.metaborg.spg.sentence.utils;

import org.metaborg.sdf2table.grammar.CharacterClassConc;
import org.metaborg.sdf2table.grammar.CharacterClassNumeric;
import org.metaborg.sdf2table.grammar.CharacterClassRange;
import org.metaborg.sdf2table.grammar.Symbol;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Stream.empty;
import static java.util.stream.Stream.of;
import static org.metaborg.spg.sentence.generator.Generator.MAXIMUM_PRINTABLE;
import static org.metaborg.spg.sentence.generator.Generator.MINIMUM_PRINTABLE;
import static org.metaborg.spg.sentence.shared.utils.StreamUtils.cons;

public class SymbolUtils {
    public static Symbol toPrintable(CharacterClassConc characterClassConc) {
        List<Symbol> symbols = characterClassToList(characterClassConc).collect(Collectors.toList());
        List<Symbol> printableSymbols = toPrintable(symbols).collect(Collectors.toList());

        return listToCharacterClass(printableSymbols);
    }

    public static Stream<Symbol> toPrintable(List<Symbol> symbols) {
        if (symbols.size() == 0) {
            return empty();
        } else if (symbols.size() == 1) {
            if (symbols.get(0) instanceof CharacterClassNumeric) {
                return of(symbols.get(0));
            }

            CharacterClassRange characterClassRange = (CharacterClassRange) symbols.get(0);

            return of(new CharacterClassRange(
                    new CharacterClassNumeric(Math.max(MINIMUM_PRINTABLE, characterClassRange.minimum())),
                    new CharacterClassNumeric(Math.min(MAXIMUM_PRINTABLE, characterClassRange.maximum()))
            ));
        } else {
            Symbol head = symbols.get(0);
            int headMinimum = getMinimum(head);
            int headMaximum = getMaximum(head);

            Stream<Symbol> tail = toPrintable(tail(symbols));

            if (headMinimum < MINIMUM_PRINTABLE) {
                if (headMaximum < MINIMUM_PRINTABLE) {
                    return tail;
                } else {
                    Symbol nhead = range(MINIMUM_PRINTABLE, headMaximum);

                    return cons(nhead, tail);
                }
            } else {
                if (headMaximum > MAXIMUM_PRINTABLE) {
                    return empty();
                } else {
                    return cons(head, tail);
                }
            }
        }
    }

    private static int getMinimum(Symbol symbol) {
        if (symbol instanceof CharacterClassRange) {
            return ((CharacterClassRange) symbol).minimum();
        } else if (symbol instanceof CharacterClassNumeric) {
            return ((CharacterClassNumeric) symbol).minimum();
        }

        throw new IllegalStateException();
    }

    private static int getMaximum(Symbol symbol) {
        if (symbol instanceof CharacterClassRange) {
            return ((CharacterClassRange) symbol).maximum();
        } else if (symbol instanceof CharacterClassNumeric) {
            return ((CharacterClassNumeric) symbol).maximum();
        }

        throw new IllegalStateException();
    }

    private static Stream<Symbol> characterClassToList(Symbol characterClass) {
        if (characterClass instanceof CharacterClassConc) {
            CharacterClassConc characterClassConc = (CharacterClassConc) characterClass;
            Symbol first = characterClassConc.first();
            Symbol second = characterClassConc.second();

            return cons(first, characterClassToList(second));
        } else if (characterClass instanceof CharacterClassRange) {
            return of(characterClass);
        } else if (characterClass instanceof CharacterClassNumeric) {
            return of(characterClass);
        }

        throw new IllegalStateException("Unsupported character class symbol: " + characterClass);
    }

    private static Symbol listToCharacterClass(List<Symbol> symbols) {
        if (symbols.size() == 1) {
            return symbols.get(0);
        } else {
            return new CharacterClassConc(symbols.get(0), listToCharacterClass(tail(symbols)));
        }
    }

    public static char get(Symbol symbol, int index) {
        if (symbol instanceof CharacterClassConc) {
            CharacterClassConc characterClassConc = (CharacterClassConc) symbol;
            int characterClassConcSize = size(characterClassConc.first());

            if (index < characterClassConcSize) {
                return get(characterClassConc.first(), index);
            } else {
                return get(characterClassConc.second(), index - characterClassConcSize);
            }
        } else if (symbol instanceof CharacterClassRange) {
            CharacterClassRange characterClassRange = (CharacterClassRange) symbol;

            return (char) (number(characterClassRange.start()) + index);
        } else if (symbol instanceof CharacterClassNumeric) {
            CharacterClassNumeric characterClassNumeric = (CharacterClassNumeric) symbol;

            return (char) number(characterClassNumeric);
        }

        throw new IllegalStateException("Unknown symbol: " + symbol);
    }

    public static int size(Symbol symbol) {
        if (symbol instanceof CharacterClassConc) {
            CharacterClassConc characterClassConc = (CharacterClassConc) symbol;

            return size(characterClassConc.first()) + size(characterClassConc.second());
        } else if (symbol instanceof CharacterClassRange) {
            CharacterClassRange characterClassRange = (CharacterClassRange) symbol;

            return number(characterClassRange.end()) - number(characterClassRange.start()) + 1;
        } else if (symbol instanceof CharacterClassNumeric) {
            return 1;
        }

        throw new IllegalStateException("Unexpected symbol: " + symbol);
    }

    public static int number(Symbol symbol) {
        if (symbol instanceof CharacterClassNumeric) {
            CharacterClassNumeric characterClassNumeric = (CharacterClassNumeric) symbol;

            return characterClassNumeric.getCharacter();
        }

        throw new IllegalStateException("Unexpected symbol: " + symbol);
    }

    private static Symbol range(int min, int max) {
        return new CharacterClassRange(
                new CharacterClassNumeric(min),
                new CharacterClassNumeric(max)
        );
    }

    private static <T> List<T> tail(List<T> list) {
        return list.subList(1, list.size());
    }
}