// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.framework.table;

/**
 * {@link ElementCollectionType} is a typesafe enum class. Each instance of
 * {@link ElementCollectionType} represents one of the types of element
 * collections that {@link TableControl} tracks.
 */
public class ElementCollectionType {
    /**
     * Represents the collection of all elements currently in the table.
     */
    public static final ElementCollectionType ALL_ELEMENTS = new ElementCollectionType("ALL_ELEMENTS"); //$NON-NLS-1$

    /**
     * Represents the collection of elements currently selected in the table.
     */
    public static final ElementCollectionType SELECTED_ELEMENTS = new ElementCollectionType("SELECTED_ELEMENTS"); //$NON-NLS-1$

    /**
     * Represents the collection of elements currently checked in the table.
     */
    public static final ElementCollectionType CHECKED_ELEMENTS = new ElementCollectionType("CHECKED_ELEMENTS"); //$NON-NLS-1$

    private final String s;

    private ElementCollectionType(final String s) {
        this.s = s;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return s;
    }
}
