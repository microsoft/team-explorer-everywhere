// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.credentials.internal;

import java.net.URI;

import com.microsoft.tfs.core.Messages;
import com.microsoft.tfs.core.credentials.CachedCredentials;
import com.microsoft.tfs.core.credentials.CredentialsManager;

/**
 * A credentials manager that does not support read or write.
 *
 * @threadsafety thread-safe
 */
public class NullCredentialsManager implements CredentialsManager {
    /**
     * {@inheritDoc}
     */
    @Override
    public String getUIMechanismName() {
        return Messages.getString("NullCredentialsManager.NullCredentialsManagerMechanismName"); //$NON-NLS-1$
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean canWrite() {
        return false;
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
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CachedCredentials getCredentials(final URI serverURI) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean setCredentials(final CachedCredentials cachedCredentials) {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean removeCredentials(final URI uri) {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean removeCredentials(final CachedCredentials cachedCredentials) {
        return false;
    }
}
