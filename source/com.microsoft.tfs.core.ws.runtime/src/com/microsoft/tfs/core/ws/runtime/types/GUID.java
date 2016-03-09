// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.ws.runtime.types;

import com.microsoft.tfs.util.Check;

/**
 * A globally unique identifier.
 *
 * @threadsafety immutable
 */
public class GUID {
    private final String guidString;

    /**
     * Creates a {@link GUID}
     *
     * @param guidString
     */
    public GUID(final String guidString) {
        Check.notNull(guidString, "guidString"); //$NON-NLS-1$

        this.guidString = guidString;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return guidString;
    }
}
