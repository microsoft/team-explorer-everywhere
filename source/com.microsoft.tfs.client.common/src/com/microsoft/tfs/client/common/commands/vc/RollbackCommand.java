// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.commands.vc;

import java.text.MessageFormat;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;

import com.microsoft.tfs.client.common.Messages;
import com.microsoft.tfs.client.common.TFSCommonClientPlugin;
import com.microsoft.tfs.client.common.commands.TFSCommand;
import com.microsoft.tfs.client.common.commands.helpers.NonFatalCommandHelper;
import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.core.clients.versioncontrol.GetStatus;
import com.microsoft.tfs.core.clients.versioncontrol.RollbackOptions;
import com.microsoft.tfs.core.clients.versioncontrol.conflicts.ConflictDescription;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.LockLevel;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.RecursionType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;
import com.microsoft.tfs.core.clients.versioncontrol.specs.ItemSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.LatestVersionSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.VersionSpec;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.LocaleUtil;
import com.microsoft.tfs.util.StringUtil;
import com.microsoft.tfs.util.tasks.CanceledException;

public class RollbackCommand extends TFSCommand {
    private final Log log = LogFactory.getLog(RollbackCommand.class);

    private final TFSRepository repository;
    private final String itemPath;
    private final VersionSpec versionFrom;
    private final VersionSpec versionTo;
    private final RecursionType recursionType;
    private final RollbackOptions rollbackOptions;

    private final NonFatalCommandHelper nonFatalHelper;

    private ConflictDescription[] conflictDescriptions;
    private boolean hasConflicts = false;

    public RollbackCommand(
        final TFSRepository repository,
        final String itemPath,
        final VersionSpec versionFrom,
        final VersionSpec versionTo,
        final RecursionType recursionType,
        final RollbackOptions rollbackOptions) {
        log.trace("RollbackCommand creating"); //$NON-NLS-1$

        Check.notNull(repository, "repository"); //$NON-NLS-1$
        Check.notNull(versionFrom, "versionFrom"); //$NON-NLS-1$
        Check.notNull(versionTo, "versionTo"); //$NON-NLS-1$
        Check.notNull(recursionType, "recursionType"); //$NON-NLS-1$
        Check.notNull(rollbackOptions, "rollbackOptions"); //$NON-NLS-1$

        log.trace("item - " + itemPath); //$NON-NLS-1$
        log.trace("versionFrom - " + versionFrom.toString()); //$NON-NLS-1$
        log.trace("versionTo - " + versionTo.toString()); //$NON-NLS-1$
        log.trace("rollbackOptions - " + rollbackOptions.toString()); //$NON-NLS-1$

        this.repository = repository;
        this.itemPath = itemPath;
        this.versionFrom = versionFrom;
        this.versionTo = versionTo;
        this.recursionType = recursionType;
        this.rollbackOptions = rollbackOptions;

        nonFatalHelper = new NonFatalCommandHelper(repository);

        setCancellable(true);

        log.trace("RollbackCommand created"); //$NON-NLS-1$
    }

    public boolean hasConflicts() {
        return hasConflicts;
    }

    public ConflictDescription[] getConflictDescriptions() {
        return conflictDescriptions;
    }

    @Override
    public String getName() {
        if (itemPath != null) {
            final String messageFormat = Messages.getString("RollbackCommand.CommandNameFormat"); //$NON-NLS-1$
            return MessageFormat.format(messageFormat, itemPath, versionFrom.toString(), versionTo.toString());
        } else {
            final String messageFormat = Messages.getString("RollbackCommand.EntireChangesetCommandNameFormat"); //$NON-NLS-1$
            return MessageFormat.format(messageFormat, versionFrom.toString());
        }
    }

    @Override
    public String getErrorDescription() {
        return (Messages.getString("RollbackCommand.ErrorDescription")); //$NON-NLS-1$
    }

    @Override
    public String getLoggingDescription() {
        if (itemPath != null) {
            final String messageFormat = Messages.getString("RollbackCommand.CommandNameFormat", LocaleUtil.ROOT); //$NON-NLS-1$
            return MessageFormat.format(messageFormat, itemPath, versionFrom.toString(), versionTo.toString());
        } else {
            final String messageFormat =
                Messages.getString("RollbackCommand.EntireChangesetCommandNameFormat", LocaleUtil.ROOT); //$NON-NLS-1$
            return MessageFormat.format(messageFormat, versionFrom.toString());
        }
    }

