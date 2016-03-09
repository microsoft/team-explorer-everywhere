// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.wit.form;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;

import com.microsoft.tfs.client.common.server.TFSServer;
import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.TFSCommonUIClientPlugin;
import com.microsoft.tfs.client.common.ui.framework.helper.MessageBoxHelpers;
import com.microsoft.tfs.client.common.ui.framework.helper.TableViewerSorter;
import com.microsoft.tfs.client.common.ui.framework.helper.UIHelpers;
import com.microsoft.tfs.client.common.ui.framework.image.ImageHelper;
import com.microsoft.tfs.client.common.ui.framework.tooltip.IToolTipProvider;
import com.microsoft.tfs.client.common.ui.helpers.WorkItemEditorHelper;
import com.microsoft.tfs.client.common.ui.teamexplorer.helpers.ArtifactLinkHelpers;
import com.microsoft.tfs.client.common.ui.wit.OpenWorkItemWithAction;
import com.microsoft.tfs.client.common.ui.wit.form.link.LinkDialog;
import com.microsoft.tfs.client.common.ui.wit.form.link.LinkUIRegistry;
import com.microsoft.tfs.client.common.ui.wit.form.link.NewLinkedWorkItemAction;
import com.microsoft.tfs.core.artifact.ArtifactID;
import com.microsoft.tfs.core.clients.workitem.CoreFieldReferenceNames;
import com.microsoft.tfs.core.clients.workitem.WorkItem;
import com.microsoft.tfs.core.clients.workitem.WorkItemClient;
import com.microsoft.tfs.core.clients.workitem.WorkItemStateAdapter;
import com.microsoft.tfs.core.clients.workitem.WorkItemStateListener;
import com.microsoft.tfs.core.clients.workitem.exceptions.WorkItemLinkValidationException;
import com.microsoft.tfs.core.clients.workitem.fields.Field;
import com.microsoft.tfs.core.clients.workitem.fields.FieldDefinition;
import com.microsoft.tfs.core.clients.workitem.fields.FieldDefinitionCollection;
import com.microsoft.tfs.core.clients.workitem.fields.FieldType;
import com.microsoft.tfs.core.clients.workitem.fields.FieldUsages;
import com.microsoft.tfs.core.clients.workitem.form.WIFormLinkColumn;
import com.microsoft.tfs.core.clients.workitem.form.WIFormLinksControlExternalLinkFilters;
import com.microsoft.tfs.core.clients.workitem.form.WIFormLinksControlOptions;
import com.microsoft.tfs.core.clients.workitem.form.WIFormLinksControlWILinkFilters;
import com.microsoft.tfs.core.clients.workitem.form.WIFormLinksControlWITypeFilterEnum;
import com.microsoft.tfs.core.clients.workitem.form.WIFormLinksControlWITypeFilters;
import com.microsoft.tfs.core.clients.workitem.internal.link.LinkCollectionChangedListener;
import com.microsoft.tfs.core.clients.workitem.internal.link.LinkCollectionImpl;
import com.microsoft.tfs.core.clients.workitem.link.ExternalLink;
import com.microsoft.tfs.core.clients.workitem.link.Hyperlink;
import com.microsoft.tfs.core.clients.workitem.link.Link;
import com.microsoft.tfs.core.clients.workitem.link.LinkCollection;
import com.microsoft.tfs.core.clients.workitem.link.RegisteredLinkTypeNames;
import com.microsoft.tfs.core.clients.workitem.link.RelatedLink;
import com.microsoft.tfs.core.clients.workitem.link.WorkItemLinkTypeCollection;
import com.microsoft.tfs.core.clients.workitem.query.WorkItemCollection;
import com.microsoft.tfs.util.Check;

public class WorkItemLinksControl extends BaseWITComponentControl {
    private static final Log log = LogFactory.getLog(WorkItemLinksControl.class);

    private LinkUIRegistry linkUiRegistry;

    private IAction openAction;
    private IAction[] openWithActions;
    private IAction addAction;
    private IAction deleteAction;
    private IAction copyDescriptionToClipboardAction;
    private IAction copyURIToClipboardAction;

    private ColumnMetadata[] columnMetadatas;
    private final WIFormLinksControlOptions linksControlOptions;
    private LinkCollectionChangedListener linkCollectionChangedListener;
    private WorkItemStateListener workItemStateListener;

    private Button openButton;
    private Button newButton;
    private Button addButton;
    private Button deleteButton;

    private static String LINKATTRIBUTE_REFNAME_LINKTYPE = "System.Links.LinkType"; //$NON-NLS-1$
    private static String LINKATTRIBUTE_REFNAME_DESCRIPTION = "System.Links.Description"; //$NON-NLS-1$
    private static String LINKATTRIBUTE_REFNAME_COMMENT = "System.Links.Comment"; //$NON-NLS-1$

