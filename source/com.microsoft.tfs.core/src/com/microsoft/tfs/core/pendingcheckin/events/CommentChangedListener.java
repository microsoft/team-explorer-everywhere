// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.pendingcheckin.events;

import java.util.EventListener;

import com.microsoft.tfs.core.pendingcheckin.PendingCheckinWorkItems;

/**
 * <p>
 * Defines an interface for listeners of the {@link CommentChangedEvent}.
 * </p>
 *
 * @since TEE-SDK-10.1
 * @threadsafety thread-safe
 */
public interface CommentChangedListener extends EventListener {
    /**
     * Invoked when the user's check-in comment changes
     * {@link PendingCheckinWorkItems} object.
     *
     * @param e
     *        the event that describes the changes.
     */
    public void onCommentChanged(CommentChangedEvent e);
}
