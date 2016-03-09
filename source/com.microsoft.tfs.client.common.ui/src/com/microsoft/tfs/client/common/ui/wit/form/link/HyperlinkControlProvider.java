// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.wit.form.link;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.core.clients.workitem.WorkItem;
import com.microsoft.tfs.core.clients.workitem.link.Link;
import com.microsoft.tfs.core.clients.workitem.link.LinkFactory;
import com.microsoft.tfs.core.clients.workitem.link.LinkTextMaxLengths;
import com.microsoft.tfs.core.clients.workitem.link.RegisteredLinkType;

public class HyperlinkControlProvider implements LinkControlProvider {
    private Text locationText;
    private Text commentText;
    private String errorMessage;

    @Override
    public String getDisplayName(final RegisteredLinkType linkType) {
        return LinkUIRegistry.HYPERLINK_PROVIDER_DISPLAY_NAME;
    }

    @Override
    public void initialize(final Composite composite) {
        final Label locationLabel = new Label(composite, SWT.NONE);
        locationLabel.setText(Messages.getString("HyperlinkControlProvider.LocationLabelText")); //$NON-NLS-1$

        locationText = new Text(composite, SWT.BORDER);
        locationText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        locationText.setTextLimit(LinkTextMaxLengths.HYPERLINK_LOCATION_MAX_LENGTH);

        final Label commentLabel = new Label(composite, SWT.NONE);
        commentLabel.setText(Messages.getString("HyperlinkControlProvider.CommentLabelText")); //$NON-NLS-1$

        commentText = new Text(composite, SWT.BORDER);
        commentText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        commentText.setTextLimit(LinkTextMaxLengths.COMMENT_MAX_LENGTH);
    }

    @Override
    public Link[] getLinks() {
        return new Link[] {
            LinkFactory.newHyperlink(locationText.getText(), commentText.getText(), false)
        };
    }

    public void updateLink(final Link link) {
        link.setComment(commentText.getText());
    }

    @Override
    public String getErrorMessage() {
        return errorMessage;
    }

    @Override
    public boolean isValid(final boolean forEdit, final WorkItem linkingWorkItem) {
        if (!forEdit) {
            if (locationText.getText().trim().length() == 0) {
                errorMessage = Messages.getString("HyperlinkControlProvider.HyperlinkCannotBeBlank"); //$NON-NLS-1$
                return false;
            }
        }

        return true;
    }
}
