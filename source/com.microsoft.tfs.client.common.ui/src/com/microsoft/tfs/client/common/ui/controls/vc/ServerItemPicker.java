// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.controls.vc;

import java.util.Arrays;

import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;

import com.microsoft.tfs.client.common.ui.framework.helper.SWTUtil;
import com.microsoft.tfs.client.common.ui.framework.layout.GridDataBuilder;
import com.microsoft.tfs.client.common.ui.vc.serveritem.ServerItemSource;
import com.microsoft.tfs.client.common.ui.vc.serveritem.ServerItemType;
import com.microsoft.tfs.client.common.ui.vc.serveritem.TypedServerItem;
import com.microsoft.tfs.core.clients.versioncontrol.path.ServerPath;
import com.microsoft.tfs.util.Check;

public class ServerItemPicker extends Composite implements ServerItemControl {
    private final ServerPathCombo combo;
    private final ServerItemTable serverItemTable;

    private ServerItemSource serverItemSource;
    private TypedServerItem[] currentHierarchy;

    public ServerItemPicker(final Composite parent, final int style) {
        super(parent, style);

        final GridLayout layout = SWTUtil.gridLayout(this, 1);

        combo = new ServerPathCombo(this, SWT.NONE);

        GridDataBuilder.newInstance().hGrab().hFill().applyTo(combo);

        combo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                setCurrentFolderPath(combo.getPath());
            }
        });

        serverItemTable = new ServerItemTable(this, SWT.NONE);
        GridDataBuilder.newInstance().grab().fill().hSpan(layout).applyTo(serverItemTable);

        serverItemTable.addDoubleClickListener(new IDoubleClickListener() {
            @Override
            public void doubleClick(final DoubleClickEvent event) {
                final IStructuredSelection selection = (IStructuredSelection) event.getSelection();
                final TypedServerItem item = (TypedServerItem) selection.getFirstElement();
                if (item.getType() == ServerItemType.FILE) {
                    return;
                }
                setCurrentFolder(item);
            }
        });
    }

    public void setServerItemSource(final ServerItemSource serverItemSource) {
        this.serverItemSource = serverItemSource;
        setCurrentFolder(null);
    }

    @Override
    public void setSelectedItem(final TypedServerItem selectedItem) {
        Check.notNull(selectedItem, "selectedItem"); //$NON-NLS-1$

        if (selectedItem.getType() == ServerItemType.ROOT) {
            setCurrentFolderPath(ServerPath.ROOT);
            return;
        }

        setCurrentFolder(selectedItem.getParent());
        serverItemTable.setSelectedServerItem(selectedItem);
    }

    @Override
    public TypedServerItem getSelectedItem() {
        return serverItemTable.getSelectedServerItem();
    }

    public void setCurrentFolderPath(final String serverPath) {
        setCurrentFolder(new TypedServerItem(serverPath, ServerItemType.FOLDER));
    }

    public void setCurrentFolder(final TypedServerItem currentFolder) {
        if (currentFolder == null) {
            currentHierarchy = null;
            combo.clear();
            combo.setEnabled(false);
            serverItemTable.setServerItems(null);
            return;
        }

        if (serverItemSource == null) {
            throw new IllegalStateException(
                "You must set a non-null ServerItemSource before setting a non-null ServerItem"); //$NON-NLS-1$
        }

        combo.setServerName(serverItemSource.getServerName());

        if (currentHierarchy != null && currentHierarchy[currentHierarchy.length - 1].equals(currentFolder)) {
            return;
        }

        currentHierarchy = currentFolder.getHierarchy();

        combo.setPath(currentFolder.getServerPath());
        combo.setEnabled(true);

        final TypedServerItem[] childItems =
            serverItemSource.getChildren(currentHierarchy[currentHierarchy.length - 1]);
        Arrays.sort(childItems);
        serverItemTable.setServerItems(childItems);
        serverItemTable.selectFirst();

        serverItemTable.setFocus();
    }

    public ServerItemTable getServerItemTable() {
        return serverItemTable;
    }
}
