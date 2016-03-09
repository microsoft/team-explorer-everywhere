// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.framework.configuration.internal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.microsoft.tfs.core.TFSConfigurationServer;
import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.core.clients.framework.catalog.CatalogNode;
import com.microsoft.tfs.core.clients.framework.catalog.CatalogQueryOptions;
import com.microsoft.tfs.core.clients.framework.configuration.TFSEntity;
import com.microsoft.tfs.core.clients.framework.configuration.TFSEntitySession;
import com.microsoft.tfs.core.clients.framework.configuration.catalog.TFSCatalogEntity;
import com.microsoft.tfs.core.clients.framework.configuration.entities.OrganizationalRootEntity;
import com.microsoft.tfs.util.Check;

public class TFSCatalogEntitySession implements TFSEntitySession {
    private final TFSConfigurationServer connection;

    private final TFSCatalogEntity[] rootEntities;

    /**
     * We hold a cache of objects that were created. This is because clients may
     * query for objects by specific path, or for dependencies of other objects.
     * These objects are then unrooted from the tree, so we wish to resolve
     * already queried objects in this cache instead of creating duplicates.
     */
    private final Map<String, TFSCatalogEntity> objectCache = new HashMap<String, TFSCatalogEntity>();

    TFSCatalogEntitySession(final TFSConfigurationServer connection) {
        this.connection = connection;

        final CatalogNode[] rootNodes = connection.getCatalogService().getRootNodes();

        rootEntities = new TFSCatalogEntity[rootNodes.length];

        for (int i = 0; i < rootNodes.length; i++) {
            rootEntities[i] = TFSCatalogEntityFactory.newEntity(this, rootNodes[i]);

            /* Root objects, by definition, have no parent */
            rootEntities[i].setParent(null);

            objectCache.put(rootNodes[i].getFullPath(), rootEntities[i]);
        }
    }

    TFSCatalogEntitySession(final TFSTeamProjectCollection connection) {
        this(connection.getConfigurationServer());
    }

    public TFSConfigurationServer getConnection() {
        return connection;
    }

    @Override
    public OrganizationalRootEntity getOrganizationalRoot() {
        for (final TFSEntity object : rootEntities) {
            if (object instanceof OrganizationalRootEntity) {
                return (OrganizationalRootEntity) object;
            }
        }

        return null;
    }

    TFSEntity loadEntity(final String path) {
        Check.notNull(path, "path"); //$NON-NLS-1$

        if (path.length() == 0) {
            return null;
        }

        TFSCatalogEntity configurationObject;

        synchronized (objectCache) {
            if (objectCache.containsKey(path)) {
                configurationObject = objectCache.get(path);
            } else {
                final CatalogNode[] objectNodes = connection.getCatalogService().queryNodes(new String[] {
                    path
                }, null, CatalogQueryOptions.EXPAND_DEPENDENCIES);

                if (objectNodes.length == 1) {
                    configurationObject = TFSCatalogEntityFactory.newEntity(this, objectNodes[0]);
                } else {
                    configurationObject = null;
                }

                objectCache.put(path, configurationObject);
            }
        }

        return configurationObject;
    }

    public TFSEntity loadParent(final TFSCatalogEntity childEntity) {
        Check.notNull(childEntity, "childEntity"); //$NON-NLS-1$

        final String parentPath = childEntity.getCatalogNode().getParentPath();

        if (parentPath == null || parentPath.length() == 0) {
            return null;
        }

        TFSCatalogEntity parent;

        synchronized (objectCache) {
            if (objectCache.containsKey(parentPath)) {
                parent = objectCache.get(parentPath);
            } else {
                final CatalogNode[] objectNodes = connection.getCatalogService().queryNodes(new String[] {
                    parentPath
                }, null, CatalogQueryOptions.EXPAND_DEPENDENCIES);

                if (objectNodes.length == 1) {
                    parent = TFSCatalogEntityFactory.newEntity(this, objectNodes[0]);
                } else {
                    parent = null;
                }

                objectCache.put(parentPath, parent);
            }
        }

        return parent;
    }

    public List<TFSEntity> loadChildren(final TFSCatalogEntity parentEntity) {
        Check.notNull(parentEntity, "parentEntity"); //$NON-NLS-1$

        final CatalogNode catalogNode = parentEntity.getCatalogNode();

        final CatalogNode[] childNodes = connection.getCatalogService().queryNodes(new String[] {
            catalogNode.getFullPath() + "*" ////$NON-NLS-1$
        }, null, CatalogQueryOptions.EXPAND_DEPENDENCIES);

        final List<TFSEntity> children = new ArrayList<TFSEntity>();

        for (int i = 0; i < childNodes.length; i++) {
            TFSCatalogEntity child;

            synchronized (objectCache) {
                if ((child = objectCache.get(childNodes[i].getFullPath())) == null) {
                    child = TFSCatalogEntityFactory.newEntity(this, childNodes[i]);
                    objectCache.put(childNodes[i].getFullPath(), child);
                }
            }

            child.setParent(parentEntity);
            children.add(child);
        }

        return children;
    }

    public void loadDependencies(
        final TFSCatalogEntity entity,
        final Map<String, TFSEntity> singletonDependencyMap,
        final Map<String, List<TFSEntity>> dependencySetMap,
        final List<TFSEntity> allDependencyList) {
        Check.notNull(entity, "entity"); //$NON-NLS-1$

        final CatalogNode catalogNode = entity.getCatalogNode();

        if (catalogNode.getDependencyGroup() != null) {
            /* Handle singleton dependencies */
            final Map<String, CatalogNode> singletonDependencyNodes = catalogNode.getDependencyGroup().getSingletons();

            for (final Entry<String, CatalogNode> singletonEntry : singletonDependencyNodes.entrySet()) {
                final String key = singletonEntry.getKey();
                final CatalogNode singletonNode = singletonEntry.getValue();

                TFSCatalogEntity singleton;

                synchronized (objectCache) {
                    if ((singleton = objectCache.get(singletonNode.getFullPath())) == null) {
                        singleton = TFSCatalogEntityFactory.newEntity(this, singletonNode);
                        objectCache.put(singletonNode.getFullPath(), singleton);
                    }
                }

                singletonDependencyMap.put(key, singleton);

                if (singleton != null) {
                    allDependencyList.add(singleton);
                }
            }

            /* Handle the dependency sets */
            final Map<String, ArrayList<CatalogNode>> dependencySets = catalogNode.getDependencyGroup().getSets();

            for (final Entry<String, ArrayList<CatalogNode>> setEntry : dependencySets.entrySet()) {
                final String key = setEntry.getKey();

                final List<TFSEntity> setObjects = new ArrayList<TFSEntity>();

                for (final CatalogNode setNode : setEntry.getValue()) {
                    TFSCatalogEntity setObject;

                    synchronized (objectCache) {
                        if ((setObject = objectCache.get(setNode.getFullPath())) == null) {
                            setObject = TFSCatalogEntityFactory.newEntity(this, setNode);
                            objectCache.put(setNode.getFullPath(), setObject);
                        }
                    }

                    if (setObject != null) {
                        setObjects.add(setObject);
                        allDependencyList.add(setObject);
                    }
                }

                dependencySetMap.put(key, setObjects);
            }
        }
    }
}
