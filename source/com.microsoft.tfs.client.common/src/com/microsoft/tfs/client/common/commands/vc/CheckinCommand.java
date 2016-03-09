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

import com.microsoft.tfs.client.common.Messages;
import com.microsoft.tfs.client.common.TFSCommonClientPlugin;
import com.microsoft.tfs.client.common.commands.TFSConnectedCommand;
import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.core.clients.versioncontrol.CheckinFlags;
import com.microsoft.tfs.core.clients.versioncontrol.conflicts.ConflictDescription;
import com.microsoft.tfs.core.clients.versioncontrol.conflicts.ConflictDescriptionFactory;
import com.microsoft.tfs.core.clients.versioncontrol.events.ConflictEvent;
import com.microsoft.tfs.core.clients.versioncontrol.events.ConflictListener;
import com.microsoft.tfs.core.clients.versioncontrol.exceptions.ActionDeniedBySubscriberException;
import com.microsoft.tfs.core.clients.versioncontrol.exceptions.CheckinException;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Conflict;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ItemType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PendingChange;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PolicyOverrideInfo;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.RecursionType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;
import com.microsoft.tfs.core.clients.versioncontrol.specs.ItemSpec;
import com.microsoft.tfs.core.pendingcheckin.PendingCheckin;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.LocaleUtil;

/**
 * Checks in the changes in a {@link PendingCheckin} (with optional
 * {@link PolicyOverrideInfo}). Does not perform validation on the
 * {@link PendingCheckin}.
 *
 * @threadsafety thread-compatible
 */
public class CheckinCommand extends TFSConnectedCommand {
    private final CheckinConflictListener conflictListener = new CheckinConflictListener();

    private final TFSRepository repository;
    private final String author;
    private final String authorDisplayName;
    private final PendingCheckin pendingCheckin;
    private final PolicyOverrideInfo policyOverrideInfo;

    /* Whether to query conflicts that occur or not */
    private volatile boolean queryConflicts = false;
    private volatile int changeset = -1;
    private volatile boolean resolvable = false;
    private volatile boolean allResolved = false;
    private volatile CheckinFlags checkinFlags = CheckinFlags.NONE;

    private volatile ConflictDescription[] conflictDescriptions;

    public CheckinCommand(
        final TFSRepository repository,
        final PendingCheckin pendingCheckin,
        final PolicyOverrideInfo policyOverrideInfo) {
        this(repository, pendingCheckin, policyOverrideInfo, null, null);
    }

    public CheckinCommand(
        final TFSRepository repository,
        final PendingCheckin pendingCheckin,
        final PolicyOverrideInfo policyOverrideInfo,
        final String author,
        final String authorDisplayName) {
        Check.notNull(repository, "repository"); //$NON-NLS-1$
        Check.notNull(pendingCheckin, "pendingCheckin"); //$NON-NLS-1$

        this.repository = repository;
        this.author = author;
        this.authorDisplayName = authorDisplayName;
        this.pendingCheckin = pendingCheckin;
        this.policyOverrideInfo = policyOverrideInfo;

        setConnection(repository.getConnection());
        setCancellable(true);
    }

    @Override
    public String getName() {
        return Messages.getString("CheckinCommand.CommandText"); //$NON-NLS-1$
    }

    @Override
    public String getErrorDescription() {
        return Messages.getString("CheckinCommand.ErrorText"); //$NON-NLS-1$
    }

    @Override
    public String getLoggingDescription() {
        return Messages.getString("CheckinCommand.CommandText", LocaleUtil.ROOT); //$NON-NLS-1$
    }

    public void setOverrideGatedCheckinOption() {
        checkinFlags = checkinFlags.combine(CheckinFlags.OVERRIDE_GATED_CHECK_IN);
    }

    /**
     * Sets whether checkin conflicts should be autoresolved (defaults to true).
     *
     * @param autoResolveConflicts
     *        true to auto-resolve conflicts, false to not
     */
    public void setAutoResolveConflicts(final boolean autoResolveConflicts) {
        if (autoResolveConflicts) {
            checkinFlags = checkinFlags.remove(CheckinFlags.NO_AUTO_RESOLVE);
        } else {
            checkinFlags = checkinFlags.combine(CheckinFlags.NO_AUTO_RESOLVE);
        }
    }

    public void setQueryConflicts(final boolean queryConflicts) {
        this.queryConflicts = queryConflicts;
    }

