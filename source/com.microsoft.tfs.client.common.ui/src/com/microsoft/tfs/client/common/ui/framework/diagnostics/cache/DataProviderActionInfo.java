// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.framework.diagnostics.cache;

import com.microsoft.tfs.client.common.ui.framework.diagnostics.extend.DataProviderAction;

public class DataProviderActionInfo implements Comparable {
    private final String id;
    private final String label;
    private final DataProviderAction action;

    public DataProviderActionInfo(final String id, final String label, final DataProviderAction action) {
        this.id = id;
        this.label = label;
        this.action = action;
    }

    @Override
    public int compareTo(final Object o) {
        final DataProviderActionInfo other = (DataProviderActionInfo) o;
        return label.compareToIgnoreCase(other.label);
    }

    public DataProviderAction getAction() {
        return action;
    }

    public String getID() {
        return id;
    }

    public String getLabel() {
        return label;
    }
}
