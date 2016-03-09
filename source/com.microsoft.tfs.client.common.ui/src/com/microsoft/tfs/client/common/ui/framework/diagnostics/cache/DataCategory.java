// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.framework.diagnostics.cache;

public class DataCategory implements Comparable {
    private final String id;
    private final String label;
    private final String labelNOLOC;

    DataCategory(final String id, final String label, final String labelNOLOC) {
        this.id = id;
        this.label = label;
        this.labelNOLOC = labelNOLOC;
    }

    @Override
    public int compareTo(final Object o) {
        final DataCategory other = (DataCategory) o;
        return label.compareToIgnoreCase(other.label);
    }

    public String getID() {
        return id;
    }

    public String getLabel() {
        return label;
    }

    public String getLabelNOLOC() {
        return labelNOLOC;
    }
}
