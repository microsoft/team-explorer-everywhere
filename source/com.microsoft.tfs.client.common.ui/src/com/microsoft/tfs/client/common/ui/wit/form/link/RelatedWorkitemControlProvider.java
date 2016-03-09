// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.wit.form.link;

import java.text.MessageFormat;
import java.util.ArrayList;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.microsoft.tfs.client.common.server.TFSServer;
import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.wit.dialogs.WorkItemPickerDialog;
import com.microsoft.tfs.core.clients.workitem.WorkItem;
import com.microsoft.tfs.core.clients.workitem.WorkItemClient;
import com.microsoft.tfs.core.clients.workitem.form.WIFormLinksControlWITypeFilters;
import com.microsoft.tfs.core.clients.workitem.link.Link;
import com.microsoft.tfs.core.clients.workitem.link.LinkFactory;
import com.microsoft.tfs.core.clients.workitem.link.LinkTextMaxLengths;
import com.microsoft.tfs.core.clients.workitem.link.RegisteredLinkType;
import com.microsoft.tfs.core.clients.workitem.link.WorkItemLinkTypeEnd;
import com.microsoft.tfs.core.clients.workitem.link.WorkItemLinkUtils;

public class RelatedWorkitemControlProvider implements LinkControlProvider {
    private final TFSServer server;
    private final WorkItem sourceWorkItem;
    private final WIFormLinksControlWITypeFilters wiFilters;

    private Text idText;
    private Text descriptionText;
    private Text commentText;

    private boolean validated = false;
    private boolean isValid;
    private WorkItem[] targetWorkItems;
    private WorkItemLinkTypeEnd relatedLinkType;

    private String errorMessage;

    public RelatedWorkitemControlProvider(
        final TFSServer server,
        final WorkItem sourceWorkItem,
        final WIFormLinksControlWITypeFilters wiFilters) {
        this.server = server;
        this.sourceWorkItem = sourceWorkItem;
        this.wiFilters = wiFilters;
        relatedLinkType = null;
    }

    @Override
    public String getDisplayName(final RegisteredLinkType linkType) {
        return LinkUIRegistry.WORKITEM_PROVIDER_DISPLAY_NAME;
    }

