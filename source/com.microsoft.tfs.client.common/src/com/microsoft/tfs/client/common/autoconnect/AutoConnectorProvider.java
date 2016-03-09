// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.autoconnect;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.microsoft.tfs.client.common.util.ExtensionLoader;

/**
 * Utility methods to find and load the {@link AutoConnector} extension.
 *
 * @threadsafety unknown
 */
public abstract class AutoConnectorProvider {
    public static final String EXTENSION_POINT_ID = "com.microsoft.tfs.client.common.autoConnector"; //$NON-NLS-1$

    private static final Log log = LogFactory.getLog(AutoConnectorProvider.class);

    private static final Object lock = new Object();
    private static AutoConnector autoConnector;

    private AutoConnectorProvider() {
    }

    /**
     * @return the {@link AutoConnector} loaded via the
     *         {@link AutoConnector#EXTENSION_POINT_ID} extension point , or
     *         <code>null</code> if none was contributed
     */
    public static AutoConnector getAutoConnector() {
        /*
         * If we do not yet have a repository manager provider, query the
         * extensions available
         */
        synchronized (lock) {
            if (autoConnector == null) {
                try {
                    autoConnector = (AutoConnector) ExtensionLoader.loadSingleExtensionClass(EXTENSION_POINT_ID);
                } catch (final Exception e) {
                    log.warn("Could not load auto connect manager for the product", e); //$NON-NLS-1$
                }
            }

            return autoConnector;
        }
    }
}
