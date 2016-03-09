// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.framework.helper;

import java.util.Comparator;

public abstract class ElementLongComparator implements Comparator {
    @Override
    public int compare(final Object o1, final Object o2) {
        final long l1 = getLong(o1);
        final long l2 = getLong(o2);

        if (l1 == l2) {
            return 0;
        }

        return (l1 < l2) ? -1 : 1;
    }

    protected abstract long getLong(Object element);
}
