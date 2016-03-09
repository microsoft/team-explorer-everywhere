// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.controls.vc;

import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;

import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.framework.table.TableColumnData;
import com.microsoft.tfs.client.common.ui.framework.table.TableControl;
import com.microsoft.tfs.client.common.ui.framework.viewer.FolderFileLabelProvider;
import com.microsoft.tfs.client.common.vc.TypedItemSpec;
import com.microsoft.tfs.core.clients.versioncontrol.path.LocalPath;
import com.microsoft.tfs.core.clients.versioncontrol.path.ServerPath;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ItemType;
import com.microsoft.tfs.core.clients.versioncontrol.specs.ItemSpec;

/**
 * <p>
 * {@link TypedItemSpecTable} is a control that displays a collection of
 * {@link ItemSpec}s in a table.
 * </p>
 *
 * <p>
 * The supported style bits that can be used with {@link TypedItemSpecTable} are
 * defined by the base class {@link TableControl}.
 * </p>
 *
 * @see ItemSpec
 * @see TableControl
 */
public class TypedItemSpecTable extends TableControl {
    private static final String FOLDER_COLUMN_ID = "folder"; //$NON-NLS-1$
    private static final String NAME_COLUMN_ID = "name"; //$NON-NLS-1$

    public TypedItemSpecTable(final Composite parent, final int style) {
        this(parent, style, null);
    }

    public TypedItemSpecTable(final Composite parent, final int style, final String viewDataKey) {
        super(parent, style, TypedItemSpec.class, viewDataKey);

        final TableColumnData[] columnData = new TableColumnData[] {
            new TableColumnData(Messages.getString("TypedItemSpecTable.ColumnHeaderName"), 100, 0.4F, NAME_COLUMN_ID), //$NON-NLS-1$
            new TableColumnData(
                Messages.getString("TypedItemSpecTable.ColumnHeaderFolder"), //$NON-NLS-1$
                100,
                0.6F,
                FOLDER_COLUMN_ID)
        };

        setupTable(true, true, columnData);

        setUseDefaultContentProvider();
        getViewer().setLabelProvider(new LabelProvider());
        setEnableTooltips(false);
    }

    public void setTypedItemSpecs(final TypedItemSpec[] items) {
        setElements(items);
    }

    public TypedItemSpec[] getTypedItemSpecs() {
        return (TypedItemSpec[]) getElements();
    }

    public void setSelectedTypedItemSpecs(final TypedItemSpec[] items) {
        setSelectedElements(items);
    }

    public void setSelectedTypedItemSpec(final TypedItemSpec item) {
        setSelectedElement(item);
    }

    public TypedItemSpec[] getSelectedTypedItemSpecs() {
        return (TypedItemSpec[]) getSelectedElements();
    }

    public TypedItemSpec[] getSelectedTypedItemSpec() {
        return (TypedItemSpec[]) getSelectedElement();
    }

    public void setCheckedTypedItemSpecs(final TypedItemSpec[] resources) {
        setCheckedElements(resources);
    }

    public TypedItemSpec[] getCheckedTypedItemSpecs() {
        return (TypedItemSpec[]) getCheckedElements();
    }

    private static class LabelProvider extends FolderFileLabelProvider implements ITableLabelProvider {
        @Override
        public Image getColumnImage(final Object element, final int columnIndex) {
            if (columnIndex != 0) {
                return null;
            }

            final TypedItemSpec item = (TypedItemSpec) element;

            if (ItemType.FOLDER == item.getType()) {
                return getImageForFolder();
            }

            if (ServerPath.isServerPath(item.getItem())) {
                return getImageForFile(ServerPath.getFileName(item.getItem()));
            } else {
                return getImageForFile(LocalPath.getFileName(item.getItem()));
            }
        }

        @Override
        public String getColumnText(final Object element, final int columnIndex) {
            final TypedItemSpec item = (TypedItemSpec) element;

            final boolean isServerPath = ServerPath.isServerPath(item.getItem());

            switch (columnIndex) {
                case 0:
                    if (isServerPath) {
                        return ServerPath.getFileName(item.getItem());
                    } else {
                        return LocalPath.getFileName(item.getItem());
                    }
                case 1:
                    if (isServerPath) {
                        return ServerPath.getParent(item.getItem());
                    } else {
                        return LocalPath.getParent(item.getItem());
                    }
                default:
                    return ""; //$NON-NLS-1$
            }
        }
    }
}
