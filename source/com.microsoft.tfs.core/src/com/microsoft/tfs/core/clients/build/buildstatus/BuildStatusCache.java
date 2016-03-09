// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.build.buildstatus;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.core.clients.build.IQueuedBuild;
import com.microsoft.tfs.core.memento.Memento;
import com.microsoft.tfs.core.memento.XMLMemento;
import com.microsoft.tfs.core.persistence.LockMode;
import com.microsoft.tfs.core.persistence.PersistenceStore;
import com.microsoft.tfs.core.util.internal.MementoRepositorySerializer;
import com.microsoft.tfs.util.Check;

/**
 * A collection of {@link BuildStatus}es which the user is interested in. Can be
 * persisted via {@link PersistenceStore}.
 *
 * @since TEE-SDK-10.1
 */
public class BuildStatusCache {
    private static final Log log = LogFactory.getLog(BuildStatusCache.class);

    private static final String CHILD_STORE_NAME = "TEE-BuildStatus"; //$NON-NLS-1$
    private static final String FILE_EXTENSION = ".xml"; //$NON-NLS-1$

    private static final String BUILD_STATUS_MEMENTO_NAME = "buildStatus"; //$NON-NLS-1$
    private static final String PROJECT_COLLECTION_PROPERTY_NAME = "projectCollection"; //$NON-NLS-1$
    private static final String BUILD_MEMENTO_NAME = "build"; //$NON-NLS-1$
    private static final String BUILD_ID_PROPERTY_NAME = "buildId"; //$NON-NLS-1$

    private final Memento memento;

    private BuildStatusCache(final Memento buildStatusMemento) {
        Check.notNull(buildStatusMemento, "buildStatusMemento"); //$NON-NLS-1$

        memento = buildStatusMemento;
    }

    public static BuildStatusCache load(final TFSTeamProjectCollection connection) {
        Check.notNull(connection, "connection"); //$NON-NLS-1$

        return load(
            connection.getPersistenceStoreProvider().getCachePersistenceStore(),
            connection.getInstanceID().getGUIDString());
    }

    public static BuildStatusCache load(final PersistenceStore baseStore, final String projectCollectionId) {
        Check.notNull(baseStore, "baseStore"); //$NON-NLS-1$
        Check.notNull(projectCollectionId, "projectCollectionId"); //$NON-NLS-1$

        final PersistenceStore currentStore = baseStore.getChildStore(CHILD_STORE_NAME);

        try {
            if (currentStore.containsItem(projectCollectionId + FILE_EXTENSION) == true) {
                final Memento buildStatusMemento = (Memento) currentStore.retrieveItem(
                    projectCollectionId + FILE_EXTENSION,
                    LockMode.WAIT_FOREVER,
                    null,
                    new MementoRepositorySerializer());

                if (buildStatusMemento != null) {
                    return new BuildStatusCache(buildStatusMemento);
                }
            }
        } catch (final Exception e) {
            log.warn("unable to load build status cache", e); //$NON-NLS-1$
        }

        final XMLMemento buildStatusMemento = new XMLMemento(BUILD_STATUS_MEMENTO_NAME);
        buildStatusMemento.putString(PROJECT_COLLECTION_PROPERTY_NAME, projectCollectionId);

        return new BuildStatusCache(buildStatusMemento);
    }

    public void save(final TFSTeamProjectCollection collection) {
        Check.notNull(collection, "collection"); //$NON-NLS-1$

        save(
            collection.getPersistenceStoreProvider().getCachePersistenceStore(),
            collection.getInstanceID().getGUIDString());
    }

    public void save(final PersistenceStore baseStore, final String projectCollectionId) {
        Check.notNull(baseStore, "baseStore"); //$NON-NLS-1$
        Check.notNull(projectCollectionId, "projectCollectionId"); //$NON-NLS-1$

        try {
            baseStore.getChildStore(CHILD_STORE_NAME).storeItem(
                projectCollectionId + FILE_EXTENSION,
                memento,
                LockMode.WAIT_FOREVER,
                null,
                new MementoRepositorySerializer());
        } catch (final Exception e) {
            log.warn("unable to save workspace cache", e); //$NON-NLS-1$
        }
    }

    public void addBuild(final IQueuedBuild build) {
        Check.notNull(build, "build"); //$NON-NLS-1$

        addBuild(build.getID());
    }

    public void addBuild(final int id) {
        final Memento buildChild = memento.createChild(BUILD_MEMENTO_NAME);

        buildChild.putInteger(BUILD_ID_PROPERTY_NAME, id);
    }

    public void setBuilds(final IQueuedBuild[] builds) {
        Check.notNull(builds, "builds"); //$NON-NLS-1$

        memento.removeChildren(BUILD_MEMENTO_NAME);

        for (int i = 0; i < builds.length; i++) {
            addBuild(builds[i]);
        }
    }

    public void removeBuild(final int id) {
        for (final Memento buildChild : memento.getChildren(BUILD_MEMENTO_NAME)) {
            if (buildChild.getInteger(BUILD_ID_PROPERTY_NAME).equals(Integer.valueOf(id))) {
                memento.removeChild(buildChild);
                break;
            }
        }
    }

    public List<Integer> getBuilds() {
        final List<Integer> buildIdList = new ArrayList<Integer>();
        final Memento[] buildChildren = memento.getChildren(BUILD_MEMENTO_NAME);

        for (final Memento buildChild : buildChildren) {
            buildIdList.add(buildChild.getInteger(BUILD_ID_PROPERTY_NAME));
        }

        return buildIdList;
    }
}
