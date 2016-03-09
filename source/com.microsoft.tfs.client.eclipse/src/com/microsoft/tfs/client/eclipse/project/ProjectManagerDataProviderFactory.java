// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse.project;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.microsoft.tfs.client.common.util.ExtensionLoader;

/**
 *
 *
 * @threadsafety unknown
 */
public class ProjectManagerDataProviderFactory {
    private static final Log log = LogFactory.getLog(ProjectManagerDataProviderFactory.class);

    private static final String EXTENSION_POINT_ID = "com.microsoft.tfs.client.eclipse.projectManagerDataProvider"; //$NON-NLS-1$

    private static final Object dataProviderLock = new Object();
    private static ProjectManagerDataProvider dataProvider;

    /**
     * @return the {@link ProjectManagerDataProvider} contributed via extension
     *         point, a default ({@link ProjectManagerDataProvider}) if none is
     *         available from the extension point, never <code>null</code>
     */
    static ProjectManagerDataProvider getDataProvider() {
        /*
         * If we do not yet have a data provider, query the extensions available
         */
        synchronized (dataProviderLock) {
            if (dataProvider == null) {
                try {
                    dataProvider =
                        (ProjectManagerDataProvider) ExtensionLoader.loadSingleExtensionClass(EXTENSION_POINT_ID);
                } catch (final Exception e) {
                    log.warn("Could not load project manager data provider for the product, using default", e); //$NON-NLS-1$
                    dataProvider = new ProjectManagerDataProvider();
                }
            }

            return dataProvider;
        }
    }

}
