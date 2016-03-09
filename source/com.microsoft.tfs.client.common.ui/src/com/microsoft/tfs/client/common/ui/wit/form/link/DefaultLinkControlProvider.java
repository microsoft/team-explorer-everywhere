// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.wit.form.link;

import java.text.MessageFormat;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.framework.layout.GridDataBuilder;
import com.microsoft.tfs.core.clients.workitem.WorkItem;
import com.microsoft.tfs.core.clients.workitem.link.Link;
import com.microsoft.tfs.core.clients.workitem.link.RegisteredLinkType;
import com.microsoft.tfs.core.product.ProductInformation;

public class DefaultLinkControlProvider implements LinkControlProvider {
    @Override
    public String getDisplayName(final RegisteredLinkType linkType) {
        return linkType.getName();
    }

    @Override
    public String getErrorMessage() {
        return MessageFormat.format(
            Messages.getString("DefaultLinkControlProvider.ErrorMessageFormat"), //$NON-NLS-1$
            ProductInformation.getCurrent().getFamilyShortName());
    }

    @Override
    public Link[] getLinks() {
        return null;
    }

    @Override
    public void initialize(final Composite composite) {
        final Label errorLabel = new Label(composite, SWT.WRAP);
        errorLabel.setText(getErrorMessage());
        GridDataBuilder.newInstance().grab().fill().applyTo(errorLabel);
    }

    @Override
    public boolean isValid(final boolean forEdit, final WorkItem linkingWorkItem) {
        return false;
    }

    public void updateLink(final Link link) {
    }
}
