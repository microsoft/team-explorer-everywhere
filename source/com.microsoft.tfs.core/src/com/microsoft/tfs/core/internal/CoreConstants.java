// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.internal;

import com.microsoft.tfs.core.config.DefaultConnectionAdvisor;

public interface CoreConstants {
    /**
     * The default vendor name used when none was configured for a specific
     * class or configuration.
     */
    public static final String DEFAULT_VENDOR_NAME = "Microsoft-Team-Explorer-SDK"; //$NON-NLS-1$

    /**
     * The default application name used for settings storage when none was
     * configured.
     */
    public static final String DEFAULT_PERSISTENCE_APPLICATION_NAME = "Unknown-Application"; //$NON-NLS-1$

    /**
     * The default version to use as the "current" version for persistence
     * classes, if none was otherwise configured.
     */
    public static final String DEFAULT_PERSISTENCE_VERSION = "default"; //$NON-NLS-1$

    /**
     * The name {@link DefaultConnectionAdvisor} uses for its cache persistence
     * store.
     */
    public static final String DEFAULT_CACHE_CHILD_NAME = "Cache"; //$NON-NLS-1$

    /**
     * The name {@link DefaultConnectionAdvisor} uses for its configuration
     * persistence store.
     */
    public static final String DEFAULT_CONFIGURATION_CHILD_NAME = "Configuration"; //$NON-NLS-1$
}
