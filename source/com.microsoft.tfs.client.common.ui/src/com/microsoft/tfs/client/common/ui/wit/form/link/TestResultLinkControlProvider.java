// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.wit.form.link;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.framework.layout.GridDataBuilder;
import com.microsoft.tfs.core.clients.workitem.WorkItem;
import com.microsoft.tfs.core.clients.workitem.link.Link;
import com.microsoft.tfs.core.clients.workitem.link.RegisteredLinkType;

/**
 * Test result link control provider contains only a label which instructs users
 * how to create this type of link. This provider always returns true for
 * isValid and an empty link array so that the OK button can be pressed to
 * dismiss the dialog without altering any links.
 */
public class TestResultLinkControlProvider implements LinkControlProvider {
    @Override
    public String getDisplayName(final RegisteredLinkType linkType) {
        return LinkUIRegistry.TESTRESULT_PROVIDER_DISPLAY_NAME;
    }

    @Override
    public void initialize(final Composite composite) {
        final Label label = new Label(composite, SWT.WRAP);
        label.setText(Messages.getString("TestResultLinkControlProvider.SummaryText")); //$NON-NLS-1$
        GridDataBuilder.newInstance().fill().hint(300, SWT.DEFAULT).hGrab().vAlign(SWT.CENTER).applyTo(label);
    }

    @Override
    public Link[] getLinks() {
        return new Link[0];
    }

    public void updateLink(final Link link) {
    }

    @Override
    public String getErrorMessage() {
        return ""; //$NON-NLS-1$
    }

    @Override
    public boolean isValid(final boolean forEdit, final WorkItem linkingWorkItem) {
        return true;
    }
}
