// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse.ui.tasks.vc;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.swt.widgets.Shell;

import com.microsoft.tfs.client.common.framework.command.JobOptions;
import com.microsoft.tfs.client.common.ui.framework.command.UICommandExecutorFactory;
import com.microsoft.tfs.client.common.ui.tasks.BaseTask;
import com.microsoft.tfs.client.eclipse.tpignore.TPIgnoreCache;
import com.microsoft.tfs.client.eclipse.ui.commands.vc.AppendTPIgnorePatternsCommand;
import com.microsoft.tfs.util.Check;

public class TPIgnoreTask extends BaseTask {
    private final IResource[] resources;

    /**
     * Updates the .tpignore file with a pattern for each given
     * {@link IResource}. The file is not checked for existing matches (may
     * create duplicates) and is created if it does not exist. Resources must
     * all share the same {@link IProject}.
     *
     * @param shell
     *        a shell to use for raising warnings and errors (must not be
     *        <code>null</code>)
     * @param resources
     *        the resources to create entries for (must not be <code>null</code>
     *        or empty, must all share the same {@link IProject})
     */
    public TPIgnoreTask(final Shell shell, final IResource[] resources) {
        super(shell);

        Check.notNullOrEmpty(resources, "resources"); //$NON-NLS-1$

        this.resources = resources;

        /*
         * Using a job command executor is important for a specific reason: the
         * ignore command we run changes workspace resources, which can cause
         * resource change listeners (from our plug-in or others) to do
         * complicated things which might raise an error dialog. If we use the
         * default UI command executor instead of a job executor, this task
         * might raise a conflicting progress dialog while that resource change
         * listener is showing an error dialog, causing a live lock of the UI on
         * some platforms (user can't dismiss either dialog).
         */
        setCommandExecutor(
            UICommandExecutorFactory.newUIJobCommandExecutor(getShell(), new JobOptions().setUser(true)));
    }

    @Override
    public IStatus run() {
        /*
         * Since this task's resources must all be from one project, simply get
         * the .tpignore file for the first resource.
         */
        final IFile ignoreFile = TPIgnoreCache.getIgnoreFile(resources[0]);

        /*
         * Because the semantics of this task are so simple (always adds lines),
         * the command does all the work.
         */
        return getCommandExecutor().execute(new AppendTPIgnorePatternsCommand(ignoreFile, resources));
    }
}
