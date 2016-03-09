// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.framework.helper;

import java.util.Comparator;
import java.util.Date;

public abstract class ElementDateComparator implements Comparator {
    @Override
    public int compare(final Object o1, final Object o2) {
        final Date d1 = getDate(o1);
        final Date d2 = getDate(o2);

        if (d1 == null) {
            return d2 == null ? 0 : -1;
        }

        if (d2 == null) {
            return 1;
        }

        return d1.compareTo(d2);
    }

    protected abstract Date getDate(Object element);
}
