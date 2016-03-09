// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.wit.form.link;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.TFSCommonUIClientPlugin;
import com.microsoft.tfs.client.common.ui.framework.dialog.BaseDialog;
import com.microsoft.tfs.client.common.ui.framework.helper.MessageBoxHelpers;
import com.microsoft.tfs.client.common.ui.helpers.ComboHelper;
import com.microsoft.tfs.core.clients.workitem.WorkItem;
import com.microsoft.tfs.core.clients.workitem.WorkItemClient;
import com.microsoft.tfs.core.clients.workitem.exceptions.WorkItemLinkValidationException;
import com.microsoft.tfs.core.clients.workitem.form.WIFormLinksControlExternalLinkFilters;
import com.microsoft.tfs.core.clients.workitem.form.WIFormLinksControlOptions;
import com.microsoft.tfs.core.clients.workitem.form.WIFormLinksControlWILinkFilters;
import com.microsoft.tfs.core.clients.workitem.link.Link;
import com.microsoft.tfs.core.clients.workitem.link.LinkCollection;
import com.microsoft.tfs.core.clients.workitem.link.RegisteredLinkType;
import com.microsoft.tfs.core.clients.workitem.link.RegisteredLinkTypeCollection;
import com.microsoft.tfs.core.clients.workitem.link.RegisteredLinkTypeNames;
import com.microsoft.tfs.core.clients.workitem.link.RelatedLink;
import com.microsoft.tfs.core.clients.workitem.link.WorkItemLinkTypeCollection;
import com.microsoft.tfs.core.clients.workitem.link.WorkItemLinkTypeEnd;
import com.microsoft.tfs.core.clients.workitem.link.WorkItemLinkTypeEndCollection;

public class LinkDialog extends BaseDialog {
    private static final String DIALOG_SETTINGS_SECTION_KEY = "link-dialog"; //$NON-NLS-1$
    private static final String DIALOG_SETTINGS_LINK_TYPE_KEY = "link-type"; //$NON-NLS-1$

    private Link[] links;
    private final LinkCollection linkCollection;
    private final LinkUIRegistry linkUiRegistry;
    private final WorkItem workItem;

    private StackLayout linkDetailsGroupStack;
    private int selectedIndex = -1;
    private String initialLinkTypeName;

    /**
     * Link options from the WIT type definition which apply to this dialog
     * session.
     */
    private final WIFormLinksControlOptions linksControlOptions;

    /**
     * The display names in the order shown in the combo.
     */
    private String[] linkTypeDisplayNames;

    /**
     * Map link display names to registered link type names.
     */
    private final HashMap<String, String> mapDisplayNameToRegisteredName = new HashMap<String, String>();

    /**
     * Map registered link type name to corresponding composite.
     */
    private final HashMap<String, Composite> mapLinkDetailsComposites = new HashMap<String, Composite>();

    /**
     * Map related link display name to a related link type end.
     */
    private final HashMap<String, WorkItemLinkTypeEnd> mapRelatedNameToLinkTypeEnd =
        new HashMap<String, WorkItemLinkTypeEnd>();

    public LinkDialog(
        final Shell parentShell,
        final WorkItem workItem,
        final LinkUIRegistry linkUiRegistry,
        final WIFormLinksControlOptions linksControlOptions) {
        super(parentShell);
        this.workItem = workItem;
        this.linkCollection = workItem.getLinks();
        this.linkUiRegistry = linkUiRegistry;
        this.linksControlOptions = linksControlOptions;

        final IDialogSettings uiSettings = TFSCommonUIClientPlugin.getDefault().getDialogSettings();
        final IDialogSettings dialogSettings = uiSettings.getSection(DIALOG_SETTINGS_SECTION_KEY);
        if (dialogSettings != null) {
            initialLinkTypeName = dialogSettings.get(DIALOG_SETTINGS_LINK_TYPE_KEY);
        }
    }

    public Link[] getLinks() {
        return links;
    }

