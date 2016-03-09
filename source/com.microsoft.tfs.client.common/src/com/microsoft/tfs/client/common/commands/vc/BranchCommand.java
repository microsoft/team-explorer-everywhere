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
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.RecursionType;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.VersionSpec;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.LocaleUtil;

public class BranchCommand extends TFSConnectedCommand {
    private final TFSRepository repository;
    private final String sourceServerPath;
    private final String targetServerPath;
    private final VersionSpec version;
    private final LockLevel lockLevel;
    private final RecursionType recursionType;
    private final GetOptions getOptions;
    private final PendChangesOptions pendOptions;

    private final NonFatalCommandHelper nonFatalHelper;

    public BranchCommand(
        final TFSRepository repository,
        final String sourceServerPath,
        final String targetServerPath,
        final VersionSpec version,
        final GetOptions getOptions) {
        this(
            repository,
            sourceServerPath,
            targetServerPath,
            version,
            LockLevel.UNCHANGED,
            RecursionType.FULL,
            getOptions,
            PendChangesOptions.NONE);
    }

    public BranchCommand(
        final TFSRepository repository,
        final String sourceServerPath,
        final String targetServerPath,
        final VersionSpec version,
        final LockLevel lockLevel,
        final RecursionType recursionType,
        final GetOptions getOptions,
        final PendChangesOptions pendOptions) {
        Check.notNull(repository, "repository"); //$NON-NLS-1$
        Check.notNull(sourceServerPath, "sourceServerPath"); //$NON-NLS-1$
        Check.notNull(targetServerPath, "targetServerPath"); //$NON-NLS-1$
        Check.notNull(version, "version"); //$NON-NLS-1$
        Check.notNull(lockLevel, "lockLevel"); //$NON-NLS-1$
        Check.notNull(recursionType, "recursionType"); //$NON-NLS-1$
        Check.notNull(getOptions, "getOptions"); //$NON-NLS-1$
        Check.notNull(pendOptions, "pendOptions"); //$NON-NLS-1$

        this.repository = repository;
        this.sourceServerPath = sourceServerPath;
        this.targetServerPath = targetServerPath;
        this.version = version;
        this.lockLevel = lockLevel;
        this.recursionType = recursionType;
        this.getOptions = getOptions;
        this.pendOptions = pendOptions;

        nonFatalHelper = new NonFatalCommandHelper(repository);

        setConnection(repository.getConnection());
    }

    @Override
    public String getName() {
        final String messageFormat = Messages.getString("BranchCommand.CommandTextFormat"); //$NON-NLS-1$
        return MessageFormat.format(messageFormat, targetServerPath);
    }

    @Override
    public String getErrorDescription() {
        final String messageFormat = Messages.getString("BranchCommand.ErrorTextFormat"); //$NON-NLS-1$
        return MessageFormat.format(messageFormat, targetServerPath);
    }

    @Override
    public String getLoggingDescription() {
        final String messageFormat = Messages.getString("BranchCommand.CommandTextFormat", LocaleUtil.ROOT); //$NON-NLS-1$
        return MessageFormat.format(messageFormat, targetServerPath);
    }

    @Override
    protected IStatus doRun(final IProgressMonitor progressMonitor) throws Exception {
        nonFatalHelper.hookupListener();

        int pendCount;
        try {
            pendCount = repository.getWorkspace().pendBranch(
                sourceServerPath,
                targetServerPath,
                version,
                lockLevel,
                recursionType,
                getOptions,
                pendOptions);
        } finally {
            nonFatalHelper.unhookListener();
        }

        if (nonFatalHelper.hasNonFatals()) {
            final int severity = pendCount == 0 ? IStatus.ERROR : IStatus.WARNING;
            final String message = pendCount == 0 ? Messages.getString("BranchCommand.ErrorText") //$NON-NLS-1$
                : Messages.getString("BranchCommand.WarningText"); //$NON-NLS-1$

            return nonFatalHelper.getMultiStatus(severity, message);
        }

        return Status.OK_STATUS;
    }
}
