// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.metadata;

import com.microsoft.tfs.core.ws.runtime.types.AnyContentType;
import com.microsoft.tfs.util.Check;

public class MetadataUpdateResults {
    private final long proxyTime;
    private final long dbUpdateTime;
    private final AnyContentType metadataResponse;
    private final String dbStamp;

    public MetadataUpdateResults(
        final long proxyTime,
        final long dbUpdateTime,
        final AnyContentType metadataResponse,
        final String dbStamp) {
        Check.notNull(metadataResponse, "metadataResponse"); //$NON-NLS-1$
        Check.notNull(dbStamp, "dbStamp"); //$NON-NLS-1$

        this.proxyTime = proxyTime;
        this.dbUpdateTime = dbUpdateTime;
        this.metadataResponse = metadataResponse;
        this.dbStamp = dbStamp;
    }

    public String getDBStamp() {
        return dbStamp;
    }

    public long getDBUpdateTime() {
        return dbUpdateTime;
    }

    public AnyContentType getMetadataResponse() {
        return metadataResponse;
    }

    public long getProxyTime() {
        return proxyTime;
    }

    /**
     * Frees temporary resources (temporary files) associated with these
     * results. This method must be called when the results are no longer used,
     * and the results cannot be used after this method is called.
     */
    public void dispose() {
        metadataResponse.dispose();
    }
}
