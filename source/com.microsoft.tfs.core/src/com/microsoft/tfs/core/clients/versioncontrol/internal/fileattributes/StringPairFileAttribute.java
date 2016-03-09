// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.internal.fileattributes;

import com.microsoft.tfs.util.Check;

/**
 * Represents a file attribute that pairs two arbitrary strings. The first
 * string is the short name and the second is any string that does not contain a
 * pipe character ("|").
 *
 * This class thread-safe.
 */
public final class StringPairFileAttribute extends FileAttributeImpl {
    private String value;

    /**
     * Separates the key from the value in the serialized version of this class.
     */
    public final static char SEPARATOR = '=';

    /**
     * Creates a string pair file attribute of the given name with the given
     * string value.
     *
     * @param name
     *        the name of this attribute (must not be <code>null</code> or
     *        empty). Must not contain the equals character ("=") or the pipe
     *        character ("|").
     * @param value
     *        the string value of this attribute. Must not contain a pipe
     *        character ("|"). (must not be <code>null</code>)
     */
    public StringPairFileAttribute(final String name, final String value) {
        super(name);
        Check.notNull(value, "value"); //$NON-NLS-1$
        this.value = value;
    }

    /**
     * Tests whether the given string looks like a serialized string pair file
     * attribute (contains a separator, "=").
     *
     * @param serializedAttribute
     *        the serialized attribute to test (must not be <code>null</code>)
     * @return true if the given string contains the separator, false if not.
     */
    public static boolean looksLikeStringPairFileAttribute(final String serializedAttribute) {
        Check.notNull(serializedAttribute, "serializedAttribute"); //$NON-NLS-1$

        return serializedAttribute.indexOf(SEPARATOR) != -1;
    }

    /**
     * Parses the given serialized attribute (a string consisting of text like
     * "name=value" or "zap=" or "baz") into a string pair file attribute. If
     * there is no text after the equals, the value member is an empty string.
     * If there is no equals, the entire string is the short name.
     *
     * Leading and trailing whitespace is stripped from both key and value
     * before being tested for emptyness.
     *
     * @param serializedAttribute
     *        the serialized attribute string (like "name=value" or "zap=" or
     *        "baz"). If <code>null</code> or empty, null is returned.
     * @returns the parsed file attribute, or null if the given serialized
     *          attribute was <code>null</code> or empty, or if the attribute
     *          started with an equals.
     */
    public static StringPairFileAttribute parse(final String serializedAttribute) {
        if (serializedAttribute == null || serializedAttribute.length() == 0) {
            return null;
        }

        /*
         * Split on the first separator sign and parse the two sides.
         */
        final int separatorIndex = serializedAttribute.indexOf(SEPARATOR);
        if (separatorIndex < 0) {
            return new StringPairFileAttribute(serializedAttribute.trim(), ""); //$NON-NLS-1$
        }

        final String keyString = serializedAttribute.substring(0, separatorIndex).trim();
        if (keyString.length() == 0) {
            return null;
        }

        final String valueString = serializedAttribute.substring(separatorIndex + 1);
        return new StringPairFileAttribute(keyString, valueString.trim());
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#toString()
     */
    @Override
    public synchronized String toString() {
        return getName() + SEPARATOR + ((value != null) ? value.toString() : ""); //$NON-NLS-1$
    }

    /**
     * @return this attribute's string value.
     */
    public String getValue() {
        return value;
    }

    /**
     * @param value
     *        the attribute's string value.
     */
    public synchronized void setValue(final String value) {
        Check.notNull(value, "value"); //$NON-NLS-1$
        this.value = value;
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
        if (obj instanceof StringPairFileAttribute == false) {
            return false;
        }
        if (obj == this) {
            return true;
        }

        final StringPairFileAttribute other = (StringPairFileAttribute) obj;

        return (other.getName().equals(getName()) && other.getValue().equals(getValue()));
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
        result = result * 37 + ((getValue() == null) ? 0 : getValue().hashCode());

        return result;
    }
}
