package org.metaborg.spg.sentence.eclipse.job;

import org.antlr.v4.runtime.*;
import org.antlr.v4.tool.Grammar;
import org.antlr.v4.tool.Rule;
import org.metaborg.core.language.ILanguageImpl;
import org.metaborg.core.syntax.ParseException;
import org.metaborg.spoofax.core.syntax.ISpoofaxSyntaxService;
import org.metaborg.spoofax.core.unit.ISpoofaxInputUnit;
import org.metaborg.spoofax.core.unit.ISpoofaxParseUnit;
import org.metaborg.spoofax.core.unit.ISpoofaxUnitService;

import java.io.IOException;

public abstract class DifferenceJob extends SentenceJob {
    private final ISpoofaxUnitService unitService;
    private final ISpoofaxSyntaxService syntaxService;

    public DifferenceJob(ISpoofaxUnitService unitService, ISpoofaxSyntaxService syntaxService, String name) {
        super(name);

        this.unitService = unitService;
        this.syntaxService = syntaxService;
    }

    protected boolean canParseAntlr(Grammar grammar, String antlrStartSymbol, String text) throws IOException {
        return parseAntlr(grammar, antlrStartSymbol, text).getNumberOfSyntaxErrors() == 0;
    }

    protected Parser parseAntlr(Grammar grammar, String antlrStartSymbol, String text) throws IOException {
        CharStream charStream = CharStreams.fromString(text);
        LexerInterpreter lexer = grammar.createLexerInterpreter(charStream);
        lexer.removeErrorListener(ConsoleErrorListener.INSTANCE);

        CommonTokenStream tokens = new CommonTokenStream(lexer);
        ParserInterpreter parser = grammar.createParserInterpreter(tokens);

        Rule startRule = grammar.getRule(antlrStartSymbol);
        parser.parse(startRule.index);

        return parser;
    }

    protected boolean canParseSpoofax(ILanguageImpl languageImpl, String text) throws ParseException {
        return parseSpoofax(languageImpl, text).success();
    }

    protected ISpoofaxParseUnit parseSpoofax(ILanguageImpl languageImpl, String text) throws ParseException {
        ISpoofaxInputUnit inputUnit = unitService.inputUnit(text, languageImpl, null);

        return syntaxService.parse(inputUnit);
    }
}
