// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.internal.fileattributes;

import com.microsoft.tfs.util.Check;

/**
 * Represents a boolean file attribute whose very existence signifies a boolean
 * "true" or "on".
 *
 * This class thread-safe.
 */
public final class BooleanFileAttribute extends FileAttributeImpl {
    /**
     * Creates a boolean file attribute with the given name. The very existence
     * of an instance of this class in a collection signifies a boolean "true"
     * or "on".
     *
     * @param name
     *        the name of this attribute (must not be <code>null</code> or
     *        empty). Must not contain the pipe character ("|").
     */
    public BooleanFileAttribute(final String name) {
        super(name);
    }

    /**
     * Parses the given serialized attribute (a string that does not contain the
     * pipe character ("|")) into a boolean file attribute.
     *
     * Leading and trailing whitespace is stripped from the attribute name.
     *
     * @param serializedAttribute
     *        the serialized attribute string. (must not be <code>null</code>)
     * @returns the parsed file attribute, or null if the serialized attribute
     *          was empty after leading and trailing whitespace was removed.
     */
    public static BooleanFileAttribute parse(String serializedAttribute) {
        Check.notNullOrEmpty(serializedAttribute, "serializedAttribute"); //$NON-NLS-1$

        serializedAttribute = serializedAttribute.trim();

        if (serializedAttribute.length() == 0) {
            return null;
        }

        return new BooleanFileAttribute(serializedAttribute);
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#toString()
     */
    @Override
    public synchronized String toString() {
        return getName();
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(final Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj instanceof BooleanFileAttribute == false) {
            return false;
        }
        if (obj == this) {
            return true;
        }

        final BooleanFileAttribute other = (BooleanFileAttribute) obj;

        return (other.getName().equals(getName()));
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        int result = 17;

        result = result * 37 + ((getName() == null) ? 0 : getName().hashCode());

        return result;
    }
}
