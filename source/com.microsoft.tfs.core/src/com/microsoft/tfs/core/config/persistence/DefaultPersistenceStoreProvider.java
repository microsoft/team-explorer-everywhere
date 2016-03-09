// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.config.persistence;

import com.microsoft.tfs.core.persistence.FilesystemPersistenceStore;
import com.microsoft.tfs.core.persistence.VersionedVendorFilesystemPersistenceStore;

/**
 * <p>
 * A default implementation of the {@link PersistenceStoreProvider} interface
 * that uses standard discovery mechanisms (common with Visual Studio) to locate
 * the cache and configuration file directories.
 * </p>
 * <p>
 * This class is a singleton. Access the instance through {@link #INSTANCE}.
 * </p>
 *
 * @see PersistenceStoreProvider
 *
 * @since TEE-SDK-11.0
 * @threadsafety thread-safe
 */
public class DefaultPersistenceStoreProvider implements PersistenceStoreProvider {
    // Strings used to build a VersionedVendorFilesystemPersistenceStore (must
    // match VS strings)

    protected static final String VENDOR_NAME = "Microsoft"; //$NON-NLS-1$
    protected static final String APPLICATION_NAME = "Team Foundation"; //$NON-NLS-1$
    protected static final String VERSION = "4.0"; //$NON-NLS-1$

    // Names of children under the base store (must match VS strings)

    protected static final String CACHE_CHILD_NAME = "Cache"; //$NON-NLS-1$
    protected static final String CONFIGURATION_CHILD_NAME = "Configuration"; //$NON-NLS-1$
    protected static final String LOG_CHILD_NAME = "Logs"; //$NON-NLS-1$

    /*
     * Compute static stores for each type.
     */

    private static final VersionedVendorFilesystemPersistenceStore basePersistenceStore =
        new VersionedVendorFilesystemPersistenceStore(VENDOR_NAME, APPLICATION_NAME, VERSION);

    private static final VersionedVendorFilesystemPersistenceStore cachePersistenceStore =
        (VersionedVendorFilesystemPersistenceStore) basePersistenceStore.getChildStore(CACHE_CHILD_NAME);

    private static final VersionedVendorFilesystemPersistenceStore configurationPersistenceStore =
        (VersionedVendorFilesystemPersistenceStore) basePersistenceStore.getChildStore(CONFIGURATION_CHILD_NAME);

    private static final VersionedVendorFilesystemPersistenceStore logPersistenceStore =
        (VersionedVendorFilesystemPersistenceStore) basePersistenceStore.getChildStore(LOG_CHILD_NAME);

    /**
     * Singleton instance of this class.
     */
    public static final DefaultPersistenceStoreProvider INSTANCE = new DefaultPersistenceStoreProvider();

    /**
     * Most uses of this class should be through the static {@link #INSTANCE}
     * field.
     *
     * Not private to permit subclassing.
     */
    protected DefaultPersistenceStoreProvider() {
    }

    /**
     * {@inheritDoc}
     * <p>
     * Returns {@link #cachePersistenceStore}.
     */
    @Override
    public FilesystemPersistenceStore getCachePersistenceStore() {
        return cachePersistenceStore;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Returns {@link #configurationPersistenceStore}.
     */
    @Override
    public FilesystemPersistenceStore getConfigurationPersistenceStore() {
        return configurationPersistenceStore;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Returns {@link #LOG_PERSISTENCE_STORE}.
     */
    @Override
    public FilesystemPersistenceStore getLogPersistenceStore() {
        return logPersistenceStore;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Computed using only values returned by public methods. Suitable for
     * derived classes which only modify the values returned by public methods.
     */
    @Override
    public boolean equals(final Object obj) {
        if (obj == this) {
            return true;
        }

        if (obj instanceof DefaultPersistenceStoreProvider == false) {
            return false;
        }

        final DefaultPersistenceStoreProvider other = (DefaultPersistenceStoreProvider) obj;

        return getCachePersistenceStore().equals(other.getCachePersistenceStore())
            && getConfigurationPersistenceStore().equals(other.getConfigurationPersistenceStore())
            && getLogPersistenceStore().equals(other.getLogPersistenceStore());
    }

    /**
     * {@inheritDoc}
     * <p>
     * Computed using ony values returned by public methods. Suitable for
     * derived classes which only modify the values returned by public methods.
     */
    @Override
    public int hashCode() {
        int result = 17;

        result = result * 37 + (getCachePersistenceStore().hashCode());
        result = result * 37 + (getConfigurationPersistenceStore().hashCode());
        result = result * 37 + (getLogPersistenceStore().hashCode());

        return result;
    }
}
