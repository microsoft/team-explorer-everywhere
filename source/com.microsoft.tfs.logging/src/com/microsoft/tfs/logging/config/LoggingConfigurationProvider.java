// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.logging.config;

import java.net.URL;

/**
 * Provides a URL to a log4j configuration file.
 *
 * @threadsafety thread-compatible
 * @since TEE-SDK-10.1
 */
public interface LoggingConfigurationProvider {
    /**
     * Attempt to locate a resource for configuring the logging system.
     *
     * @return the URL of a configuration resource, or <code>null</code> if none
     *         could be found
     */
    URL getConfigurationURL();
}
