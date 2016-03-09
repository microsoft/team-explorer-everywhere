// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.build.flags;

/**
 * Describes the workspace mapping depth.
 *
 * @since TEE-SDK-10.1
 */
public class WorkspaceMappingDepth {
    public static final WorkspaceMappingDepth FULL = new WorkspaceMappingDepth(120, "Full"); //$NON-NLS-1$
    public static final WorkspaceMappingDepth ONE_LEVEL = new WorkspaceMappingDepth(1, "One Level"); //$NON-NLS-1$

    private final int value;
    private final String description;

    private WorkspaceMappingDepth(final int value, final String description) {
        this.value = value;
        this.description = description;
    }

    /**
     * Convert an integer value of the {@link WorkspaceMappingDepth} into the
     * corresponding strongly typed {@link WorkspaceMappingDepth}
     *
     * @param value
     *        the numeric value
     * @return the {@link WorkspaceMappingDepth} that matches the value or null
     *         if there was no match
     */
    public static final WorkspaceMappingDepth fromValue(final int value) {
        // Microsoft API defaults mapping depth to FULL.
        if (value == 0 || value == FULL.getValue()) {
            return FULL;
        }
        if (value == ONE_LEVEL.getValue()) {
            return ONE_LEVEL;
        }
        return null;
    }

    /**
     * @return the integer value associated with this workspace mapping depth.
     */
    public int getValue() {
        return value;
    }

    /**
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(final Object other) {
        if (other == null || !(other instanceof WorkspaceMappingDepth)) {
            return false;
        }
        return value == ((WorkspaceMappingDepth) other).getValue();
    }

    @Override
    public String toString() {
        return description;
    }

}
