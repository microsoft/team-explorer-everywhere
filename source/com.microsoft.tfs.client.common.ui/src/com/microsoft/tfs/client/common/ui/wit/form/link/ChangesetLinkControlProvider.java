// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.wit.form.link;

import java.text.MessageFormat;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.common.server.TFSServer;
import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.TFSCommonUIClientPlugin;
import com.microsoft.tfs.client.common.ui.dialogs.vc.FindChangesetDialog;
import com.microsoft.tfs.core.artifact.ArtifactID;
import com.microsoft.tfs.core.artifact.ArtifactIDFactory;
import com.microsoft.tfs.core.clients.versioncontrol.VersionControlClient;
import com.microsoft.tfs.core.clients.workitem.WorkItem;
import com.microsoft.tfs.core.clients.workitem.WorkItemClient;
import com.microsoft.tfs.core.clients.workitem.link.Link;
import com.microsoft.tfs.core.clients.workitem.link.LinkFactory;
import com.microsoft.tfs.core.clients.workitem.link.LinkTextMaxLengths;
import com.microsoft.tfs.core.clients.workitem.link.RegisteredLinkType;
import com.microsoft.tfs.core.clients.workitem.link.RegisteredLinkTypeCollection;
import com.microsoft.tfs.core.clients.workitem.link.RegisteredLinkTypeNames;

public class ChangesetLinkControlProvider implements LinkControlProvider {
    private final TFSServer server;

    private Text changesetText;
    private Text commentText;

    private ArtifactID changesetArtifactId;

    private String errorMessage;

    public ChangesetLinkControlProvider(final TFSServer server) {
        this.server = server;
    }

    @Override
    public String getDisplayName(final RegisteredLinkType linkType) {
        return LinkUIRegistry.CHANGESET_PROVIDER_DISPLAY_NAME;
    }

    @Override
    public void initialize(final Composite composite) {
        ((GridLayout) composite.getLayout()).numColumns = 3;

        final Label changeSetLabel = new Label(composite, SWT.NONE);
        changeSetLabel.setText(Messages.getString("ChangesetLinkControlProvider.ChangesetLabelText")); //$NON-NLS-1$

        changesetText = new Text(composite, SWT.BORDER);
        changesetText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        final Button browseButton = new Button(composite, SWT.NONE);
        browseButton.setText(Messages.getString("ChangesetLinkControlProvider.BrowseButtonText")); //$NON-NLS-1$
        browseButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                browseForChangeset(((Button) e.widget).getShell());
            }
        });

        final Label commentLabel = new Label(composite, SWT.NONE);
        commentLabel.setText(Messages.getString("ChangesetLinkControlProvider.CommentLabelText")); //$NON-NLS-1$

        commentText = new Text(composite, SWT.BORDER);
        final GridData gd = new GridData(SWT.FILL, SWT.CENTER, true, false);
        gd.horizontalSpan = 2;
        commentText.setLayoutData(gd);
        commentText.setTextLimit(LinkTextMaxLengths.COMMENT_MAX_LENGTH);
    }

    private void browseForChangeset(final Shell shell) {
        final TFSRepository repository =
            TFSCommonUIClientPlugin.getDefault().getProductPlugin().getRepositoryManager().getDefaultRepository();
        final FindChangesetDialog changesetDialog = new FindChangesetDialog(shell, repository);

        if (changesetDialog.open() == IDialogConstants.OK_ID) {
            final int id = changesetDialog.getSelectedChangeset().getChangesetID();
            if (id > 0) {
                changesetText.setText(String.valueOf(id));
            }
        }
    }

    @Override
    public Link[] getLinks() {
        return new Link[] {
            LinkFactory.newExternalLink(getChangesetLinkType(), changesetArtifactId, commentText.getText(), false)
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
    public boolean isValid(final boolean forEdit, final WorkItem linkingWorkitem) {
        if (forEdit) {
            return true;
        }

        final String inputText = changesetText.getText();

        if (inputText.trim().length() == 0) {
            errorMessage = Messages.getString("ChangesetLinkControlProvider.MustSpecifiyChangesetId"); //$NON-NLS-1$
            return false;
        }

        int inputId;
        try {
            inputId = Integer.parseInt(inputText);
        } catch (final NumberFormatException ex) {
            final String messageFormat = Messages.getString("ChangesetLinkControlProvider.InvalidChangsetIdFormat"); //$NON-NLS-1$
            errorMessage = MessageFormat.format(messageFormat, inputText);
            return false;
        }

        if (inputId <= 0) {
            final String messageFormat = Messages.getString("ChangesetLinkControlProvider.MustBeGreaterThanZeroFormat"); //$NON-NLS-1$
            errorMessage = MessageFormat.format(messageFormat, Integer.toString(inputId));
            return false;
        }

        VersionControlClient client;
        try {
            client = (VersionControlClient) server.getConnection().getClient(VersionControlClient.class);
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }

        try {
            client.getChangeset(inputId);
        } catch (final Exception ex) {
            final String messageFormat = Messages.getString("ChangesetLinkControlProvider.ChangesetDoesNotExistFormat"); //$NON-NLS-1$
            errorMessage = MessageFormat.format(messageFormat, inputText);
            return false;
        }

        changesetArtifactId = ArtifactIDFactory.newChangesetArtifactID(inputId);

        return true;
    }

    private RegisteredLinkType getChangesetLinkType() {
        try {
            final WorkItemClient client = (WorkItemClient) server.getConnection().getClient(WorkItemClient.class);
            final RegisteredLinkTypeCollection linkTypes = client.getRegisteredLinkTypes();
            final RegisteredLinkType linkType = linkTypes.get(RegisteredLinkTypeNames.CHANGESET);
            if (linkType == null) {
                final String messageFormat =
                    Messages.getString("ChangesetLinkControlProvider.LinkTypeDidNotExistFormat"); //$NON-NLS-1$
                final String message = MessageFormat.format(messageFormat, RegisteredLinkTypeNames.CHANGESET);
                throw new IllegalStateException(message);
            }
            return linkType;
        } catch (final Exception ex) {
            throw new RuntimeException(ex);
        }
    }
}
