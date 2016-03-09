// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.credentials;

import com.microsoft.tfs.client.common.credentials.internal.EclipseCredentialsManager.EclipseGitCredentialsManager;
import com.microsoft.tfs.client.common.credentials.internal.EclipseCredentialsManager.EclipseTeeCredentialsManager;
import com.microsoft.tfs.core.config.ConnectionAdvisor;
import com.microsoft.tfs.core.config.persistence.PersistenceStoreProvider;
import com.microsoft.tfs.core.credentials.CredentialsManager;
import com.microsoft.tfs.util.Check;

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
public class EclipseCredentialsManagerFactory {
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
        Check.notNull(persistenceProvider, "persistenceProvider"); //$NON-NLS-1$

        /*
         * EclipseCredentialsManager uses platformCredentialsManager for
         * Username/Password credentials and Eclipse secure storage for
         * CookieCredentials
         */
        return new EclipseTeeCredentialsManager(persistenceProvider);
    }

    public static CredentialsManager getGitCredentialsManager() {
        /*
         * EGit uses Eclipse secure storage only, i.e.
         * platformCredentialsManager is null
         */
        return new EclipseGitCredentialsManager();
    }
}
