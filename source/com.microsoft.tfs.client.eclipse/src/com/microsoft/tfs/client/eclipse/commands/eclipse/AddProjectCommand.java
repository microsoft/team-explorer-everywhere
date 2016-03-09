// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse.commands.eclipse;

import java.text.MessageFormat;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import com.microsoft.tfs.client.common.commands.TFSCommand;
import com.microsoft.tfs.client.common.commands.vc.AddCommand;
import com.microsoft.tfs.client.common.commands.vc.UndoCommand;
import com.microsoft.tfs.client.common.framework.command.MultiCommandHelper;
import com.microsoft.tfs.client.common.framework.command.UndoableCommand;
import com.microsoft.tfs.client.common.framework.resources.command.ResourceChangingCommand;
import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.eclipse.Messages;
import com.microsoft.tfs.client.eclipse.TFSEclipseClientPlugin;
import com.microsoft.tfs.client.eclipse.tpignore.TPIgnoreDocument;
import com.microsoft.tfs.core.clients.versioncontrol.GetOptions;
import com.microsoft.tfs.core.clients.versioncontrol.PendChangesOptions;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.LockLevel;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.RecursionType;
import com.microsoft.tfs.core.clients.versioncontrol.specs.ItemSpec;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.LocaleUtil;

public final class AddProjectCommand extends TFSCommand implements UndoableCommand {
    private final TFSRepository repository;
    private final IProject project;

    private String[] addPaths;

    public AddProjectCommand(final TFSRepository repository, final IProject project) {
        this.repository = repository;
        this.project = project;
    }

    @Override
    public String getName() {
        return Messages.getString("AddProjectCommand.CommandTextFormat"); //$NON-NLS-1$
    }

    @Override
    public String getErrorDescription() {
        return Messages.getString("AddProjectCommand.ErrorTextFormat"); //$NON-NLS-1$
    }

    @Override
    public String getLoggingDescription() {
        return Messages.getString("AddProjectCommand.CommandTextFormat", LocaleUtil.ROOT); //$NON-NLS-1$
    }

    @Override
    protected IStatus doRun(final IProgressMonitor progressMonitor) throws Exception {
        final MultiCommandHelper helper = new MultiCommandHelper(progressMonitor);

        final EnumerateProjectFilesCommand enumerateProjectFilesCommand = new EnumerateProjectFilesCommand(project);
        final IStatus status = helper.runSubCommand(enumerateProjectFilesCommand);

        if (!status.isOK()) {
            return status;
        }

        addPaths = enumerateProjectFilesCommand.getPaths();

        /*
         * There must be at least one item so the server path can be created to
         * ensure a valid working folder mapping (which other Team Explorer
         * Everywhere plug-in behavior assumes). This will only really ever
         * happen if a .tpignore file or some other filter mechanism has
         * prevented EnumerateProjectFilesCommand from returning any files.
         */
        if (addPaths.length == 0) {
            return new Status(
                Status.ERROR,
                TFSEclipseClientPlugin.PLUGIN_ID,
                0,
                MessageFormat.format(
                    Messages.getString("AddProjectCommand.NoFilesRemainedErrorFormat"), //$NON-NLS-1$
                    project.getName(),
                    TPIgnoreDocument.DEFAULT_FILENAME),
                null);
        }

        return helper.runSubCommand(
            new AddCommand(repository, addPaths, false, LockLevel.UNCHANGED, GetOptions.NONE, PendChangesOptions.NONE));
    }

    @Override
    public IStatus rollback(final IProgressMonitor progressMonitor) throws Exception {
        Check.notNull(addPaths, "addPaths"); //$NON-NLS-1$

        if (addPaths.length == 0) {
            return Status.OK_STATUS;
        }

        final ItemSpec[] itemSpecs = new ItemSpec[addPaths.length];

        for (int i = 0; i < itemSpecs.length; i++) {
            itemSpecs[i] = new ItemSpec(addPaths[i], RecursionType.NONE);
        }

        return new ResourceChangingCommand(new UndoCommand(repository, itemSpecs, GetOptions.NONE)).run(
            progressMonitor);
    }
}