    private static String LINKATTRIBUTE_DISPNAME_LINKTYPE =
        Messages.getString("WorkItemLinksControl.ColumnNameLinkType"); //$NON-NLS-1$
    private static String LINKATTRIBUTE_DISPNAME_DESCRIPTION =
        Messages.getString("WorkItemLinksControl.ColumnNameDescription"); //$NON-NLS-1$
    private static String LINKATTRIBUTE_DISPNAME_COMMENT = Messages.getString("WorkItemLinksControl.ColumnNameComment"); //$NON-NLS-1$

    private final ImageHelper imageHelper = new ImageHelper(TFSCommonUIClientPlugin.PLUGIN_ID);

    public WorkItemLinksControl(
        final Composite parent,
        final int style,
        final TFSServer server,
        final WorkItem workItemInput,
        final WIFormLinksControlOptions options) {
        super(parent, style | SWT.MULTI, server, workItemInput);
        linksControlOptions = options;

        addDisposeListener(new DisposeListener() {
            @Override
            public void widgetDisposed(final DisposeEvent e) {
                imageHelper.dispose();
            }
        });
    }

    @Override
    protected void hookInit() {
        // Create and wire the actions for this control.
        setupActions();

        linkUiRegistry = new LinkUIRegistry(
            getServer(),
            getWorkItem(),
            (linksControlOptions == null) ? null : linksControlOptions.getWorkItemTypeFilters());

        // handle initial validation and populate table with the work item.
        bindWorkItemToTable();

        final WorkItem workItem = getWorkItem();

        // Create a listener for work item save events.
        workItemStateListener = new WorkItemStateAdapter() {
            @Override
            public void saved(final WorkItem workItem) {
                UIHelpers.runOnUIThread(getDisplay(), true, new Runnable() {
                    @Override
                    public void run() {
                        if (isDisposed()) {
                            return;
                        }

                        newButton.setEnabled(true);
                    }
                });
            }

            @Override
            public void synchedToLatest(final WorkItem workItem) {
                UIHelpers.runOnUIThread(getDisplay(), true, new Runnable() {
                    @Override
                    public void run() {
                        refresh();
                    }
                });
            }
        };

        // Add a listener for work item save events
        workItem.addWorkItemStateListener(workItemStateListener);

        // Create a listener for changes to the work items link collection.
        linkCollectionChangedListener = new LinkCollectionChangedListener() {
            @Override
            public void linkAdded(final Link link, final LinkCollection collection) {
                UIHelpers.runOnUIThread(getDisplay(), true, new Runnable() {
                    @Override
                    public void run() {
                        refresh();
                    }
                });
            }

            @Override
            public void linkRemoved(final Link link, final LinkCollection collection) {
                UIHelpers.runOnUIThread(getDisplay(), true, new Runnable() {
                    @Override
                    public void run() {
                        refresh();
                    }
                });
            }

            @Override
            public void linkTargetsUpdated(final LinkCollection collection) {
                UIHelpers.runOnUIThread(getDisplay(), true, new Runnable() {
                    @Override
                    public void run() {
                        refresh();
                    }
                });
            }
        };

        // Get access to the internal methods of the LinkCollection
        final LinkCollectionImpl linkCollectionImpl = (LinkCollectionImpl) workItem.getLinks();

        // Add a listener for changes to the link collection.
        linkCollectionImpl.addLinkCollectionChangedListener(linkCollectionChangedListener);

        // Remove the link collection and work item state listeners when this
        // link control is disposed.
        addDisposeListener(new DisposeListener() {
            @Override
            public void widgetDisposed(final DisposeEvent e) {
                linkCollectionImpl.removeLinkCollectionChangedListener(linkCollectionChangedListener);
                workItem.removeWorkItemStateListener(workItemStateListener);
            }
        });
    }

    private void performOpen() {
        final Link link = (Link) getSelectedItem();

        if (link instanceof Hyperlink) {
            ArtifactLinkHelpers.openHyperlinkLink(getShell(), ((Hyperlink) link).getLocation());
        } else if (link instanceof RelatedLink) {
            final RelatedLink relatedLink = (RelatedLink) link;
            WorkItem workItem;
            try {
                workItem =
                    getServer().getConnection().getWorkItemClient().getWorkItemByID(relatedLink.getTargetWorkItemID());
            } catch (final Exception e) {
                throw new RuntimeException(e);
            }
            WorkItemEditorHelper.openEditor(getServer(), workItem);
        } else if (link instanceof ExternalLink) {
            final ArtifactID artifactID = ((ExternalLink) link).getArtifactID();
            ArtifactLinkHelpers.openArtifact(getShell(), artifactID);
        }
    }

