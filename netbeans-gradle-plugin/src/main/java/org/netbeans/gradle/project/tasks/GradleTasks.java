package org.netbeans.gradle.project.tasks;

import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jtrim.cancel.CancellationToken;
import org.jtrim.utils.ExceptionHelper;
import org.netbeans.gradle.project.NbGradleProject;
import org.netbeans.gradle.project.NbStrings;
import org.netbeans.gradle.project.api.task.CommandCompleteListener;
import org.netbeans.gradle.project.api.task.GradleActionProviderContext;

public final class GradleTasks {
    private static final Logger LOGGER = Logger.getLogger(GradleTasks.class.getName());

    private static GradleCommandSpecFactory toSpecFactory(final GradleTaskDefFactory taskDefFactory) {
        ExceptionHelper.checkNotNullArgument(taskDefFactory, "taskDefFactory");

        return new GradleCommandSpecFactory() {
            @Override
            public String getDisplayName() {
                return taskDefFactory.getDisplayName();
            }

            @Override
            public GradleCommandSpec tryCreateCommandSpec(CancellationToken cancelToken) throws Exception {
                GradleTaskDef result = taskDefFactory.tryCreateTaskDef(cancelToken);
                return result != null
                        ? new GradleCommandSpec(result, null)
                        : null;
            }
        };
    }

    public static Runnable createAsyncGradleTask(
            NbGradleProject project,
            GradleTaskDefFactory taskDefFactory,
            Set<GradleActionProviderContext> actionContexts,
            CommandCompleteListener listener) {
        return createAsyncGradleTaskFromSpec(project, toSpecFactory(taskDefFactory), actionContexts, listener);
    }

    public static Runnable createAsyncGradleTaskFromSpec(
            NbGradleProject project,
            GradleCommandSpecFactory taskDefFactory,
            Set<GradleActionProviderContext> actionContexts,
            CommandCompleteListener listener) {
        return new AsyncGradleTask(project, taskDefFactory, actionContexts, listener);
    }

    public static CommandCompleteListener projectTaskCompleteListener(final NbGradleProject project) {
        ExceptionHelper.checkNotNullArgument(project, "project");

        return new CommandCompleteListener() {
            @Override
            public void onComplete(Throwable error) {
                if (error != null) {
                    LOGGER.log(error instanceof Exception ? Level.INFO : Level.SEVERE,
                            "Gradle build failure.",
                            error);

                    String buildFailureMessage = NbStrings.getGradleTaskFailure();
                    project.displayError(buildFailureMessage, error);
                }
            }
        };
    }

    private GradleTasks() {
        throw new AssertionError();
    }
}
