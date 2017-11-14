package org.metaborg.spg.sentence.generator;

import com.google.inject.Inject;
import org.metaborg.core.language.ILanguageImpl;
import org.metaborg.core.project.IProject;
import org.metaborg.core.resource.IResourceService;
import org.metaborg.sdf2table.grammar.NormGrammar;
import org.metaborg.sdf2table.io.GrammarReader;
import org.metaborg.spg.sentence.printer.Printer;
import org.metaborg.spoofax.core.build.SpoofaxCommonPaths;
import org.metaborg.spoofax.core.terms.ITermFactoryService;
import org.spoofax.interpreter.terms.ITermFactory;

import java.io.File;
import java.util.Collections;
import java.util.List;

public class GeneratorFactory {
    private final IResourceService resourceService;
    private final ITermFactoryService termFactoryService;

    @Inject
    public GeneratorFactory(IResourceService resourceService, ITermFactoryService termFactoryService) {
        this.resourceService = resourceService;
        this.termFactoryService = termFactoryService;
    }

    public Generator create(ILanguageImpl language, IProject project, Printer printer) throws Exception {
        ITermFactory termFactory = termFactoryService.getGeneric();
        GrammarReader grammarReader = new GrammarReader(termFactory);
        SpoofaxCommonPaths spoofaxCommonPaths = new SpoofaxCommonPaths(project.location());

        File syntaxMainFile = getSyntaxMainFile(spoofaxCommonPaths, language);
        List<String> syntaxPath = getSyntaxPath(spoofaxCommonPaths);
        NormGrammar grammar = grammarReader.readGrammar(syntaxMainFile, syntaxPath);

        return new Generator(printer, termFactory, grammar);
    }

    protected File getSyntaxMainFile(SpoofaxCommonPaths spoofaxCommonPaths, ILanguageImpl language) {
        return resourceService.localFile(spoofaxCommonPaths.syntaxSrcGenMainNormFile(language.belongsTo().name()));
    }

    protected File getSyntaxDirectory(SpoofaxCommonPaths spoofaxCommonPaths) {
        return resourceService.localFile(spoofaxCommonPaths.syntaxSrcGenDir());
    }

    protected List<String> getSyntaxPath(SpoofaxCommonPaths spoofaxCommonPaths) {
        File syntaxDirectory = getSyntaxDirectory(spoofaxCommonPaths);

        return Collections.singletonList(syntaxDirectory.getAbsolutePath());
    }
}