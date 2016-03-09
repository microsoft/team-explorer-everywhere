// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.commands.vc;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;

import com.microsoft.tfs.client.common.Messages;
import com.microsoft.tfs.client.common.TFSCommonClientPlugin;
import com.microsoft.tfs.client.common.commands.TFSConnectedCommand;
import com.microsoft.tfs.client.common.commands.helpers.NonFatalCommandHelper;
import com.microsoft.tfs.client.common.commands.helpers.PendingChangeCommandHelper;
import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.common.util.StatusHelper;
import com.microsoft.tfs.core.clients.versioncontrol.GetStatus;
import com.microsoft.tfs.core.clients.versioncontrol.UnshelveResult;
import com.microsoft.tfs.core.clients.versioncontrol.WebServiceLevel;
import com.microsoft.tfs.core.clients.versioncontrol.conflicts.ConflictDescription;
import com.microsoft.tfs.core.clients.versioncontrol.conflicts.ConflictDescriptionFactory;
import com.microsoft.tfs.core.clients.versioncontrol.events.NonFatalErrorEvent;
import com.microsoft.tfs.core.clients.versioncontrol.exceptions.UnshelveException;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PendingChange;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Shelveset;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Warning;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.WarningType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;
import com.microsoft.tfs.core.clients.versioncontrol.specs.ItemSpec;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.LocaleUtil;

public class UnshelveCommand extends TFSConnectedCommand {
    private final TFSRepository repository;

    private final String name;
    private final String ownerName;
    private final ItemSpec[] itemSpecs;

    private final NonFatalCommandHelper nonFatalHelper;
    private final PendingChangeCommandHelper pendingChangeHelper;

    private final boolean queryPendingChanges = false;
    private final boolean queryConflicts = true;
    private boolean deleteShelveset = false;

    private GetStatus status;
    private Shelveset shelveset;

    private PendingChange[] pendingChanges;

    private boolean hasConflicts = false;
    private ConflictDescription[] conflictDescriptions;

    private final boolean autoResolveConflicts;

    /*
     * We have unresolved conflicts from a previous unshelve that prevent us
     * from unshelving.
     */
    private boolean hasUnresolvedConflicts = false;

    public UnshelveCommand(
        final TFSRepository repository,
        final Shelveset shelveset,
        final ItemSpec[] itemSpecs,
        final boolean deleteShelveset,
        final boolean autoResolveConflicts) {
        this(
            repository,
            shelveset.getName(),
            shelveset.getOwnerName(),
            itemSpecs,
            deleteShelveset,
            autoResolveConflicts);
    }

    private UnshelveCommand(
        final TFSRepository repository,
        final String name,
        final String ownerName,
        final ItemSpec[] itemSpecs,
        final boolean deleteShelveset,
        final boolean autoResolveConflicts) {
        Check.notNull(repository, "repository"); //$NON-NLS-1$
        Check.notNull(name, "name"); //$NON-NLS-1$
        Check.notNull(ownerName, "ownerName"); //$NON-NLS-1$

        this.repository = repository;
        this.name = name;
        this.ownerName = ownerName;
        this.itemSpecs = itemSpecs;
        this.deleteShelveset = deleteShelveset;
        this.autoResolveConflicts = autoResolveConflicts;

        nonFatalHelper = new NonFatalCommandHelper(repository);
        pendingChangeHelper = new PendingChangeCommandHelper(repository);

        setConnection(repository.getConnection());
        setCancellable(true);
    }

    @Override
    public String getName() {
        final String messageFormat = Messages.getString("UnshelveCommand.CommandTextFormat"); //$NON-NLS-1$
        return MessageFormat.format(messageFormat, name);
    }

    @Override
    public String getErrorDescription() {
        return (Messages.getString("UnshelveCommand.ErrorText")); //$NON-NLS-1$
    }

    @Override
    public String getLoggingDescription() {
        final String messageFormat = Messages.getString("UnshelveCommand.CommandTextFormat", LocaleUtil.ROOT); //$NON-NLS-1$
        return MessageFormat.format(messageFormat, name);
    }

