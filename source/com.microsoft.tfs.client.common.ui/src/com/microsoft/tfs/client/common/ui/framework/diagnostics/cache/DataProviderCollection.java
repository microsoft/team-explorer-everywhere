// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.framework.diagnostics.cache;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.microsoft.tfs.client.common.ui.framework.diagnostics.Adapters;
import com.microsoft.tfs.client.common.ui.framework.diagnostics.extend.ExportType;

public class DataProviderCollection {
    private Map categoriesToProviders;
    private Map optionalExportCategoriesToProviders;
    private final DataProviderWrapper[] dataProviders;

    public DataProviderCollection(final DataProviderWrapper[] dataProviders) {
        this.dataProviders = dataProviders;
    }

    public DataProviderWrapper[] getDataProvidersWithExportHandlers() {
        final List results = new ArrayList();

        for (int i = 0; i < dataProviders.length; i++) {
            if (dataProviders[i].isShouldExport() && dataProviders[i].getDataProviderInfo().hasExportHandlers()) {
                results.add(dataProviders[i]);
            }
        }

        return (DataProviderWrapper[]) results.toArray(new DataProviderWrapper[results.size()]);
    }

    public DataProviderWrapper[] getExportableDataProviders() {
        final List results = new ArrayList();

        for (int i = 0; i < dataProviders.length; i++) {
            if (dataProviders[i].isShouldExport()) {
                results.add(dataProviders[i]);
            }
        }

        return (DataProviderWrapper[]) results.toArray(new DataProviderWrapper[results.size()]);
    }

    public DataProviderWrapper[] getOwnTabDataProviders() {
        final List results = new ArrayList();

        for (int i = 0; i < dataProviders.length; i++) {
            if (dataProviders[i].getDataProviderInfo().isOwnTab()) {
                results.add(dataProviders[i]);
            }
        }

        Collections.sort(results);

        return (DataProviderWrapper[]) results.toArray(new DataProviderWrapper[results.size()]);
    }

    public Map getDirectoryProvidersMap() {
        final Map map = new HashMap();

        for (int i = 0; i < dataProviders.length; i++) {
            final Object data = dataProviders[i].getData();
            final File file = (File) Adapters.get(data, File.class);

            /* We need to support non-existent directories. */
            if (file != null && (file.isDirectory() || !file.exists())) {
                List list = (List) map.get(dataProviders[i].getDataProviderInfo().getCategory());
                if (list == null) {
                    list = new ArrayList();
                    map.put(dataProviders[i].getDataProviderInfo().getCategory(), list);
                }
                list.add(dataProviders[i]);
            }
        }

        return map;
    }

    public DataCategory[] getSortedCategories() {
        final List categories = new ArrayList();

        for (int i = 0; i < dataProviders.length; i++) {
            final DataCategory category = dataProviders[i].getDataProviderInfo().getCategory();
            if (!categories.contains(category)) {
                categories.add(category);
            }
        }

        Collections.sort(categories);

        return (DataCategory[]) categories.toArray(new DataCategory[categories.size()]);
    }

    public DataCategory[] getSortedCategoriesWithOptionalExportProviders() {
        populateOptionalExportMap();

        final List list = new ArrayList(optionalExportCategoriesToProviders.keySet());
        Collections.sort(list);

        return (DataCategory[]) list.toArray(new DataCategory[list.size()]);
    }

    public DataProviderWrapper[] getSortedOptionalExportProvidersForCategory(final DataCategory category) {
        populateOptionalExportMap();

        final List list = (List) optionalExportCategoriesToProviders.get(category);
        return (DataProviderWrapper[]) list.toArray(new DataProviderWrapper[list.size()]);
    }

    private synchronized void populateOptionalExportMap() {
        synchronized (this) {
            if (optionalExportCategoriesToProviders == null) {
                optionalExportCategoriesToProviders = new HashMap();
                for (int i = 0; i < dataProviders.length; i++) {
                    if (dataProviders[i].getDataProviderInfo().getExportType() == ExportType.OPTIONAL) {
                        List list = (List) optionalExportCategoriesToProviders.get(
                            dataProviders[i].getDataProviderInfo().getCategory());
                        if (list == null) {
                            list = new ArrayList();
                            optionalExportCategoriesToProviders.put(
                                dataProviders[i].getDataProviderInfo().getCategory(),
                                list);
                        }
                        list.add(dataProviders[i]);
                    }
                }

                for (final Iterator it = optionalExportCategoriesToProviders.values().iterator(); it.hasNext();) {
                    final List list = (List) it.next();
                    Collections.sort(list);
                }
            }
        }
    }

    public boolean hasOptionalExportDataProviders() {
        populateOptionalExportMap();

        return optionalExportCategoriesToProviders.size() > 0;
    }

    public DataProviderWrapper[] getSortedProvidersForCategory(final DataCategory category) {
        synchronized (this) {
            if (categoriesToProviders == null) {
                categoriesToProviders = new HashMap();
                for (int i = 0; i < dataProviders.length; i++) {
                    List list = (List) categoriesToProviders.get(dataProviders[i].getDataProviderInfo().getCategory());
                    if (list == null) {
                        list = new ArrayList();
                        categoriesToProviders.put(dataProviders[i].getDataProviderInfo().getCategory(), list);
                    }
                    list.add(dataProviders[i]);
                }

                for (final Iterator it = categoriesToProviders.values().iterator(); it.hasNext();) {
                    final List list = (List) it.next();
                    Collections.sort(list);
                }
            }
        }

        final List list = (List) categoriesToProviders.get(category);
        return (DataProviderWrapper[]) list.toArray(new DataProviderWrapper[list.size()]);
    }
}
