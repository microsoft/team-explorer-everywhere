// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.config.httpclient;

import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.core.config.ConnectionAdvisor;
import com.microsoft.tfs.core.httpclient.HttpClient;

/**
 * <p>
 * An {@link HTTPClientFactory} is used by a {@link TFSTeamProjectCollection} to
 * lazily create an {@link HttpClient} instance. An {@link HTTPClientFactory} is
 * supplied to a {@link TFSTeamProjectCollection} by a {@link ConnectionAdvisor}
 * .
 * </p>
 *
 * <p>
 * {@link TFSTeamProjectCollection} allows only a single thread to use a
 * {@link HTTPClientFactory}, and {@link TFSTeamProjectCollection} does not
 * retain any reference to a {@link HTTPClientFactory} after it is finished
 * using it.
 * </p>
 *
 * <p>
 * For a default implementation, see {@link DefaultHTTPClientFactory}.
 * </p>
 *
 * @see TFSTeamProjectCollection
 * @see ConnectionAdvisor
 * @see DefaultHTTPClientFactory
 *
 * @since TEE-SDK-10.1
 * @threadsafety thread-compatible
 */
public interface HTTPClientFactory {
    /**
     * Called to obtain a new {@link HttpClient} instance.
     *
     * @return a new {@link HttpClient} (must not be <code>null</code>)
     */
    public HttpClient newHTTPClient();

    /**
     * <p>
     * Called to dispose an {@link HttpClient} that was previously obtained from
     * a call to {@link #newHTTPClient()}.
     * </p>
     *
     * <p>
     * This method is called by {@link TFSTeamProjectCollection#close()} to
     * clean up any http client resources. It can be assumed that the specified
     * {@link HttpClient} instance will no longer be used after calling this
     * method.
     * </p>
     *
     * @param httpClient
     *        an {@link HttpClient} instance to dispose (must not be
     *        <code>null</code>)
     */
    public void dispose(HttpClient httpClient);
}
