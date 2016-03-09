// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.wit.form.link;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.MessageFormat;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.common.server.TFSServer;
import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.TFSCommonUIClientPlugin;
import com.microsoft.tfs.client.common.ui.dialogs.vc.FindChangesetDialog;
import com.microsoft.tfs.client.common.ui.dialogs.vc.ServerItemTreeDialog;
import com.microsoft.tfs.client.common.ui.vc.serveritem.ServerItemType;
import com.microsoft.tfs.client.common.ui.vc.serveritem.VersionedItemSource;
import com.microsoft.tfs.core.artifact.ArtifactID;
import com.microsoft.tfs.core.clients.registration.ToolNames;
import com.microsoft.tfs.core.clients.versioncontrol.VersionControlClient;
import com.microsoft.tfs.core.clients.versioncontrol.VersionControlConstants;
import com.microsoft.tfs.core.clients.versioncontrol.path.ServerPath;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Item;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.ChangesetVersionSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.LatestVersionSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.VersionSpec;
import com.microsoft.tfs.core.clients.workitem.WorkItem;
import com.microsoft.tfs.core.clients.workitem.WorkItemClient;
import com.microsoft.tfs.core.clients.workitem.link.Link;
import com.microsoft.tfs.core.clients.workitem.link.LinkFactory;
import com.microsoft.tfs.core.clients.workitem.link.LinkTextMaxLengths;
import com.microsoft.tfs.core.clients.workitem.link.RegisteredLinkType;
import com.microsoft.tfs.core.clients.workitem.link.RegisteredLinkTypeCollection;
import com.microsoft.tfs.core.clients.workitem.link.RegisteredLinkTypeNames;

public class VersionedItemLinkProvider implements LinkControlProvider {
    /**
     * IMPLEMENTATION DETAIL (keep private) The encoding to use when URL
     * encoding/decoding. This must match {@link ArtifactID}.
     */
    private static final String URL_ENCODING = "UTF-8"; //$NON-NLS-1$

    private Text versionedItemText;
    private Text changesetText;
    private Text commentText;

    private boolean latestVersionMode;

    private ArtifactID versionedItemArtifactId;

    private String errorMessage;

    private final TFSServer server;

    public VersionedItemLinkProvider(final TFSServer server) {
        this.server = server;
    }

    @Override
    public String getDisplayName(final RegisteredLinkType linkType) {
        return LinkUIRegistry.VERSIONEDITEM_PROVIDER_DISPLAY_NAME;
    }

