// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.commands.vc;

import java.text.MessageFormat;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;

import com.microsoft.tfs.client.common.Messages;
import com.microsoft.tfs.client.common.TFSCommonClientPlugin;
import com.microsoft.tfs.client.common.commands.TFSCommand;
import com.microsoft.tfs.client.common.commands.helpers.NonFatalCommandHelper;
import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.core.clients.versioncontrol.GetOptions;
import com.microsoft.tfs.core.clients.versioncontrol.PendChangesOptions;
import com.microsoft.tfs.core.clients.versioncontrol.conflicts.ConflictDescription;
import com.microsoft.tfs.core.clients.versioncontrol.path.LocalPath;
import com.microsoft.tfs.core.clients.versioncontrol.path.ServerPath;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.LockLevel;
import com.microsoft.tfs.core.clients.versioncontrol.specs.ItemSpec;
import com.microsoft.tfs.core.util.FileEncoding;
import com.microsoft.tfs.util.Check;

public class EditCommand extends TFSCommand {
    private final TFSRepository repository;
    private final ItemSpec[] itemSpecs;
    private final LockLevel[] lockLevels;
    private final FileEncoding[] fileEncodings;
    private final GetOptions getOptions;
    private final PendChangesOptions pendOptions;

    private final NonFatalCommandHelper nonFatalHelper;

    private final boolean queryConflicts;

    private ConflictDescription[] conflictDescriptions;

    public EditCommand(final TFSRepository repository, final ItemSpec[] itemSpecs, final LockLevel lockLevel) {
        this(repository, itemSpecs, lockLevel, null, GetOptions.NONE, PendChangesOptions.NONE, true);
    }

    public EditCommand(
        final TFSRepository repository,
        final ItemSpec[] itemSpecs,
        final LockLevel lockLevel,
        final FileEncoding fileEncoding,
        final GetOptions getOptions,
        final PendChangesOptions pendOptions,
        final boolean queryConflicts) {
        this(
            repository,
            itemSpecs,
            fillLockLevelArray(lockLevel, itemSpecs.length),
            fillFileEncodingArray(fileEncoding, itemSpecs.length),
            getOptions,
            pendOptions,
            queryConflicts);
    }

    public EditCommand(
        final TFSRepository repository,
        final ItemSpec[] itemSpecs,
        final LockLevel[] lockLevels,
        final FileEncoding[] fileEncodings,
        final GetOptions getOptions,
        final PendChangesOptions pendOptions,
        final boolean queryConflicts) {
        Check.notNull(repository, "repository"); //$NON-NLS-1$
        Check.notNullOrEmpty(itemSpecs, "itemSpecs"); //$NON-NLS-1$
        Check.notNull(lockLevels, "lockLevels"); //$NON-NLS-1$
        Check.notNull(getOptions, "getOptions"); //$NON-NLS-1$
        Check.notNull(pendOptions, "pendOptions"); //$NON-NLS-1$

        this.repository = repository;
        this.itemSpecs = itemSpecs;
        this.lockLevels = lockLevels;
        this.fileEncodings = fileEncodings;
        this.getOptions = getOptions;
        this.pendOptions = pendOptions;
        this.queryConflicts = queryConflicts;

        nonFatalHelper = new NonFatalCommandHelper(repository);
    }

    @Override
    public String getName() {
        if (itemSpecs.length == 1) {
            final String filename = ServerPath.isServerPath(itemSpecs[0].getItem())
                ? ServerPath.getFileName(itemSpecs[0].getItem()) : LocalPath.getFileName(itemSpecs[0].getItem());

            final String messageFormat = Messages.getString("EditCommand.CommandTextFormat"); //$NON-NLS-1$
            return MessageFormat.format(messageFormat, filename);
        } else {
            final String messageFormat = Messages.getString("EditCommand.ErrorTextFormat"); //$NON-NLS-1$
            return MessageFormat.format(messageFormat, itemSpecs.length);
        }
    }

    @Override
    public String getErrorDescription() {
        if (itemSpecs.length == 1) {
            return (Messages.getString("EditCommand.SingleFileErrorText")); //$NON-NLS-1$
        } else {
            return (Messages.getString("EditCommand.MultiFileErrorText")); //$NON-NLS-1$
        }
    }

    @Override
    public String getLoggingDescription() {
        if (itemSpecs.length == 1) {
            return (MessageFormat.format(
                "Pending edit for {0} (locklevel {1})", //$NON-NLS-1$
                itemSpecs[0].getItem(),
                lockLevels[0].toUIString()));
        } else {
            return (MessageFormat.format("Pending edit for {0} items", itemSpecs.length)); //$NON-NLS-1$
        }
    }

    private static final LockLevel[] fillLockLevelArray(final LockLevel lockLevel, final int length) {
        final LockLevel[] array = new LockLevel[length];

        for (int i = 0; i < length; i++) {
            array[i] = lockLevel;
        }

        return array;
    }

    private static final FileEncoding[] fillFileEncodingArray(final FileEncoding encoding, final int length) {
        final FileEncoding[] array = new FileEncoding[length];

        for (int i = 0; i < length; i++) {
            array[i] = encoding;
        }

        return array;
    }

    public boolean hasConflicts() {
        return (conflictDescriptions != null && conflictDescriptions.length > 0);
    }

    public ConflictDescription[] getConflictDescriptions() {
        return conflictDescriptions;
    }

    @Override
    protected IStatus doRun(final IProgressMonitor progressMonitor) throws Exception {
        nonFatalHelper.hookupListener();

        int editCount;
        try {
            editCount =
                repository.getWorkspace().pendEdit(itemSpecs, lockLevels, fileEncodings, getOptions, pendOptions);
        } finally {
            nonFatalHelper.unhookListener();
        }

        /* Query conflicts if get latest on checkout is enabled. */

        if (queryConflicts) {
            final SubProgressMonitor conflictsMonitor = new SubProgressMonitor(progressMonitor, 1);

            final QueryConflictsCommand conflictsCommand = new QueryConflictsCommand(repository, itemSpecs);
            final IStatus conflictsStatus = conflictsCommand.run(conflictsMonitor);

            if (!conflictsStatus.isOK()) {
                return conflictsStatus;
            }

            conflictDescriptions = conflictsCommand.getConflictDescriptions();
        }

        if (editCount < itemSpecs.length) {
            final int errorCount = itemSpecs.length - editCount;
            final int severity = (editCount > 0) ? IStatus.WARNING : IStatus.ERROR;

            return nonFatalHelper.getBestStatus(
                severity,
                errorCount,
                Messages.getString("EditCommand.FilesCouldNotBeCheckedOutFormat")); //$NON-NLS-1$
        }

        if (queryConflicts && conflictDescriptions.length > 0) {
            return new Status(
                IStatus.WARNING,
                TFSCommonClientPlugin.PLUGIN_ID,
                0,
                Messages.getString("EditCommand.ConflictsOccurredCheckingOutFiles"), //$NON-NLS-1$
                null);
        }

        return Status.OK_STATUS;
    }
}
