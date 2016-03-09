// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.build.flags;

import com.microsoft.tfs.core.internal.wrappers.EnumerationWrapper;

import ms.tfs.build.buildservice._04._DefinitionQueueStatus;

public class DefinitionQueueStatus extends EnumerationWrapper {
    public static final DefinitionQueueStatus ENABLED = new DefinitionQueueStatus(_DefinitionQueueStatus.Enabled);
    public static final DefinitionQueueStatus PAUSED = new DefinitionQueueStatus(_DefinitionQueueStatus.Paused);
    public static final DefinitionQueueStatus DISABLED = new DefinitionQueueStatus(_DefinitionQueueStatus.Disabled);

    private DefinitionQueueStatus(final _DefinitionQueueStatus status) {
        super(status);
    }

    public _DefinitionQueueStatus getWebServiceObject() {
        return (_DefinitionQueueStatus) this.webServiceObject;
    }

    public static DefinitionQueueStatus fromWebServiceObject(final _DefinitionQueueStatus webServiceObject) {
        if (webServiceObject == null) {
            return null;
        }
        return (DefinitionQueueStatus) EnumerationWrapper.fromWebServiceObject(webServiceObject);
    }

    @Override
    public boolean equals(final Object o) {
        return super.equals(o);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }
}
