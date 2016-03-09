// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.controls.vc;

import org.eclipse.core.resources.IResource;
import org.eclipse.swt.widgets.Composite;

import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.framework.table.TableColumnData;
import com.microsoft.tfs.client.common.ui.framework.table.TableControl;

/**
 * <p>
 * {@link ResourceTable} is a control that displays a collection of
 * {@link IResource}s in a table.
 * </p>
 *
 * <p>
 * The supported style bits that can be used with {@link ResourceTable} are
 * defined by the base class {@link TableControl}.
 * </p>
 *
 * @see IResource
 * @see TableControl
 */
public class ResourceTable extends TableControl {
    private static final String FOLDER_COLUMN_ID = "folder"; //$NON-NLS-1$
    private static final String NAME_COLUMN_ID = "name"; //$NON-NLS-1$

    public ResourceTable(final Composite parent, final int style) {
        this(parent, style, null);
    }

    public ResourceTable(final Composite parent, final int style, final String viewDataKey) {
        super(parent, style, IResource.class, viewDataKey);

        setupTable();
        setUseViewerDefaults();
    }

    public void setResources(final IResource[] resources) {
        setElements(resources);
    }

    public IResource[] getResources() {
        return (IResource[]) getElements();
    }

    public void setSelectedResources(final IResource[] resources) {
        setSelectedElements(resources);
    }

    public void setSelectedResource(final IResource resource) {
        setSelectedElement(resource);
    }

    public IResource[] getSelectedResources() {
        return (IResource[]) getSelectedElements();
    }

    public IResource[] getSelectedResource() {
        return (IResource[]) getSelectedElement();
    }

    public void setCheckedResources(final IResource[] resources) {
        setCheckedElements(resources);
    }

    public IResource[] getCheckedResources() {
        return (IResource[]) getCheckedElements();
    }

    private void setupTable() {
        final TableColumnData[] columnData = new TableColumnData[] {
            new TableColumnData(Messages.getString("ResourceTable.ColumnHeaderNameText"), 100, NAME_COLUMN_ID), //$NON-NLS-1$
            new TableColumnData(Messages.getString("ResourceTable.ColumnHeaderFolderText"), 100, FOLDER_COLUMN_ID) //$NON-NLS-1$
        };

        setupTable(true, false, columnData);
    }

    @Override
    protected String getColumnText(final Object element, final int columnIndex) {
        final IResource resource = (IResource) element;
        switch (columnIndex) {
            case 0:
                return resource.getName();
            case 1:
                return resource.getParent().getLocation().toOSString();
            default:
                return super.getColumnText(element, columnIndex);
        }
    }
}