    @Override
    public void initialize(final Composite composite) {
        ((GridLayout) composite.getLayout()).numColumns = 5;

        final Label versionedItemLabel = new Label(composite, SWT.NONE);
        versionedItemLabel.setText(Messages.getString("VersionedItemLinkProvider.VersionedItemLabelText")); //$NON-NLS-1$

        versionedItemText = new Text(composite, SWT.BORDER);
        GridData gd = new GridData(SWT.FILL, SWT.CENTER, true, false);
        gd.horizontalSpan = 3;
        versionedItemText.setLayoutData(gd);

        final Button browseVersionedItemsButton = new Button(composite, SWT.NONE);
        browseVersionedItemsButton.setText(Messages.getString("VersionedItemLinkProvider.BrowseButtonText")); //$NON-NLS-1$
        browseVersionedItemsButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                browseForVersionedItem(((Button) e.widget).getShell());
            }
        });

        final Label versionLabel = new Label(composite, SWT.NONE);
        versionLabel.setText(Messages.getString("VersionedItemLinkProvider.VersionLabelText")); //$NON-NLS-1$

        final Combo linkToCombo = new Combo(composite, SWT.DROP_DOWN | SWT.READ_ONLY);
        linkToCombo.add(Messages.getString("VersionedItemLinkProvider.LatestVersionChoice")); //$NON-NLS-1$
        linkToCombo.add(Messages.getString("VersionedItemLinkProvider.ChangesetChoice")); //$NON-NLS-1$

        final Label changesetLabel = new Label(composite, SWT.NONE);
        changesetLabel.setText(Messages.getString("VersionedItemLinkProvider.ChangesetLabelText")); //$NON-NLS-1$

        changesetText = new Text(composite, SWT.BORDER);
        changesetText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));

        final Button browseChangesetsButton = new Button(composite, SWT.NONE);
        browseChangesetsButton.setText(Messages.getString("VersionedItemLinkProvider.BrowseButton2Text")); //$NON-NLS-1$
        browseChangesetsButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                browseForChangeset(((Button) e.widget).getShell());
            }
        });

        final Label commentLabel = new Label(composite, SWT.NONE);
        commentLabel.setText(Messages.getString("VersionedItemLinkProvider.CommentLabelText")); //$NON-NLS-1$

        commentText = new Text(composite, SWT.BORDER);
        gd = new GridData(SWT.FILL, SWT.CENTER, true, false);
        gd.horizontalSpan = 4;
        commentText.setLayoutData(gd);
        commentText.setTextLimit(LinkTextMaxLengths.COMMENT_MAX_LENGTH);

        latestVersionMode = true;
        linkToCombo.select(0);
        changesetLabel.setVisible(false);
        changesetText.setVisible(false);
        browseChangesetsButton.setVisible(false);

        linkToCombo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                latestVersionMode = (linkToCombo.getSelectionIndex() == 0);
                changesetLabel.setVisible(!latestVersionMode);
                changesetText.setVisible(!latestVersionMode);
                browseChangesetsButton.setVisible(!latestVersionMode);
            }
        });
    }

    private void browseForChangeset(final Shell shell) {
        final TFSRepository repository =
            TFSCommonUIClientPlugin.getDefault().getProductPlugin().getRepositoryManager().getDefaultRepository();
        final FindChangesetDialog changesetDialog = new FindChangesetDialog(shell, repository);

        final String inputPath = versionedItemText.getText().trim();
        if (ServerPath.isServerPath(inputPath)) {
            changesetDialog.setPath(inputPath);
        }

        if (changesetDialog.open() == IDialogConstants.OK_ID) {
            final int id = changesetDialog.getSelectedChangeset().getChangesetID();
            if (id > 0) {
                changesetText.setText(String.valueOf(id));
            }
        }
    }

    private void browseForVersionedItem(final Shell shell) {
        final String startingItem =
            ServerPath.isServerPath(versionedItemText.getText()) ? versionedItemText.getText() : ServerPath.ROOT;

        final ServerItemTreeDialog chooseItemDialog =
            new ServerItemTreeDialog(
                shell,
                Messages.getString("VersionedItemLinkProvider.BrowseDialogTitle"), //$NON-NLS-1$
                startingItem,
                new VersionedItemSource(server),
                ServerItemType.ALL);

        if (chooseItemDialog.open() == IDialogConstants.OK_ID) {
            versionedItemText.setText(chooseItemDialog.getSelectedServerPath());
        }
    }

    @Override
    public Link[] getLinks() {
        return new Link[] {
            LinkFactory.newExternalLink(
                getVersionedItemLinkType(),
                versionedItemArtifactId,
                commentText.getText(),
                false)
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
        if (forEdit) {
            return true;
        }

        final String inputPath = versionedItemText.getText().trim();
        if (inputPath.length() == 0) {
            errorMessage = Messages.getString("VersionedItemLinkProvider.EnterPathToVersionedItem"); //$NON-NLS-1$
            return false;
        }

        if (!inputPath.startsWith("$/")) //$NON-NLS-1$
        {
            errorMessage = Messages.getString("VersionedItemLinkProvider.VersionedItemPathInvalid"); //$NON-NLS-1$
            return false;
        }

        VersionSpec versionSpec;

        int changesetId = -1;
        if (!latestVersionMode) {
            final String inputChangeset = changesetText.getText();
            if (inputChangeset.trim().length() == 0) {
                errorMessage = Messages.getString("VersionedItemLinkProvider.MustSpecifyChangesetId"); //$NON-NLS-1$
                return false;
            }

            try {
                changesetId = Integer.parseInt(inputChangeset);
            } catch (final NumberFormatException ex) {
                final String messageFormat = Messages.getString("VersionedItemLinkProvider.InvalidChangesetIdFormat"); //$NON-NLS-1$
                errorMessage = MessageFormat.format(messageFormat, inputChangeset);
                return false;
            }

            if (changesetId <= 0) {
                final String messageFormat =
                    Messages.getString("VersionedItemLinkProvider.MustBeGreaterThanZeroFormat"); //$NON-NLS-1$
                errorMessage = MessageFormat.format(messageFormat, Integer.toString(changesetId));
                return false;
            }

            versionSpec = new ChangesetVersionSpec(changesetId);
        } else {
            versionSpec = LatestVersionSpec.INSTANCE;
        }

        VersionControlClient client;
        try {
            client = (VersionControlClient) server.getConnection().getClient(VersionControlClient.class);
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }

        Item item = null;
        try {
            item = client.getItem(inputPath, versionSpec);
        } catch (final Exception ex) {
            errorMessage = ex.getLocalizedMessage();
            return false;
        }

        if (item == null) {
            if (latestVersionMode) {
                final String messageFormat =
                    Messages.getString("VersionedItemLinkProvider.SpecifiedVersionNotFoundFormat"); //$NON-NLS-1$
                errorMessage = MessageFormat.format(messageFormat, inputPath);
            } else {
                final String messageFormat = Messages.getString("VersionedItemLinkProvider.ItemDoesNotExistFormat"); //$NON-NLS-1$
                errorMessage = MessageFormat.format(messageFormat, inputPath, Integer.toString(changesetId));
            }
            return false;
        }

        String toolSpecificId;
        String artifactType;
        if (latestVersionMode) {
            toolSpecificId = String.valueOf(item.getItemID());
            artifactType = VersionControlConstants.LATEST_ITEM_ARTIFACT_TYPE;
        } else {
            try {
                /*
                 * Skip the leading $/ when creating the input path, then URL
                 * escape it. This is to escape any special URL characters (eg,
                 * '&'). Yes, we encode the item twice. This is what the server
                 * expects and requires, and very bad things can happen if you
                 * do not do this.
                 */
                final String escapedPath = URLEncoder.encode(inputPath.substring(2), URL_ENCODING);

                toolSpecificId = URLEncoder.encode(escapedPath
                    + "&changesetVersion=" //$NON-NLS-1$
                    + changesetId
                    + "&deletionId=" //$NON-NLS-1$
                    + item.getDeletionID(), URL_ENCODING);
            } catch (final UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }

            artifactType = VersionControlConstants.VERSIONED_ITEM_ARTIFACT_TYPE;
        }

        versionedItemArtifactId = new ArtifactID(ToolNames.VERSION_CONTROL, artifactType, toolSpecificId);

        return true;
    }

    private RegisteredLinkType getVersionedItemLinkType() {
        try {
            final WorkItemClient client = (WorkItemClient) server.getConnection().getClient(WorkItemClient.class);
            final RegisteredLinkTypeCollection linkTypes = client.getRegisteredLinkTypes();
            final RegisteredLinkType linkType = linkTypes.get(RegisteredLinkTypeNames.VERSIONED_ITEM);
            if (linkType == null) {
                final String messageFormat = Messages.getString("VersionedItemLinkProvider.LinkTypeDoesNotExistFormat"); //$NON-NLS-1$
                final String message = MessageFormat.format(messageFormat, RegisteredLinkTypeNames.VERSIONED_ITEM);
                throw new IllegalStateException(message);
            }
            return linkType;
        } catch (final Exception ex) {
            throw new RuntimeException(ex);
        }
    }

}
