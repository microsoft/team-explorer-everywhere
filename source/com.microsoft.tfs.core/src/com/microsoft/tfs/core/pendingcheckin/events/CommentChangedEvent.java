// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.pendingcheckin.events;

import com.microsoft.tfs.core.clients.CoreClientEvent;
import com.microsoft.tfs.core.clients.versioncontrol.events.EventSource;
import com.microsoft.tfs.core.pendingcheckin.PendingCheckinWorkItems;

/**
 * <p>
 * Event fired when the user's check-in comment text changes in a
 * {@link PendingCheckinWorkItems} object.
 * </p>
 *
 * @since TEE-SDK-10.1
 * @threadsafety immutable
 */
public class CommentChangedEvent extends CoreClientEvent {
    private final String newComment;

    /**
     * Describes a check-in comment change.
     *
     * @param newComment
     *        the user's new comment (may be <code>null</code>)
     */
    public CommentChangedEvent(final EventSource source, final String newComment) {
        super(source);

        this.newComment = newComment;
    }

    /**
     * @return the user's new check-in comment text, which may be
     *         <code>null</code>
     */
    public String getNewComment() {
        return newComment;
    }
}
