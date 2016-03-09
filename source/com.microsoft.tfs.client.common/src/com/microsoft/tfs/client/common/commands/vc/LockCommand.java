// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.commands.vc;

import java.text.MessageFormat;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import com.microsoft.tfs.client.common.Messages;
import com.microsoft.tfs.client.common.commands.TFSConnectedCommand;
import com.microsoft.tfs.client.common.commands.helpers.NonFatalCommandHelper;
import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.core.clients.versioncontrol.GetOptions;
import com.microsoft.tfs.core.clients.versioncontrol.PendChangesOptions;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.LockLevel;
import com.microsoft.tfs.core.clients.versioncontrol.specs.ItemSpec;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.LocaleUtil;

public class LockCommand extends TFSConnectedCommand {
    private final TFSRepository repository;
    private final ItemSpec[] itemSpecs;
    private final LockLevel lockLevel;
    private final GetOptions getOptions;
    private final PendChangesOptions pendOptions;

    private final NonFatalCommandHelper nonFatalHelper;

    private int lockCount;

    public LockCommand(final TFSRepository repository, final ItemSpec[] itemSpecs, final LockLevel lockLevel) {
        this(repository, itemSpecs, lockLevel, GetOptions.NONE, PendChangesOptions.NONE);
    }

    public LockCommand(
        final TFSRepository repository,
        final ItemSpec[] itemSpecs,
        final LockLevel lockLevel,
        final GetOptions getOptions,
        final PendChangesOptions pendOptions) {
        Check.notNull(repository, "repository"); //$NON-NLS-1$
        Check.notNull(itemSpecs, "itemSpecs"); //$NON-NLS-1$
        Check.notNull(lockLevel, "lockLevel"); //$NON-NLS-1$
        Check.notNull(getOptions, "getOptions"); //$NON-NLS-1$
        Check.notNull(pendOptions, "pendOptions"); //$NON-NLS-1$

        this.repository = repository;
        this.itemSpecs = itemSpecs;
        this.lockLevel = lockLevel;
        this.getOptions = getOptions;
        this.pendOptions = pendOptions;

        nonFatalHelper = new NonFatalCommandHelper(repository);

        setConnection(repository.getConnection());
    }

    @Override
    public String getName() {
        if (itemSpecs.length == 1) {
            if (lockLevel == LockLevel.NONE) {
                final String messageFormat = Messages.getString("LockCommand.UnlockingFileFormat"); //$NON-NLS-1$
                return MessageFormat.format(messageFormat, itemSpecs[0].getItem());
            } else {
                final String messageFormat = Messages.getString("LockCommand.LockingFileFormat"); //$NON-NLS-1$
                return MessageFormat.format(messageFormat, itemSpecs[0].getItem());
            }
        } else {
            if (lockLevel == LockLevel.NONE) {
                final String messageFormat = Messages.getString("LockCommand.UnlockingFilesFormat"); //$NON-NLS-1$
                return MessageFormat.format(messageFormat, itemSpecs.length);
            } else {
                final String messageFormat = Messages.getString("LockCommand.LockingFilesFormat"); //$NON-NLS-1$
                return MessageFormat.format(messageFormat, itemSpecs.length);
            }
        }
    }

    @Override
    public String getErrorDescription() {

        if (itemSpecs.length == 1) {
            if (lockLevel == LockLevel.NONE) {
                return (Messages.getString("LockCommand.ErrorUnlockingFile")); //$NON-NLS-1$
            } else {
                return (Messages.getString("LockCommand.ErrorLockingFile")); //$NON-NLS-1$
            }
        } else {
            if (lockLevel == LockLevel.NONE) {
                return (Messages.getString("LockCommand.ErrorUnlockingFiles")); //$NON-NLS-1$
            } else {
                return (Messages.getString("LockCommand.ErrorLockingFiles")); //$NON-NLS-1$
            }
        }
    }

    @Override
    public String getLoggingDescription() {

        if (itemSpecs.length == 1) {
            if (lockLevel == LockLevel.NONE) {
                final String messageFormat = Messages.getString("LockCommand.UnlockingFileFormat", LocaleUtil.ROOT); //$NON-NLS-1$
                return MessageFormat.format(messageFormat, itemSpecs[0].getItem());
            } else {
                return (MessageFormat.format(
                    "Locking {0} (locklevel {1})", //$NON-NLS-1$
                    itemSpecs[0].getItem(),
                    lockLevel.toUIString()));
            }
        } else {
            if (lockLevel == LockLevel.NONE) {
                final String messageFormat = Messages.getString("LockCommand.UnlockingFilesFormat", LocaleUtil.ROOT); //$NON-NLS-1$
                return MessageFormat.format(messageFormat, itemSpecs.length);
            } else {
                return (MessageFormat.format(
                    "Locking {0} files (locklevel {1})", //$NON-NLS-1$
                    itemSpecs.length,
                    lockLevel.toUIString()));
            }
        }
    }

    @Override
    protected IStatus doRun(final IProgressMonitor progressMonitor) throws Exception {
        nonFatalHelper.hookupListener();

        try {
            lockCount = repository.getWorkspace().setLock(itemSpecs, lockLevel, getOptions, pendOptions);
        } finally {
            nonFatalHelper.unhookListener();
        }

        if (lockCount < itemSpecs.length && nonFatalHelper.hasNonFatals()) {
            final int errorCount = itemSpecs.length - lockCount;
            final int severity = (lockCount > 0) ? IStatus.WARNING : IStatus.ERROR;

            return nonFatalHelper.getBestStatus(
                severity,
                errorCount,
                errorCount > 1 ? Messages.getString("LockCommand.MultiLockStatusNotChangedFormat") //$NON-NLS-1$
                    : Messages.getString("LockCommand.SingleLockStatusNotChangedFormat")); //$NON-NLS-1$
        }

        return Status.OK_STATUS;
    }
}