    @Override
    public void initialize(final Composite composite) {
        ((GridLayout) composite.getLayout()).numColumns = 3;

        final Label workItemIdLabel = new Label(composite, SWT.NONE);
        workItemIdLabel.setText(Messages.getString("RelatedWorkitemControlProvider.WorkItemIdLabelText")); //$NON-NLS-1$

        idText = new Text(composite, SWT.BORDER);
        idText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        idText.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(final FocusEvent e) {
                validated = false;
            }

            @Override
            public void focusLost(final FocusEvent e) {
                validate(idText.getText());
            }
        });

        final Button browseButton = new Button(composite, SWT.NONE);
        browseButton.setText(Messages.getString("RelatedWorkitemControlProvider.BrowseButtonText")); //$NON-NLS-1$
        browseButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                browseForWorkItem(((Button) e.widget).getShell());
            }
        });

        final Label descriptionLabel = new Label(composite, SWT.NONE);
        descriptionLabel.setText(Messages.getString("RelatedWorkitemControlProvider.DescriptionLabelText")); //$NON-NLS-1$

        descriptionText = new Text(composite, SWT.BORDER | SWT.READ_ONLY);
        GridData gd = new GridData(SWT.FILL, SWT.CENTER, true, false);
        gd.horizontalSpan = 2;
        descriptionText.setLayoutData(gd);

        final Label commentLabel = new Label(composite, SWT.NONE);
        commentLabel.setText(Messages.getString("RelatedWorkitemControlProvider.CommentLabelText")); //$NON-NLS-1$

        commentText = new Text(composite, SWT.BORDER);
        gd = new GridData(SWT.FILL, SWT.CENTER, true, false);
        gd.horizontalSpan = 2;
        commentText.setLayoutData(gd);
        commentText.setTextLimit(LinkTextMaxLengths.COMMENT_MAX_LENGTH);
    }

    private void browseForWorkItem(final Shell shell) {
        final WorkItemPickerDialog dialog = new WorkItemPickerDialog(
            shell,
            server,
            (WorkItemClient) server.getConnection().getClient(WorkItemClient.class),
            sourceWorkItem.getType().getProject(),
            wiFilters,
            !isRestrictedToOneLink());

        if (dialog.open() == IDialogConstants.OK_ID) {
            targetWorkItems = dialog.getSelectedWorkItems();
            idText.setText(WorkItemLinkUtils.buildCommaSeparatedWorkItemIDList(targetWorkItems));
            descriptionText.setText(WorkItemLinkUtils.buildDescriptionFromWorkItems(targetWorkItems));
            isValid = true;
            validated = true;
        }
    }

    private void validate(final String input) {
        validated = true;
        isValid = false;

        if (input.trim().length() == 0) {
            errorMessage = Messages.getString("RelatedWorkitemControlProvider.MustEnterWorkItemId"); //$NON-NLS-1$
            descriptionText.setText(getErrorDescriptionText(errorMessage));
            return;
        }

        int[] workItemIds;
        try {
            workItemIds = WorkItemLinkUtils.buildWorkItemIDListFromText(input);
        } catch (final NumberFormatException ex) {
            errorMessage = ex.getLocalizedMessage();
            descriptionText.setText(getErrorDescriptionText(errorMessage));
            return;
        }

        targetWorkItems = null;
        final ArrayList<WorkItem> success = new ArrayList<WorkItem>();
        final ArrayList<String> failed = new ArrayList<String>();

        try {
            final WorkItemClient client = (WorkItemClient) server.getConnection().getClient(WorkItemClient.class);
            for (int i = 0; i < workItemIds.length; i++) {
                final WorkItem workItem = client.getWorkItemByID(workItemIds[i]);
                if (workItem != null) {
                    success.add(workItem);
                } else {
                    failed.add(String.valueOf(workItemIds[i]));
                }
            }
        } catch (final Exception ex) {
            throw new RuntimeException(ex);
        }

        if (failed.size() > 0) {
            final String messageFormat =
                Messages.getString("RelatedWorkitemControlProvider.DoesNotExistForPermissionDeniedFormat"); //$NON-NLS-1$
            errorMessage = MessageFormat.format(messageFormat, failed.get(0));
            descriptionText.setText(getErrorDescriptionText(errorMessage));
            return;
        }

        targetWorkItems = success.toArray(new WorkItem[success.size()]);
        descriptionText.setText(WorkItemLinkUtils.buildDescriptionFromWorkItems(targetWorkItems));
        isValid = true;
    }

    public void setLinkType(final WorkItemLinkTypeEnd value) {
        relatedLinkType = value;
    }

    @Override
    public Link[] getLinks() {
        final ArrayList<Link> links = new ArrayList<Link>();
        if (targetWorkItems != null) {
            // zero represents "related" link in WIT version 1 and 2.
            final int linkId = (relatedLinkType != null) ? relatedLinkType.getID() : 0;
            for (int i = 0; i < targetWorkItems.length; i++) {
                links.add(
                    LinkFactory.newRelatedLink(
                        sourceWorkItem,
                        targetWorkItems[i],
                        linkId,
                        commentText.getText(),
                        false));
            }
        }
        return links.toArray(new Link[links.size()]);
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

        if (!validated) {
            validate(idText.getText());
        }

        if (!isValid) {
            return false;
        }

        // Check for a link to itself.
        for (int i = 0; i < targetWorkItems.length; i++) {
            final WorkItem targetWorkItem = targetWorkItems[i];
            if (targetWorkItem.getFields().getID() == linkingWorkItem.getFields().getID()) {
                errorMessage = Messages.getString("RelatedWorkitemControlProvider.CannotHaveRelatedLinkToSelf"); //$NON-NLS-1$
                targetWorkItems = null;
                isValid = false;
                return false;
            }
        }

        // Check for a link to an excluded type if filters are specified.
        if (wiFilters != null) {
            StringBuffer sb = null;
            for (int i = 0; i < targetWorkItems.length; i++) {
                final WorkItem targetWorkItem = targetWorkItems[i];
                if (!wiFilters.includes(targetWorkItem.getType().getName())) {
                    if (sb == null) {
                        sb = new StringBuffer();
                    }
                    if (sb.length() > 0) {
                        sb.append(Messages.getString("RelatedWorkitemControlProvider.WorkItemIdListSeparator")); //$NON-NLS-1$
                    }
                    sb.append(String.valueOf(targetWorkItem.getID()));
                }
            }

            if (sb != null) {
                final String messageFormat =
                    Messages.getString("RelatedWorkitemControlProvider.CannotLinkToItemsFormat"); //$NON-NLS-1$
                errorMessage = MessageFormat.format(messageFormat, sb.toString());
                isValid = false;
                return false;
            }
        }

        // Make sure we aren't multi-selecting for a one-to-many link.
        if (isRestrictedToOneLink() && targetWorkItems.length > 1) {
            final String messageFormat = Messages.getString("RelatedWorkitemControlProvider.LinkTypeRestrictedFormat"); //$NON-NLS-1$
            errorMessage = MessageFormat.format(messageFormat, relatedLinkType.getName());
            isValid = false;
            return false;
        }

        return true;
    }

    /**
     * Returns true if the selected work item link type is restricted to one
     * link.
     */
    private boolean isRestrictedToOneLink() {
        return relatedLinkType != null
            && relatedLinkType.getLinkType().isOneToMany()
            && !relatedLinkType.isForwardLink();
    }

    private String getErrorDescriptionText(final String message) {
        final String messageFormat = Messages.getString("RelatedWorkitemControlProvider.ErrorDescriptionFormat"); //$NON-NLS-1$
        return MessageFormat.format(messageFormat, message);
    }
}
