// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.controls.vc.properties;

import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.controls.generic.BaseControl;
import com.microsoft.tfs.client.common.ui.vc.tfsitem.TFSFolder;
import com.microsoft.tfs.client.common.ui.vc.tfsitem.TFSItem;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ExtendedItem;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ItemIdentifier;
import com.microsoft.tfs.jni.FileSystemAttributes;
import com.microsoft.tfs.jni.FileSystemUtils;

public class AdvancedPropertiesTab implements PropertiesTab {
    private static final String TFS_ITEM_SOURCE = "TFSItem"; //$NON-NLS-1$
    private static final String TFS_FOLDER_SOURCE = "TFSFolder"; //$NON-NLS-1$
    private static final String EXTENDED_ITEM_SOURCE = "ExtendedItem"; //$NON-NLS-1$
    private static final String LOCAL_SOURCE = "Local"; //$NON-NLS-1$

    private Table table;

    private class AdvancedPropertiesControl extends BaseControl {
        public AdvancedPropertiesControl(final Composite parent, final int style) {
            super(parent, style);

            final FillLayout layout = new FillLayout();
            layout.marginWidth = 0;
            layout.marginHeight = 0;
            layout.spacing = getSpacing();
            setLayout(layout);

            table = new Table(this, SWT.BORDER | SWT.FULL_SELECTION);
            table.setLinesVisible(true);
            table.setHeaderVisible(true);

            final TableLayout tableLayout = new TableLayout();
            table.setLayout(tableLayout);

            tableLayout.addColumnData(new ColumnWeightData(1, true));
            final TableColumn column1 = new TableColumn(table, SWT.NONE);
            column1.setText(Messages.getString("AdvancedPropertiesTab.ColumnNameProperty")); //$NON-NLS-1$
            column1.setResizable(true);

            tableLayout.addColumnData(new ColumnWeightData(1, true));
            final TableColumn column2 = new TableColumn(table, SWT.NONE);
            column2.setText(Messages.getString("AdvancedPropertiesTab.ColumnNameValue")); //$NON-NLS-1$
            column2.setResizable(true);

            tableLayout.addColumnData(new ColumnWeightData(1, true));
            final TableColumn column3 = new TableColumn(table, SWT.NONE);
            column3.setText(Messages.getString("AdvancedPropertiesTab.ColumnNameSource")); //$NON-NLS-1$
            column3.setResizable(true);
        }
    }

    private void addProperty(final String name, final Object value, final String source) {
        final TableItem item = new TableItem(table, SWT.NONE);
        item.setText(0, name);
        item.setText(
            1,
            (value != null ? value.toString() : Messages.getString("AdvancedPropertiesTab.NullPropertyValue"))); //$NON-NLS-1$
        item.setText(2, source);
    }

