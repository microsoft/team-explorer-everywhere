// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.framework.diagnostics.cache;

public class SupportContactCategory implements Comparable {
    private final String id;
    private final String label;

    SupportContactCategory(final String id, final String label) {
        this.id = id;
        this.label = label;
    }

    @Override
    public int compareTo(final Object o) {
        final SupportContactCategory other = (SupportContactCategory) o;
        return label.compareToIgnoreCase(other.label);
    }

    public String getID() {
        return id;
    }

    public String getLabel() {
        return label;
    }
}
