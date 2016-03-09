// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.config.serveruri;

import java.net.URI;
import java.net.URISyntaxException;

import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.core.config.ConnectionAdvisor;
import com.microsoft.tfs.core.config.IllegalConfigurationException;

/**
 * <p>
 * An {@link ServerURIProvider} is used by a {@link TFSTeamProjectCollection} to
 * lazily create a TF server {@link URI}. An {@link ServerURIProvider} is
 * supplied to a {@link TFSTeamProjectCollection} by a {@link ConnectionAdvisor}
 * .
 * </p>
 *
 * <p>
 * {@link TFSTeamProjectCollection} allows only a single thread to use a
 * {@link ServerURIProvider}, and {@link TFSTeamProjectCollection} does not
 * retain any reference to a {@link ServerURIProvider} after it is finished
 * using it.
 * </p>
 *
 * <p>
 * For a default implementation, see {@link DefaultServerURIProvider}.
 * </p>
 *
 * @see TFSTeamProjectCollection
 * @see ConnectionAdvisor
 * @see DefaultServerURIProvider
 *
 * @since TEE-SDK-10.1
 * @threadsafety thread-compatible
 */
public interface ServerURIProvider {
    /**
     * Called to obtain the server {@link URI}.
     *
     * @return the server {@link URI} to use (must not be <code>null</code>)
     * @throws URISyntaxException
     * @throws IllegalConfigurationException
     */
    public URI getServerURI() throws URISyntaxException, IllegalConfigurationException;
}
