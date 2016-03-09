// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.commonstructure;

import java.text.MessageFormat;

import com.microsoft.tfs.util.Check;

/**
 * <p>
 * Enumermation of known CssStructureTypes.
 * </p>
 *
 * @since TEE-SDK-10.1
 * @threadsafety immutable
 */
public class CSSStructureType {
    /**
     * Areas.
     */
    public static final CSSStructureType PROJECT_MODEL_HIERARCHY = new CSSStructureType("ProjectModelHierarchy"); //$NON-NLS-1$

    /**
     * Iterations.
     */
    public static final CSSStructureType PROJECT_LIFECYCLE = new CSSStructureType("ProjectLifecycle"); //$NON-NLS-1$

    /**
     * Resolves the given string to a {@link CSSStructureType}, or throws
     * {@link IllegalArgumentException} if the type is unknown.
     *
     * @param structureType
     *        the structure type string (must not be <code>null</code>)
     * @return the {@link CSSStructureType} that matches the given string
     */
    public static CSSStructureType fromString(final String structureType) {
        Check.notNull(structureType, "structureType"); //$NON-NLS-1$

        if (PROJECT_MODEL_HIERARCHY.getStructureType().equals(structureType)) {
            return PROJECT_MODEL_HIERARCHY;
        }
        if (PROJECT_LIFECYCLE.getStructureType().equals(structureType)) {
            return PROJECT_LIFECYCLE;
        }
        throw new IllegalArgumentException(MessageFormat.format("Unkown structure type \"{0}\"", structureType)); //$NON-NLS-1$
    }

    private final String structureType;

    private CSSStructureType(final String structureType) {
        this.structureType = structureType;
    }

    public String getStructureType() {
        return structureType;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return structureType;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return structureType.hashCode();
    }

    /**
     * {@inheritDoc}
     */
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
        return structureType.equals(obj.toString());
    }
}
