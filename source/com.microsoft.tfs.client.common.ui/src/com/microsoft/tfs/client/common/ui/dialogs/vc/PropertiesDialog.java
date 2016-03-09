// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.dialogs.vc;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.controls.vc.properties.PropertiesComposite;
import com.microsoft.tfs.client.common.ui.controls.vc.properties.PropertiesTab;
import com.microsoft.tfs.client.common.ui.framework.dialog.BaseDialog;
import com.microsoft.tfs.client.common.ui.vc.tfsitem.TFSItem;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ItemIdentifier;

public class PropertiesDialog extends BaseDialog {
    private final TFSRepository repository;
    private final TFSItem item;
    private final ItemIdentifier itemId;
    private final List<PropertiesTab> propertiesTabs = new ArrayList<PropertiesTab>();

    public PropertiesDialog(
        final Shell parentShell,
        final TFSRepository repository,
        final TFSItem item,
        final ItemIdentifier itemId) {
        super(parentShell);

        this.repository = repository;
        this.item = item;
        this.itemId = itemId;
    }

    public void addPropertiesTab(final PropertiesTab tab) {
        propertiesTabs.add(tab);
    }

    @Override
    protected Control createDialogArea(final Composite parent) {
        final Composite container = (Composite) super.createDialogArea(parent);
        final FillLayout fillLayout = new FillLayout();
        fillLayout.marginWidth = getHorizontalMargin();
        fillLayout.marginHeight = getVerticalMargin();
        container.setLayout(fillLayout);

        final PropertiesComposite props = new PropertiesComposite(container, SWT.NONE, repository, item, itemId);
        for (final Iterator<PropertiesTab> it = propertiesTabs.iterator(); it.hasNext();) {
            final PropertiesTab tab = it.next();
            props.addPropertiesTab(tab);
        }
        props.fillTabFolder();
        return container;
    }

    @Override
    protected void createButtonsForButtonBar(final Composite parent) {
        createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
        createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
    }

    @Override
    protected String provideDialogTitle() {
        String name = ""; //$NON-NLS-1$
        if (item != null) {
            name = item.getName();
        }
        if (itemId != null) {
            name = itemId.getItem();
        }

        final String messageFormat = Messages.getString("PropertiesDialog.DialogTitleFormat"); //$NON-NLS-1$
        return MessageFormat.format(messageFormat, name);
    }

    @Override
    public boolean close() {
        if (getReturnCode() == Dialog.OK) {
            final Iterator<PropertiesTab> iter = propertiesTabs.iterator();
            while (iter.hasNext()) {
                final PropertiesTab tab = iter.next();
                if (!tab.okPressed()) {
                    return false;
                }
            }
        }

        return super.close();
    }
}
