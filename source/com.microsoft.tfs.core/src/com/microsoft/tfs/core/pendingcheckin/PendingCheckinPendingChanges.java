// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.pendingcheckin;

import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PendingChange;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;
import com.microsoft.tfs.core.pendingcheckin.events.AffectedTeamProjectsChangedListener;
import com.microsoft.tfs.core.pendingcheckin.events.CheckedPendingChangesChangedListener;
import com.microsoft.tfs.core.pendingcheckin.events.CommentChangedListener;

/**
 * <p>
 * Describes pending changes evaluated before a checkin.
 * </p>
 *
 * @since TEE-SDK-10.1
 * @threadsafety thread-safe
 */
public interface PendingCheckinPendingChanges {
    /**
     * Adds a listener that's invoked whenever the team projects affected by
     * this pending checkin are changed.
     *
     * @param listener
     *        the listener to add (must not be <code>null</code>)
     */
    public void addAffectedTeamProjectsChangedListener(AffectedTeamProjectsChangedListener listener);

    /**
     * Removes a listener that was previously added by
     * {@link #addAffectedTeamProjectsChangedListener(AffectedTeamProjectsChangedListener)}
     * .
     *
     * @param listener
     *        the listener to remove (must not be <code>null</code>)
     */
    public void removeAffectedTeamProjectsChangedListener(AffectedTeamProjectsChangedListener listener);

    /**
     * Adds a listener that's invoked whenever the checked pending changes in
     * the user interface are changed.
     *
     * @param listener
     *        the listener to add (must not be <code>null</code>)
     */
    public void addCheckedPendingChangesChangedListener(CheckedPendingChangesChangedListener listener);

    /**
     * Removes a listener that was previously added by
     * {@link #addCheckedPendingChangesChangedListener(CheckedPendingChangesChangedListener)}
     * .
     *
     * @param listener
     *        the listener to remove (must not be <code>null</code>)
     */
    public void removeCheckedPendingChangesChangedListener(CheckedPendingChangesChangedListener listener);

    /**
     * Adds a listener that's invoked whenever the user's check-in comment
     * changes.
     *
     * @param listener
     *        the listener to add (must not be <code>null</code>)
     */
    public void addCommentChangedListener(CommentChangedListener listener);

    /**
     * Removes a listener that was previously added by
     * {@link #addCommentChangedListener(CommentChangedListener)} .
     *
     * @param listener
     *        the listener to remove (must not be <code>null</code>)
     */
    public void removeCommentChangedListener(CommentChangedListener listener);

    /**
     * @return the team project paths that are affected by changes in this
     *         pending checkin.
     */
    public String[] getAffectedTeamProjectPaths();

    /**
     * Sets the colleciton of all pending changes.
     *
     * @param changes
     *        the changes (must not be <code>null</code>)
     */
    public void setAllPendingChanges(PendingChange[] changes);

    /**
     * @return gets all pending changes for the workspace.
     */
    public PendingChange[] getAllPendingChanges();

    /**
     * @return the pending changes that are currently "checked" in the user
     *         interface for this checkin.
     */
    public PendingChange[] getCheckedPendingChanges();

    /**
     * Sets the pending changes that are currently "checked" in the user
     * interface for this checkin.
     *
     * @param changes
     *        the changes (must not be <code>null</code>)
     */
    public void setCheckedPendingChanges(PendingChange[] changes);

    /**
     * @return the comment for this checkin.
     */
    public String getComment();

    /**
     * Sets the comment for this checkin.
     *
     * @param comment
     *        the comment (may be <code>null</code>)
     */
    public void setComment(String comment);

    /**
     * @return the workspace these changes are in.
     */
    public Workspace getWorkspace();
}
