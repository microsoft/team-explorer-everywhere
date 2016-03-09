// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.controls.vc.properties;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.common.ui.vc.tfsitem.TFSItem;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ItemIdentifier;

public interface PropertiesTab {
    public void populate(TFSRepository repository, TFSItem item);

    public void populate(TFSRepository repository, ItemIdentifier itemId);

    public String getTabItemText();

    public Control setupTabItemControl(Composite parent);

    /**
     * @return <code>true</code> to allow the dialog to close,
     *         <code>false</code> to prevent the dialog from closing
     */
    public boolean okPressed();
}
