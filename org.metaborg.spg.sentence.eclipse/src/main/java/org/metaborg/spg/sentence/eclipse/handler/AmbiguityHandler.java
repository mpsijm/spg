package org.metaborg.spg.sentence.eclipse.handler;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.Window;
import org.metaborg.core.language.ILanguageImpl;
import org.metaborg.core.project.IProject;
import org.metaborg.spg.sentence.ambiguity.AmbiguityTesterConfig;
import org.metaborg.spg.sentence.eclipse.dialog.AmbiguityDialog;
import org.metaborg.spg.sentence.eclipse.exception.LanguageNotFoundException;
import org.metaborg.spg.sentence.eclipse.exception.ProjectNotFoundException;
import org.metaborg.spg.sentence.eclipse.job.JobFactory;

public class AmbiguityHandler extends SentenceHandler {
    public Object execute(ExecutionEvent executionEvent) throws ExecutionException {
        try {
            IProject project = getProject(executionEvent);
            ILanguageImpl languageImpl = getLanguageImpl(project);

            AmbiguityDialog ambiguityDialog = new AmbiguityDialog(getShell(executionEvent));
            ambiguityDialog.create();

            if (ambiguityDialog.open() == Window.OK) {
                JobFactory jobFactory = injector.getInstance(JobFactory.class);

                Job job = jobFactory.createAmbiguityJob(getConfig(ambiguityDialog), project, languageImpl);
                job.setPriority(Job.SHORT);
                job.setUser(true);
                job.schedule();
            }
        } catch (ProjectNotFoundException e) {
            MessageDialog.openError(null, "Project not found", "Cannot find a Spoofax project for generation. Did you select it in the Package Explorer?");
        } catch (LanguageNotFoundException e) {
            MessageDialog.openError(null, "Language not found", "Cannot find a Spoofax language for generation. Did you build the project?");
        }

        return null;
    }

    private AmbiguityTesterConfig getConfig(AmbiguityDialog generateDialog) {
        int maxNumberOfTerms = generateDialog.getMaxNumberOfTerms();
        int maxTermSize = generateDialog.getMaxTermSize();

        return new AmbiguityTesterConfig(maxNumberOfTerms, maxTermSize);
    }
}
