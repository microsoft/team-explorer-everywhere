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

public class UndeleteCommand extends TFSConnectedCommand {
    private final TFSRepository repository;
    private final ItemSpec[] itemSpecs;
    private final LockLevel lockLevel;
    private final GetOptions getOptions;
    private final PendChangesOptions pendChangesOptions;

    private final NonFatalCommandHelper nonFatalHelper;
    private int undeleteCount;

    public UndeleteCommand(final TFSRepository repository, final ItemSpec[] itemSpecs) {
        this(repository, itemSpecs, LockLevel.UNCHANGED, GetOptions.NONE, PendChangesOptions.NONE);
    }

    public UndeleteCommand(
        final TFSRepository repository,
        final ItemSpec[] itemSpecs,
        final LockLevel lockLevel,
        final GetOptions getOptions,
        final PendChangesOptions pendChangesOptions) {
        Check.notNull(repository, "repository"); //$NON-NLS-1$
        Check.notNull(itemSpecs, "itemSpecs"); //$NON-NLS-1$
        Check.notNull(lockLevel, "lockLevel"); //$NON-NLS-1$
        Check.notNull(getOptions, "getOptions"); //$NON-NLS-1$
        Check.notNull(pendChangesOptions, "pendChangesOptions"); //$NON-NLS-1$

        this.repository = repository;
        this.itemSpecs = itemSpecs;
        this.lockLevel = lockLevel;
        this.getOptions = getOptions;
        this.pendChangesOptions = pendChangesOptions;

        nonFatalHelper = new NonFatalCommandHelper(repository);

        setConnection(repository.getConnection());
    }

    @Override
    public String getName() {
        if (itemSpecs.length == 1) {
            final String messageFormat = Messages.getString("UndeleteCommand.SingleCommandTextFormat"); //$NON-NLS-1$
            return MessageFormat.format(messageFormat, itemSpecs[0].getItem());
        } else {
            final String messageFormat = Messages.getString("UndeleteCommand.MultiCommandTextFormat"); //$NON-NLS-1$
            return MessageFormat.format(messageFormat, itemSpecs.length);
        }
    }

    @Override
    public String getErrorDescription() {
        if (itemSpecs.length == 1) {
            final String messageFormat = Messages.getString("UndeleteCommand.SingleErrorTextFormat"); //$NON-NLS-1$
            return MessageFormat.format(messageFormat, itemSpecs[0].getItem());
        } else {
            final String messageFormat = Messages.getString("UndeleteCommand.MultiErrorTextFormat"); //$NON-NLS-1$
            return MessageFormat.format(messageFormat, itemSpecs.length);
        }
    }

    @Override
    public String getLoggingDescription() {
        if (itemSpecs.length == 1) {
            final String messageFormat = Messages.getString("UndeleteCommand.SingleCommandTextFormat", LocaleUtil.ROOT); //$NON-NLS-1$
            return MessageFormat.format(messageFormat, itemSpecs[0].getItem());
        } else {
            final String messageFormat = Messages.getString("UndeleteCommand.MultiCommandTextFormat", LocaleUtil.ROOT); //$NON-NLS-1$
            return MessageFormat.format(messageFormat, itemSpecs.length);
        }
    }

    @Override
    protected IStatus doRun(final IProgressMonitor progressMonitor) throws Exception {
        nonFatalHelper.hookupListener();

        try {
            undeleteCount =
                repository.getWorkspace().pendUndelete(itemSpecs, lockLevel, getOptions, pendChangesOptions);
        } finally {
            nonFatalHelper.unhookListener();
        }

        if (nonFatalHelper.hasNonFatals()) {
            final int errorCount = itemSpecs.length - undeleteCount;
            final int severity = (undeleteCount > 0) ? IStatus.WARNING : IStatus.ERROR;

            return nonFatalHelper.getBestStatus(
                severity,
                errorCount,
                Messages.getString("UndeleteCommand.ItemsCouldNotBeUndeletedFormat")); //$NON-NLS-1$
        }

        return Status.OK_STATUS;
    }

    public int getUndeleteCount() {
        return undeleteCount;
    }
}
