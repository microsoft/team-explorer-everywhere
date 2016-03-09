// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.config.serveruri;

import java.net.URI;
import java.net.URISyntaxException;

import com.microsoft.tfs.core.config.ConnectionInstanceData;
import com.microsoft.tfs.core.config.IllegalConfigurationException;
import com.microsoft.tfs.util.Check;

/**
 * <p>
 * A default implementation of the {@link ServerURIProvider} interface that uses
 * a {@link ConnectionInstanceData} to determine the server URI.
 * </p>
 *
 * @see ServerURIProvider
 * @see ConnectionInstanceData
 *
 * @since TEE-SDK-10.1
 * @threadsafety immutable
 */
public class DefaultServerURIProvider implements ServerURIProvider {
    private final ConnectionInstanceData connectionInstanceData;

    public DefaultServerURIProvider(final ConnectionInstanceData connectionInstanceData) {
        Check.notNull(connectionInstanceData, "connectionInstanceData"); //$NON-NLS-1$

        this.connectionInstanceData = connectionInstanceData;
    }

    @Override
    public URI getServerURI() throws URISyntaxException, IllegalConfigurationException {
        return connectionInstanceData.getServerURI();
    }
}
