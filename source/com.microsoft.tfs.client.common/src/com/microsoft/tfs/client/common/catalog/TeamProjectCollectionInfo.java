// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.catalog;

import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.GUID;

public final class TeamProjectCollectionInfo {
    private final GUID identifier;
    private final String displayName;
    private final String description;

    public TeamProjectCollectionInfo(final GUID identifier, final String displayName, final String description) {
        Check.notNull(identifier, "identifier"); //$NON-NLS-1$
        Check.notNull(displayName, "displayName"); //$NON-NLS-1$

        this.identifier = identifier;
        this.displayName = displayName;
        this.description = description;
    }

    /**
     * The unique identifier for this Team Project Collection.
     *
     * @return the GUID for this project collection (never <code>null</code>)
     */
    public GUID getIdentifier() {
        return identifier;
    }

    /**
     * The display name for this Team Project Collection.
     *
     * @return the display name for this project collection (never
     *         <code>null</code>)
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * The description for this Team Project Collection, or <code>null</code> if
     * there is none.
     *
     * @return the description for this project collection (may be
     *         <code>null</code>)
     */
    public String getDescription() {
        return description;
    }
}