    @Override
    protected IStatus doRun(final IProgressMonitor progressMonitor) throws Exception {
        final Workspace workspace = repository.getWorkspace();

        nonFatalHelper.hookupListener();
        pendingChangeHelper.hookupListener();

        try {
            progressMonitor.subTask(getName());

            final UnshelveResult result;
            try {
                result = workspace.unshelve(
                    name,
                    ownerName,
                    itemSpecs,
                    null,
                    null,
                    (workspace.getClient().getServiceLevel().getValue() >= WebServiceLevel.TFS_2012.getValue()),
                    !autoResolveConflicts);
            } finally {
                // No cancellation after this point
                setCancellable(false);
            }

            if (result == null) {
                status = null;
                shelveset = null;
            } else {
                status = result.getStatus();
                shelveset = result.getShelveset();

                if (result.getConflicts() != null && result.getConflicts().length > 0) {
                    hasConflicts = true;
                    conflictDescriptions =
                        ConflictDescriptionFactory.getConflictDescriptions(workspace, result.getConflicts(), null);
                }
            }
        } catch (final UnshelveException e) {
            /*
             * If we try to re-unshelve a conflicting shelveset, the server will
             * notify us. We will want to reopen conflict resolution if this
             * occurs.
             */
            if (nonFatalHelper.hasNonFatals()) {
                for (final NonFatalErrorEvent nonFatal : nonFatalHelper.getNonFatalErrors()) {
                    if (nonFatal.getFailure() != null
                        && nonFatal.getFailure().getMessage() != null
                        && nonFatal.getFailure().getMessage().startsWith("TF203098")) //$NON-NLS-1$
                    {
                        hasUnresolvedConflicts = true;
                    }
                }
            }

            if (nonFatalHelper.hasNonFatals() && nonFatalHelper.getNonFatalErrors().length == 1) {
                return new Status(
                    Status.ERROR,
                    TFSCommonClientPlugin.PLUGIN_ID,
                    0,
                    nonFatalHelper.getNonFatalErrors()[0].getMessage(),
                    nonFatalHelper.getNonFatalErrors()[0].getThrowable());
            } else if (nonFatalHelper.hasNonFatals()) {
                final String messageFormat = Messages.getString("UnshelveCommand.ErrorTextFormat"); //$NON-NLS-1$
                final String message = MessageFormat.format(messageFormat, name, e.getLocalizedMessage());

                return new MultiStatus(
                    TFSCommonClientPlugin.PLUGIN_ID,
                    0,
                    nonFatalHelper.getStatuses(IStatus.ERROR),
                    message,
                    null);
            }

            return new Status(Status.ERROR, TFSCommonClientPlugin.PLUGIN_ID, 0, e.getLocalizedMessage(), null);
        } finally {
            progressMonitor.worked(1);
            nonFatalHelper.unhookListener();
            pendingChangeHelper.unhookListener();
        }

        /*
         * Remove any warnings about pending changes existing in other
         * workspaces. They often will.
         */
        IStatus nonFatalStatus = null;

        if (nonFatalHelper.hasNonFatals()) {
            final int severity = (shelveset != null) ? Status.WARNING : Status.ERROR;
            final NonFatalErrorEvent[] nonFatals = nonFatalHelper.getNonFatalErrors();

            final List<NonFatalErrorEvent> filteredNonFatals = new ArrayList<NonFatalErrorEvent>();
            for (int i = 0; i < nonFatals.length; i++) {
                /*
                 * Exclude messages of the form
                 * "Warning: another user has this item checked out." These only
                 * need to go to the console.
                 */
                if (nonFatals[i].getFailure() != null) {
                    final Warning[] warnings = nonFatals[i].getFailure().getWarnings();

                    if (warnings != null) {
                        for (int j = 0; j < warnings.length; j++) {
                            /*
                             * Don't show other pending changes or stale version
                             * warnings in the UI, just let them go to the
                             * console.
                             */
                            if (warnings[j].getWarningType() != WarningType.RESOURCE_PENDING_CHANGE_WARNING
                                && warnings[j].getWarningType() != WarningType.STALE_VERSION_WARNING) {
                                filteredNonFatals.add(nonFatals[i]);
                                break;
                            }
                        }
                    }
                }
            }

            /*
             * When we get multiple unshelve warnings, we may get a warning
             * truncated message. If we hid all the warnings, then we should
             * also hide the truncation message (it will still go to the
             * console.) This prevents us from popping up a warning dialog that
             * says only "Only the first 100 warnings were shown...".
             */
            if (filteredNonFatals.size() == 1
                && filteredNonFatals.get(0).getFailure() != null
                && filteredNonFatals.get(0).getFailure().getCode() != null
                && filteredNonFatals.get(0).getFailure().getCode().equals("AllPendingChangeWarningsNotIncluded")) //$NON-NLS-1$
            {
                filteredNonFatals.remove(0);
            }

            if (!filteredNonFatals.isEmpty()) {
                final IStatus[] filteredStatuses = new IStatus[filteredNonFatals.size()];

                for (int i = 0; i < filteredNonFatals.size(); i++) {
                    filteredStatuses[i] =
                        NonFatalCommandHelper.getStatusFromNonFatal(filteredNonFatals.get(i), severity);
                }

                final String messageFormat = Messages.getString("UnshelveCommand.WarningTextFormat"); //$NON-NLS-1$
                final String message = MessageFormat.format(messageFormat, name);

                nonFatalStatus = new MultiStatus(TFSCommonClientPlugin.PLUGIN_ID, 0, filteredStatuses, message, null);
            }
        }

        if (nonFatalStatus != null || pendingChangeHelper.hasWarnings()) {
            final int severity = (shelveset != null) ? Status.WARNING : Status.ERROR;

            String description;
            if (deleteShelveset) {
                final String messageFormat = Messages.getString("UnshelveCommand.WarningNotDeletedTextFormat"); //$NON-NLS-1$
                description = MessageFormat.format(messageFormat, name);
            } else {
                final String messageFormat = Messages.getString("UnshelveCommand.WarningTextFormat"); //$NON-NLS-1$
                description = MessageFormat.format(messageFormat, name);
            }

            return StatusHelper.combine(
                TFSCommonClientPlugin.PLUGIN_ID,
                0,
                description,
                nonFatalStatus,
                pendingChangeHelper.getMultiStatus(severity, description));
        }

        if (queryPendingChanges) {
            final QueryShelvedChangesCommand pendingChangesCommand =
                new QueryShelvedChangesCommand(repository, shelveset, itemSpecs, false);

            final SubProgressMonitor pendingChangesMonitor = new SubProgressMonitor(progressMonitor, 1);
            final IStatus pendingChangesStatus = pendingChangesCommand.run(pendingChangesMonitor);

            if (!pendingChangesStatus.isOK()) {
                return pendingChangesStatus;
            }

            pendingChanges = pendingChangesCommand.getPendingChanges();
        }

        if (hasConflicts == false && queryConflicts && pendingChangeHelper.hasConflicts()) {
            final QueryShelvesetConflictsCommand conflictsCommand =
                new QueryShelvesetConflictsCommand(repository, name, ownerName);

            final SubProgressMonitor conflictsMonitor = new SubProgressMonitor(progressMonitor, 1);
            final IStatus conflictsStatus = conflictsCommand.run(conflictsMonitor);

            if (!conflictsStatus.isOK()) {
                return conflictsStatus;
            }

            hasConflicts = true;
            conflictDescriptions = conflictsCommand.getConflictDescriptions();

            return Status.OK_STATUS;
        }

        if (!pendingChangeHelper.hasConflicts() && deleteShelveset) {
            try {
                final String messageFormat = Messages.getString("UnshelveCommand.ProgressTextFormat"); //$NON-NLS-1$
                final String message = MessageFormat.format(messageFormat, name);
                progressMonitor.subTask(message);
                workspace.getClient().deleteShelveset(name, ownerName);
            } finally {
                progressMonitor.worked(1);
            }
        }
        progressMonitor.done();
        return Status.OK_STATUS;
    }

    public GetStatus getStatus() {
        return status;
    }

    public Shelveset getShelveset() {
        return shelveset;
    }

    public PendingChange[] getPendingChanges() {
        return pendingChanges;
    }

    public boolean hasConflicts() {
        return hasConflicts;
    }

    public ConflictDescription[] getConflictDescriptions() {
        return conflictDescriptions;
    }

    public boolean hasUnresolvedConflicts() {
        return hasUnresolvedConflicts;
    }
}
