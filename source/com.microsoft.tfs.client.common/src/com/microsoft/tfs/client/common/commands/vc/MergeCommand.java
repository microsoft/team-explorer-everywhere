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
import com.microsoft.tfs.client.common.commands.TFSConnectedCommand;
import com.microsoft.tfs.client.common.commands.helpers.NonFatalCommandHelper;
import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.core.clients.versioncontrol.GetStatus;
import com.microsoft.tfs.core.clients.versioncontrol.MergeFlags;
import com.microsoft.tfs.core.clients.versioncontrol.conflicts.ConflictDescription;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.LockLevel;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.RecursionType;
import com.microsoft.tfs.core.clients.versioncontrol.specs.ItemSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.VersionSpec;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.LocaleUtil;

public class MergeCommand extends TFSConnectedCommand {
    private final TFSRepository repository;
    private final String sourcePath;
    private final String targetPath;
    private final VersionSpec sourceVersionFrom;
    private final VersionSpec sourceVersionTo;
    private final LockLevel lockLevel;
    private final RecursionType recursionType;
    private final MergeFlags mergeFlags;

    private final boolean queryConflicts = true;

    private final NonFatalCommandHelper nonFatalHelper;

    private GetStatus mergeStatus;
    private boolean hasConflicts;
    private ConflictDescription[] conflictDescriptions;

    public MergeCommand(
        final TFSRepository repository,
        final String sourcePath,
        final String targetPath,
        final VersionSpec sourceVersionFrom,
        final VersionSpec sourceVersionTo) {
        this(
            repository,
            sourcePath,
            targetPath,
            sourceVersionFrom,
            sourceVersionTo,
            LockLevel.UNCHANGED,
            RecursionType.FULL,
            MergeFlags.NONE);
    }

    public MergeCommand(
        final TFSRepository repository,
        final String sourcePath,
        final String targetPath,
        final VersionSpec sourceVersionFrom,
        final VersionSpec sourceVersionTo,
        final LockLevel lockLevel,
        final RecursionType recursionType,
        final MergeFlags mergeFlags) {
        Check.notNull(repository, "repository"); //$NON-NLS-1$
        Check.notNull(sourcePath, "sourcePath"); //$NON-NLS-1$
        Check.notNull(targetPath, "targetPath"); //$NON-NLS-1$
        // sourceVersionFrom may be null
        Check.notNull(sourceVersionTo, "sourceVersionTo"); //$NON-NLS-1$
        Check.notNull(lockLevel, "lockLevel"); //$NON-NLS-1$
        Check.notNull(recursionType, "recursionType"); //$NON-NLS-1$
        Check.notNull(mergeFlags, "mergeFlags"); //$NON-NLS-1$

        this.repository = repository;
        this.sourcePath = sourcePath;
        this.targetPath = targetPath;
        this.sourceVersionFrom = sourceVersionFrom;
        this.sourceVersionTo = sourceVersionTo;
        this.lockLevel = lockLevel;
        this.recursionType = recursionType;
        this.mergeFlags = mergeFlags;

        nonFatalHelper = new NonFatalCommandHelper(repository);

        setConnection(repository.getConnection());
    }

    @Override
    public String getName() {
        final String messageFormat = Messages.getString("MergeCommand.CommandTextFormat"); //$NON-NLS-1$
        return MessageFormat.format(messageFormat, sourcePath);
    }

    @Override
    public String getErrorDescription() {
        final String messageFormat = Messages.getString("MergeCommand.ErrorTextFormat"); //$NON-NLS-1$
        return MessageFormat.format(messageFormat, sourcePath);
    }

    @Override
    public String getLoggingDescription() {
        final String messageFormat = Messages.getString("MergeCommand.CommandTextFormat", LocaleUtil.ROOT); //$NON-NLS-1$
        return MessageFormat.format(messageFormat, sourcePath);
    }

    @Override
    protected IStatus doRun(final IProgressMonitor progressMonitor) throws Exception {
        progressMonitor.beginTask(getName(), 2);
        nonFatalHelper.hookupListener();

        try {
            mergeStatus = repository.getWorkspace().merge(
                sourcePath,
                targetPath,
                sourceVersionFrom,
                sourceVersionTo,
                lockLevel,
                recursionType,
                mergeFlags);
            progressMonitor.worked(1);
        } catch (final Exception e) {
            return new Status(IStatus.ERROR, TFSCommonClientPlugin.PLUGIN_ID, 0, e.getLocalizedMessage(), null);
        } finally {
            nonFatalHelper.unhookListener();
        }

        if (mergeStatus.getNumFailures() > 0) {
            return nonFatalHelper.getBestStatus(
                IStatus.ERROR,
                mergeStatus.getNumFailures(),
                Messages.getString("MergeCommand.FileCouldNotBeMergedFormat")); //$NON-NLS-1$
        }

        if (queryConflicts && mergeStatus.getNumConflicts() > 0) {
            hasConflicts = true;

            final SubProgressMonitor conflictsMonitor = new SubProgressMonitor(progressMonitor, 1);

            final QueryConflictsCommand conflictsCommand = new QueryConflictsCommand(repository, new ItemSpec[] {
                new ItemSpec(sourcePath, recursionType)
            });
            final IStatus conflictsStatus = conflictsCommand.run(conflictsMonitor);

            progressMonitor.worked(1);

            if (!conflictsStatus.isOK()) {
                return conflictsStatus;
            }

            conflictDescriptions = conflictsCommand.getConflictDescriptions();

            return new Status(
                IStatus.WARNING,
                TFSCommonClientPlugin.PLUGIN_ID,
                0,
                Messages.getString("MergeCommand.ConflictsDuringMerge"), //$NON-NLS-1$
                null);
        }

        // In case there are no changes to merge
        if (mergeStatus.isNoActionNeeded()) {
            return new Status(
                IStatus.INFO,
                TFSCommonClientPlugin.PLUGIN_ID,
                0,
                MessageFormat.format(Messages.getString("MergeCommand.NoChangesToMergeFormat"), sourcePath, targetPath), //$NON-NLS-1$
                null);
        }
        return Status.OK_STATUS;
    }

    public GetStatus getMergeStatus() {
        return mergeStatus;
    }

    public boolean hasConflicts() {
        return hasConflicts;
    }

    public ConflictDescription[] getConflictDescriptions() {
        return conflictDescriptions;
    }
}
