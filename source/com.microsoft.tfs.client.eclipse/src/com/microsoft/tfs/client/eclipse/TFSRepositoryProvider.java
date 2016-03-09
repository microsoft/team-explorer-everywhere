// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse;

import java.text.MessageFormat;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.resources.IFileModificationValidator;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceRuleFactory;
import org.eclipse.core.resources.team.IMoveDeleteHook;
import org.eclipse.core.resources.team.ResourceRuleFactory;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.team.internal.core.PessimisticResourceRuleFactory;

import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.eclipse.filemodification.TFSFileModificationValidator;
import com.microsoft.tfs.client.eclipse.filemodification.TFSFileModificationValidatorLegacy;
import com.microsoft.tfs.client.eclipse.project.ProjectRepositoryStatus;

public class TFSRepositoryProvider extends RepositoryProvider {
    public static final String PROVIDER_ID = "com.microsoft.tfs.client.eclipse.TFSRepositoryProvider"; //$NON-NLS-1$

    /**
     * Extends {@link ResourceRuleFactory} to provide a public constructor.
     * Returned by {@link TFSRepositoryProvider#getRuleFactory()} to provide the
     * correct workspace job lock (rule) scope in later versions of Eclipse.
     *
     * @threadsafety unknown
     */
    static class TFSRepositoryProviderRuleFactory extends ResourceRuleFactory {
        public TFSRepositoryProviderRuleFactory() {
            super();
        }
    }

    private static final Log log = LogFactory.getLog(TFSRepositoryProvider.class);

    private final TFSMoveDeleteHook moveDeleteHook = new TFSMoveDeleteHook(this);

    /**
     * Eclipse provides two systems for file modification validation. Legacy
     * (pre-3.3 style) and the second style (3.3+). We supply both, but proxy to
     * a standard validator class. Note that we are explicit about class name
     * instead of importing the class to prevent classloading errors on pre-3.3
     * Eclipse.
     */
    private final TFSFileModificationValidator modificationValidator = new TFSFileModificationValidator(this);
    private Object modificationValidator2;
    private IFileModificationValidator modificationValidatorLegacy;
    private final Object modificationValidatorLock = new Object();

    @Override
    public String getID() {
        return PROVIDER_ID;
    }

    /* Project configuration with the repository provider */

    @Override
    public void configureProject() throws CoreException {
        final IProject project = getProject();

        log.debug(MessageFormat.format("Configured as repository provider for project {0}", project.getName())); //$NON-NLS-1$
    }

    @Override
    public void deconfigure() throws CoreException {
        final IProject project = getProject();

        log.debug(MessageFormat.format("Deconfigured as repository provided for project {0}", project.getName())); //$NON-NLS-1$
    }

    /* Connection at project startup */

    @Override
    public void setProject(final IProject project) {
        log.debug(MessageFormat.format("Opening repository for project {0}", project.getName())); //$NON-NLS-1$

        /**
         * The runtime and UI may not be fully formed here. We need to connect
         * as a Job, this will run as soon as the JobManager is fully formed. We
         * cannot block here, as Job#join() will fail immediately.
         */
        final Job connectJob =
            new Job(MessageFormat.format(
                Messages.getString("TFSRepositoryProvider.ConnectingProjectFormat"), //$NON-NLS-1$
                project.getName())) {
                @Override
                protected IStatus run(final IProgressMonitor monitor) {
                    TFSEclipseClientPlugin.getDefault().getProjectManager().connectIfNecessary(project);

                    return Status.OK_STATUS;
                }
            };

        connectJob.setSystem(true);
        connectJob.schedule();

        super.setProject(project);
    }

    public TFSRepository getRepository() {
        final IProject project = getProject();

        if (project == null) {
            return null;
        }

        return TFSEclipseClientPlugin.getDefault().getProjectManager().getRepository(project);
    }

    public ProjectRepositoryStatus getRepositoryStatus() {
        final IProject project = getProject();

        if (project == null) {
            return null;
        }

        return TFSEclipseClientPlugin.getDefault().getProjectManager().getProjectStatus(project);
    }

    /* Linked resources */

    @Override
    public boolean canHandleLinkedResources() {
        /* Deprecated in Eclipse 3.2, here for back compatibility. */
        return canHandleLinkedResourceURI();
    }

    @Override
    public boolean canHandleLinkedResourceURI() {
        /* Eclipse 3.2+ */
        return true;
    }

