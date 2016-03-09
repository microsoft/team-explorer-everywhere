// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core;

/**
 * <p>
 * {@link TFProxyServerSettings} is an interface that makes Team Foundation
 * Proxy (aka download proxy) settings available. A
 * {@link TFProxyServerSettings} instance is available from a
 * {@link TFSTeamProjectCollection}.
 * </p>
 *
 * <p>
 * Client notes: The return values from the {@link #isAvailable()} and
 * {@link #getURL()} methods could change at any time. Clients must not assume
 * that these values will not change. Clients should not cache these values
 * longer than the duration of a single download. Clients should follow this
 * pattern at the beginning of each download:
 * <ol>
 * <li>Call {@link #isAvailable()} - if the result is <code>false</code>, do not
 * use a TF proxy for that download.</li>
 * <li>If {@link #isAvailable()} is <code>true</code>, call {@link #getURL()}.
 * If the result is <code>null</code>, do not use a TF proxy for that download.
 * </li>
 * <li>If the url is not <code>null</code>, attempt to use that url as a proxy
 * url to perform the download.</li>
 * <li>If the download fails because of the proxy, call {@link #recordFailure()}
 * so that the {@link TFProxyServerSettings} implementation can enforce a
 * failure policy.</li>
 * </ol>
 * </p>
 *
 * <p>
 * Implementation notes: {@link TFProxyServerSettings} implementations must be
 * thread safe as they will be accessed from multiple threads, possibly
 * simultaneously. Implementations are responsible for enforcing a failure
 * policy. Implementations are notified of proxy failures through the
 * {@link #recordFailure()} method. A reasonable failure policy is to make the
 * proxy unavailable for a short period of time (e.g. 5 minutes) in response to
 * a failure.
 * </p>
 *
 * @see TFSTeamProjectCollection
 *
 * @since TEE-SDK-10.1
 * @threadsafety thread-safe
 */
public interface TFProxyServerSettings {
    /**
     * Called first by clients to determine whether this
     * {@link TFProxyServerSettings} represents a TF proxy server that is
     * available to be used. As noted above, the result of this method can
     * change at any time.
     *
     * @return <code>true</code> if there is a TF proxy server available to be
     *         used at the time of this method call
     */
    public boolean isAvailable();

    /**
     * Called second by clients to determine a TF proxy server URL to use. Note
     * that even if {@link #isAvailable()} returns <code>true</code>, clients
     * must still be prepared for a <code>null</code> return value from this
     * method, which indicates that the TF proxy server became unavailable in
     * between method calls. A non-<code>null</code> return can be used for a
     * single download as a TF proxy URL.
     *
     * @return a TF proxy URL or <code>null</code>
     */
    public String getURL();

    /**
     * Clients must call this method if a download fails because of a TF proxy
     * server. Calling this method gives {@link TFProxyServerSettings} a chance
     * to enforce a failure policy. See the javadoc for this class for the
     * algorithm that clients should follow when using a
     * {@link TFProxyServerSettings} instance.
     */
    public void recordFailure();
}
