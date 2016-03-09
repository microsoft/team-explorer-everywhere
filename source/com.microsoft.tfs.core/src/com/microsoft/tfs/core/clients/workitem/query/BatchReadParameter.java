// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.query;

/**
 * Represents an ID and revision pair that will be added to a
 * {@link BatchReadParameterCollection}.
 *
 * @since TEE-SDK-10.1
 */
public class BatchReadParameter {
    private final int id;
    private final int rev;

    public BatchReadParameter(final int id) {
        this(id, -1);
    }

    public BatchReadParameter(final int id, final int revision) {
        if (id <= 0) {
            throw new IllegalArgumentException("id must be greater than 0"); //$NON-NLS-1$
        }

        if (revision < -1 || revision == 0) {
            throw new IllegalArgumentException("rev must be greater than 0 or -1"); //$NON-NLS-1$
        }

        this.id = id;
        rev = revision;
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof BatchReadParameter) {
            final BatchReadParameter other = (BatchReadParameter) obj;
            return id == other.id;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return id;
    }

    public int getID() {
        return id;
    }

    public int getRev() {
        return rev;
    }
}
