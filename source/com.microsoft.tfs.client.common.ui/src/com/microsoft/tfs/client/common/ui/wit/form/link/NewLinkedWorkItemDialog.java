// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.wit.form.link;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.TFSCommonUIClientPlugin;
import com.microsoft.tfs.client.common.ui.framework.dialog.BaseDialog;
import com.microsoft.tfs.client.common.ui.framework.sizing.ControlSize;
import com.microsoft.tfs.client.common.ui.helpers.ComboHelper;
import com.microsoft.tfs.core.clients.workitem.WorkItem;
import com.microsoft.tfs.core.clients.workitem.WorkItemClient;
import com.microsoft.tfs.core.clients.workitem.exceptions.WorkItemLinkValidationException;
import com.microsoft.tfs.core.clients.workitem.form.WIFormLinksControlOptions;
import com.microsoft.tfs.core.clients.workitem.form.WIFormLinksControlWILinkFilters;
import com.microsoft.tfs.core.clients.workitem.form.WIFormLinksControlWITypeFilters;
import com.microsoft.tfs.core.clients.workitem.link.LinkTextMaxLengths;
import com.microsoft.tfs.core.clients.workitem.link.WorkItemLinkTypeCollection;
import com.microsoft.tfs.core.clients.workitem.link.WorkItemLinkTypeEnd;
import com.microsoft.tfs.core.clients.workitem.link.WorkItemLinkTypeEndCollection;
import com.microsoft.tfs.core.clients.workitem.project.Project;
import com.microsoft.tfs.core.clients.workitem.wittype.WorkItemType;

/**
 * Dialog to create a new work that that will be automatically linked to the
 * source work item. User selects the work item link type (e.g. child) and the
 * new work item type (e.g. bug). Optionally, a title and comment can be
 * supplied.
 *
 *
 * @threadsafety unknown
 */
public class NewLinkedWorkItemDialog extends BaseDialog {
    // User settings keys for this dialog.
    private static final String DIALOG_SETTINGS_SECTION_KEY = "new-linked-workitem-dialog"; //$NON-NLS-1$
    private static final String DIALOG_SETTINGS_LINK_TYPE_KEY = "link-type"; //$NON-NLS-1$
    private static final String DIALOG_SETTINGS_WORKITEM_TYPE_KEY = "workitem-type"; //$NON-NLS-1$

    // The initial values to be selected by the respective combos.
    private String initialLinkTypeName;
    private String initialWorkItemTypeName;

    // The source work item and client.
    private final WorkItem hostWorkItem;
    private final WorkItemClient client;

    //
    private final WIFormLinksControlOptions linksControlOptions;

    // True if the connected version of WIT supports work item link
    // types as opposed to a simple related link of earlier versions of WIT.
    boolean witVersionSupportsWILinks;

    // The selected components from the dialog.
    private int selectedLinkTypeIndex;
    private int selectedWorkItemTypeIndex;
    private String selectedTitle;
    private String selectedComment;

    // Text box controls for title and comment.
    private Text textWorkItemTitle;
    private Text textWorkItemComment;

    // The sorted list of link-type and workitem-type names for the combos.
    private String[] linkTypeDisplayNames;
    private String[] workItemTypeDisplayNames;

    // Maps of combo display names to the respective objects.
    private final Map<String, WorkItemLinkTypeEnd> mapLinkDisplayNameToType =
        new HashMap<String, WorkItemLinkTypeEnd>();
    private final Map<String, WorkItemType> mapWorkItemDisplayNameToType = new HashMap<String, WorkItemType>();

    public NewLinkedWorkItemDialog(
        final Shell shell,
        final WorkItem hostWorkItem,
        final WIFormLinksControlOptions linksControlOptions) {
        super(shell);
        this.hostWorkItem = hostWorkItem;
        client = hostWorkItem.getClient();
        this.linksControlOptions = linksControlOptions;
        witVersionSupportsWILinks = client.supportsWorkItemLinkTypes();

        final IDialogSettings uiSettings = TFSCommonUIClientPlugin.getDefault().getDialogSettings();
        final IDialogSettings dialogSettings = uiSettings.getSection(DIALOG_SETTINGS_SECTION_KEY);
        if (dialogSettings != null) {
            initialLinkTypeName = dialogSettings.get(DIALOG_SETTINGS_LINK_TYPE_KEY);
            initialWorkItemTypeName = dialogSettings.get(DIALOG_SETTINGS_WORKITEM_TYPE_KEY);
        }
    }

    @Override
    protected String provideDialogTitle() {
        return Messages.getString("NewLinkedWorkItemDialog.DialogTitle"); //$NON-NLS-1$
    }

