// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.util.serverlist;

import com.microsoft.tfs.core.config.ConnectionAdvisor;
import com.microsoft.tfs.core.config.persistence.PersistenceStoreProvider;
import com.microsoft.tfs.core.util.serverlist.internal.PersistenceStoreServerListManager;
import com.microsoft.tfs.core.util.serverlist.internal.WindowsRegistryServerListManager;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.Platform;

/**
 * Static methods to get a {@link ServerListManager} that uses the best storage
 * mechanism for the running platform.
 * <p>
 * Core classes must pass the active {@link ConnectionAdvisor}'s
 * {@link PersistenceStoreProvider} when calling
 * {@link #getServerListProvider(PersistenceStoreProvider)}.
 *
 * @threadsafety thread-safe
 */
public class ServerListManagerFactory {
    /**
     * Gets the best {@link ServerListManager} for this platform.
     *
     * @param persistenceProvider
     *        a {@link PersistenceStoreProvider} to use (must not be
     *        <code>null</code> )
     * @return a {@link ServerListManager} (never <code>null</code>)
     */
    public static ServerListManager getServerListProvider(final PersistenceStoreProvider persistenceProvider) {
        Check.notNull(persistenceProvider, "persistenceProvider"); //$NON-NLS-1$

        if (Platform.isCurrentPlatform(Platform.WINDOWS)) {
            return new WindowsRegistryServerListManager();
        }

        return new PersistenceStoreServerListManager(persistenceProvider.getConfigurationPersistenceStore());
    }
}
