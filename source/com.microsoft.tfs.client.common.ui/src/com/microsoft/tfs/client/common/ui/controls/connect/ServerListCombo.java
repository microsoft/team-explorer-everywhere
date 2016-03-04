// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.controls.connect;

import java.net.URI;
import java.util.Set;

import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.IPostSelectionProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;

import com.microsoft.tfs.client.common.ui.framework.helper.ContentProviderAdapter;
import com.microsoft.tfs.client.common.ui.framework.sizing.ControlSize;
import com.microsoft.tfs.client.common.ui.helpers.AutomationIDHelper;
import com.microsoft.tfs.client.common.ui.helpers.ComboHelper;
import com.microsoft.tfs.core.util.serverlist.ServerList;
import com.microsoft.tfs.core.util.serverlist.ServerListConfigurationEntry;
import com.microsoft.tfs.util.CollatorFactory;

public class ServerListCombo extends Composite implements IPostSelectionProvider {
    public static final String COMBO_ID = "ServerListCombo.combo"; //$NON-NLS-1$

    private final ComboViewer viewer;
    private ServerListConfigurationEntry selectedServerListEntry;

    public ServerListCombo(final Composite parent, final int style) {
        super(parent, style);

        setLayout(new FillLayout());

        final Combo combo = new Combo(this, SWT.READ_ONLY);
        AutomationIDHelper.setWidgetID(combo, COMBO_ID);
        viewer = new ComboViewer(combo);

        viewer.setLabelProvider(new LabelProvider() {
            @Override
            public String getText(final Object element) {
                return ServerListCombo.this.getText((ServerListConfigurationEntry) element);
            }
        });

        viewer.setContentProvider(new ContentProviderAdapter() {
            @Override
            public Object[] getElements(final Object input) {
                final Set<ServerListConfigurationEntry> servers = ((ServerList) input).getServers();

                return servers.toArray(new ServerListConfigurationEntry[servers.size()]);
            }
        });

        /*
         * Set a comparator and a comparer: we need to use paths with trailing
         * slashes for comparisons internally (as this is what TFSConnection
         * uses and we need parity so that we can determine URIs from
         * connections.)
         */
        viewer.setComparator(new ViewerComparator() {
            @Override
            public int compare(final Viewer viewer, final Object e1, final Object e2) {
                return CollatorFactory.getCaseInsensitiveCollator().compare(
                    ((ServerListConfigurationEntry) e1).getName(),
                    ((ServerListConfigurationEntry) e2).getName());
            }
        });

        viewer.addSelectionChangedListener(new ISelectionChangedListener() {
            @Override
            public void selectionChanged(final SelectionChangedEvent event) {
                onSelectionChanged(event);
            }
        });
    }

    public void setServerList(final ServerList serverList) {
        viewer.setInput(serverList);

        ComboHelper.setVisibleItemCount(
            viewer.getCombo(),
            serverList.getServers() == null ? 1 : serverList.getServers().size(),
            ComboHelper.MAX_VISIBLE_ITEM_COUNT);
    }

    public void setSelectedServerURI(final URI serverURI) {
        final ServerListConfigurationEntry serverListEntry;
        final ServerList serverList = (ServerList) viewer.getInput();

        if (serverList != null && serverURI != null) {
            serverListEntry = serverList.getServer(serverURI);
        } else {
            serverListEntry = null;
        }

        setSelectedServerListEntry(serverListEntry);
    }

    public void setSelectedServerListEntry(final ServerListConfigurationEntry serverListEntry) {
        final ISelection selection =
            (serverListEntry == null) ? StructuredSelection.EMPTY : new StructuredSelection(serverListEntry);

        viewer.setSelection(selection);
    }

    @Override
    public Point computeSize(final int wHint, final int hHint, final boolean changed) {
        final Point p = super.computeSize(wHint, hHint, changed);
        return new Point(ControlSize.computeCharWidth(wHint, viewer.getCombo(), 60), p.y);
    }

    @Override
    public void setEnabled(final boolean enabled) {
        super.setEnabled(enabled);
        viewer.getCombo().setEnabled(enabled);
    }

    @Override
    public void addPostSelectionChangedListener(final ISelectionChangedListener listener) {
        viewer.addPostSelectionChangedListener(listener);
    }

    @Override
    public void removePostSelectionChangedListener(final ISelectionChangedListener listener) {
        viewer.removePostSelectionChangedListener(listener);
    }

    @Override
    public void addSelectionChangedListener(final ISelectionChangedListener listener) {
        viewer.addSelectionChangedListener(listener);
    }

    @Override
    public ISelection getSelection() {
        return viewer.getSelection();
    }

    public ServerListConfigurationEntry getSelectedServerListEntry() {
        return selectedServerListEntry;
    }

    @Override
    public void removeSelectionChangedListener(final ISelectionChangedListener listener) {
        viewer.removeSelectionChangedListener(listener);
    }

    @Override
    public void setSelection(final ISelection selection) {
        viewer.setSelection(selection);
    }

    private String getText(final ServerListConfigurationEntry serverListEntry) {
        return serverListEntry.getName();
    }

    private void onSelectionChanged(final SelectionChangedEvent event) {
        final IStructuredSelection selection = (IStructuredSelection) event.getSelection();
        selectedServerListEntry = (ServerListConfigurationEntry) selection.getFirstElement();
    }
}