    /**
     * This is the new-style (Eclipse 3.2+) file modification validator API.
     *
     * {@inheritDoc}
     */
    @Override
    public org.eclipse.core.resources.team.FileModificationValidator getFileModificationValidator2() {
        synchronized (modificationValidatorLock) {
            /*
             * Must specify fully-qualified class name here, otherwise lesser
             * versions of Eclipse will choke on compiling this class due to the
             * import.
             */
            if (modificationValidator2 == null) {
                modificationValidator2 =
                    new com.microsoft.tfs.client.eclipse.filemodification.TFSFileModificationValidator2(
                        modificationValidator);
            }

            return (org.eclipse.core.resources.team.FileModificationValidator) modificationValidator2;
        }
    }

    /**
     * This is the old-style (Eclipse 3.1-) file modification validator API.
     *
     * {@inheritDoc}
     */
    @Override
    public IFileModificationValidator getFileModificationValidator() {
        synchronized (modificationValidatorLock) {
            if (modificationValidatorLegacy == null) {
                modificationValidatorLegacy = new TFSFileModificationValidatorLegacy(modificationValidator);
            }

            return modificationValidatorLegacy;
        }
    }

    /**
     * @return the {@link TFSFileModificationValidator} that implements both
     *         {@link IFileModificationValidator} and
     *         {@link org.eclipse.core.resources.team.FileModificationValidator}
     *         .
     */
    public TFSFileModificationValidator getTFSFileModificationValidator() {
        return modificationValidator;
    }

    @Override
    public IMoveDeleteHook getMoveDeleteHook() {
        return moveDeleteHook;
    }

    /**
     * This must be overridden to prevent an error dialog appearing in newer
     * versions of Eclipse when a TFS-bound project is opened via the "Open"
     * context menu from package explorer.
     *
     * Background:
     *
     * Eclipse uses a mutual exclusion structures named "rules" to prevent
     * multiple workspace jobs from running at once and messing up each other's
     * resources. Rules are created with different scopes (whole workspace, just
     * one project, just one file, etc.) depending on the type of job being run
     * inside the lock. This method returns an {@link IResourceRuleFacctory},
     * which Eclipse calls the right method on to get the correctly scoped lock.
     *
     * An important thing to know about rules: once one rules is in effect, a
     * job can begin more rules, but only if the new rule's scope is equal to or
     * smaller than the old rule.
     *
     * Around Eclipse 3.0, opening a project would cause Eclipse to call
     * {@link IResourceRuleFactory#modifyRule(org.eclipse.core.resources.IResource)}
     * and get back a rule scoped to just the project that was being opened.
     * This sounds like the right scope for opening a project, but it turns out
     * it's common to change project the project description in the job inside
     * the rule (like for CVS Import), but changing the description requires a
     * whole workspace rule! This was reported as bug 127562
     * (https://bugs.eclipse.org/bugs/show_bug.cgi?id=127562) and fixed in CVS
     * revision 1.10 of ResourceRuleFactory.java, which was released in Eclipse
     * 3.2 M5. The fix special-cased the project case and returned a workspace
     * root rule instead of the old project scope rule.
     *
     * But a few years later bug 128709 was logged
     * (https://bugs.eclipse.org/bugs/show_bug.cgi?id=128709) and this special
     * case for the project was removed at CVS revision 1.12. This fix shipped
     * in Eclipse 3.4 M5.
     *
     * So after Eclipse 3.4, project scope is now used for the rule when Eclipse
     * opens a project. Remember how when a rule is open, new rules opened by
     * the same job must be of equal or narrower scope. Well, after Eclipse
     * creates the project scope rule for opening a project, it loads our
     * {@link TFSRepositoryProvider} and calls {@link #getRuleFactory()} to get
     * a second rule (to connect the project to TFS). The default implementation
     * of {@link #getRuleFactory()} is {@link PessimisticResourceRuleFactory}
     * and returns a workspace root rule for the connect action! A workspace
     * root rule is broader in scope than a project rule, and this causes an
     * error!
     *
     * So we override the method to return the same rule factory Eclipse uses to
     * open the project in the first place, which will return a project scoped
     * rule in the second case.
     *
     * @see https://bugs.eclipse.org/bugs/show_bug.cgi?id=230533#c4
     * @see https://bugs.eclipse.org/bugs/show_bug.cgi?id=128709
     * @see https://bugs.eclipse.org/bugs/show_bug.cgi?id=129045
     */
    @Override
    public IResourceRuleFactory getRuleFactory() {
        return new TFSRepositoryProviderRuleFactory();
    }
}