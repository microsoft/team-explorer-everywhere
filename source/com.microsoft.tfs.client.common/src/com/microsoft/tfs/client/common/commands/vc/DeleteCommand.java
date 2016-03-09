// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.commands.vc;

import java.text.MessageFormat;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import com.microsoft.tfs.client.common.Messages;
import com.microsoft.tfs.client.common.commands.TFSCommand;
import com.microsoft.tfs.client.common.commands.helpers.NonFatalCommandHelper;
import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.core.clients.versioncontrol.GetOptions;
import com.microsoft.tfs.core.clients.versioncontrol.PendChangesOptions;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.LockLevel;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.RecursionType;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.LocaleUtil;

public class DeleteCommand extends TFSCommand {
    private final TFSRepository repository;
    private final String[] paths;
    private final RecursionType recursionType;
    private final LockLevel lockLevel;
    private final GetOptions getOptions;
    private final PendChangesOptions pendOptions;

    private final NonFatalCommandHelper nonFatalHelper;

    private int deleteCount;

    public DeleteCommand(final TFSRepository repository, final String[] paths) {
        this(repository, paths, RecursionType.NONE, LockLevel.UNCHANGED, GetOptions.NONE, PendChangesOptions.NONE);
    }

    public DeleteCommand(
        final TFSRepository repository,
        final String[] paths,
        final RecursionType recursionType,
        final LockLevel lockLevel,
        final GetOptions getOptions,
        final PendChangesOptions pendOptions) {
        Check.notNull(repository, "repository"); //$NON-NLS-1$
        Check.notNull(paths, "paths"); //$NON-NLS-1$
        Check.notNull(recursionType, "recursionType"); //$NON-NLS-1$
        Check.notNull(lockLevel, "lockLevel"); //$NON-NLS-1$
        Check.notNull(getOptions, "getOptions"); //$NON-NLS-1$
        Check.notNull(pendOptions, "pendOptions"); //$NON-NLS-1$

        this.repository = repository;
        this.paths = paths;
        this.recursionType = recursionType;
        this.lockLevel = lockLevel;
        this.getOptions = getOptions;
        this.pendOptions = pendOptions;

        nonFatalHelper = new NonFatalCommandHelper(repository);
    }

    @Override
    public String getName() {
        if (paths.length == 1) {
            final String messageFormat = Messages.getString("DeleteCommand.SingleItemCommandTextFormat"); //$NON-NLS-1$
            final String message = MessageFormat.format(messageFormat, paths[0]);
            return message;
        } else {
            final String messageFormat = Messages.getString("DeleteCommand.MultiItemCommandTextFormat"); //$NON-NLS-1$
            final String message = MessageFormat.format(messageFormat, paths.length);
            return message;
        }
    }

    @Override
    public String getErrorDescription() {
        if (paths.length == 1) {
            return (Messages.getString("DeleteCommand.SingleItemErrorText")); //$NON-NLS-1$
        } else {
            return (Messages.getString("DeleteCommand.MultiItemErrorText")); //$NON-NLS-1$
        }
    }

    @Override
    public String getLoggingDescription() {
        if (paths.length == 1) {
            final String messageFormat =
                Messages.getString("DeleteCommand.SingleItemCommandTextFormat", LocaleUtil.ROOT); //$NON-NLS-1$
            final String message = MessageFormat.format(messageFormat, paths[0]);
            return message;
        } else {
            final String messageFormat =
                Messages.getString("DeleteCommand.MultiItemCommandTextFormat", LocaleUtil.ROOT); //$NON-NLS-1$
            final String message = MessageFormat.format(messageFormat, paths.length);
            return message;
        }
    }

    @Override
    public IStatus doRun(final IProgressMonitor progressMonitor) throws Exception {
        nonFatalHelper.hookupListener();

        try {
            deleteCount =
                repository.getWorkspace().pendDelete(paths, recursionType, lockLevel, getOptions, pendOptions);
        } finally {
            nonFatalHelper.unhookListener();
        }

        if (nonFatalHelper.hasNonFatals()) {
            final int errorCount = paths.length - deleteCount;
            final int severity = (deleteCount > 0) ? IStatus.WARNING : IStatus.ERROR;

            return nonFatalHelper.getBestStatus(
                severity,
                errorCount,
                Messages.getString("DeleteCommand.FilesCouldNotBeDeletedFormat")); //$NON-NLS-1$
        }

        return Status.OK_STATUS;
    }

    public int getDeleteCount() {
        return deleteCount;
    }
}
