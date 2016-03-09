// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.registration;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.microsoft.tfs.core.clients.registration.internal.ServerMapSerializer;
import com.microsoft.tfs.core.persistence.LockMode;
import com.microsoft.tfs.core.persistence.PersistenceStore;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.GUID;

/**
 * Maps Team Foundation Servers to their registration data.
 *
 * @since TEE-SDK-10.1
 * @threadsafety thread-safe
 */
public class ServerMap {
    private static final Log log = LogFactory.getLog(ServerMap.class);
    protected static final String CHILD_STORE_NAME = "TEE-Registration"; //$NON-NLS-1$
    private static final String OBJECT_NAME = "server-map.xml"; //$NON-NLS-1$

    public static ServerMap load(final PersistenceStore cacheStore) {
        Check.notNull(cacheStore, "cacheStore"); //$NON-NLS-1$

        final PersistenceStore mapStore = cacheStore.getChildStore(CHILD_STORE_NAME);

        try {
            if (mapStore.containsItem(OBJECT_NAME) == false) {
                return new ServerMap();
            }

            final ServerMap map =
                (ServerMap) mapStore.retrieveItem(OBJECT_NAME, LockMode.WAIT_FOREVER, null, new ServerMapSerializer());

            if (map == null) {
                log.warn(MessageFormat.format(
                    "unable to load server map from {0}:{1} (interrupted)", //$NON-NLS-1$
                    mapStore.toString(),
                    OBJECT_NAME));

                return new ServerMap();
            }

            return map;
        } catch (final Exception e) {
            log.warn(
                MessageFormat.format("unable to load server map from {0}:{1}", mapStore.toString(), OBJECT_NAME), //$NON-NLS-1$
                e);

            return new ServerMap();
        }
    }

    private final Map map = new HashMap();

    public void save(final PersistenceStore cacheStore) {
        Check.notNull(cacheStore, "cacheStore"); //$NON-NLS-1$

        final PersistenceStore mapStore = cacheStore.getChildStore(CHILD_STORE_NAME);

        try {
            mapStore.storeItem(OBJECT_NAME, this, LockMode.WAIT_FOREVER, null, new ServerMapSerializer());
        } catch (final Exception e) {
            log.warn(MessageFormat.format("unable to save server map to {0}:{1}", mapStore, OBJECT_NAME), e); //$NON-NLS-1$
        }
    }

    public String[] getURIs() {
        final String[] uris = (String[]) map.keySet().toArray(new String[map.size()]);
        Arrays.sort(uris);
        return uris;
    }

    public String getServerID(final String uri) {
        Check.notNull(uri, "uri"); //$NON-NLS-1$

        return (String) map.get(uri.toLowerCase());
    }

    public void addServerID(final String uri, final GUID id) {
        Check.notNull(id, "id"); //$NON-NLS-1$

        addServerID(uri, id.getGUIDString());
    }

    public void addServerID(final String uri, final String id) {
        Check.notNull(uri, "uri"); //$NON-NLS-1$
        Check.notNull(id, "id"); //$NON-NLS-1$

        map.put(uri.toLowerCase(), id);
    }
}
