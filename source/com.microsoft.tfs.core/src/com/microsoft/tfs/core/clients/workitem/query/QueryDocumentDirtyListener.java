// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.query;

/**
 * Listener for when a {@link QueryDocument} changes its dirty state.
 *
 * @since TEE-SDK-10.1
 */
public interface QueryDocumentDirtyListener {
    public void dirtyStateChanged(QueryDocument queryDocument);
}
