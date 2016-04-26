// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.credentials;

import com.microsoft.tfs.core.config.ConnectionAdvisor;
import com.microsoft.tfs.core.config.persistence.PersistenceStoreProvider;
import com.microsoft.tfs.core.credentials.internal.GnomeKeyringCredentialsManager;
import com.microsoft.tfs.core.credentials.internal.KeychainCredentialsManager;
import com.microsoft.tfs.core.credentials.internal.PersistenceStoreCredentialsManager;
import com.microsoft.tfs.core.credentials.internal.WinCredentialsManager;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.Platform;

/**
 * Static methods to get a {@link CredentialsManager} with the best storage
 * mechanism for the running platform.
 * <p>
 * Core classes must pass the active {@link ConnectionAdvisor}'s
 * {@link PersistenceStoreProvider} when calling
 * {@link #getCredentialsManager(PersistenceStoreProvider)}.
 *
 * @threadsafety thread-safe
 */
public class CredentialsManagerFactory {
    /**
     * Gets the best {@link CredentialsManager} for this platform. If the
     * platform provides credential management services (Windows CredMan, Mac OS
     * X Keychain), an implementation that uses that service is returned,
     * otherwise the given {@link PersistenceStoreProvider} may be used for
     * storage. Test the returned {@link CredentialsManager} for its
     * capabilities (whether it is secure, is read-only, etc.).
     *
     * @param persistenceProvider
     *        a {@link PersistenceStoreProvider} to use if there are no secure
     *        platform storage services available (must not be <code>null</code>
     *        )
     * @return a {@link CredentialsManager} (never <code>null</code>)
     */
    public static CredentialsManager getCredentialsManager(final PersistenceStoreProvider persistenceProvider) {
        return getCredentialsManager(persistenceProvider, false);
    }

    /**
     * Gets the best {@link CredentialsManager} for this platform. If the
     * platform provides credential management services (Windows CredMan, Mac OS
     * X Keychain or PersistnaceCredentialsManager), an implementation that uses
     * that service is returned, otherwise the given
     * {@link PersistenceStoreProvider} may be used for storage. Test the
     * returned {@link CredentialsManager} for its capabilities (whether it is
     * secure, is read-only, etc.).
     *
     * @param persistenceProvider
     *        a {@link PersistenceStoreProvider} to use if there are no secure
     *        platform storage services available (must not be <code>null</code>
     *        )
     * @param usePersistanceCredentialsManager
     *        a boolean if set to true then on Mac OS use
     *        PersistanceCredentialsManager otherwise on Mac OS use keychain
     *
     * @return a {@link CredentialsManager} (never <code>null</code>)
     */
    public static CredentialsManager getCredentialsManager(
        final PersistenceStoreProvider persistenceProvider,
        final boolean usePersistanceCredentialsManager) {

        Check.notNull(persistenceProvider, "persistenceProvider"); //$NON-NLS-1$

        /*
         * All the implementations we return are very cheap to construct, so
         * build a new one each time.
         */

        /*
         * Windows uses Credential Manager for secure credential storage.
         */
        if (Platform.isCurrentPlatform(Platform.WINDOWS)) {
            return new WinCredentialsManager();
        }

        if (Platform.isCurrentPlatform(Platform.MAC_OS_X) && !usePersistanceCredentialsManager) {
            /*
             * Mac OS uses Keychain for credential storage by default unless the
             * user specifies using PersistanceStoreCredentialsManager
             */
            return new KeychainCredentialsManager();
        }

        if (Platform.isCurrentPlatform(Platform.LINUX)
            && !usePersistanceCredentialsManager
            && GnomeKeyringCredentialsManager.isGnomeKeyringSupported()) {

            /*
             * Linux uses gnome-keyring if it's available unless the user
             * specifies using PersistanceStoreCredentialsManager
             */
            return new GnomeKeyringCredentialsManager();
        }

        return new PersistenceStoreCredentialsManager(persistenceProvider.getConfigurationPersistenceStore());
    }
}
