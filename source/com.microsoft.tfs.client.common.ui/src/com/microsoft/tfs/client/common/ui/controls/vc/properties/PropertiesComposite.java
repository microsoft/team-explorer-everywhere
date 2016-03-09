// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.controls.vc.properties;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;

import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.common.ui.controls.generic.BaseControl;
import com.microsoft.tfs.client.common.ui.vc.tfsitem.TFSItem;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ItemIdentifier;

public class PropertiesComposite extends BaseControl {
    private final TFSRepository repository;
    private final TFSItem item;
    private final ItemIdentifier itemId;
    private TabFolder tabFolder;
    private final List<PropertiesTab> propertiesTabs = new ArrayList<PropertiesTab>();
    private final Map<TabItem, PropertiesTab> mapTabItemsToPropertiesTabs = new HashMap<TabItem, PropertiesTab>();
    private final Set<TabItem> initializedTabItems = new HashSet<TabItem>();

    public PropertiesComposite(
        final Composite parent,
        final int style,
        final TFSRepository repository,
        final TFSItem item,
        final ItemIdentifier itemId) {
        super(parent, style);
        this.repository = repository;
        this.item = item;
        this.itemId = itemId;
    }

    @Override
    public void dispose() {
        super.dispose();
    }

    public void addPropertiesTab(final PropertiesTab tab) {
        propertiesTabs.add(tab);
    }

    public void fillTabFolder() {
        tabFolder = new TabFolder(this, SWT.NONE);

        int ix = 0;
        for (final Iterator<PropertiesTab> it = propertiesTabs.iterator(); it.hasNext();) {
            final PropertiesTab tab = it.next();
            final TabItem item = new TabItem(tabFolder, SWT.NONE, ix++);
            item.setText(tab.getTabItemText());

            /*
             * Build a container composite to add padding around the control for
             * the tab item
             */
            final Composite tabItemContainer = new Composite(tabFolder, SWT.NONE);
            final FillLayout tabItemContainerLayout = new FillLayout();
            tabItemContainerLayout.marginWidth = getHorizontalMargin();
            tabItemContainerLayout.marginHeight = getVerticalMargin();
            tabItemContainer.setLayout(tabItemContainerLayout);

            tab.setupTabItemControl(tabItemContainer);

            item.setControl(tabItemContainer);
            mapTabItemsToPropertiesTabs.put(item, tab);
        }

        tabFolder.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                ensureSelectedTabItemsInitialized();
            }
        });

        tabFolder.setSelection(0);

        // sizeConstraint = new TableMinimumSizeConstraint(3, 10, new int[]
        // {40,40,40});
        // sizeConstraint.computeMinimumSize(table, getShell());

        ensureSelectedTabItemsInitialized();

        final FillLayout layout = new FillLayout();
        layout.marginWidth = 0;
        layout.marginHeight = 0;
        setLayout(layout);
    }

    private void ensureSelectedTabItemsInitialized() {
        final TabItem[] tabItems = tabFolder.getSelection();
        for (int i = 0; i < tabItems.length; i++) {
            if (!initializedTabItems.contains(tabItems[i])) {
                final PropertiesTab tab = mapTabItemsToPropertiesTabs.get(tabItems[i]);
                if (item != null) {
                    tab.populate(repository, item);
                }
                if (itemId != null) {
                    tab.populate(repository, itemId);
                }
                initializedTabItems.add(tabItems[i]);
            }
        }
    }
}
