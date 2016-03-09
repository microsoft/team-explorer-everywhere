// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.pendingcheckin.filters;

import java.util.ArrayList;
import java.util.List;

import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PendingChange;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;
import com.microsoft.tfs.core.pendingcheckin.PendingCheckinPendingChanges;
import com.microsoft.tfs.core.pendingcheckin.events.AffectedTeamProjectsChangedListener;
import com.microsoft.tfs.core.pendingcheckin.events.CheckedPendingChangesChangedListener;
import com.microsoft.tfs.core.pendingcheckin.events.CommentChangedListener;
import com.microsoft.tfs.util.Check;

/**
 * <p>
 * A {@link PendingCheckinPendingChanges} that wraps another
 * {@link PendingCheckinPendingChanges} and filters its pending changes before
 * they are returned by its get methods.
 * </p>
 *
 * @since TEE-SDK-10.1
 * @threadsafety immutable
 */
public final class FilterPendingCheckinPendingChanges implements PendingCheckinPendingChanges {
    private final PendingChangeFilter filter;
    private final PendingCheckinPendingChanges realPendingChanges;

    public FilterPendingCheckinPendingChanges(
        final PendingCheckinPendingChanges realPendingChanges,
        final PendingChangeFilter filter) {
        Check.notNull(realPendingChanges, "realPendingChanges"); //$NON-NLS-1$
        Check.notNull(filter, "filter"); //$NON-NLS-1$

        this.realPendingChanges = realPendingChanges;
        this.filter = filter;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PendingChange[] getCheckedPendingChanges() {
        final PendingChange[] original = realPendingChanges.getCheckedPendingChanges();

        final List changes = new ArrayList();
        for (int i = 0; i < original.length; i++) {
            if (filter.passes(original[i])) {
                changes.add(original[i]);
            }
        }

        return (PendingChange[]) changes.toArray(new PendingChange[changes.size()]);
    }

    // The rest are simple delegates.

    /**
     * {@inheritDoc}
     */
    @Override
    public void addAffectedTeamProjectsChangedListener(final AffectedTeamProjectsChangedListener listener) {
        realPendingChanges.addAffectedTeamProjectsChangedListener(listener);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addCheckedPendingChangesChangedListener(final CheckedPendingChangesChangedListener listener) {
        realPendingChanges.addCheckedPendingChangesChangedListener(listener);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addCommentChangedListener(final CommentChangedListener listener) {
        realPendingChanges.addCommentChangedListener(listener);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(final Object o) {
        // TODO integrate our filtered changes with the delegate.
        return realPendingChanges.equals(o);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String[] getAffectedTeamProjectPaths() {
        return realPendingChanges.getAffectedTeamProjectPaths();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PendingChange[] getAllPendingChanges() {
        return realPendingChanges.getAllPendingChanges();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getComment() {
        return realPendingChanges.getComment();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Workspace getWorkspace() {
        return realPendingChanges.getWorkspace();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        // TODO integrate our filtered changes with the delegate.
        return realPendingChanges.hashCode();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeAffectedTeamProjectsChangedListener(final AffectedTeamProjectsChangedListener listener) {
        realPendingChanges.removeAffectedTeamProjectsChangedListener(listener);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeCheckedPendingChangesChangedListener(final CheckedPendingChangesChangedListener listener) {
        realPendingChanges.removeCheckedPendingChangesChangedListener(listener);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeCommentChangedListener(final CommentChangedListener listener) {
        realPendingChanges.removeCommentChangedListener(listener);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setAllPendingChanges(final PendingChange[] changes) {
        realPendingChanges.setAllPendingChanges(changes);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setCheckedPendingChanges(final PendingChange[] changes) {
        realPendingChanges.setCheckedPendingChanges(changes);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setComment(final String comment) {
        realPendingChanges.setComment(comment);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return realPendingChanges.toString();
    }
}