    @Override
    protected void hookAddToDialogArea(final Composite dialogArea) {
        final GridLayout layout = new GridLayout(2, false);
        layout.marginWidth = getHorizontalMargin();
        layout.marginHeight = getVerticalMargin();
        layout.horizontalSpacing = getHorizontalSpacing();
        layout.verticalSpacing = getVerticalSpacing();
        dialogArea.setLayout(layout);

        // Create the link-type label and combo.
        final Label linkTypeLabel = new Label(dialogArea, SWT.NONE);
        linkTypeLabel.setText(Messages.getString("NewLinkedWorkItemDialog.LinkTypeLabelText")); //$NON-NLS-1$
        final Combo linkTypeCombo = new Combo(dialogArea, SWT.DROP_DOWN | SWT.READ_ONLY);
        linkTypeCombo.setLayoutData(new GridData(SWT.FILL, SWT.NONE, true, false, 1, 1));
        populateLinkTypeCombo(linkTypeCombo);

        // Create the work item details group.
        final Group linkDetailsGroup = new Group(dialogArea, SWT.NONE);
        linkDetailsGroup.setText(Messages.getString("NewLinkedWorkItemDialog.DetailsGroupText")); //$NON-NLS-1$
        linkDetailsGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));

        final GridLayout linkDetailsGroupLayout = new GridLayout(2, false);
        linkDetailsGroupLayout.marginWidth = getHorizontalMargin();
        linkDetailsGroupLayout.marginHeight = getVerticalMargin();
        linkDetailsGroupLayout.horizontalSpacing = getHorizontalSpacing();
        linkDetailsGroupLayout.verticalSpacing = getVerticalSpacing();
        linkDetailsGroup.setLayout(linkDetailsGroupLayout);

        // Create the work item type label and combo.
        final Label workItemTypeLabel = new Label(linkDetailsGroup, SWT.NONE);
        workItemTypeLabel.setText(Messages.getString("NewLinkedWorkItemDialog.TypeLabelText")); //$NON-NLS-1$
        final Combo workItemTypeCombo = new Combo(linkDetailsGroup, SWT.DROP_DOWN | SWT.READ_ONLY);
        workItemTypeCombo.setLayoutData(new GridData(SWT.FILL, SWT.NONE, true, false, 1, 1));
        populateWorkItemTypeCombo(workItemTypeCombo);

        // Create the title label and text box.
        final Label workItemTitle = new Label(linkDetailsGroup, SWT.NONE);
        workItemTitle.setText(Messages.getString("NewLinkedWorkItemDialog.TitleLabelText")); //$NON-NLS-1$
        textWorkItemTitle = new Text(linkDetailsGroup, SWT.BORDER);
        textWorkItemTitle.setLayoutData(new GridData(SWT.FILL, SWT.NONE, true, false, 1, 1));
        textWorkItemTitle.setTextLimit(LinkTextMaxLengths.WORK_ITEM_TITLE_MAX_LENGTH);
        ControlSize.setCharWidthHint(textWorkItemTitle, 60);

        // Create the comment label and text box.
        final Label workItemComment = new Label(linkDetailsGroup, SWT.NONE);
        workItemComment.setText(Messages.getString("NewLinkedWorkItemDialog.CommentLabelText")); //$NON-NLS-1$
        textWorkItemComment = new Text(linkDetailsGroup, SWT.BORDER);
        textWorkItemComment.setLayoutData(new GridData(SWT.FILL, SWT.NONE, true, false, 1, 1));
        textWorkItemComment.setTextLimit(LinkTextMaxLengths.COMMENT_MAX_LENGTH);
        ControlSize.setCharWidthHint(textWorkItemTitle, 60);

        // Add listener for the link-type combo.
        linkTypeCombo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                selectedLinkTypeIndex = linkTypeCombo.getSelectionIndex();
            }
        });

        // Add listener for the link-type combo.
        workItemTypeCombo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                selectedWorkItemTypeIndex = workItemTypeCombo.getSelectionIndex();
            }
        });
    }

    @Override
    protected void okPressed() {
        selectedTitle = textWorkItemTitle.getText();
        selectedComment = textWorkItemComment.getText();

        // Save the selected link-type in user setting so the next launch of the
        // dialog will
        // initially select the last used link-type.
        final IDialogSettings uiSettings = TFSCommonUIClientPlugin.getDefault().getDialogSettings();
        IDialogSettings dialogSettings = uiSettings.getSection(DIALOG_SETTINGS_SECTION_KEY);
        if (dialogSettings == null) {
            dialogSettings = uiSettings.addNewSection(DIALOG_SETTINGS_SECTION_KEY);
        }
        dialogSettings.put(DIALOG_SETTINGS_LINK_TYPE_KEY, linkTypeDisplayNames[selectedLinkTypeIndex]);
        dialogSettings.put(DIALOG_SETTINGS_WORKITEM_TYPE_KEY, workItemTypeDisplayNames[selectedWorkItemTypeIndex]);

        super.okPressed();
    }

    public int getSelectedLinkTypeID() {
        if (witVersionSupportsWILinks) {
            final WorkItemLinkTypeEnd end = mapLinkDisplayNameToType.get(linkTypeDisplayNames[selectedLinkTypeIndex]);
            return end.getOppositeEnd().getID();
        } else {
            // Use link type 0 for "related" links in older versions of WIT
            return 0;
        }
    }

    public WorkItemType getSelectedWorkItemType() {
        return mapWorkItemDisplayNameToType.get(workItemTypeDisplayNames[selectedWorkItemTypeIndex]);
    }

    public String getTitle() {
        return selectedTitle;
    }

    public String getComment() {
        return selectedComment;
    }

    private void populateLinkTypeCombo(final Combo combo) {
        // Get the link type filter definitions, if any.
        WIFormLinksControlWILinkFilters wiFilters = null;
        if (linksControlOptions != null) {
            wiFilters = linksControlOptions.getWorkItemLinkFilters();
        }

        final List<String> listLinkTypeNames = new ArrayList<String>();

        // A link type can conflict with another link due to
        // restrictions based on the link type and the set of links
        // already in the collection. We keep track of a conflicting
        // link type only to use in an error message in the case that
        // there are no non-conflicting link types to display in the
        // dialog.
        WorkItemLinkTypeEnd conflictingWorkItemLinkType = null;

        if (witVersionSupportsWILinks) {
            final WorkItemLinkTypeCollection types = client.getLinkTypes();
            final WorkItemLinkTypeEndCollection endTypes = types.getLinkTypeEnds();

            // Iterate work item link types and filter types which should be
            // excluded.
            for (final WorkItemLinkTypeEnd linkTypeEnd : endTypes) {
                final int linkTypeId = linkTypeEnd.getID();
                final String linkReferenceName = types.getReferenceName(linkTypeId);
                final boolean isForward = types.isForwardLink(linkTypeId);
                final boolean isReverse = types.isReverseLink(linkTypeId);

                if (wiFilters == null || wiFilters.includes(linkReferenceName, isForward, isReverse)) {
                    if (!LinkDialog.hasConflictingLink(linkTypeEnd, hostWorkItem.getLinks())) {
                        final String displayName = linkTypeEnd.getName();
                        listLinkTypeNames.add(displayName);
                        mapLinkDisplayNameToType.put(displayName, linkTypeEnd);
                    } else {
                        conflictingWorkItemLinkType = linkTypeEnd;
                    }
                }
            }
        } else {
            listLinkTypeNames.add(Messages.getString("NewLinkedWorkItemDialog.RelatedLinkType")); //$NON-NLS-1$
        }

        // Bail now if there are no links that can be added.
        if (listLinkTypeNames.size() == 0) {
            if (conflictingWorkItemLinkType != null) {
                throw new WorkItemLinkValidationException(
                    MessageFormat.format(
                        Messages.getString("LinkDialog.AlreadyHaveALinkFormat"), //$NON-NLS-1$
                        conflictingWorkItemLinkType.getName()));
            } else {
                throw new WorkItemLinkValidationException(Messages.getString("LinkDialog.NoLinkTypesToAdd")); //$NON-NLS-1$
            }
        }

        // Sort the display names.
        linkTypeDisplayNames = listLinkTypeNames.toArray(new String[listLinkTypeNames.size()]);
        Arrays.sort(linkTypeDisplayNames);
        selectedLinkTypeIndex = ComboHelper.populateCombo(combo, linkTypeDisplayNames, initialLinkTypeName);
    }

    private void populateWorkItemTypeCombo(final Combo combo) {
        // Get the link type filter definitions, if any.
        WIFormLinksControlWITypeFilters wiFilters = null;
        if (linksControlOptions != null) {
            wiFilters = linksControlOptions.getWorkItemTypeFilters();
        }

        final List<String> listWorkItemTypeNames = new ArrayList<String>();
        final Project project = hostWorkItem.getType().getProject();
        final WorkItemType[] workItemTypes = project.getVisibleWorkItemTypes();

        for (final WorkItemType workItemType : workItemTypes) {
            if (wiFilters == null || wiFilters.includes(workItemType.getName())) {
                listWorkItemTypeNames.add(workItemType.getName());
                mapWorkItemDisplayNameToType.put(workItemType.getName(), workItemType);
            }
        }

        // Sort the display names.
        workItemTypeDisplayNames = listWorkItemTypeNames.toArray(new String[listWorkItemTypeNames.size()]);
        Arrays.sort(workItemTypeDisplayNames);
        selectedWorkItemTypeIndex = ComboHelper.populateCombo(combo, workItemTypeDisplayNames, initialWorkItemTypeName);
    }
}
