// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.framework.catalog;

import com.microsoft.tfs.util.BitField;

/**
 * Options for querying the catalog.
 *
 * @since TEE-SDK-10.1
 */
public class CatalogQueryOptions extends BitField {
    public static final CatalogQueryOptions NONE = new CatalogQueryOptions(0);
    public static final CatalogQueryOptions EXPAND_DEPENDENCIES = new CatalogQueryOptions(1);
    public static final CatalogQueryOptions INCLUDE_PARENTS = new CatalogQueryOptions(2);

    protected CatalogQueryOptions(final int value) {
        super(value);
    }

    public boolean contains(final CatalogQueryOptions other) {
        return super.containsInternal(other);
    }
}
