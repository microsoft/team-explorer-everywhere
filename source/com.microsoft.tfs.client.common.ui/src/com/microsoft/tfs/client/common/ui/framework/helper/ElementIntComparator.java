// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.framework.helper;

import java.util.Comparator;

public abstract class ElementIntComparator implements Comparator {
    @Override
    public int compare(final Object o1, final Object o2) {
        final int i1 = getInt(o1);
        final int i2 = getInt(o2);

        if (i1 == i2) {
            return 0;
        }

        return (i1 < i2) ? -1 : 1;
    }

    protected abstract int getInt(Object element);
}
