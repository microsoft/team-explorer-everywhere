// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.internal.fileattributes;

import java.util.Comparator;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.CollatorFactory;

/**
 * A file attributes collection is a set of {@link FileAttribute}s.
 */
public class FileAttributesCollection implements Iterable<FileAttribute> {
    private final Map<String, FileAttribute> attributes;

    private static final FileAttributeNameComparator NAME_COMPARATOR = new FileAttributeNameComparator();

    /**
     * Creates a new collection of file attributes from the given attributes.
     *
     * @param initialAttributes
     *        The file attributes to populate (may not be <code>null</code>)
     */
    public FileAttributesCollection(final FileAttribute[] initialAttributes) {
        Check.notNull(initialAttributes, "initialAttributes"); //$NON-NLS-1$

        attributes = new TreeMap<String, FileAttribute>(NAME_COMPARATOR);

        for (final FileAttribute attribute : initialAttributes) {
            Check.notNull(attribute, "attribute"); //$NON-NLS-1$
            attributes.put(attribute.getName(), attribute);
        }
    }

    private FileAttributesCollection(final Map<String, FileAttribute> attributes) {
        this.attributes = attributes;
    }

    /**
     * @return the count of attributes in this collection.
     */
    public int size() {
        return attributes.size();
    }

    /**
     * Tests whether this collection contains the given attribute.
     *
     * @param name
     *        the name of the file attribute to look for (must not be
     *        <code>null</code> or empty)
     * @return true if this collection contains the given attribute, false if it
     *         does not.
     */
    public boolean containsAttribute(final String name) {
        Check.notNullOrEmpty(name, "name"); //$NON-NLS-1$

        return attributes.containsKey(name);
    }

    /**
     * Tests whether this collection contains the given attribute and the given
     * attribute is a boolean attribute.
     *
     * @param name
     *        the name of the file attribute to look for (must not be
     *        <code>null</code> or empty)
     * @return true if this collection contains the given attribute, false if it
     *         does not.
     */
    public boolean containsBooleanAttribute(final String name) {
        Check.notNullOrEmpty(name, "name"); //$NON-NLS-1$

        final FileAttribute attribute = attributes.get(name);

        return (attribute != null && attribute instanceof BooleanFileAttribute);
    }

    /**
     * Gets the file attribute by the given name
     *
     * @param name
     *        the name of the file attribute to look for (must not be
     *        <code>null</code> or empty)
     * @return the given attribute or <code>null</code> if it was not specified
     */
    public FileAttribute getFileAttribute(final String name) {
        Check.notNullOrEmpty(name, "name"); //$NON-NLS-1$

        return attributes.get(name);
    }

    /**
     * Finds the given boolean file attribute in this collection.
     *
     * @param name
     *        the name of the boolean file attribute to get (must not be
     *        <code>null</code> or empty).
     * @return the matching attribute or null if none matched.
     */
    public BooleanFileAttribute getBooleanFileAttribute(final String name) {
        Check.notNullOrEmpty(name, "name"); //$NON-NLS-1$

        final FileAttribute match = attributes.get(name);

        if (match != null && match instanceof BooleanFileAttribute) {
            return (BooleanFileAttribute) match;
        }

        return null;
    }

    /**
     * Finds the given string pair file attribute in this collection by name.
     *
     * @param name
     *        the name of the string pair file attribute to get (must not be
     *        <code>null</code> or empty)
     * @return the matching attribute or null if none matched.
     */
    public StringPairFileAttribute getStringPairFileAttribute(final String name) {
        Check.notNullOrEmpty(name, "name"); //$NON-NLS-1$

        final FileAttribute match = attributes.get(name);

        if (match != null && match instanceof StringPairFileAttribute) {
            return (StringPairFileAttribute) match;
        }

        return null;
    }

    /**
     * Merges the current attributes collection with the given attributes. The
     * other attributes will be added to this collection only if they are not
     * present already.
     *
     * @param other
     *        The other attributes to merge with (may be <code>null</code>, in
     *        which case <code>this</code> is simply returned)
     * @return A collection that represents the merged collection.
     */
    public FileAttributesCollection mergeWith(final FileAttributesCollection other) {
        if (other == null || other.size() == 0) {
            return this;
        }

        final Map<String, FileAttribute> merged = new TreeMap<String, FileAttribute>(NAME_COMPARATOR);

        for (final FileAttribute attribute : attributes.values()) {
            merged.put(attribute.getName(), attribute);
        }

        for (final FileAttribute attribute : other) {
            if (!merged.containsKey(attribute.getName())) {
                merged.put(attribute.getName(), attribute);
            }
        }

        return new FileAttributesCollection(merged);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Iterator<FileAttribute> iterator() {
        return attributes.values().iterator();
    }

    private static final class FileAttributeNameComparator implements Comparator<String> {
        @Override
        public int compare(final String one, final String two) {
            return CollatorFactory.getCaseSensitiveCollator(Locale.ENGLISH).compare(one, two);
        }
    }
}
