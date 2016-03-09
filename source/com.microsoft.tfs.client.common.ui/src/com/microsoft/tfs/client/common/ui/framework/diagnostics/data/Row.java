// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.framework.diagnostics.data;

public class Row {
    private final Object[] values;
    private final Object tag;

    public Row(final Object values[]) {
        this(values, null);
    }

    public Row(final Object[] values, final Object tag) {
        this.values = values;
        this.tag = tag;
    }

    public Object getTag() {
        return tag;
    }

    public Object[] getValues() {
        return values;
    }
}
