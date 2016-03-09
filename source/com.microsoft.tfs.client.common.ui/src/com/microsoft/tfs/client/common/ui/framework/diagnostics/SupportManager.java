// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.framework.diagnostics;

import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;

import com.microsoft.tfs.client.common.ui.TFSCommonUIClientPlugin;
import com.microsoft.tfs.client.common.ui.framework.diagnostics.cache.DataCategoryCache;
import com.microsoft.tfs.client.common.ui.framework.diagnostics.cache.DataProviderActionCache;
import com.microsoft.tfs.client.common.ui.framework.diagnostics.cache.DataProviderCache;
import com.microsoft.tfs.client.common.ui.framework.diagnostics.cache.ExportHandlerCache;
import com.microsoft.tfs.client.common.ui.framework.diagnostics.cache.ImageCache;
import com.microsoft.tfs.client.common.ui.framework.diagnostics.cache.SupportProviderCache;
import com.microsoft.tfs.client.common.ui.framework.diagnostics.extend.DataProvider;
import com.microsoft.tfs.client.common.ui.framework.diagnostics.extend.ExportType;

/**
 * SupportManager is a singleton that replaces the previous support/diagnostics
 * plugin.
 *
 * @threadsafety unknown
 */
public final class SupportManager {
    private final static Object lock = new Object();
    private static SupportManager instance;

    /*
     * Caches
     */
    private final DataCategoryCache dataCategoryCache;
    private final DataProviderActionCache dataProviderActionCache;
    private final ExportHandlerCache exportHandlerCache;
    private final DataProviderCache dataProviderCache;
    private final SupportProviderCache supportProviderCache;
    private final ImageCache imageCache;

    private SupportManager() {
        dataCategoryCache = new DataCategoryCache();
        dataProviderActionCache = new DataProviderActionCache();
        exportHandlerCache = new ExportHandlerCache();
        dataProviderCache = new DataProviderCache(dataCategoryCache, dataProviderActionCache, exportHandlerCache);
        supportProviderCache = new SupportProviderCache();
        imageCache = new ImageCache();
    }

    public static SupportManager getInstance() {
        synchronized (lock) {
            if (instance == null) {
                instance = new SupportManager();
            }

            return instance;
        }
    }

    public void replace(final String id, final DataProvider provider) {
        dataProviderCache.replace(id, provider);
    }

    public void replaceWith(final DataProvider provider) {
        dataProviderCache.replaceWith(provider);
    }

    public void contributeDataProvider(
        final String id,
        final String label,
        final String labelNOLOC,
        final DataProvider dataProvider,
        final String categoryId,
        final ExportType exportType,
        final boolean isOwnTab) {
        dataProviderCache.contributeDataProvider(id, label, labelNOLOC, dataProvider, categoryId, exportType, isOwnTab);
    }

    public static IExtension[] getExtensions(final String extensionPointName) {
        return Platform.getExtensionRegistry().getExtensionPoint(
            TFSCommonUIClientPlugin.PLUGIN_ID,
            extensionPointName).getExtensions();
    }

    public DataCategoryCache getDataCategoryCache() {
        return dataCategoryCache;
    }

    public DataProviderCache getDataProviderCache() {
        return dataProviderCache;
    }

    public ImageCache getImageCache() {
        return imageCache;
    }

    public SupportProviderCache getSupportProviderCache() {
        return supportProviderCache;
    }

    public static void log(final int level, final String message) {
        log(level, message, null);
    }

    public static void log(final int level, final String message, final Throwable t) {
        TFSCommonUIClientPlugin.getDefault().getLog().log(
            new Status(level, TFSCommonUIClientPlugin.PLUGIN_ID, 0, message, t));
    }
}
