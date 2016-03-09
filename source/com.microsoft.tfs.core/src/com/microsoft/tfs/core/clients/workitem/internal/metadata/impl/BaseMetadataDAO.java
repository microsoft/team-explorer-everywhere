// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.metadata.impl;

import com.microsoft.tfs.core.clients.workitem.internal.metadata.Metadata;
import com.microsoft.tfs.core.internal.db.dao.BaseDAOImplementation;

public class BaseMetadataDAO extends BaseDAOImplementation {
    private Metadata metadata;

    public void setMetadata(final Metadata metadata) {
        this.metadata = metadata;
    }

    protected Metadata getMetadata() {
        return metadata;
    }
}
