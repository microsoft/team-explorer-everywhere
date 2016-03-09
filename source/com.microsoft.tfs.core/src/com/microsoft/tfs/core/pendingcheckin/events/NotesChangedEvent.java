// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.pendingcheckin.events;

import com.microsoft.tfs.core.clients.CoreClientEvent;
import com.microsoft.tfs.core.clients.versioncontrol.events.EventSource;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.CheckinNote;
import com.microsoft.tfs.core.pendingcheckin.PendingCheckinNotes;
import com.microsoft.tfs.util.Check;

/**
 * <p>
 * Event fired when the user's check-in notes change in a
 * {@link PendingCheckinNotes} object.
 * </p>
 *
 * @since TEE-SDK-10.1
 * @threadsafety thread-compatible
 */
public class NotesChangedEvent extends CoreClientEvent {
    private final CheckinNote checkinNote;

    /**
     * Describes a check-in note change.
     *
     * @param checkinNote
     *        the user's new check-in note (may be <code>null</code>)
     */
    public NotesChangedEvent(final EventSource source, final CheckinNote checkinNote) {
        super(source);
        Check.notNull(checkinNote, "checkinNote"); //$NON-NLS-1$
        this.checkinNote = checkinNote;
    }

    /**
     * @return the new {@link CheckinNote} (never <code>null</code>)
     */
    public CheckinNote getCheckinNote() {
        return checkinNote;
    }
}