    @Override
    protected IStatus doRun(final IProgressMonitor progressMonitor) throws Exception {
        IStatus status = Status.OK_STATUS;
        final Workspace workspace = repository.getWorkspace();

        try {
            workspace.getClient().getEventEngine().addConflictListener(conflictListener);

            changeset = workspace.checkIn(
                pendingCheckin.getPendingChanges().getCheckedPendingChanges(),
                author,
                authorDisplayName,
                pendingCheckin.getPendingChanges().getComment(),
                pendingCheckin.getCheckinNotes().getCheckinNotes(),
                pendingCheckin.getWorkItems().getCheckedWorkItems(),
                policyOverrideInfo,
                checkinFlags);
        } catch (final ActionDeniedBySubscriberException e) {
            if (progressMonitor.isCanceled()) {
                return Status.CANCEL_STATUS;
            }

            status = new Status(IStatus.ERROR, TFSCommonClientPlugin.PLUGIN_ID, 0, e.getLocalizedMessage(), e);
        } catch (final CheckinException e) {
            if (progressMonitor.isCanceled()) {
                return Status.CANCEL_STATUS;
            }

            if (e.allConflictsResolved()) {
                allResolved = true;
            }
            /* We have resolvable conflicts */
            else if (e.isAnyResolvable()) {
                resolvable = true;
            }

            status = createConflictStatus(e);
        } finally {
            workspace.getClient().getEventEngine().removeConflictListener(conflictListener);
        }

        if (progressMonitor.isCanceled()) {
            return Status.CANCEL_STATUS;
        }

        if (!allResolved && resolvable && queryConflicts) {
            progressMonitor.subTask(Messages.getString("CheckinCommand.ProgressLabelQueryConflicts")); //$NON-NLS-1$
            queryConflicts(workspace);
        }

        return status;
    }

    private IStatus createConflictStatus(final CheckinException exception) {
        final ConflictEvent[] conflictEvents = conflictListener.getConflictEvents();

        if (conflictEvents.length == 0) {
            return new Status(
                IStatus.ERROR,
                TFSCommonClientPlugin.PLUGIN_ID,
                0,
                exception.getLocalizedMessage(),
                exception);
        }

        final List<IStatus> statusList = new ArrayList<IStatus>();
        for (int i = 0; i < conflictEvents.length; i++) {
            if (conflictEvents[i].getMessage().startsWith("TF203011")) //$NON-NLS-1$
            {
                final String messageFormat = Messages.getString("CheckinCommand.DownloadConflictFormat"); //$NON-NLS-1$
                final String message = MessageFormat.format(messageFormat, conflictEvents[i].getServerItem());
                statusList.add(new Status(IStatus.ERROR, TFSCommonClientPlugin.PLUGIN_ID, 0, message, null));
            } else {
                statusList.add(
                    new Status(
                        IStatus.ERROR,
                        TFSCommonClientPlugin.PLUGIN_ID,
                        0,
                        conflictEvents[i].getMessage(),
                        null));
            }
        }

        if (statusList.size() == 0) {
            return new Status(
                IStatus.ERROR,
                TFSCommonClientPlugin.PLUGIN_ID,
                0,
                Messages.getString("CheckinCommand.UnknownExceptionOccurred"), //$NON-NLS-1$
                null);
        } else if (statusList.size() == 1) {
            return statusList.get(0);
        }

        return new MultiStatus(
            TFSCommonClientPlugin.PLUGIN_ID,
            0,
            statusList.toArray(new IStatus[statusList.size()]),
            Messages.getString("CheckinCommand.ProblemsPreventedCheckin"), //$NON-NLS-1$
            null);
    }

    private void queryConflicts(final Workspace workspace) {
        final List<ItemSpec> itemSpecList = new ArrayList<ItemSpec>();

        final PendingChange[] pendingChanges = pendingCheckin.getPendingChanges().getCheckedPendingChanges();
        for (int i = 0; i < pendingChanges.length; i++) {
            itemSpecList.add(
                new ItemSpec(
                    pendingChanges[i].getServerItem(),
                    pendingChanges[i].getItemType() == ItemType.FILE ? RecursionType.NONE : RecursionType.FULL));
        }

        final ItemSpec[] queryItemSpecs = itemSpecList.toArray(new ItemSpec[itemSpecList.size()]);

        final Conflict[] conflicts = workspace.queryConflicts(queryItemSpecs);
        conflictDescriptions = ConflictDescriptionFactory.getConflictDescriptions(workspace, conflicts, queryItemSpecs);
    }

    public int getChangeset() {
        return changeset;
    }

    public boolean hasResolvableConflicts() {
        return resolvable;
    }

    public boolean allConflictsResolved() {
        return allResolved;
    }

    public ConflictDescription[] getCheckinConflicts() {
        return conflictDescriptions;
    }

    /*
     * A simple listener to handle conflicts for our checkin. Note that this
     * class is not thread safe. Be sure to hook / unhook this before calling
     * getConflictEvents().
     */
    private class CheckinConflictListener implements ConflictListener {
        private final List<ConflictEvent> conflictEventList = new ArrayList<ConflictEvent>();
        private ConflictEvent[] conflictEvents;

        @Override
        public void onConflict(final ConflictEvent e) {
            conflictEventList.add(e);
        }

        public ConflictEvent[] getConflictEvents() {
            if (conflictEvents == null) {
                conflictEvents = conflictEventList.toArray(new ConflictEvent[conflictEventList.size()]);
            }

            return conflictEvents;
        }
    }
}
