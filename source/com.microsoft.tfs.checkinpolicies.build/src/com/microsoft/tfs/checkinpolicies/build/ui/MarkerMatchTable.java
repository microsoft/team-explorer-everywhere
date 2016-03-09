// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.checkinpolicies.build.ui;

import org.eclipse.swt.widgets.Composite;

import com.microsoft.tfs.checkinpolicies.build.Messages;
import com.microsoft.tfs.checkinpolicies.build.settings.MarkerMatch;
import com.microsoft.tfs.client.common.ui.framework.table.TableColumnData;
import com.microsoft.tfs.client.common.ui.framework.table.TableControl;

/**
 * A table of configured Eclipse resource markers.
 */
public class MarkerMatchTable extends TableControl {
    private static final String MARKER_COLUMN_NAME = "marker"; //$NON-NLS-1$

    public MarkerMatchTable(final Composite parent, final int style) {
        this(parent, style, null);
    }

    public MarkerMatchTable(final Composite parent, final int style, final String viewDataKey) {
        super(parent, style, MarkerMatch.class, viewDataKey);

        final TableColumnData[] columnData = new TableColumnData[] {
            new TableColumnData(Messages.getString("MarkerMatchTable.ColumnNameMarker"), 400, MARKER_COLUMN_NAME), //$NON-NLS-1$
        };

        setupTable(false, false, columnData);

        setUseViewerDefaults();
        setEnableTooltips(false);
    }

    public void setMarkers(final MarkerMatch[] Markers) {
        setElements(Markers);
    }

    public MarkerMatch[] getMarkers() {
        return (MarkerMatch[]) getElements();
    }

    public void setSelectedMarkers(final MarkerMatch[] Markers) {
        setSelectedElements(Markers);
    }

    public void setSelectedMarker(final MarkerMatch Marker) {
        setSelectedElement(Marker);
    }

    public MarkerMatch[] getSelectedMarkers() {
        return (MarkerMatch[]) getSelectedElements();
    }

    public MarkerMatch getSelectedMarker() {
        return (MarkerMatch) getSelectedElement();
    }

    @Override
    protected String getColumnText(final Object element, final String columnPropertyName) {
        final MarkerMatch tableData = (MarkerMatch) element;

        if (MARKER_COLUMN_NAME.equals(columnPropertyName)) {
            return tableData.getMarkerType();
        }

        return null;
    }
}
