// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.framework.catalog;

import java.text.MessageFormat;

/**
 * Static class that holds all of the well-known roots of the catalog.
 *
 * @threadsafety threadsafe
 * @since TEE-SDK-10.1
 */
public class CatalogTree {

    /**
     * Organizational Nodes in the Catalog.
     */
    public static final CatalogTree ORGANIZATIONAL = new CatalogTree(0, "Organizational"); //$NON-NLS-1$

    /**
     * Infrastructure Nodes in the Catalog - not currently used.
     */
    public static final CatalogTree INFRASTRUCTURE = new CatalogTree(1, "Infrastructure"); //$NON-NLS-1$

    private final int value;
    private final String name;

    private CatalogTree(final int value, final String name) {
        this.value = value;
        this.name = name;
    }

    @Override
    public String toString() {
        return MessageFormat.format("CatalogTree [name={0}, value={1}]", name, value); //$NON-NLS-1$
    }

    public int toInt() {
        return value;
    }

    @Override
    public int hashCode() {
        return 31 * value;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final CatalogTree other = (CatalogTree) obj;
        return value == other.value;
    }

}
