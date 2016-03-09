// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.pendingcheckin.events;

import java.util.EventListener;

import com.microsoft.tfs.core.pendingcheckin.PendingCheckinNotes;

/**
 * <p>
 * Defines an interface for listeners of the {@link NotesChangedEvent}.
 * </p>
 *
 * @since TEE-SDK-10.1
 * @threadsafety thread-safe
 */
public interface NotesChangedListener extends EventListener {
    /**
     * Invoked when the user's check-in notes change in a
     * {@link PendingCheckinNotes} object.
     *
     * @param e
     *        the event that describes the changes.
     */
    public void onNotesChanged(NotesChangedEvent e);
}
