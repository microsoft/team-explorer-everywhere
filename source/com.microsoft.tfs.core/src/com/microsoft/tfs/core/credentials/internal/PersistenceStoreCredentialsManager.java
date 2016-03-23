// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.credentials.internal;

import java.net.URI;
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.microsoft.tfs.core.Messages;
import com.microsoft.tfs.core.credentials.CachedCredentials;
import com.microsoft.tfs.core.credentials.CredentialsManager;
import com.microsoft.tfs.core.persistence.LockMode;
import com.microsoft.tfs.core.persistence.PersistenceSecurity;
import com.microsoft.tfs.core.persistence.PersistenceStore;
import com.microsoft.tfs.core.util.ServerURIComparator;
import com.microsoft.tfs.core.util.URIUtils;
import com.microsoft.tfs.util.Check;

/**
 * An implementation of {@link CredentialsManager} that can read and write to
 * the <b>unsecure</b> {@link PersistenceStore} mechanism.
 *
 * @threadsafety thread-safe
 */
public class PersistenceStoreCredentialsManager implements CredentialsManager {
    private static final Log log = LogFactory.getLog(PersistenceStoreCredentialsManager.class);

    private static final String CHILD_STORE_NAME = "TEE-Servers"; //$NON-NLS-1$
    private static final String OBJECT_NAME = "Credentials.xml"; //$NON-NLS-1$

    private final PersistenceStore configurationStore;

    public PersistenceStoreCredentialsManager(final PersistenceStore configurationStore) {
        Check.notNull(configurationStore, "configurationStore"); //$NON-NLS-1$

        this.configurationStore = configurationStore;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getUIMechanismName() {
        return Messages.getString("PersistenceStoreCredentialsManager.PersistenceStore"); //$NON-NLS-1$
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean canWrite() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isSecure() {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CachedCredentials[] getCredentials() {
        final Collection<CachedCredentials> credentials = load().values();

        return credentials.toArray(new CachedCredentials[credentials.size()]);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CachedCredentials getCredentials(URI serverURI) {
        Check.notNull(serverURI, "serverURI"); //$NON-NLS-1$

        // Reduce to scheme, host, port
        serverURI = URIUtils.removePathAndQueryParts(serverURI);

        // The map normalizes URIs for comparision
        return load().get(serverURI);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean setCredentials(CachedCredentials cachedCredentials) {
        Check.notNull(cachedCredentials, "cachedCredentials"); //$NON-NLS-1$
        Check.notNull(cachedCredentials.getURI(), "cachedCredentials.getURI()"); //$NON-NLS-1$

        // Reduce to scheme, host, port
        final URI serverURI = URIUtils.removePathAndQueryParts(cachedCredentials.getURI());
        cachedCredentials =
            new CachedCredentials(serverURI, cachedCredentials.getUsername(), cachedCredentials.getPassword());

        try {
            final Map<URI, CachedCredentials> credentialsMap = load();

            // Even though the map uses a custom comparator that knows to ignore
            // slashes, tidy up the string so it looks good in the cache file.
            credentialsMap.put(URIUtils.removeTrailingSlash(serverURI), cachedCredentials);

            save(credentialsMap);

            return true;
        } catch (final Exception e) {
            log.warn("Unable to save credentials cache", e); //$NON-NLS-1$
            return false;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean removeCredentials(final CachedCredentials cachedCredentials) {
        Check.notNull(cachedCredentials, "cachedCredentials"); //$NON-NLS-1$
        Check.notNull(cachedCredentials.getURI(), "cachedCredentials.getURI()"); //$NON-NLS-1$

        return removeCredentials(cachedCredentials.getURI());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean removeCredentials(final URI uri) {
        Check.notNull(uri, "uri"); //$NON-NLS-1$

        // Reduce to scheme, host, port
        final URI serverURI = URIUtils.removePathAndQueryParts(uri);

        try {
            final Map<URI, CachedCredentials> credentialsMap = load();

            // Even though the map uses a custom comparator that knows to ignore
            // slashes, tidy up the string so it looks good in the cache file.
            final CachedCredentials removedCredentials = credentialsMap.remove(serverURI);

            save(credentialsMap);

            return (removedCredentials != null);
        } catch (final Exception e) {
            log.warn("Unable to remove entry from credentials cache", e); //$NON-NLS-1$
            return false;
        }
    }

    private Map<URI, CachedCredentials> load() {
        final PersistenceStore currentStore = configurationStore.getChildStore(CHILD_STORE_NAME);

        try {
            if (currentStore.containsItem(OBJECT_NAME) == false) {
                return newMap();
            }

            @SuppressWarnings("unchecked")
            final Map<URI, CachedCredentials> credentialsMap = (Map<URI, CachedCredentials>) currentStore.retrieveItem(
                OBJECT_NAME,
                LockMode.WAIT_FOREVER,
                null,
                new CachedCredentialsSerializer());

            // Will be null on XML serialization error
            if (credentialsMap == null) {
                return newMap();
            }

            return credentialsMap;
        } catch (final Exception e) {
            log.warn("Unable to load credentials cache", e); //$NON-NLS-1$
            return newMap();
        }
    }

    private boolean save(final Map<URI, CachedCredentials> credentialsMap) {
        Check.notNull(credentialsMap, "credentialsMap"); //$NON-NLS-1$

        try {
            return configurationStore.getChildStore(CHILD_STORE_NAME).storeItem(
                OBJECT_NAME,
                credentialsMap,
                LockMode.WAIT_FOREVER,
                null,
                new CachedCredentialsSerializer(),
                PersistenceSecurity.PRIVATE);
        } catch (final Exception e) {
            log.warn("Unable to save credentials cache", e); //$NON-NLS-1$ r
            return false;
        }
    }

    protected static Map<URI, CachedCredentials> newMap() {
        return new TreeMap<URI, CachedCredentials>(ServerURIComparator.INSTANCE);
    }
}