    @Override
    public void populate(final TFSRepository repository, final TFSItem item) {
        /*
         * Properties from the TFSItem class itself
         */
        addProperty("Full Path", item.getFullPath(), TFS_ITEM_SOURCE); //$NON-NLS-1$
        addProperty("Class", item.getClass().getName(), TFS_ITEM_SOURCE); //$NON-NLS-1$
        addProperty("Identity Hashcode", Integer.toHexString(System.identityHashCode(item)), TFS_ITEM_SOURCE); //$NON-NLS-1$

        /*
         * TFSFolder-only properties
         */
        if (item instanceof TFSFolder) {
            final TFSFolder folder = (TFSFolder) item;
            addProperty("Children Cached", String.valueOf(folder.areChildrenCached()), TFS_FOLDER_SOURCE); //$NON-NLS-1$
            if (folder.areChildrenCached()) {
                addProperty("Children Size", String.valueOf(folder.getChildren().size()), TFS_FOLDER_SOURCE); //$NON-NLS-1$
            }
        }

        /*
         * ExtendedItem properties
         */
        final ExtendedItem extendedItem = item.getExtendedItem();
        if (extendedItem != null) {
            addProperty("lver", String.valueOf(extendedItem.getLocalVersion()), EXTENDED_ITEM_SOURCE); //$NON-NLS-1$
            addProperty("did", String.valueOf(extendedItem.getDeletionID()), EXTENDED_ITEM_SOURCE); //$NON-NLS-1$
            addProperty("latest", String.valueOf(extendedItem.getLatestVersion()), EXTENDED_ITEM_SOURCE); //$NON-NLS-1$
            addProperty("type", extendedItem.getItemType(), EXTENDED_ITEM_SOURCE); //$NON-NLS-1$
            addProperty("enc", String.valueOf(extendedItem.getEncoding()), EXTENDED_ITEM_SOURCE); //$NON-NLS-1$
            addProperty("itemid", String.valueOf(extendedItem.getItemID()), EXTENDED_ITEM_SOURCE); //$NON-NLS-1$
            addProperty("local", extendedItem.getLocalItem(), EXTENDED_ITEM_SOURCE); //$NON-NLS-1$
            addProperty("titem", extendedItem.getTargetServerItem(), EXTENDED_ITEM_SOURCE); //$NON-NLS-1$
            addProperty("sitem", extendedItem.getSourceServerItem(), EXTENDED_ITEM_SOURCE); //$NON-NLS-1$
            addProperty("chg", extendedItem.getPendingChange().toString(), EXTENDED_ITEM_SOURCE); //$NON-NLS-1$
            addProperty("ochg", String.valueOf(extendedItem.hasOtherPendingChange()), EXTENDED_ITEM_SOURCE); //$NON-NLS-1$
            addProperty("lock", extendedItem.getLockLevel(), EXTENDED_ITEM_SOURCE); //$NON-NLS-1$
            addProperty("lowner", extendedItem.getLockOwner(), EXTENDED_ITEM_SOURCE); //$NON-NLS-1$
        } else {
            addProperty("ExtendedItem", Messages.getString("AdvancedPropertiesTab.Null"), TFS_ITEM_SOURCE); //$NON-NLS-1$ //$NON-NLS-2$
        }

        /*
         * Local properties
         */
        final String localPath = item.getLocalPath();
        if (localPath != null) {
            final FileSystemAttributes localFileAttributes = FileSystemUtils.getInstance().getAttributes(localPath);

            addProperty("Exists", String.valueOf(localFileAttributes.exists()), LOCAL_SOURCE); //$NON-NLS-1$
            addProperty(
                "Type", //$NON-NLS-1$
                (localFileAttributes.isDirectory() ? Messages.getString("AdvancedPropertiesTab.Directory") //$NON-NLS-1$
                    : Messages.getString("AdvancedPropertiesTab.File")), //$NON-NLS-1$
                LOCAL_SOURCE);

            if (localFileAttributes.exists() && !localFileAttributes.isDirectory()) {
                addProperty("Length", String.valueOf(localFileAttributes.getSize()), LOCAL_SOURCE); //$NON-NLS-1$
                addProperty("Readonly", String.valueOf(localFileAttributes.isReadOnly()), LOCAL_SOURCE); //$NON-NLS-1$
            }
        }
    }

    @Override
    public void populate(final TFSRepository repository, final ItemIdentifier item) {
        addProperty("Full Path", item.getItem(), TFS_ITEM_SOURCE); //$NON-NLS-1$
        addProperty("Class", item.getClass().getName(), TFS_ITEM_SOURCE); //$NON-NLS-1$
        addProperty("Identity Hashcode", Integer.toHexString(System.identityHashCode(item)), TFS_ITEM_SOURCE); //$NON-NLS-1$
        addProperty("ExtendedItem", Messages.getString("AdvancedPropertiesTab.Null"), TFS_ITEM_SOURCE); //$NON-NLS-1$ //$NON-NLS-2$
    }

    @Override
    public boolean okPressed() {
        return true;
    }

    @Override
    public String getTabItemText() {
        return Messages.getString("AdvancedPropertiesTab.TabItemText"); //$NON-NLS-1$
    }

    @Override
    public Control setupTabItemControl(final Composite parent) {
        return new AdvancedPropertiesControl(parent, SWT.NONE);
    }
}
