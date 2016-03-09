// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.repository;

import java.lang.reflect.Array;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.microsoft.tfs.util.Check;

public abstract class RepositoryMap {
    /**
     * The type of the mapped objects in this {@link RepositoryMap}. Never
     * <code>null</code>.
     */
    private final Class mappedType;

    /**
     * Maps from mapped {@link Object} to {@link TFSRepository}. Guarded by
     * {@link #lock}. Never <code>null</code>.
     */
    private final Map toRepository = new HashMap();

    /**
     * Maps from {@link TFSRepository} to {@link Set} of mapped objects. Guarded
     * by {@link #lock}. Never <code>null</code>.
     */
    private final Map fromRepository = new HashMap();

    /**
     * Guards {@link #toRepository} and {@link #fromRepository}. Never
     * <code>null</code>.
     */
    private final Object lock = new Object();

    protected RepositoryMap(final Class mappedType) {
        Check.notNull(mappedType, "mappedType"); //$NON-NLS-1$

        if (mappedType.isPrimitive()) {
            throw new IllegalArgumentException("mappedType must not be primitive"); //$NON-NLS-1$
        }

        this.mappedType = mappedType;
    }

    public final Class getMappedType() {
        return mappedType;
    }

    public final TFSRepository[] getRepositories() {
        synchronized (lock) {
            return (TFSRepository[]) fromRepository.keySet().toArray(new TFSRepository[fromRepository.size()]);
        }
    }

    protected final void addMappingInternal(final TFSRepository repository, final Object mappedObject) {
        Check.notNull(mappedObject, "mappedObject"); //$NON-NLS-1$

        addMappingsInternal(repository, new Object[] {
            mappedObject
        });
    }

    protected final void addMappingsInternal(final TFSRepository repository, final Object[] mappedObjects) {
        Check.notNull(repository, "repository"); //$NON-NLS-1$
        Check.notNull(mappedObjects, "mappedObjects"); //$NON-NLS-1$

        if (mappedObjects.length == 0) {
            return;
        }

        synchronized (lock) {
            Set set = (Set) fromRepository.get(repository);
            if (set == null) {
                set = new HashSet();
                fromRepository.put(repository, set);
            }

            for (int i = 0; i < mappedObjects.length; i++) {
                if (mappedObjects[i] == null) {
                    throw new IllegalArgumentException("object in collection is null"); //$NON-NLS-1$
                }

                set.add(mappedObjects[i]);
                toRepository.put(mappedObjects[i], repository);
            }
        }
    }

    protected final TFSRepository getRepositoryInternal(final Object mappedObject) {
        Check.notNull(mappedObject, "mappedObject"); //$NON-NLS-1$

        synchronized (lock) {
            return (TFSRepository) toRepository.get(mappedObject);
        }
    }

    protected final Object[] getMappedObjectsInternal(final TFSRepository repository) {
        Check.notNull(repository, "repository"); //$NON-NLS-1$

        synchronized (lock) {
            final Set set = (Set) fromRepository.get(repository);

            if (set == null) {
                final String messageFormat = "the repository [{0}] is not contained in this map"; //$NON-NLS-1$
                final String message = MessageFormat.format(messageFormat, repository);
                throw new IllegalArgumentException(message);
            }

            return collectionToTypedArray(set);
        }
    }

    protected final Object[] getAllMappedObjectsInternal() {
        final Set allMappedObjects = new HashSet();

        synchronized (lock) {
            for (final Iterator it = fromRepository.values().iterator(); it.hasNext();) {
                final Set repositorySet = (Set) it.next();
                allMappedObjects.addAll(repositorySet);
            }
        }

        return collectionToTypedArray(allMappedObjects);
    }

    protected final RepositoryMap subMapInternal(final Object[] subset) {
        final RepositoryMap subMap = newInstance();

        for (int i = 0; i < subset.length; i++) {
            final TFSRepository repository = getRepositoryInternal(subset[i]);
            subMap.addMappingInternal(repository, subset[i]);
        }

        return subMap;
    }

    protected RepositoryMap newInstance() {
        try {
            return getClass().newInstance();
        } catch (final Exception e) {
            final String messageFormat =
                "RepositoryMap subclass [{0}] does not allow instantiation via Class.newInstance and does not override newInstance()"; //$NON-NLS-1$
            final String message = MessageFormat.format(messageFormat, getClass().getName());
            throw new RuntimeException(message, e);
        }
    }

    private Object[] collectionToTypedArray(final Collection c) {
        final Object[] typedArray = (Object[]) Array.newInstance(mappedType, c.size());
        return c.toArray(typedArray);
    }
}
