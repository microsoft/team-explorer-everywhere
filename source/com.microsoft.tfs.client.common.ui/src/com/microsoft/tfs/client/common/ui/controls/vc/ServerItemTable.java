// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.controls.vc;

import org.eclipse.swt.widgets.Composite;

import com.microsoft.tfs.client.common.ui.framework.table.TableControl;
import com.microsoft.tfs.client.common.ui.vc.serveritem.ServerItemLabelProvider;
import com.microsoft.tfs.client.common.ui.vc.serveritem.ServerItemType;
import com.microsoft.tfs.client.common.ui.vc.serveritem.TypedServerItem;

public class ServerItemTable extends TableControl {
    public ServerItemTable(final Composite parent, final int style) {
        this(parent, style, null);
    }

    public ServerItemTable(final Composite parent, final int style, final String viewDataKey) {
        super(parent, style, TypedServerItem.class, viewDataKey);

        setUseDefaultContentProvider();
        getViewer().setLabelProvider(new ServerItemLabelProvider());
    }

    public void setServerItems(final TypedServerItem[] serverItems) {
        setElements(serverItems);
    }

    public TypedServerItem[] getServerItems() {
        return (TypedServerItem[]) getElements();
    }

    public void setSelectedServerItems(final TypedServerItem[] serverItems) {
        setSelectedElements(serverItems);
    }

    public void setSelectedServerItem(final TypedServerItem serverItem) {
        final String serverItemName = serverItem.getName();
        final ServerItemType serverItemType = serverItem.getType();

        for (final TypedServerItem item : getServerItems()) {
            if (item.getType() == serverItemType && item.getName().equalsIgnoreCase(serverItemName)) {
                setSelectedElement(serverItem);
                break;
            }
        }
    }

    public TypedServerItem[] getSelectedServerItems() {
        return (TypedServerItem[]) getSelectedElements();
    }

    public TypedServerItem getSelectedServerItem() {
        return (TypedServerItem) getSelectedElement();
    }
}
