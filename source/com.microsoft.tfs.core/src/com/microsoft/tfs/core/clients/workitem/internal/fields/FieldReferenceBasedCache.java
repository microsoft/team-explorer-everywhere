// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.fields;

import java.text.MessageFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * A general purpose cache where the key is a reference to a field definition (a
 * field def display name, field def reference name, and field def id).
 *
 * This class understands semantics of what is allowed for field definition
 * names. Specifically, objects in the cache can be retrieved by passing a
 * single string that is either a display name or reference name.
 *
 * It should be used anywhere internally int the WIT OM where there is a need to
 * look things by by field name and/or id.
 */
public class FieldReferenceBasedCache<T> {
    private final Object lock = new Object();
    private final Map<String, T> referenceNames = new HashMap<String, T>();
    private final Map<String, T> referenceNamesCaseSensitive = new HashMap<String, T>();
    private final Map<String, T> displayNames = new HashMap<String, T>();
    private final Map<String, T> displayNamesCaseSensitive = new HashMap<String, T>();
    private final Map<Integer, T> ids = new HashMap<Integer, T>();
    private final Set<T> valueSet = new HashSet<T>();

    public Set<T> values() {
        synchronized (lock) {
            return Collections.unmodifiableSet(valueSet);
        }
    }

    public int size() {
        synchronized (lock) {
            return referenceNames.size();
        }
    }

    public void clear() {
        synchronized (lock) {
            referenceNames.clear();
            referenceNamesCaseSensitive.clear();
            displayNames.clear();
            displayNamesCaseSensitive.clear();
            ids.clear();
            valueSet.clear();
        }
    }

    public void put(final T object, final String displayName, final String referenceName, final int id) {
        if (!isDisplayName(displayName)) {
            throw new IllegalArgumentException(
                MessageFormat.format(
                    "the name [{0}] is not a valid display name ({1},{2},{3})", //$NON-NLS-1$
                    displayName,
                    displayName,
                    referenceName,
                    Integer.toString(id)));
        }
        if (!isReferenceName(referenceName)) {
            throw new IllegalArgumentException(
                MessageFormat.format(
                    "the name [{0}] is not a valid reference name ({1},{2},{3})", //$NON-NLS-1$
                    referenceName,
                    displayName,
                    referenceName,
                    Integer.toString(id)));
        }
        synchronized (lock) {
            referenceNames.put(createKey(referenceName, false), object);
            referenceNamesCaseSensitive.put(createKey(referenceName, true), object);
            displayNames.put(createKey(displayName, false), object);
            displayNamesCaseSensitive.put(createKey(displayName, true), object);
            ids.put(new Integer(id), object);
            valueSet.add(object);
        }
    }

    public T get(final int id) {
        final Integer key = new Integer(id);
        synchronized (lock) {
            return ids.get(key);
        }
    }

    public T get(final String fieldName) {
        return get(fieldName, true, true, false);
    }

    public T get(
        final String fieldName,
        final boolean matchReferenceName,
        final boolean matchDisplayName,
        final boolean caseSensitive) {
        if (matchReferenceName && matchDisplayName) {
            if (isReferenceName(fieldName)) {
                return getByReferenceName(fieldName, caseSensitive);
            } else {
                return getByDisplayName(fieldName, caseSensitive);
            }
        } else if (!matchReferenceName && matchDisplayName) {
            if (!isDisplayName(fieldName)) {
                throw new IllegalArgumentException(MessageFormat.format(
                    "the name [{0}] is not a display name", //$NON-NLS-1$
                    fieldName));
            }
            return getByDisplayName(fieldName, caseSensitive);
        } else if (matchReferenceName && !matchDisplayName) {
            if (!isReferenceName(fieldName)) {
                throw new IllegalArgumentException(MessageFormat.format(
                    "the name [{0}] is not a reference name", //$NON-NLS-1$
                    fieldName));
            }
            return getByReferenceName(fieldName, caseSensitive);
        } else {
            throw new IllegalArgumentException("matchReferenceName, matchDisplayName, or both must be true"); //$NON-NLS-1$
        }
    }

    private T getByReferenceName(final String name, final boolean caseSensitive) {
        final String key = createKey(name, caseSensitive);
        synchronized (lock) {
            final Map<String, T> cache = (caseSensitive ? referenceNamesCaseSensitive : referenceNames);
            return cache.get(key);
        }
    }

    private T getByDisplayName(final String name, final boolean caseSensitive) {
        final String key = createKey(name, caseSensitive);
        synchronized (lock) {
            final Map<String, T> cache = (caseSensitive ? displayNamesCaseSensitive : displayNames);
            return cache.get(key);
        }
    }

    private String createKey(final String input, final boolean caseSensitive) {
        return (caseSensitive ? input : input.toLowerCase());
    }

    public static boolean isReferenceName(final String name) {
        if (name == null) {
            return false;
        }
        return name.indexOf('.') != -1;
    }

    public static boolean isDisplayName(final String name) {
        if (name == null) {
            return false;
        }
        return name.trim().length() > 0;
    }
}
