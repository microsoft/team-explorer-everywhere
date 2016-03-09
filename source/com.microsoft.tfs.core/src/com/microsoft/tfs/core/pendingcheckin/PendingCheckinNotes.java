// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.pendingcheckin;

import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.CheckinNote;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.CheckinNoteFieldDefinition;
import com.microsoft.tfs.core.pendingcheckin.events.NotesChangedListener;

/**
 * <p>
 * Contains the check-in notes in the current pending change set.
 * </p>
 *
 * @since TEE-SDK-10.1
 * @threadsafety thread-safe
 */
public interface PendingCheckinNotes {
    /**
     * Adds a listener that's invoked whenever user's check-in notes in the user
     * interface are changed (via {@link #setCheckinNotes(CheckinNote)}.
     *
     * @param listener
     *        the listener to add (must not be <code>null</code>)
     */
    public void addNotesChangedListener(NotesChangedListener listener);

    /**
     * Removes a listener that was previously added by
     * {@link #addNotesChangedListener(NotesChangedListener)}.
     *
     * @param listener
     *        the listener to remove (must not be <code>null</code>)
     */
    public void removeNotesChangedListener(NotesChangedListener listener);

    /**
     * Evaluates the checkin notes that have been set on this object. It is up
     * to implementations of this class to provide a means of configuring the
     * TFS connection and other version control information required to evaluate
     * notes.
     *
     * @return any failures encountered during evaluation.
     */
    public CheckinNoteFailure[] evaluate();

    /**
     * @return the checkin notes to evaluate.
     */
    public CheckinNote getCheckinNotes();

    /**
     * @param note
     *        the checkin notes to evaluate (must not be <code>null</code>)
     */
    public void setCheckinNotes(CheckinNote note);

    /**
     * Gets the field definitions that the checkin note was last evaluated
     * against. If {@link #evaluate()} has not yet been called, returns null.
     *
     * @return the field definitions (only non-null after {@link #evaluate()}
     *         has been called).
     */
    public CheckinNoteFieldDefinition[] getFieldDefinitions();
}
