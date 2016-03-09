// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.util.serverlist.internal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.microsoft.tfs.core.persistence.LockMode;
import com.microsoft.tfs.core.persistence.PersistenceStore;
import com.microsoft.tfs.core.util.serverlist.ServerList;
import com.microsoft.tfs.core.util.serverlist.ServerListManager;
import com.microsoft.tfs.util.Check;

/**
 * The default implementation of {@link ServerListManager}, using the
 * {@link PersistenceStore} mechanism.
 *
 * @threadsafety unknown
 */
public class PersistenceStoreServerListManager implements ServerListManager {
    private static final Log log = LogFactory.getLog(PersistenceStoreServerListManager.class);

    private static final String CHILD_STORE_NAME = "TEE-Servers"; //$NON-NLS-1$
    private static final String OBJECT_NAME = "Servers.xml"; //$NON-NLS-1$

    private final PersistenceStore configurationStore;

    public PersistenceStoreServerListManager(final PersistenceStore configurationStore) {
        Check.notNull(configurationStore, "configurationStore"); //$NON-NLS-1$

        this.configurationStore = configurationStore;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ServerList getServerList() {
        return load();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean setServerList(final ServerList serverList) {
        Check.notNull(serverList, "serverList"); //$NON-NLS-1$

        try {
            save(serverList);
            return true;
        } catch (final Exception e) {
            log.warn("Unable to save servers list", e); //$NON-NLS-1$
            return false;
        }
    }

    private ServerList load() {
        final PersistenceStore currentStore = configurationStore.getChildStore(CHILD_STORE_NAME);

        try {
            if (currentStore.containsItem(OBJECT_NAME) == false) {
                return new ServerList();
            }

            final ServerList serverList = (ServerList) currentStore.retrieveItem(
                OBJECT_NAME,
                LockMode.WAIT_FOREVER,
                null,
                new ServerListSerializer());

            // Will be null on XML serialization error
            if (serverList == null) {
                return new ServerList();
            }

            return serverList;
        } catch (final Exception e) {
            log.warn("Unable to load servers list", e); //$NON-NLS-1$
            return new ServerList();
        }
    }

    private boolean save(final ServerList serverList) {
        Check.notNull(serverList, "serverList"); //$NON-NLS-1$

        try {
            return configurationStore.getChildStore(CHILD_STORE_NAME).storeItem(
                OBJECT_NAME,
                serverList,
                LockMode.WAIT_FOREVER,
                null,
                new ServerListSerializer());

        } catch (final Exception e) {
            log.warn("Unable to save servers list", e); //$NON-NLS-1$ r
            return false;
        }
    }
}
