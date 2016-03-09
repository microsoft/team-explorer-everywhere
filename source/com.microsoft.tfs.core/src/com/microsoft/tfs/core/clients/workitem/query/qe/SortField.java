// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.query.qe;

/**
 * @since TEE-SDK-10.1
 */
public class SortField extends DisplayField {
    private boolean ascending;

    public SortField(final String fieldName, final boolean ascending) {
        super(fieldName, 0);
        this.ascending = ascending;
    }

    public boolean isAscending() {
        return ascending;
    }

    public void setAscending(final boolean ascending) {
        this.ascending = ascending;
    }
}