    @Override
    protected void hookAddToDialogArea(final Composite dialogArea) {
        final GridLayout layout = new GridLayout(2, false);
        layout.marginWidth = getHorizontalMargin();
        layout.marginHeight = getVerticalMargin();
        layout.horizontalSpacing = getHorizontalSpacing();
        layout.verticalSpacing = getVerticalSpacing();
        dialogArea.setLayout(layout);

        /*
         * create top label
         */
        final Label label = new Label(dialogArea, SWT.NONE);
        GridData gd = new GridData();
        gd.horizontalSpan = 2;
        label.setLayoutData(gd);
        label.setText(Messages.getString("LinkDialog.SelectLinkTypeLabelText")); //$NON-NLS-1$

        /*
         * create combo label
         */
        final Label linkTypeLabel = new Label(dialogArea, SWT.NONE);
        linkTypeLabel.setText(Messages.getString("LinkDialog.LinkTypeLabelText")); //$NON-NLS-1$

        /*
         * create link combo
         */
        final Combo linkTypeCombo = new Combo(dialogArea, SWT.DROP_DOWN | SWT.READ_ONLY);
        gd = new GridData();
        gd.horizontalAlignment = SWT.FILL;
        gd.grabExcessHorizontalSpace = true;
        linkTypeCombo.setLayoutData(gd);

        /*
         * populate the link combo
         */
        populateLinkTypeCombo(linkTypeCombo);

        /*
         * create link details group
         */
        final Group linkDetailsGroup = new Group(dialogArea, SWT.NONE);
        linkDetailsGroup.setText(Messages.getString("LinkDialog.DetailsGroupText")); //$NON-NLS-1$
        gd = new GridData();
        gd.horizontalSpan = 2;
        gd.horizontalAlignment = SWT.FILL;
        gd.verticalAlignment = SWT.FILL;
        gd.grabExcessHorizontalSpace = true;
        gd.grabExcessVerticalSpace = true;
        linkDetailsGroup.setLayoutData(gd);

        /*
         * populate the details group
         */
        populateLinkDetailsGroup(linkDetailsGroup);

        /*
         * create the selection listener for the link type combo
         */
        linkTypeCombo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                selectedIndex = linkTypeCombo.getSelectionIndex();
                processSelectedLinkType();

                linkDetailsGroupStack.topControl = getSelectedComposite();
                linkDetailsGroup.layout();
            }
        });
    }

    @Override
    protected void okPressed() {
        final LinkControlProvider linkControlProvider = getSelectedLinkControlProvider();

        if (linkControlProvider.isValid(false, workItem)) {
            // Create the link based on information collected by the link
            // control provider.
            try {
                links = linkControlProvider.getLinks();
            } catch (final Exception e) {
                MessageDialog.openError(
                    getShell(),
                    Messages.getString("LinkDialog.AddLinkErrorDialogTitle"), //$NON-NLS-1$
                    e.getLocalizedMessage());
                return;
            }

            // Don't allow a duplicate link.
            for (int i = 0; i < links.length; i++) {
                final Link link = links[i];
                if (linkCollection.contains(link)) {
                    String message;
                    if (link instanceof RelatedLink) {
                        final RelatedLink relatedLink = (RelatedLink) link;
                        final String messageFormat = Messages.getString("LinkDialog.LinkAlreadyExistsFormat"); //$NON-NLS-1$
                        message =
                            MessageFormat.format(messageFormat, Integer.toString(relatedLink.getTargetWorkItemID()));
                    } else {
                        message = Messages.getString("LinkDialog.InvalidDuplicateLink"); //$NON-NLS-1$
                    }

                    MessageBoxHelpers.errorMessageBox(
                        getShell(),
                        Messages.getString("LinkDialog.AddLinkErrorDialogTitle"), //$NON-NLS-1$
                        message);
                    return;
                }
            }

            // Save the selected link-type in user setting so the next launch of
            // the dialog will
            // initially select the last used link-type.
            final IDialogSettings uiSettings = TFSCommonUIClientPlugin.getDefault().getDialogSettings();
            IDialogSettings dialogSettings = uiSettings.getSection(DIALOG_SETTINGS_SECTION_KEY);
            if (dialogSettings == null) {
                dialogSettings = uiSettings.addNewSection(DIALOG_SETTINGS_SECTION_KEY);
            }
            dialogSettings.put(DIALOG_SETTINGS_LINK_TYPE_KEY, linkTypeDisplayNames[selectedIndex]);
            super.okPressed();
        } else {
            final String errorMessage = linkControlProvider.getErrorMessage();
            MessageBoxHelpers.errorMessageBox(
                getShell(),
                Messages.getString("LinkDialog.ErrorDialogTitle"), //$NON-NLS-1$
                errorMessage);
        }
    }

    /**
     * Create a composite for each link control provider and build a map which
     * associates the registered link name type with a composite. Only one of
     * the composites will be visible at a time, depending on which link type is
     * selected in the link-type combo.
     *
     * @param linkDetailsGroup
     *        The group box which will host each composite.
     */
    private void populateLinkDetailsGroup(final Group linkDetailsGroup) {
        linkDetailsGroupStack = new StackLayout();
        linkDetailsGroup.setLayout(linkDetailsGroupStack);

        LinkControlProvider provider;
        Composite providerComposite;

        provider = linkUiRegistry.getLinkControlProvider(RegisteredLinkTypeNames.WORKITEM);
        providerComposite = createLinkControlProviderComposite(linkDetailsGroup);
        provider.initialize(providerComposite);
        mapLinkDetailsComposites.put(RegisteredLinkTypeNames.WORKITEM, providerComposite);

        provider = linkUiRegistry.getLinkControlProvider(RegisteredLinkTypeNames.HYPERLINK);
        providerComposite = createLinkControlProviderComposite(linkDetailsGroup);
        provider.initialize(providerComposite);
        mapLinkDetailsComposites.put(RegisteredLinkTypeNames.HYPERLINK, providerComposite);

        provider = linkUiRegistry.getLinkControlProvider(RegisteredLinkTypeNames.CHANGESET);
        providerComposite = createLinkControlProviderComposite(linkDetailsGroup);
        provider.initialize(providerComposite);
        mapLinkDetailsComposites.put(RegisteredLinkTypeNames.CHANGESET, providerComposite);

        provider = linkUiRegistry.getLinkControlProvider(RegisteredLinkTypeNames.VERSIONED_ITEM);
        providerComposite = createLinkControlProviderComposite(linkDetailsGroup);
        provider.initialize(providerComposite);
        mapLinkDetailsComposites.put(RegisteredLinkTypeNames.VERSIONED_ITEM, providerComposite);

        provider = linkUiRegistry.getLinkControlProvider(RegisteredLinkTypeNames.STORYBOARD);
        providerComposite = createLinkControlProviderComposite(linkDetailsGroup);
        provider.initialize(providerComposite);
        mapLinkDetailsComposites.put(RegisteredLinkTypeNames.STORYBOARD, providerComposite);

        /*
         * All other composites that were not explicitly added (ie, those who
         * get the default composite)
         */
        for (final Iterator<String> i = mapDisplayNameToRegisteredName.values().iterator(); i.hasNext();) {
            final String registeredName = i.next();

            if (!registeredName.equals(RegisteredLinkTypeNames.WORKITEM)
                && !registeredName.equals(RegisteredLinkTypeNames.HYPERLINK)
                && !registeredName.equals(RegisteredLinkTypeNames.CHANGESET)
                && !registeredName.equals(RegisteredLinkTypeNames.VERSIONED_ITEM)) {
                provider = linkUiRegistry.getLinkControlProvider(registeredName);
                providerComposite = createLinkControlProviderComposite(linkDetailsGroup);
                provider.initialize(providerComposite);
                mapLinkDetailsComposites.put(registeredName, providerComposite);
            }
        }

        linkDetailsGroupStack.topControl = getSelectedComposite();
    }

    /**
     * Create a Composite to host a LinkControlProviders UI. This Composite is
     * visible when the link type associated with this composite is selected in
     * the link-type combo.
     *
     * @param linkDetailsGroup
     *        The group box which hosts the Composite.
     *
     * @return The newly created Composite.
     */
    private Composite createLinkControlProviderComposite(final Group linkDetailsGroup) {
        final Composite composite = new Composite(linkDetailsGroup, SWT.NONE);

        final GridLayout layout = new GridLayout(2, false);
        layout.marginWidth = getHorizontalMargin();
        layout.marginHeight = getVerticalMargin();
        composite.setLayout(layout);

        return composite;
    }

    /**
     * Returns the Composite associated with the selected link type.
     */
    private Composite getSelectedComposite() {
        return mapLinkDetailsComposites.get(mapDisplayNameToRegisteredName.get(linkTypeDisplayNames[selectedIndex]));
    }

    /**
     * Returns the LinkControlProvider associated with the selected link type.
     */
    private LinkControlProvider getSelectedLinkControlProvider() {
        return linkUiRegistry.getLinkControlProvider(
            mapDisplayNameToRegisteredName.get(linkTypeDisplayNames[selectedIndex]));
    }

    /**
     * Returns the registered related name for the selected link type.
     */
    private WorkItemLinkTypeEnd getSelectedWorkItemLinkTypeEnd() {
        return mapRelatedNameToLinkTypeEnd.get(linkTypeDisplayNames[selectedIndex]);
    }

    /**
     * Populate the specified combo with display names for link types which can
     * be created in the current context. The list is populated with both
     * external link types (hyperlink, changeset, etc) and releated work item
     * link types (child, parent, etc). Link filter options are used to
     * determine if a particular link type should not be included in the drop
     * down. A link type will also be excluded from the drop down if creating a
     * new link of that type would cause a conflict with an existing link in
     * this collection.
     *
     *
     * @param combo
     *        The combo to populate.
     */
    private void populateLinkTypeCombo(final Combo combo) {
        // Get the link type filter definitions, if any.
        WIFormLinksControlWILinkFilters wiFilters = null;
        WIFormLinksControlExternalLinkFilters externalFilters = null;

        if (linksControlOptions != null) {
            wiFilters = linksControlOptions.getWorkItemLinkFilters();
            externalFilters = linksControlOptions.getExternalLinkFilters();
        }

        // Get the related work item types.
        final WorkItemClient client = workItem.getClient();
        final RegisteredLinkTypeCollection externalTypes = client.getRegisteredLinkTypes();

        final ArrayList<String> listLinkTypeNames = new ArrayList<String>();
        final boolean hasWorkItemLinks = client.supportsWorkItemLinkTypes();

        // A link type can conflict with another link due to
        // restrictions based on the link type and the set of links
        // already in the collection. We keep track of a conflicting
        // link type only to use in an error message in the case that
        // there are no non-conflicting link types to display in the
        // dialog.
        WorkItemLinkTypeEnd conflictingWorkItemLinkType = null;

        if (hasWorkItemLinks) {
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
                    if (!hasConflictingLink(linkTypeEnd, linkCollection)) {
                        final String displayName = linkTypeEnd.getName();
                        listLinkTypeNames.add(displayName);
                        mapDisplayNameToRegisteredName.put(displayName, RegisteredLinkTypeNames.WORKITEM);
                        mapRelatedNameToLinkTypeEnd.put(displayName, linkTypeEnd);
                    } else {
                        conflictingWorkItemLinkType = linkTypeEnd;
                    }
                }
            }
        }

        // Iterate external link types and filter types which should be
        // excluded.
        for (final RegisteredLinkType externalType : externalTypes) {
            if (!hasWorkItemLinks || !externalType.getName().equalsIgnoreCase(RegisteredLinkTypeNames.WORKITEM)) {
                if (externalFilters == null || externalFilters.includes(externalType.getName())) {
                    final String registeredTypeName = externalType.getName();
                    final String displayName =
                        linkUiRegistry.getLinkControlProvider(registeredTypeName).getDisplayName(externalType);
                    listLinkTypeNames.add(displayName);
                    mapDisplayNameToRegisteredName.put(displayName, registeredTypeName);
                }
            }
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
        selectedIndex = ComboHelper.populateCombo(combo, linkTypeDisplayNames, initialLinkTypeName);

        if (selectedIndex != -1) {
            processSelectedLinkType();
        }
    }

    @Override
    protected String provideDialogTitle() {
        return Messages.getString("LinkDialog.AddLinkDialogTitle"); //$NON-NLS-1$
    }

    /**
     * Process a selection change for link type.
     */
    private void processSelectedLinkType() {
        final LinkControlProvider linkControlProvider = getSelectedLinkControlProvider();
        if (linkControlProvider instanceof RelatedWorkitemControlProvider) {
            // Note this can be null when connected to a pre-version3 server.
            final WorkItemLinkTypeEnd relatedLinkTypeEnd = getSelectedWorkItemLinkTypeEnd();
            if (relatedLinkTypeEnd != null) {
                ((RelatedWorkitemControlProvider) linkControlProvider).setLinkType(relatedLinkTypeEnd);
            }
        }
    }

    /**
     * Return true adding a new link of the specified end type would cause a
     * conflict with the existing set of links on this work item.
     *
     *
     * @param linkTypeEnd
     *        The link type to test.
     *
     * @return True if an existing link would conflict any any new link of the
     *         specified type.
     */
    public static boolean hasConflictingLink(
        final WorkItemLinkTypeEnd linkTypeEnd,
        final LinkCollection linkCollection) {
        if (linkTypeEnd.getLinkType().isOneToMany() && !linkTypeEnd.isForwardLink()) {
            for (final Link existingLink : linkCollection) {
                if (existingLink instanceof RelatedLink) {
                    final RelatedLink existingRelatedLink = (RelatedLink) existingLink;
                    if (existingRelatedLink.getWorkItemLinkTypeID() == linkTypeEnd.getID()) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
