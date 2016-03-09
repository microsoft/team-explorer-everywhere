// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.config.persistence;

import com.microsoft.tfs.core.TFSConnection;
import com.microsoft.tfs.core.config.ConnectionAdvisor;
import com.microsoft.tfs.core.persistence.FilesystemPersistenceStore;
import com.microsoft.tfs.core.persistence.PersistenceStore;

/**
 * <p>
 * A {@link PersistenceStoreProvider} is used by a {@link TFSConnection} to
 * locate cache and configuration files. A {@link PersistenceStoreProvider} is
 * supplied to a {@link TFSConnection} by a {@link ConnectionAdvisor}.
 * </p>
 *
 * <p>
 * {@link TFSConnection} allows multiple threads to use a
 * {@link PersistenceStoreProvider} concurrently.
 * </p>
 *
 * <p>
 * For a default implementation, see {@link DefaultPersistenceStoreProvider}.
 * </p>
 *
 * @see TFSConnection
 * @see ConnectionAdvisor
 * @see DefaultPersistenceStoreProvider
 *
 * @since TEE-SDK-11.0
 * @threadsafety thread-compatible
 */
public interface PersistenceStoreProvider {
    /**
     * <p>
     * Gets the {@link PersistenceStore} for storing cache information. Cache
     * information is information that can be re-created from server data but
     * improves performance to have it locally.
     * </p>
     *
     * @return a {@link FilesystemPersistenceStore} object for storing cache
     *         data, never <code>null</code>
     */
    public FilesystemPersistenceStore getCachePersistenceStore();

    /**
     * <p>
     * Gets the {@link PersistenceStore} for storing non-cache configuration
     * information. User configuration information is unlike cache information
     * in that it cannot be recreated without input from the user.
     * </p>
     *
     * @return a {@link FilesystemPersistenceStore} object for storing non-cache
     *         configuration data, never <code>null</code>
     */
    public FilesystemPersistenceStore getConfigurationPersistenceStore();

    /**
     * <p>
     * Gets the {@link PersistenceStore} for storing log files. Returning
     * <code>null</code> disables logging to files.
     * </p>
     *
     * @return a {@link FilesystemPersistenceStore} object for storing non-cache
     *         configuration data, <code>null</code> to disable storing log
     *         files on disk
     */
    public FilesystemPersistenceStore getLogPersistenceStore();

}
