package org.metaborg.spg.sentence.eclipse.job;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import org.antlr.v4.runtime.*;
import org.antlr.v4.tool.Rule;
import org.apache.commons.vfs2.FileObject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.metaborg.core.MetaborgException;
import org.metaborg.core.language.ILanguageImpl;
import org.metaborg.core.project.IProject;
import org.metaborg.core.syntax.ParseException;
import org.metaborg.spg.sentence.antlr.generator.Generator;
import org.metaborg.spg.sentence.antlr.generator.GeneratorFactory;
import org.metaborg.spg.sentence.antlr.grammar.Grammar;
import org.metaborg.spg.sentence.antlr.grammar.GrammarFactory;
import org.metaborg.spg.sentence.antlr.shrinker.Shrinker;
import org.metaborg.spg.sentence.antlr.shrinker.ShrinkerFactory;
import org.metaborg.spg.sentence.antlr.term.Term;
import org.metaborg.spoofax.core.unit.ISpoofaxInputUnit;

import java.io.File;
import java.io.IOException;
import java.util.Optional;
import java.util.stream.Stream;

import static org.metaborg.spg.sentence.antlr.functional.Utils.uncheck;

public class DifferenceJob extends SentenceJob {
    private final GrammarFactory grammarFactory;
    private final GeneratorFactory generatorFactory;
    private final ShrinkerFactory shrinkerFactory;

    private final IProject project;
    private final ILanguageImpl language;

    @Inject
    public DifferenceJob(
            GrammarFactory grammarFactory,
            GeneratorFactory generatorFactory,
            ShrinkerFactory shrinkerFactory,
            @Assisted IProject project,
            @Assisted ILanguageImpl language) {
        super("Difference test");

        this.grammarFactory = grammarFactory;
        this.generatorFactory = generatorFactory;
        this.shrinkerFactory = shrinkerFactory;
        this.project = project;
        this.language = language;
    }

    @Override
    protected IStatus run(IProgressMonitor progressMonitor) {
        SubMonitor subMonitor = SubMonitor.convert(progressMonitor, 100);

        // TODO: Generate terms from SDF, parse with ANTLR.

        // Generate terms from ANTLR, parse with SDF.
        try {
            generateAntlr(subMonitor);
        } catch (IOException | MetaborgException e) {
            e.printStackTrace();
        }

        return Status.OK_STATUS;
    }

    private void generateAntlr(SubMonitor subMonitor) throws IOException, MetaborgException {
        // TODO: Get all these things dynamically.
        File antlrLanguageFile = new File("/Users/martijn/Projects/metaborg-antlr/org.metaborg.lang.antlr/target/org.metaborg.lang.antlr-0.1.0-SNAPSHOT.spoofax-language");
        File spoofaxLanguageFile = new File("/Users/martijn/Projects/java-front/lang.java/target/lang.java-1.0.0-SNAPSHOT.spoofax-language");
        File antlrGrammarFile = new File("/Users/martijn/Projects/metaborg-antlr/org.metaborg.lang.antlr.examples/java.g");
        String antlrStartSymbol = "compilationUnit";
        int maxSize = 10000000;
        ILanguageImpl antlrLanguageImpl = getLanguageImpl(antlrLanguageFile);
        ILanguageImpl spoofaxLanguageImpl = getLanguageImpl(spoofaxLanguageFile);
        org.antlr.v4.tool.Grammar antlrGrammar = org.antlr.v4.tool.Grammar.load(antlrGrammarFile.getAbsolutePath());

        // --

        Grammar grammar = grammarFactory.create(antlrGrammarFile, antlrLanguageImpl);
        Generator generator = generatorFactory.create(grammar);
        Shrinker shrinker = shrinkerFactory.create(generator, grammar);

        for (int i = 0; i < 1000; i++) {
        		subMonitor.split(1);
            Optional<Term> treeOpt = generator.generate(antlrStartSymbol, maxSize);

            if (treeOpt.isPresent()) {
                Term term = treeOpt.get();
                String sentence = term.toString(true);

                stream.println(sentence);

                boolean spoofaxResult = canSpoofaxParse(spoofaxLanguageImpl, sentence);

                if (!spoofaxResult) {
                    boolean antlrResult = canAntlrParse(antlrGrammar, antlrStartSymbol, sentence);

                    if (antlrResult) {
                        stream.println("Legal ANTLRv4 illegal SDF3 sentence:");
                        stream.println(sentence);

                        while (true) {
                            Stream<Term> shrunkTrees = shrinker.shrink(term);

                            Optional<Term> anyShrunkTree = shrunkTrees
                                    .filter(uncheck(shrunkTree -> !canSpoofaxParse(spoofaxLanguageImpl, shrunkTree.toString())))
                                    .filter(uncheck(shrunkTree -> canAntlrParse(antlrGrammar, antlrStartSymbol, shrunkTree.toString())))
                                    .findFirst();

                            if (anyShrunkTree.isPresent()) {
                                stream.println("Shrunk sentence to smaller sentence:");
                                stream.println(anyShrunkTree.get().toString());

                                term = anyShrunkTree.get();
                            } else {
                                break;
                            }
                        }
                    } else {
                        stream.println("Unparsable sentence: " + sentence);
                    }
                }
            }
        }
    }

    private ILanguageImpl getLanguageImpl(File antlrLanguageFile) throws MetaborgException {
        FileObject languageLocation = spoofax.resourceService.resolve(antlrLanguageFile);

        return spoofax.languageDiscoveryService.languageFromArchive(languageLocation);
    }

    private boolean canAntlrParse(org.antlr.v4.tool.Grammar grammar, String antlrStartSymbol, String text) throws IOException {
        CharStream charStream = CharStreams.fromString(text);
        LexerInterpreter lexer = grammar.createLexerInterpreter(charStream);
        lexer.removeErrorListener(ConsoleErrorListener.INSTANCE);

        CommonTokenStream tokens = new CommonTokenStream(lexer);
        ParserInterpreter parser = grammar.createParserInterpreter(tokens);

        Rule startRule = grammar.getRule(antlrStartSymbol);
        parser.parse(startRule.index);

        return parser.getNumberOfSyntaxErrors() == 0;
    }

    private boolean canSpoofaxParse(ILanguageImpl languageImpl, String text) throws ParseException {
        ISpoofaxInputUnit inputUnit = spoofax.unitService.inputUnit(text, languageImpl, null);

        return spoofax.syntaxService.parse(inputUnit).success();
    }
}
