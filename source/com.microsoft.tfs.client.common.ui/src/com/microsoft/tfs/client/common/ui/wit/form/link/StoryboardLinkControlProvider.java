// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.wit.form.link;

import java.io.File;
import java.net.URI;
import java.text.MessageFormat;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import com.microsoft.tfs.client.common.server.TFSServer;
import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.core.artifact.ArtifactID;
import com.microsoft.tfs.core.artifact.ArtifactIDFactory;
import com.microsoft.tfs.core.clients.workitem.WorkItem;
import com.microsoft.tfs.core.clients.workitem.WorkItemClient;
import com.microsoft.tfs.core.clients.workitem.link.ExternalLink;
import com.microsoft.tfs.core.clients.workitem.link.Link;
import com.microsoft.tfs.core.clients.workitem.link.LinkFactory;
import com.microsoft.tfs.core.clients.workitem.link.LinkTextMaxLengths;
import com.microsoft.tfs.core.clients.workitem.link.RegisteredLinkType;
import com.microsoft.tfs.core.clients.workitem.link.RegisteredLinkTypeCollection;
import com.microsoft.tfs.core.clients.workitem.link.RegisteredLinkTypeNames;
import com.microsoft.tfs.core.util.URIUtils;

public class StoryboardLinkControlProvider implements LinkControlProvider {
    private Text locationText;
    private Text commentText;
    private String errorMessage;

    final private TFSServer server;

    public StoryboardLinkControlProvider(final TFSServer server) {
        this.server = server;
    }

    @Override
    public String getDisplayName(final RegisteredLinkType linkType) {
        return LinkUIRegistry.STORYBOARD_PROVIDER_DISPLAY_NAME;
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
    public boolean isValid(final boolean forEdit, final WorkItem linkingWorkItem) {
        if (!forEdit) {
            final String location = locationText.getText().trim();

            // The location cannot be empty.
            if (location.length() == 0) {
                errorMessage = Messages.getString("StoryboardLinkControlProvider.StoryboardURLCannotBeEmpty"); //$NON-NLS-1$
                return false;
            }

            // The location must be a valid storyboard artifact format.
            if (!isValidStoryboardLocation(location)) {
                final String format = Messages.getString("StoryboardLinkControlProvider.InvalidStoryboardUriFormat"); //$NON-NLS-1$
                errorMessage = MessageFormat.format(format, location);
                return false;
            }

            // The specified link must not already exist.
            if (linkAlreadyExists(location, linkingWorkItem)) {
                final String format = Messages.getString("StoryboardLinkControlProvider.StoryboardAlreadyLinkedFormat"); //$NON-NLS-1$
                errorMessage = MessageFormat.format(format, location);
                return false;
            }
        }

        return true;
    }

    @Override
    public String getErrorMessage() {
        return errorMessage;
    }

    @Override
    public Link[] getLinks() {
        return new Link[] {
            LinkFactory.newExternalLink(getStoryboardLinkType(), getArtifactID(), commentText.getText(), false)
        };
    }

    private ArtifactID getArtifactID() {
        return ArtifactIDFactory.newStoryboardArtifactID(locationText.getText());
    }

    private RegisteredLinkType getStoryboardLinkType() {
        try {
            final WorkItemClient client = (WorkItemClient) server.getConnection().getClient(WorkItemClient.class);
            final RegisteredLinkTypeCollection linkTypes = client.getRegisteredLinkTypes();
            final RegisteredLinkType linkType = linkTypes.get(RegisteredLinkTypeNames.STORYBOARD);
            if (linkType == null) {
                final String messageFormat = Messages.getString("VersionedItemLinkProvider.LinkTypeDoesNotExistFormat"); //$NON-NLS-1$
                final String message = MessageFormat.format(messageFormat, RegisteredLinkTypeNames.STORYBOARD);
                throw new IllegalStateException(message);
            }
            return linkType;
        } catch (final Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Test if the specified location is a valid URI. If not, test that it is a
     * valid file name.
     *
     *
     * @param location
     *        The user specified location to test.
     * @return True if the location is a value URI or file path format.
     */
    private boolean isValidStoryboardLocation(final String location) {
        // Allow UNC style path names with at least one segment after a host
        // name that is at least one character long.
        if (location.startsWith("\\\\")) //$NON-NLS-1$
        {
            final String path = location.substring(2);
            final int index = path.indexOf('\\');

            // must be non-slash+, slash, additional character+
            return index > 0 && path.length() > index + 1;
        }

        try {
            // Test to see if this is a valid URI. NOTE: A UNC style file path
            // succeeds when calling newURI.
            final URI uri = URIUtils.newURI(location);

            // HTTP or HTTPS must be specified.
            if ("http".equalsIgnoreCase(uri.getScheme()) == false && //$NON-NLS-1$
                "https".equalsIgnoreCase(uri.getScheme()) == false) //$NON-NLS-1$
            {
                return false;
            }

            // There must be a non-empty path specified.
            final String path = uri.getPath();
            return path != null && path.length() > 0;
        } catch (final IllegalArgumentException e) {
            try {
                new File(location).toURI().toString();
                return true;
            } catch (final Throwable t) {
            }
        }
        return false;
    }

    /**
     * Search the list of existing links associated with the specified work
     * item. Returns true if a link already exists at the specified location.
     *
     * @param location
     *        A file path or uri to a candidate storyboard.
     * @param linkingWorkItem
     *        The work item to check for a duplicate link.
     * @return True is the link already exists for this work item.
     */
    private boolean linkAlreadyExists(final String location, final WorkItem linkingWorkItem) {
        for (final Link existingLink : linkingWorkItem.getLinks()) {
            if (existingLink.getLinkType().getName().equals(RegisteredLinkTypeNames.STORYBOARD)) {
                final ArtifactID existingArtifactID = ((ExternalLink) existingLink).getArtifactID();
                if (existingArtifactID.getToolSpecificID().equalsIgnoreCase(location)) {
                    return true;
                }
            }
        }

        return false;
    }
}
