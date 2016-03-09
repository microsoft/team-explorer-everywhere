// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.pendingcheckin;

import com.microsoft.tfs.core.clients.versioncontrol.events.EventSource;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PendingChange;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;
import com.microsoft.tfs.core.pendingcheckin.events.AffectedTeamProjectsChangedEvent;
import com.microsoft.tfs.core.pendingcheckin.events.AffectedTeamProjectsChangedListener;
import com.microsoft.tfs.core.pendingcheckin.events.CheckedPendingChangesChangedEvent;
import com.microsoft.tfs.core.pendingcheckin.events.CheckedPendingChangesChangedListener;
import com.microsoft.tfs.core.pendingcheckin.events.CommentChangedEvent;
import com.microsoft.tfs.core.pendingcheckin.events.CommentChangedListener;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.listeners.SingleListenerFacade;

/**
 * <p>
 * Standard implementation of {@link PendingCheckinPendingChanges}.
 * </p>
 * <p>
 * The {@link AffectedTeamProjects} object given during construction will be
 * updated by calls to {@link #setCheckedPendingChanges(PendingChange[])}, so
 * callers can check this collection directly, or must construct a new object if
 * they do not want theirs modified.
 * </p>
 *
 * @since TEE-SDK-10.1
 * @threadsafety thread-safe
 */
public class StandardPendingCheckinPendingChanges implements PendingCheckinPendingChanges {
    private final SingleListenerFacade checkedPendingChangesChangedEventListeners =
        new SingleListenerFacade(CheckedPendingChangesChangedListener.class);

    private final SingleListenerFacade affectedTeamProjectsChangedEventListeners =
        new SingleListenerFacade(AffectedTeamProjectsChangedListener.class);

    private final SingleListenerFacade commentChangedEventListeners =
        new SingleListenerFacade(CommentChangedListener.class);

    private final AffectedTeamProjects affectedTeamProjects;

    private PendingChange[] allChanges;
    private PendingChange[] checkedChanges;
    private String comment;
    private final Workspace workspace;

    /**
     * The {@link AffectedTeamProjects} object given will be modified. See class
     * notes.
     *
     * @param workspace
     *        the workspace these changes are for (must not be <code>null</code>
     *        )
     * @param allChanges
     *        all pending changes for this workspace (must not be
     *        <code>null</code>)
     * @param checkedChanges
     *        the pending changes that are "checked" in the user interface for
     *        checkin (must not be <code>null</code>)
     * @param affectedTeamProjects
     *        the team projects affected by this checkin (must not be
     *        <code>null</code>)
     * @param comment
     *        the comment (may be null).
     */
    public StandardPendingCheckinPendingChanges(
        final Workspace workspace,
        final PendingChange[] allChanges,
        final PendingChange[] checkedChanges,
        final AffectedTeamProjects affectedTeamProjects,
        final String comment) {
        Check.notNull(workspace, "workspace"); //$NON-NLS-1$
        Check.notNull(affectedTeamProjects, "affectedTeamProjects"); //$NON-NLS-1$
        Check.notNull(allChanges, "allChanges"); //$NON-NLS-1$
        Check.notNull(checkedChanges, "checkedChanges"); //$NON-NLS-1$

        this.workspace = workspace;
        this.affectedTeamProjects = affectedTeamProjects;
        this.allChanges = allChanges;
        this.checkedChanges = checkedChanges;
        this.comment = comment;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addAffectedTeamProjectsChangedListener(final AffectedTeamProjectsChangedListener listener) {
        affectedTeamProjectsChangedEventListeners.addListener(listener);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeAffectedTeamProjectsChangedListener(final AffectedTeamProjectsChangedListener listener) {
        affectedTeamProjectsChangedEventListeners.removeListener(listener);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addCheckedPendingChangesChangedListener(final CheckedPendingChangesChangedListener listener) {
        checkedPendingChangesChangedEventListeners.addListener(listener);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeCheckedPendingChangesChangedListener(final CheckedPendingChangesChangedListener listener) {
        checkedPendingChangesChangedEventListeners.removeListener(listener);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addCommentChangedListener(final CommentChangedListener listener) {
        commentChangedEventListeners.addListener(listener);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeCommentChangedListener(final CommentChangedListener listener) {
        commentChangedEventListeners.removeListener(listener);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized String[] getAffectedTeamProjectPaths() {
        return affectedTeamProjects.getTeamProjectPaths();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized PendingChange[] getAllPendingChanges() {
        return allChanges;
    }

    /**
     * Sets all pending changes for this checkin.
     *
     * @param changes
     *        all pending changes for this checkin (must not be
     *        <code>null</code>)
     */
    @Override
    public synchronized void setAllPendingChanges(final PendingChange[] changes) {
        Check.notNull(changes, "changes"); //$NON-NLS-1$
        allChanges = changes;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized PendingChange[] getCheckedPendingChanges() {
        return checkedChanges;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void setCheckedPendingChanges(final PendingChange[] changes) {
        Check.notNull(changes, "changes"); //$NON-NLS-1$

        checkedChanges = changes;

        /*
         * Notify listeners that checked pending changes have changed.
         */

        ((CheckedPendingChangesChangedListener) checkedPendingChangesChangedEventListeners.getListener()).onCheckedPendingChangesChanged(
            new CheckedPendingChangesChangedEvent(EventSource.newFromHere()));

        if (affectedTeamProjects.set(changes)) {
            final String[] teamProjectServerPaths = affectedTeamProjects.getTeamProjectPaths();

            ((AffectedTeamProjectsChangedListener) affectedTeamProjectsChangedEventListeners.getListener()).onAffectedTeamProjectsChanged(
                new AffectedTeamProjectsChangedEvent(EventSource.newFromHere(), teamProjectServerPaths));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized String getComment() {
        return comment;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void setComment(final String comment) {
        this.comment = comment;

        ((CommentChangedListener) commentChangedEventListeners.getListener()).onCommentChanged(
            new CommentChangedEvent(EventSource.newFromHere(), comment));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized Workspace getWorkspace() {
        return workspace;
    }
}