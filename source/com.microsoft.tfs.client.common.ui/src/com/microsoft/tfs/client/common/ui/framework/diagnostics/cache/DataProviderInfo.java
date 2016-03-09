// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.framework.diagnostics.cache;

import java.util.ArrayList;
import java.util.List;

import com.microsoft.tfs.client.common.ui.framework.diagnostics.extend.DataProvider;
import com.microsoft.tfs.client.common.ui.framework.diagnostics.extend.ExportType;

public class DataProviderInfo implements Comparable {
    private final String id;
    private final String label;
    private final String labelNOLOC;
    private final DataProvider dataProvider;
    private final DataCategory category;
    private final ExportType exportType;
    private final boolean ownTab;

    private final List<DataProviderActionInfo> actions = new ArrayList<DataProviderActionInfo>();
    private DataProviderActionInfo defaultAction;

    private final List<ExportHandlerReference> exportHandlers = new ArrayList<ExportHandlerReference>();

    public DataProviderInfo(
        final String id,
        final String label,
        final String labelNOLOC,
        final DataProvider dataProvider,
        final DataCategory category,
        final ExportType exportType,
        final boolean ownTab) {
        this.id = id;
        this.label = label;
        this.labelNOLOC = labelNOLOC;
        this.dataProvider = dataProvider;
        this.category = category;
        this.exportType = exportType;
        this.ownTab = ownTab;
    }

    @Override
    public int compareTo(final Object o) {
        final DataProviderInfo other = (DataProviderInfo) o;
        int c = category.compareTo(other.category);
        if (c == 0) {
            c = label.compareToIgnoreCase(other.label);
        }
        return c;
    }

    void addExportHandler(final ExportHandlerReference exportHandler) {
        exportHandlers.add(exportHandler);
    }

    void addAction(final DataProviderActionInfo action, final boolean isDefault) {
        actions.add(action);
        if (isDefault) {
            defaultAction = action;
        }
    }

    public boolean hasActions() {
        return actions.size() > 0;
    }

    public DataProviderActionInfo[] getActions() {
        return actions.toArray(new DataProviderActionInfo[actions.size()]);
    }

    public DataProviderActionInfo getDefaultAction() {
        return defaultAction;
    }

    public ExportHandlerReference[] getExportHandlers() {
        return exportHandlers.toArray(new ExportHandlerReference[exportHandlers.size()]);
    }

    public boolean hasExportHandlers() {
        return exportHandlers.size() > 0;
    }

    public DataCategory getCategory() {
        return category;
    }

    public DataProvider getDataProvider() {
        return dataProvider;
    }

    public String getID() {
        return id;
    }

    public String getLabel() {
        return label;
    }

    public String getLabelNOLOC() {
        return labelNOLOC;
    }

    public ExportType getExportType() {
        return exportType;
    }

    public boolean isOwnTab() {
        return ownTab;
    }
}
