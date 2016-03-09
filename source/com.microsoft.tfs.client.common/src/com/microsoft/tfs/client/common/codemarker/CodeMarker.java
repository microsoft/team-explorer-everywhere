// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.codemarker;

import com.microsoft.tfs.util.Check;

/**
 * A code marker is used for client instrumentation - that is, delivery of
 * notifications from the client to the test harness when a certain state has
 * been reached. (For example, the client begins running a background job, the
 * client has finished running a background job.)
 *
 * These were designed to be delivered only to the test framework, using them
 * for intra-client communication should be considered dubious.
 */
public class CodeMarker {
    private final String codeMarkerId;

    public CodeMarker(final String codeMarkerId) {
        Check.notNull(codeMarkerId, "codeMarkerId"); //$NON-NLS-1$

        this.codeMarkerId = codeMarkerId;
    }

    public final String getCodeMarkerID() {
        return codeMarkerId;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj == null || !(obj instanceof CodeMarker)) {
            return false;
        }

        return (((CodeMarker) obj).codeMarkerId.equals(codeMarkerId));
    }

    @Override
    public int hashCode() {
        return codeMarkerId.hashCode();
    }
}
