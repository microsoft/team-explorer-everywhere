// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.controls.connect;

import org.eclipse.jface.viewers.IElementComparer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;

import com.microsoft.tfs.client.common.catalog.TeamProjectCollectionInfo;
import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.TFSCommonUIClientPlugin;
import com.microsoft.tfs.client.common.ui.framework.helper.TableViewerSorter;
import com.microsoft.tfs.client.common.ui.framework.image.ImageHelper;
import com.microsoft.tfs.client.common.ui.framework.table.TableColumnData;
import com.microsoft.tfs.client.common.ui.framework.table.TableControl;

public class ProjectCollectionTable extends TableControl {
    private final ImageHelper imageHelper = new ImageHelper(TFSCommonUIClientPlugin.PLUGIN_ID);

    public ProjectCollectionTable(final Composite parent, final int style) {
        super(parent, (style | SWT.FULL_SELECTION), TeamProjectCollectionInfo.class, null);

        final TableColumnData[] columnData = new TableColumnData[] {
            new TableColumnData(Messages.getString("ProjectCollectionTable.ColumnNameName"), 10, 1.0F, "name") //$NON-NLS-1$ //$NON-NLS-2$
        };

        setupTable(false, false, columnData);

        setUseViewerDefaults();
        setEnableTooltips(true);

        final TableViewerSorter sorter = new TableViewerSorter(getViewer());
        getViewer().setSorter(sorter);

        getViewer().setComparer(new IElementComparer() {
            @Override
            public boolean equals(final Object a, final Object b) {
                if (a instanceof TeamProjectCollectionInfo && b instanceof TeamProjectCollectionInfo) {
                    return ((TeamProjectCollectionInfo) a).getIdentifier().equals(
                        ((TeamProjectCollectionInfo) b).getIdentifier());
                }

                return a.equals(b);
            }

            @Override
            public int hashCode(final Object element) {
                return ((TeamProjectCollectionInfo) element).getIdentifier().hashCode();
            }
        });

        addDisposeListener(new DisposeListener() {
            @Override
            public void widgetDisposed(final DisposeEvent e) {
                imageHelper.dispose();
            }
        });
    }

    public void setProjectCollections(final TeamProjectCollectionInfo[] projectCollections) {
        setElements(projectCollections);
    }

    public TeamProjectCollectionInfo[] getProjectCollection() {
        return (TeamProjectCollectionInfo[]) getElements();
    }

    public void setSelectedProjectCollection(final TeamProjectCollectionInfo projectCollection) {
        setSelectedElement(projectCollection);
    }

    public TeamProjectCollectionInfo getSelectedProjectCollection() {
        return (TeamProjectCollectionInfo) getSelectedElement();
    }

    @Override
    protected String getColumnText(final Object element, final String column) {
        if ("name".equals(column)) //$NON-NLS-1$
        {
            return ((TeamProjectCollectionInfo) element).getDisplayName();
        }

        return Messages.getString("ProjectCollectionTable.UnknownNameText"); //$NON-NLS-1$
    }

    @Override
    protected Image getColumnImage(final Object element, final String column) {
        if ("name".equals(column)) //$NON-NLS-1$
        {
            return imageHelper.getImage("images/common/team_foundation_server.gif"); //$NON-NLS-1$
        }

        return null;
    }

    @Override
    public String getTooltipText(final Object element, final int columnIndex) {
        final String description = ((TeamProjectCollectionInfo) element).getDescription();

        if (description != null && description.length() > 0) {
            return description;
        }

        return null;
    }
}
