// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.query;

/**
 * Listener for when a {@link QueryDocument} is saved.
 *
 * @since TEE-SDK-10.1
 */
public interface QueryDocumentSaveListener {
    public void onQueryDocumentSaved(QueryDocument queryDocument);
}
