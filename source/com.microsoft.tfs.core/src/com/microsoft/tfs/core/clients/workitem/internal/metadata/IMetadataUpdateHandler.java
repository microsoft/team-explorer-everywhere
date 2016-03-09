// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.metadata;

import com.microsoft.tfs.core.ws.runtime.types.AnyContentType;

import ms.tfs.workitemtracking.clientservices._03._MetadataTableHaveEntry;

public interface IMetadataUpdateHandler {
    public _MetadataTableHaveEntry[] getHaveEntries();

    public void addMetadataChangeListener(IMetadataChangeListener listener);

    /**
     * Updates the metadata with the given metadata content and database stamp.
     * Registered changed listeners are notified of the updates.
     *
     * @param metadata
     *        the metadata to update from (must not be <code>null</code>)
     * @param newDbStamp
     *        the database stamp (must not be <code>null</code>)
     * @return the time it took (in milliseconds) to perform the update
     *         including invoking the change listeners
     */
    public long updateMetadata(final AnyContentType metadata, final String newDbStamp);

    /**
     * Updates the metadata from web service data. Registered change listeners
     * are notified of the updates.
     */
    public void update();

    /**
     * Like {@link #update()} but optionally returns the results for the caller
     * to inspect. If results are returned, the caller must property dispose of
     * them.
     *
     * @param wantResults
     *        if true, results are returned (and the caller must dispose them),
     *        if false null is returned
     * @return if wantResults was true, the results (see
     *         {@link MetadataUpdateResults#dispose()}, otherwise null
     */
    public MetadataUpdateResults update(boolean wantResults);
}