    /**
     * Performs the action to open a related work item in a specific editor.
     *
     *
     * @param editorID
     *        The workbench ID of the editor.
     */
    private void performOpenRelatedWorkItemWith(final String editorID) {
        final Object selectedItem = getSelectedItem();
        Check.isTrue(selectedItem instanceof RelatedLink, "selectedItem instanceof RelatedLink"); //$NON-NLS-1$

        final RelatedLink relatedLink = (RelatedLink) selectedItem;
        WorkItem workItem;

        try {
            final int targetWorkItemID = relatedLink.getTargetWorkItemID();
            workItem = getServer().getConnection().getWorkItemClient().getWorkItemByID(targetWorkItemID);
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
        WorkItemEditorHelper.openEditor(getServer(), workItem, editorID);
    }

    private void performNew() {
        new NewLinkedWorkItemAction(getShell(), getServer(), getWorkItem(), linksControlOptions).run();
    }

    private void performAdd() {
        try {
            final LinkDialog dialog = new LinkDialog(getShell(), getWorkItem(), linkUiRegistry, linksControlOptions);
            if (dialog.open() == IDialogConstants.OK_ID) {
                final Link[] links = dialog.getLinks();
                for (int i = 0; i < links.length; i++) {
                    getWorkItem().getLinks().add(links[i]);
                }
                getWorkItemForm().updateWorkItemLinkTargetsColumns();
            }
        } catch (final WorkItemLinkValidationException e) {
            MessageDialog.openError(
                getShell(),
                Messages.getString("WorkItemLinksControl.ErrorDialogTitle"), //$NON-NLS-1$
                e.getLocalizedMessage());
        }
    }

    private void performDelete() {
        if (!MessageBoxHelpers.dialogConfirmPrompt(
            getShell(),
            Messages.getString("WorkItemLinksControl.ConfirmDialogTitle"), //$NON-NLS-1$
            Messages.getString("WorkItemLinksControl.ConfirmDialogText"))) //$NON-NLS-1$
        {
            return;
        }

        final Object[] selectedLinks = getSelectedItems();
        for (int i = 0; i < selectedLinks.length; i++) {
            getWorkItem().getLinks().remove((Link) selectedLinks[i]);
        }
    }

    private void performCopyDescriptionToClipboard() {
        UIHelpers.copyToClipboard(((Link) getSelectedItem()).getDescription());
    }

    private void copyURIToClipboard() {
        UIHelpers.copyToClipboard(((ExternalLink) getSelectedItem()).getURI());
    }

    /**
     * Returns the containing WorkItemForm for this control. Follows the parent
     * chain until the WorkItemForm is found. This method will never return null
     * and throws if a WorkItemForm is not found in the parent chain.
     *
     *
     * @return The WorkItemForm from the parent chain of this control.
     */
    private WorkItemForm getWorkItemForm() {
        Composite parent = getParent();
        while (parent != null && !(parent instanceof WorkItemForm)) {
            parent = parent.getParent();
        }

        if (parent != null) {
            return (WorkItemForm) parent;
        }

        throw new IllegalStateException(Messages.getString("WorkItemLinksControl.CantFindParent")); //$NON-NLS-1$
    }

    @Override
    protected void handleSelectionChanged(final Object[] selectedItems) {
        final boolean oneLinkSelected = (selectedItems.length == 1);
        final boolean openable = oneLinkSelected && isSelectedLinkOpenable();
        final boolean externalLink = oneLinkSelected && (getSelectedItem() instanceof ExternalLink);

        boolean deleteAllowed = (selectedItems.length > 0);
        for (int i = 0; i < selectedItems.length; i++) {
            final Link link = (Link) selectedItems[i];
            if (link.isReadOnly()) {
                deleteAllowed = false;
                break;
            }
        }

        openAction.setEnabled(openable);
        addAction.setEnabled(true);
        deleteAction.setEnabled(deleteAllowed);

        copyDescriptionToClipboardAction.setEnabled(oneLinkSelected);
        copyURIToClipboardAction.setEnabled(externalLink);

        openButton.setEnabled(openable);
        deleteButton.setEnabled(deleteAllowed);
    }

    private void setupActions() {
        openAction = new Action() {
            @Override
            public void run() {
                performOpen();
            }
        };
        openAction.setText(Messages.getString("WorkItemLinksControl.OpenActionText")); //$NON-NLS-1$

        final List<WorkItemEditorInfo> editors = WorkItemEditorHelper.getWorkItemEditors();
        if (editors != null && editors.size() > 0) {
            int count = 0;
            openWithActions = new OpenWorkItemWithAction[editors.size()];

            for (final WorkItemEditorInfo editor : editors) {
                openWithActions[count++] = new OpenWorkItemWithAction(editor.getDisplayName(), editor.getEditorID()) {
                    @Override
                    public void run() {
                        performOpenRelatedWorkItemWith(this.getEditorID());
                    }
                };
            }
        }

        addAction = new Action() {
            @Override
            public void run() {
                performAdd();
            }
        };
        addAction.setText(Messages.getString("WorkItemLinksControl.AddActionText")); //$NON-NLS-1$

        deleteAction = new Action() {
            @Override
            public void run() {
                performDelete();
            }
        };
        deleteAction.setText(Messages.getString("WorkItemLinksControl.DeleteActionText")); //$NON-NLS-1$

        copyDescriptionToClipboardAction = new Action() {
            @Override
            public void run() {
                performCopyDescriptionToClipboard();
            }
        };
        copyDescriptionToClipboardAction.setText(Messages.getString("WorkItemLinksControl.CopyDescriptionActionText")); //$NON-NLS-1$

        copyURIToClipboardAction = new Action() {
            @Override
            public void run() {
                copyURIToClipboard();
            }
        };
        copyURIToClipboardAction.setText(Messages.getString("WorkItemLinksControl.CopyUriActionText")); //$NON-NLS-1$
    }

    @Override
    protected IToolTipProvider getToolTipProvider() {
        return new IToolTipProvider() {
            @Override
            public String getToolTipText(final Object element) {
                final Link link = (Link) element;

                if (link.isReadOnly()) {
                    return Messages.getString("WorkItemLinksControl.LockedLinkTooltipText"); //$NON-NLS-1$
                }

                final StringBuffer buffer = new StringBuffer();

                String messageFormat;
                String message;

                messageFormat = Messages.getString("WorkItemLinksControl.LinkTypeTooltipFormat"); //$NON-NLS-1$
                message = MessageFormat.format(messageFormat, link.getLinkType().getName());
                buffer.append(message);
                buffer.append(NEWLINE);

                messageFormat = Messages.getString("WorkItemLinksControl.CommentTooltipFormat"); //$NON-NLS-1$
                message = MessageFormat.format(messageFormat, (link.getComment() != null ? link.getComment() : "")); //$NON-NLS-1$
                buffer.append(message);
                buffer.append(NEWLINE);

                if (link instanceof RelatedLink) {
                    final int workItemId = ((RelatedLink) link).getTargetWorkItemID();
                    messageFormat = Messages.getString("WorkItemLinksControl.RelatedItemTooltipFormat"); //$NON-NLS-1$
                    message = MessageFormat.format(messageFormat, Integer.toString(workItemId));
                    buffer.append(message);
                } else if (link instanceof Hyperlink) {
                    final String location = ((Hyperlink) link).getLocation();
                    messageFormat = Messages.getString("WorkItemLinksControl.LocationTooltipFormat"); //$NON-NLS-1$
                    message = MessageFormat.format(messageFormat, location);
                    buffer.append(message);
                } else {
                    final String uri = ((ExternalLink) link).getURI();
                    messageFormat = Messages.getString("WorkItemLinksControl.UriTooltipFormat"); //$NON-NLS-1$
                    message = MessageFormat.format(messageFormat, uri);
                    buffer.append(message);
                }

                return buffer.toString();
            }
        };
    }

    @Override
    protected void fillMenuBeforeShow(final IMenuManager manager) {
        manager.add(openAction);

        if (openWithActions != null && selectionIsSingleRelatedLink()) {
            final IMenuManager subMenu = new MenuManager(Messages.getString("QueryResultsControl.OpenWithCommandText")); //$NON-NLS-1$
            manager.add(subMenu);
            manager.add(new Separator());

            for (final IAction action : openWithActions) {
                subMenu.add(action);
            }
        }

        manager.add(addAction);
        manager.add(deleteAction);

        manager.add(new Separator());

        manager.add(copyDescriptionToClipboardAction);
        manager.add(copyURIToClipboardAction);
    }

    @Override
    protected Object[] getItemsFromWorkItem(final WorkItem workItem) {
        // If no options were supplied there is no filtering to perform.
        if (linksControlOptions == null) {
            final ArrayList<Link> links = new ArrayList<Link>();
            for (final Link link : workItem.getLinks()) {
                if (!link.isPendingDelete()) {
                    links.add(link);
                }
            }
            return links.toArray();
        }

        // Filter the links based on the specified options.
        final WorkItemLinkTypeCollection linkTypes = workItem.getClient().getLinkTypes();
        final WIFormLinksControlWILinkFilters wiLinkFilters = linksControlOptions.getWorkItemLinkFilters();
        final WIFormLinksControlWITypeFilters wiTypeFilters = linksControlOptions.getWorkItemTypeFilters();
        final WIFormLinksControlExternalLinkFilters externalLinkFilters = linksControlOptions.getExternalLinkFilters();

        // Filter the full list by link type. Track the IDs of work item links.
        final ArrayList<Link> filteredByLinkType = new ArrayList<Link>();
        final ArrayList<Link> workItemLinks = new ArrayList<Link>();

        for (final Link link : workItem.getLinks()) {
            if (link.isPendingDelete()) {
                continue;
            }

            if (link instanceof ExternalLink || link instanceof Hyperlink) {
                if (externalLinkFilters == null || externalLinkFilters.includes(link.getLinkType().getName())) {
                    filteredByLinkType.add(link);
                }
            } else if (link instanceof RelatedLink) {
                final int linkTypeId = ((RelatedLink) link).getWorkItemLinkTypeID();
                final String linkReferenceName = linkTypes.getReferenceName(linkTypeId);
                final boolean isForward = linkTypes.isForwardLink(linkTypeId);
                final boolean isReverse = linkTypes.isReverseLink(linkTypeId);

                if (wiLinkFilters == null || wiLinkFilters.includes(linkReferenceName, isForward, isReverse)) {
                    filteredByLinkType.add(link);
                    workItemLinks.add(link);
                }
            }
        }

        // If there are no work-item type filters, we are done.
        if (wiTypeFilters == null || workItemLinks.size() == 0) {
            return filteredByLinkType.toArray();
        }

        // If the work-item type filter says INCLUDEALL we are done.
        final WIFormLinksControlWITypeFilterEnum filterType = wiTypeFilters.getFilter();
        if (filterType == WIFormLinksControlWITypeFilterEnum.INCLUDEALL) {
            return filteredByLinkType.toArray();
        }

        // Create a SQL query to further filter the list by work item type.
        final int[] candidateWorkItemIds = new int[workItemLinks.size()];
        for (int i = 0; i < workItemLinks.size(); i++) {
            candidateWorkItemIds[i] = ((RelatedLink) workItemLinks.get(i)).getTargetWorkItemID();
        }

        final String projectName = workItem.getType().getProject().getName();
        final String wiql = wiTypeFilters.createFilterWIQLQuery(candidateWorkItemIds, projectName);

        // TODO: try/catch here?
        final WorkItemCollection workItems = workItem.getClient().query(wiql);

        // The workItem IDs returned from the query are the work item links to
        // keep.
        final HashSet<Integer> mapWorkItemIds = new HashSet<Integer>();
        final int[] workItemIds = workItems.getIDs();
        for (int i = 0; i < workItemIds.length; i++) {
            mapWorkItemIds.add(new Integer(workItemIds[i]));
        }

        final ArrayList<Link> filteredByWorkItemType = new ArrayList<Link>();
        for (int i = 0; i < filteredByLinkType.size(); i++) {
            final Link link = filteredByLinkType.get(i);

            if (link instanceof RelatedLink) {
                final Integer boxedId = new Integer(((RelatedLink) link).getTargetWorkItemID());
                if (mapWorkItemIds.contains(boxedId)) {
                    filteredByWorkItemType.add(link);
                }
            } else {
                filteredByWorkItemType.add(link);
            }
        }

        return filteredByWorkItemType.toArray();
    }

    /**
     * Returns the display names for each column in the result grid. For columns
     * which reference a work item field, the display name comes from the set of
     * work item field definitions maintained by the work item client. For
     * column which reference a link attribute, the display name is a hard coded
     * value.
     *
     * In addition to building the list of display names, this method
     * initializes the columnMetadatas member of this link control. The column
     * metadata is used to help identify the correct property or field being
     * represented by this column when text is requested for the cell in the
     * grid.
     */
    @Override
    protected String[] getTableColumnNames() {
        final ArrayList<ColumnMetadata> listColumnMetadata = new ArrayList<ColumnMetadata>();
        final ArrayList<String> listColumnWorkItemReferences = new ArrayList<String>();

        // Default to link properties if no columns specified, or created
        // columns defined in options.
        if (linksControlOptions == null || linksControlOptions.getLinkColumns() == null) {
            // No columns specified. Default to the link properties.
            listColumnMetadata.add(
                new ColumnMetadata(LINKATTRIBUTE_REFNAME_LINKTYPE, LINKATTRIBUTE_DISPNAME_LINKTYPE, true));

            listColumnMetadata.add(
                new ColumnMetadata(LINKATTRIBUTE_REFNAME_DESCRIPTION, LINKATTRIBUTE_DISPNAME_DESCRIPTION, true));

            listColumnMetadata.add(
                new ColumnMetadata(LINKATTRIBUTE_REFNAME_COMMENT, LINKATTRIBUTE_DISPNAME_COMMENT, true));
        } else {
            // Column definitions appear in the link control options. Define the
            // column metadatas.
            final WIFormLinkColumn[] columns = linksControlOptions.getLinkColumns().getLinkColumns();
            final FieldDefinitionCollection fieldDefinitions = getWorkItem().getClient().getFieldDefinitions();

            // Always place the link type column first since we don't do
            // grouping.
            listColumnMetadata.add(
                0,
                new ColumnMetadata(LINKATTRIBUTE_REFNAME_LINKTYPE, LINKATTRIBUTE_DISPNAME_LINKTYPE, true));

            for (int i = 0; i < columns.length; i++) {
                final WIFormLinkColumn column = columns[i];
                if (column.getLinkAttribute() != null) {
                    // Column definition is for a link property.
                    final String linkAttribute = column.getLinkAttribute();

                    // Skip the link type column if encountered since we always
                    // add it as the first column.
                    if (!linkAttribute.equalsIgnoreCase(LINKATTRIBUTE_REFNAME_LINKTYPE)) {
                        final String displayName = linkAttributeToDisplayName(linkAttribute);
                        if (displayName != null) {
                            listColumnMetadata.add(new ColumnMetadata(linkAttribute, displayName, true));
                        }
                    }
                } else if (column.getRefName() != null) {
                    // Column definition is for a work item field.
                    final String wiRefName = column.getRefName();
                    final FieldDefinition fieldDef = fieldDefinitions.get(wiRefName);

                    // Exclude this column if the reference name is not intended
                    // for use with work items. Any column that we accept here
                    // ultimately appears as a column name in a SELECT statement
                    // for the WORK ITEMS table which retrieve the data to
                    // display
                    // in this column. The EMC SCRUM template includes a column
                    // with a reference name for LinkType which is not a WIT
                    // field and should be excluded. The LinkType column is
                    // always
                    // included with TEE, so the EMC template still works as
                    // intended.
                    if (fieldDef != null && fieldDef.getUsage() == FieldUsages.WORK_ITEM) {
                        Comparator<Link> comparator = null;
                        final FieldType type = fieldDef.getFieldType();

                        // Instantiate a sorting comparator for this column if
                        // it is a WIT field with type INTEGER, DOUBLE, or
                        // DATETIME. All other field types are compared as
                        // strings.
                        if (type == FieldType.INTEGER || type == FieldType.DOUBLE || type == FieldType.DATETIME) {
                            comparator = new RelatedLinkFieldComparator(wiRefName);
                        }

                        listColumnMetadata.add(new ColumnMetadata(wiRefName, fieldDef.getName(), false, comparator));
                        listColumnWorkItemReferences.add(wiRefName);
                    }
                }
            }
        }

        // Initialize the column metadata and work item field reference members
        columnMetadatas = listColumnMetadata.toArray(new ColumnMetadata[listColumnMetadata.size()]);

        // Merge the field reference names defined for columns on this control
        // into the set
        // of all WIT columns maintained by the LinksCollection across all
        // LinksControls on
        // the WIT form.
        if (listColumnWorkItemReferences.size() > 0) {
            final String[] fieldReferences =
                listColumnWorkItemReferences.toArray(new String[listColumnWorkItemReferences.size()]);

            ((LinkCollectionImpl) getWorkItem().getLinks()).mergeColumnFieldReferences(fieldReferences);
        }

        // Get the display names.
        final String[] columnNames = new String[columnMetadatas.length];
        for (int i = 0; i < columnNames.length; i++) {
            columnNames[i] = columnMetadatas[i].getDisplayName();
        }

        // Return the display names for each column.
        return columnNames;
    }

    /**
     * Override addSorting to supply sort comparators for columns that require
     * non-string comparisons.
     */
    @Override
    protected void addSorting(final TableViewer tableViewer) {
        final TableViewerSorter sorter = new TableViewerSorter(tableViewer);

        for (int i = 0; i < columnMetadatas.length; i++) {
            final ColumnMetadata columnMetadata = columnMetadatas[i];
            final Comparator<Link> comparator = columnMetadata.getComparator();

            if (comparator != null) {
                sorter.setComparator(i, comparator);
            }
        }

        tableViewer.setSorter(sorter);
    }

    /**
     * Returns the image to be displayed in the specified column of the grid for
     * the specified row.
     *
     * @param element
     *        The object associated with the row in the grid (a link).
     *
     * @param columnIndex
     *        The index of the column we want to retrieve an image for.
     *
     * @returns Returns the image for the cell in teh grid for the given
     *          row/column. Returns null if there is no image for the cell.
     */
    @Override
    protected Image getImageForColumn(final Object element, final int columnIndex) {
        if (columnIndex == 0) {
            final Link link = (Link) element;

            if (link.isReadOnly()) {
                return imageHelper.getImage("/images/wit/LockedLink.gif"); //$NON-NLS-1$
            }
        }

        return null;
    }

    /**
     * Returns the text to be displayed in the specified column of the grid for
     * the specified row. The returned text comes from either a field on the
     * linked work item (when the link is a work item link) or it comes from a
     * property on the link itself. It is valid for there to be no data
     * available for the specified row/column. For example, the column which
     * displays a field from a work item link will always be empty when the link
     * is not a releatd link.
     *
     * @param element
     *        The object associated with a row in the grid (a link).
     *
     * @param columnIndex
     *        The index of the column we want to retrieve text for.
     *
     * @returns Returns the display text for the cell in the grid for the given
     *          row/column. Returns null if there is no data available for the
     *          cell.
     */
    @Override
    protected String getTextForColumn(final Object element, final int columnIndex) {
        // Get the objects represented by the specified row/column arguments.
        final Link link = (Link) element;
        final ColumnMetadata columnData = columnMetadatas[columnIndex];

        if (columnData.isWorkItemField()) {
            // Work item columns only show data for work item links.
            if (link instanceof RelatedLink) {
                // Get the target work item and referenced field name.
                final RelatedLink relatedLink = (RelatedLink) link;
                final WorkItem linkedWorkItem = relatedLink.getTargetWorkItem();
                final String wiFieldRefName = columnData.getReferenceName();

                // Nothing is returned we the linked work item has not been set.
                if (linkedWorkItem != null) {
                    // Retrieve the field referenced by this column. It's valid
                    // for
                    // the field not to exist for a particular work item type.
                    if (linkedWorkItem.getFields().contains(wiFieldRefName)) {
                        // Return the value of the field from the work item.
                        final Object fieldValue = linkedWorkItem.getFields().getField(wiFieldRefName).getValue();
                        if (fieldValue != null) {
                            return fieldValue.toString();
                        }
                    }
                }
            } else {
                // Show the description in the Title field for external links.
                if (columnData.getReferenceName().equalsIgnoreCase(CoreFieldReferenceNames.TITLE)) {
                    return link.getDescription();
                }
            }
        } else {
            // The column represents a property from the link itself.
            final String linkRefName = columnData.getReferenceName();
            if (linkRefName.equalsIgnoreCase(LINKATTRIBUTE_REFNAME_LINKTYPE)) {
                if (link instanceof RelatedLink) {
                    final RelatedLink relatedLink = (RelatedLink) link;
                    final WorkItemClient client = getWorkItem().getClient();

                    if (client.supportsWorkItemLinkTypes()) {
                        return client.getLinkTypes().getDisplayName(relatedLink.getWorkItemLinkTypeID());
                    } else {
                        return LinkUIRegistry.getDisplayName(link.getLinkType().getName());
                    }
                } else {
                    return LinkUIRegistry.getDisplayName(link.getLinkType().getName());
                }
            } else if (linkRefName.equalsIgnoreCase(LINKATTRIBUTE_REFNAME_DESCRIPTION)) {
                return link.getDescription();
            } else if (linkRefName.equalsIgnoreCase(LINKATTRIBUTE_REFNAME_COMMENT)) {
                return link.getComment();
            }
        }

        // There was no data available for this row/column.
        return null;
    }

    @Override
    protected void handleItemDoubleClick(final Object selectedItem) {
        if (isSelectedLinkOpenable()) {
            performOpen();
        }
    }

    /**
     * @return Returns true if the selection is a single item and the selected
     *         item is a related link.
     */
    private boolean selectionIsSingleRelatedLink() {
        final Object[] selectedItems = getSelectedItems();
        if (selectedItems != null && selectedItems.length == 1) {
            return selectedItems[0] instanceof RelatedLink;
        }

        return false;
    }

    private boolean isSelectedLinkOpenable() {
        final Link link = (Link) getSelectedItem();

        if (link instanceof Hyperlink || link instanceof RelatedLink) {
            return true;
        }

        if (link instanceof ExternalLink
            && (link.getLinkType().getName().equals(RegisteredLinkTypeNames.CHANGESET)
                || link.getLinkType().getName().equals(RegisteredLinkTypeNames.VERSIONED_ITEM)
                || link.getLinkType().getName().equals(RegisteredLinkTypeNames.STORYBOARD)
                || link.getLinkType().getName().equals(RegisteredLinkTypeNames.COMMIT))) {
            return true;
        }

        return false;
    }

    @Override
    protected void createButtons(final Composite parent) {
        newButton =
            createButton(parent, Messages.getString("WorkItemLinksControl.NewButtonText"), new SelectionAdapter() //$NON-NLS-1$
        {
                @Override
                public void widgetSelected(final SelectionEvent e) {
                    performNew();
                }
            });
        newButton.setToolTipText(Messages.getString("WorkItemLinksControl.NewLinkedItemTooltipText")); //$NON-NLS-1$
        newButton.setEnabled(getWorkItem().getID() != 0);

        addButton =
            createButton(parent, Messages.getString("WorkItemLinksControl.AddLinkButtonText"), new SelectionAdapter() //$NON-NLS-1$
        {
                @Override
                public void widgetSelected(final SelectionEvent e) {
                    performAdd();
                }
            });
        addButton.setToolTipText(Messages.getString("WorkItemLinksControl.AddLinkButtonTooltip")); //$NON-NLS-1$

        openButton =
            createButton(parent, Messages.getString("WorkItemLinksControl.OpenButtonText"), new SelectionAdapter() //$NON-NLS-1$
        {
                @Override
                public void widgetSelected(final SelectionEvent e) {
                    performOpen();
                }
            });
        openButton.setToolTipText(Messages.getString("WorkItemLinksControl.OpenLinkedItemButtonTooltip")); //$NON-NLS-1$

        deleteButton =
            createButton(parent, Messages.getString("WorkItemLinksControl.DeleteButtonText"), new SelectionAdapter() //$NON-NLS-1$
        {
                @Override
                public void widgetSelected(final SelectionEvent e) {
                    performDelete();
                }
            });
        deleteButton.setToolTipText(Messages.getString("WorkItemLinksControl.DeleteButtonTooltip")); //$NON-NLS-1$
    }

    @Override
    protected int getNumberOfButtons() {
        return 4;
    }

    private static String linkAttributeToDisplayName(final String linkAttribute) {
        if (linkAttribute.equalsIgnoreCase(LINKATTRIBUTE_REFNAME_LINKTYPE)) {
            return LINKATTRIBUTE_DISPNAME_LINKTYPE;
        } else if (linkAttribute.equalsIgnoreCase(LINKATTRIBUTE_REFNAME_DESCRIPTION)) {
            return LINKATTRIBUTE_DISPNAME_DESCRIPTION;
        } else if (linkAttribute.equalsIgnoreCase(LINKATTRIBUTE_REFNAME_COMMENT)) {
            return LINKATTRIBUTE_DISPNAME_COMMENT;
        } else {
            final String messageFormat = Messages.getString("WorkItemLinksControl.UnknownAttributeFormat"); //$NON-NLS-1$
            final String message = MessageFormat.format(messageFormat, linkAttribute);
            throw new IllegalArgumentException(message);
        }
    }

    /**
     * A class to compare a field from the target of two work item link. Numeric
     * comparisons are computed for INTEGER and DOUBLE field types. A date
     * comparison is compuated for a DATETIME field. All other field types are
     * compared as stinrgs.
     *
     * An instance of this class is associated with a table column in a
     * TableViewerSorter class.
     *
     *
     */
    private class RelatedLinkFieldComparator implements Comparator<Link> {
        private final String fieldReferenceName;

        public RelatedLinkFieldComparator(final String fieldReferenceName) {
            this.fieldReferenceName = fieldReferenceName;
        }

        @Override
        public int compare(final Link link0, final Link link1) {
            RelatedLink relatedLink0 = null;
            RelatedLink relatedLink1 = null;

            if (link0 instanceof RelatedLink) {
                relatedLink0 = (RelatedLink) link0;
            }

            if (link1 instanceof RelatedLink) {
                relatedLink1 = (RelatedLink) link1;
            }

            if (relatedLink0 == null || relatedLink1 == null) {
                return compareWithNull(relatedLink0, relatedLink1);
            }

            final WorkItem workItem0 = relatedLink0.getTargetWorkItem();
            final WorkItem workItem1 = relatedLink1.getTargetWorkItem();

            if (workItem0 == null || workItem1 == null) {
                return compareWithNull(workItem0, workItem1);
            }

            final Field field0 = workItem0.getFields().getField(fieldReferenceName);
            final Field field1 = workItem1.getFields().getField(fieldReferenceName);

            if (field0 == null || field1 == null) {
                return compareWithNull(field0, field1);
            }

            final Object value0 = field0.getValue();
            final Object value1 = field1.getValue();

            if (value0 == null || value1 == null) {
                return compareWithNull(value0, value1);
            }

            if (field0.getFieldDefinition().getFieldType() == FieldType.INTEGER) {
                return ((Integer) value0).compareTo(((Integer) value1));
            } else if (field0.getFieldDefinition().getFieldType() == FieldType.DOUBLE) {
                return ((Double) value0).compareTo(((Double) value1));
            } else if (field0.getFieldDefinition().getFieldType() == FieldType.DATETIME) {
                return ((Date) value0).compareTo(((Date) value1));
            } else {
                return ((String) value0).compareTo(((String) value1));
            }
        }

        private int compareWithNull(final Object obj0, final Object obj1) {
            Check.isTrue(obj0 == null || obj1 == null, "obj0 == null || obj1 == null"); //$NON-NLS-1$

            if (obj0 == null && obj1 == null) {
                return 0;
            } else if (obj0 == null) {
                return -1;
            } else {
                return 1;
            }
        }
    }

    private class ColumnMetadata {
        private final String referenceName;
        private final String displayName;
        private final boolean isLinkAttribute;
        private final Comparator<Link> comparator;

        public ColumnMetadata(final String referenceName, final String displayName, final boolean isLinkAttribute) {
            this(referenceName, displayName, isLinkAttribute, null);
        }

        public ColumnMetadata(
            final String referenceName,
            final String displayName,
            final boolean isLinkAttribute,
            final Comparator<Link> comparator) {
            this.referenceName = referenceName;
            this.displayName = displayName;
            this.isLinkAttribute = isLinkAttribute;
            this.comparator = comparator;
        }

        public String getReferenceName() {
            return referenceName;
        }

        public String getDisplayName() {
            return displayName;
        }

        public Comparator<Link> getComparator() {
            return comparator;
        }

        public boolean isWorkItemField() {
            return !isLinkAttribute;
        }
    }
}
