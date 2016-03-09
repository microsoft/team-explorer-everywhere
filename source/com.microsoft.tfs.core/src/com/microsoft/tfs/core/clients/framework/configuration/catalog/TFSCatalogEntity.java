// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.framework.configuration.catalog;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.microsoft.tfs.core.clients.framework.catalog.CatalogNode;
import com.microsoft.tfs.core.clients.framework.catalog.CatalogService;
import com.microsoft.tfs.core.clients.framework.configuration.TFSEntity;
import com.microsoft.tfs.core.clients.framework.configuration.TFSEntitySession;
import com.microsoft.tfs.core.clients.framework.configuration.internal.TFSCatalogEntitySession;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.GUID;

/**
 * A {@link CatalogService} entity.
 *
 * @since TEE-SDK-10.1
 */
public abstract class TFSCatalogEntity implements TFSEntity {
    private final TFSCatalogEntitySession session;
    private final CatalogNode catalogNode;

    private final Object lock = new Object();

    private boolean parentLoaded = false;
    private TFSEntity parent;

    private List<TFSEntity> children;

    private boolean dependenciesLoaded = false;
    private HashMap<String, TFSEntity> dependencySingletonMap;
    private HashMap<String, List<TFSEntity>> dependencySetMap;
    private List<TFSEntity> dependencyList;

    public TFSCatalogEntity(final TFSCatalogEntitySession session, final CatalogNode catalogNode) {
        Check.notNull(session, "session"); //$NON-NLS-1$
        Check.notNull(catalogNode, "catalogNode"); //$NON-NLS-1$

        this.session = session;
        this.catalogNode = catalogNode;
    }

    protected TFSEntitySession getSession() {
        return session;
    }

    public CatalogNode getCatalogNode() {
        return catalogNode;
    }

    @Override
    public GUID getResourceID() {
        return new GUID(catalogNode.getResource().getResourceTypeIdentifier());
    }

    @Override
    public String getDisplayName() {
        return catalogNode.getResource().getDisplayName();
    }

    @Override
    public String getDescription() {
        return catalogNode.getResource().getDescription();
    }

    public String getProperty(final String propertyName) {
        Check.notNull(propertyName, "propertyName"); //$NON-NLS-1$

        return catalogNode.getResource().getProperties().get(propertyName);
    }

    @Override
    public String getDisplayPath() {
        final StringBuffer displayPath = new StringBuffer();

        if (parent != null) {
            displayPath.append(parent.getDisplayPath());
        }

        displayPath.append("\\"); //$NON-NLS-1$

        if (getDisplayName() != null) {
            displayPath.append(getDisplayName());
        }

        return displayPath.toString();
    }

    public void setParent(final TFSEntity parent) {
        this.parent = parent;
        parentLoaded = true;
    }

    /**
     * Returns the direct parent of this configuration object. May be
     * <code>null</code> for the root of the tree.
     *
     * @return The direct parent, or <code>null</code>
     */
    @Override
    public TFSEntity getParent() {
        synchronized (lock) {
            if (!parentLoaded) {
                parent = session.loadParent(this);
                parentLoaded = true;
            }

            return parent;
        }
    }

    public <T extends TFSEntity> T getAncestorOfType(final Class<T> type) {
        Check.notNull(type, "type"); //$NON-NLS-1$

        for (TFSEntity ancestor = getParent(); ancestor != null; ancestor = ancestor.getParent()) {
            if (type.isInstance(ancestor)) {
                return (T) ancestor;
            }
        }

        return null;
    }

    /**
     * Note: you must hold a lock while calling this method.
     *
     * @return
     */
    private List<TFSEntity> getChildrenList() {
        synchronized (lock) {
            if (children == null) {
                children = session.loadChildren(this);
            }

            return children;
        }
    }

    public TFSEntity[] getChildren() {
        synchronized (lock) {
            final List<TFSEntity> childrenList = getChildrenList();

            return childrenList.toArray(new TFSEntity[childrenList.size()]);
        }
    }

    public <T extends TFSEntity> T[] getChildrenOfType(final Class<T> type) {
        Check.notNull(type, "type"); //$NON-NLS-1$

        synchronized (lock) {
            return getTypedList(getChildrenList(), type);
        }
    }

    public <T extends TFSEntity> T getChildOfType(final Class<T> type) {
        final T[] children = getChildrenOfType(type);

        if (children.length == 1) {
            return children[0];
        }

        return null;
    }

    private boolean loadDependencies() {
        synchronized (lock) {
            if (dependenciesLoaded) {
                return false;
            }

            dependencySingletonMap = new HashMap<String, TFSEntity>();
            dependencySetMap = new HashMap<String, List<TFSEntity>>();
            dependencyList = new ArrayList<TFSEntity>();

            session.loadDependencies(this, dependencySingletonMap, dependencySetMap, dependencyList);

            dependenciesLoaded = true;
        }

        return true;
    }

    public TFSEntity getSingletonDependency(final String key) {
        synchronized (lock) {
            loadDependencies();

            return dependencySingletonMap.get(key);
        }
    }

    public TFSEntity[] getDependencies() {
        synchronized (lock) {
            loadDependencies();

            return dependencyList.toArray(new TFSEntity[dependencyList.size()]);
        }
    }

    public <T extends TFSEntity> T[] getDependenciesOfType(final Class<T> type) {
        Check.notNull(type, "type"); //$NON-NLS-1$

        synchronized (lock) {
            loadDependencies();

            return getTypedList(dependencyList, type);
        }
    }

    public <T extends TFSEntity> T getDependencyOfType(final Class<T> type) {
        final T[] dependencies = getDependenciesOfType(type);

        if (dependencies.length == 1) {
            return dependencies[0];
        }

        return null;
    }

    private static <T extends TFSEntity> T[] getTypedList(final List<TFSEntity> list, final Class<T> type) {
        Check.notNull(list, "list"); //$NON-NLS-1$
        Check.notNull(type, "type"); //$NON-NLS-1$

        final List<TFSEntity> matchList = new ArrayList<TFSEntity>();

        for (final TFSEntity object : list) {
            if (type.isInstance(object)) {
                matchList.add(object);
            }
        }

        return matchList.toArray((T[]) Array.newInstance(type, matchList.size()));
    }
}
