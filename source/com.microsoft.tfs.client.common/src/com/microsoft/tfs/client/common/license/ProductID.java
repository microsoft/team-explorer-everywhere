// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.license;

import com.microsoft.tfs.util.Check;

public final class ProductID {
    public static final ProductID DEFAULT_PRODUCT_ID = new ProductID("01477-169-250456-016581"); //$NON-NLS-1$

    private final String id;

    ProductID(final String id) {
        Check.notNull(id, "id"); //$NON-NLS-1$

        this.id = id;
    }

    public String getID() {
        return id;
    }
}