    @Override
    protected IStatus doRun(final IProgressMonitor progressMonitor) throws Exception {
        log.trace("doRun entered"); //$NON-NLS-1$
        nonFatalHelper.hookupListener();

        try {
            progressMonitor.beginTask(getName(), IProgressMonitor.UNKNOWN);
            final Workspace workspace = repository.getWorkspace();

            ItemSpec[] items = null;

            if (!StringUtil.isNullOrEmpty(itemPath)) {
                if (!workspace.isServerPathMapped(itemPath)) {
                    final String messageNotMappedFormat = Messages.getString("RollbackCommand.NotMappedMessageFormat"); //$NON-NLS-1$
                    final String messageNotMapped = MessageFormat.format(messageNotMappedFormat, itemPath);
                    return new Status(IStatus.ERROR, TFSCommonClientPlugin.PLUGIN_ID, messageNotMapped);
                }

                items = new ItemSpec[1];
                items[0] = new ItemSpec(itemPath, recursionType);
            }

            // call to OM to perform the rollback.
            final GetStatus getStatus = workspace.rollback(
                items,
                items == null ? null : LatestVersionSpec.INSTANCE,
                versionFrom,
                versionTo,
                LockLevel.UNCHANGED,
                rollbackOptions,
                null);

            if (progressMonitor.isCanceled()) {
                return Status.CANCEL_STATUS;
            }

            log.trace("doRun: getStatus.isCanceled              - " + getStatus.isCanceled()); //$NON-NLS-1$
            log.trace("doRun: getStatus.isNoActionNeeded        - " + getStatus.isNoActionNeeded()); //$NON-NLS-1$
            log.trace("doRun: getStatus.haveResolvableWarnings  - " + getStatus.haveResolvableWarnings()); //$NON-NLS-1$
            log.trace("doRun: getStatus.getNumConflicts         - " + getStatus.getNumConflicts()); //$NON-NLS-1$
            log.trace("doRun: getStatus.getNumFailures          - " + getStatus.getNumFailures()); //$NON-NLS-1$
            log.trace("doRun: getStatus.getNumOperations        - " + getStatus.getNumOperations()); //$NON-NLS-1$
            log.trace("doRun: getStatus.getNumResolvedConflicts - " + getStatus.getNumResolvedConflicts()); //$NON-NLS-1$
            log.trace("doRun: getStatus.getNumUpdated           - " + getStatus.getNumUpdated()); //$NON-NLS-1$
            log.trace("doRun: getStatus.getNumWarnings          - " + getStatus.getNumWarnings()); //$NON-NLS-1$

            if (getStatus.isNoActionNeeded()) {
                return new Status(
                    IStatus.ERROR,
                    TFSCommonClientPlugin.PLUGIN_ID,
                    Messages.getString("RollbackCommand.NoChangesToRollback")); //$NON-NLS-1$
            }

            if (getStatus.getNumConflicts() > 0) {
                final QueryConflictsCommand queryCommand = new QueryConflictsCommand(repository, items);

                final IStatus queryStatus = queryCommand.run(new SubProgressMonitor(progressMonitor, 1));

                if (progressMonitor.isCanceled()) {
                    return Status.CANCEL_STATUS;
                }

                log.trace("doRun: queryStatus.isOK            - " + queryStatus.isOK()); //$NON-NLS-1$
                log.trace("doRun: queryStatus.isMultiStatus   - " + queryStatus.isMultiStatus()); //$NON-NLS-1$
                log.trace("doRun: queryStatus.getSeverity     - " + queryStatus.getSeverity()); //$NON-NLS-1$
                log.trace("doRun: queryStatus.getMessage      - " + queryStatus.getMessage()); //$NON-NLS-1$
                if (queryStatus.getException() != null) {
                    log.trace("doRun: queryStatus.getException    - " + queryStatus.getException()); //$NON-NLS-1$
                }
                if (!queryStatus.isOK()) {
                    return queryStatus;
                }

                conflictDescriptions = queryCommand.getConflictDescriptions();
                hasConflicts = conflictDescriptions.length > 0;

                if (progressMonitor.isCanceled()) {
                    return Status.CANCEL_STATUS;
                }

                log.trace("doRun: conflictDescriptions.length - " + conflictDescriptions.length); //$NON-NLS-1$

                return new Status(
                    IStatus.WARNING,
                    TFSCommonClientPlugin.PLUGIN_ID,
                    0,
                    Messages.getString("RollbackCommand.ConflictsWhileRollback"), //$NON-NLS-1$
                    null);
            }

            if (nonFatalHelper.hasNonFatals()) {
                return nonFatalHelper.getMultiStatus(
                    IStatus.ERROR,
                    Messages.getString("RollbackCommand.NonFatalErrors")); //$NON-NLS-1$
            }
        } catch (final CanceledException e) {
            log.trace("doRun cancelled"); //$NON-NLS-1$
            return Status.CANCEL_STATUS;
        } catch (final Exception e) {
            log.trace("doRun exception", e); //$NON-NLS-1$
            return new Status(IStatus.ERROR, TFSCommonClientPlugin.PLUGIN_ID, e.getLocalizedMessage());
        } finally {
            nonFatalHelper.unhookListener();
            progressMonitor.done();
        }

        log.trace("doRun finished"); //$NON-NLS-1$

        return Status.OK_STATUS;
    }

}
