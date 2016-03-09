// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teamexplorer.sections;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ImageHyperlink;

import com.microsoft.tfs.client.common.commands.wit.GetWorkItemByIDCommand;
import com.microsoft.tfs.client.common.framework.command.ICommandExecutor;
import com.microsoft.tfs.client.common.server.TFSServer;
import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.TFSCommonUIClientPlugin;
import com.microsoft.tfs.client.common.ui.framework.command.UICommandExecutorFactory;
import com.microsoft.tfs.client.common.ui.framework.helper.ComboBoxCellEditorHelper;
import com.microsoft.tfs.client.common.ui.framework.helper.ContentProviderAdapter;
import com.microsoft.tfs.client.common.ui.framework.helper.SWTUtil;
import com.microsoft.tfs.client.common.ui.framework.helper.UIHelpers;
import com.microsoft.tfs.client.common.ui.framework.image.ImageHelper;
import com.microsoft.tfs.client.common.ui.framework.layout.GridDataBuilder;
import com.microsoft.tfs.client.common.ui.framework.table.TableColumnData;
import com.microsoft.tfs.client.common.ui.framework.table.TableViewerUtils;
import com.microsoft.tfs.client.common.ui.framework.table.tooltip.TableTooltipStyledTextInfo;
import com.microsoft.tfs.client.common.ui.framework.table.tooltip.TableTooltipStyledTextManager;
import com.microsoft.tfs.client.common.ui.framework.table.tooltip.TableTooltipStyledTextProvider;
import com.microsoft.tfs.client.common.ui.helpers.AutomationIDHelper;
import com.microsoft.tfs.client.common.ui.helpers.WorkItemEditorHelper;
import com.microsoft.tfs.client.common.ui.teamexplorer.TeamExplorerContext;
import com.microsoft.tfs.client.common.ui.teamexplorer.TeamExplorerEvents;
import com.microsoft.tfs.client.common.ui.teamexplorer.TeamExplorerResizeListener;
import com.microsoft.tfs.client.common.ui.teamexplorer.favorites.QueryFavoriteItem;
import com.microsoft.tfs.client.common.ui.teamexplorer.helpers.PageHelpers;
import com.microsoft.tfs.client.common.ui.teamexplorer.helpers.WorkItemHelpers;
import com.microsoft.tfs.client.common.ui.teamexplorer.internal.TeamExplorerHelpers;
import com.microsoft.tfs.client.common.ui.teamexplorer.internal.pendingchanges.AssociatedWorkItemsChangedListener;
import com.microsoft.tfs.core.clients.favorites.FavoriteItem;
import com.microsoft.tfs.core.clients.favorites.FavoritesStoreFactory;
import com.microsoft.tfs.core.clients.favorites.IFavoritesStore;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.WorkItemCheckinInfo;
import com.microsoft.tfs.core.clients.workitem.CoreFieldReferenceNames;
import com.microsoft.tfs.core.clients.workitem.WorkItem;
import com.microsoft.tfs.core.clients.workitem.WorkItemClient;
import com.microsoft.tfs.core.clients.workitem.query.StoredQuery;
import com.microsoft.tfs.core.clients.workitem.queryhierarchy.QueryDefinition;
import com.microsoft.tfs.core.clients.workitem.queryhierarchy.QueryFolder;
import com.microsoft.tfs.core.clients.workitem.queryhierarchy.QueryItem;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.NewlineUtils;

public class TeamExplorerPendingChangesWorkItemsSection extends TeamExplorerPendingChangesBaseSection {
    private final ImageHelper imageHelper = new ImageHelper(TFSCommonUIClientPlugin.PLUGIN_ID);

    private final AssociatedWorkItemsListener listener = new AssociatedWorkItemsListener();

    private TableViewer tableViewer;
    private Label emptyLabel;
    private Composite addWorkItemComposite;
    private Text idTextbox;

    private QueryFavoriteItem[] myFavorites;
    private QueryFavoriteItem[] teamFavorites;

    public static final String WORKITEMID_TEXTBOX_ID =
        "com.microsoft.tfs.client.common.ui.teamexplorer.sections.TeamExplorerPendingChangesWorkItemsSection.workItemIDText"; //$NON-NLS-1$

    public static final String ADD_BUTTON_ID =
        "com.microsoft.tfs.client.common.ui.teamexplorer.sections.TeamExplorerPendingChangesWorkItemsSection.addButton"; //$NON-NLS-1$

    private TeamExplorerPendingChangesWorkItemsSectionState state;

    @Override
    public void initialize(final IProgressMonitor monitor, final TeamExplorerContext context, final Object state) {
        initialize(monitor, context);
        if (state instanceof TeamExplorerPendingChangesWorkItemsSectionState) {
            this.state = (TeamExplorerPendingChangesWorkItemsSectionState) state;
        }
    }

    @Override
    public Composite getSectionContent(
        final FormToolkit toolkit,
        final Composite parent,
        final int style,
        final TeamExplorerContext context) {
        // Create the container composite.
        final Composite composite = toolkit.createComposite(parent);
        // Create disconnected view if not connected.
        if (!context.isConnectedToCollection()) {
            SWTUtil.gridLayout(composite, 1, true, 0, 5);
            createDisconnectedContent(toolkit, composite);
            return composite;
        }

        // Create a menu manager for the query drop down.
        final MenuManager queryMenuManager = new MenuManager("#popup"); //$NON-NLS-1$
        queryMenuManager.setRemoveAllWhenShown(true);
        queryMenuManager.addMenuListener(new IMenuListener() {
            @Override
            public void menuAboutToShow(final IMenuManager manager) {
                fillQueryMenu(manager);
            }
        });

        // Form-style border painting not enabled (0 pixel margins OK) because
        // no applicable controls in this composite
        SWTUtil.gridLayout(composite, 3, false, 0, 0);

        // Add the 'Queries' link.
        final String text1 = Messages.getString("TeamExplorerPendingChangesWorkItemsSection.QueriesLinkText"); //$NON-NLS-1$
        final Menu menu = queryMenuManager.createContextMenu(composite.getShell());
        final ImageHyperlink link = PageHelpers.createDropHyperlink(toolkit, composite, text1, menu);
        GridDataBuilder.newInstance().applyTo(link);

        // Add a separator.
        final Label separator = toolkit.createLabel(composite, "|", SWT.VERTICAL); //$NON-NLS-1$
        GridDataBuilder.newInstance().vFill().applyTo(separator);

        // Create the 'Add by ID' link.
        final String text2 = Messages.getString("TeamExplorerPendingChangesWorkItemsSection.AddWorkItemIDLinkText"); //$NON-NLS-1$
        final ImageHyperlink idLink = PageHelpers.createDropHyperlink(toolkit, composite, text2);
        GridDataBuilder.newInstance().hFill().hGrab().applyTo(idLink);

        // Add listener to show/hide the 'Add by ID' composite.
        idLink.addHyperlinkListener(new HyperlinkAdapter() {
            @Override
            public void linkActivated(final HyperlinkEvent e) {
                TeamExplorerHelpers.toggleCompositeVisibility(addWorkItemComposite);
                TeamExplorerHelpers.relayoutContainingScrolledComposite(composite.getParent());
            }
        });

        // Create the 'Add by ID' composite.
        addWorkItemComposite = createAddWorkItemByIDComposite(toolkit, composite);
        GridDataBuilder.newInstance().hSpan(3).applyTo(addWorkItemComposite);

        // Restore 'Add by ID' composite hidden state.
        if (state == null || !state.isWorkItemCompositeVisible()) {
            TeamExplorerHelpers.toggleCompositeVisibility(addWorkItemComposite);
        }

        // Create the empty label, which is visible when the table is empty.
        final String emptyListText =
            Messages.getString("TeamExplorerPendingChangesWorkItemsSection.NoAssocWorkItemsText"); //$NON-NLS-1$

        emptyLabel = toolkit.createLabel(composite, emptyListText);
        GridDataBuilder.newInstance().hFill().hGrab().hSpan(3).applyTo(emptyLabel);

        // Create the table viewer, which is visible when the table is
        // non-empty.
        createTableViewer(toolkit, composite);
        tableViewer.setInput(getModel().getAssociatedWorkItems());

        GridDataBuilder.newInstance().fill().grab().hSpan(3).applyTo(tableViewer.getTable());

        // Show either the table or the 'empty label'.
        showOrHideTable(getModel().getAssociatedWorkItemCount() > 0);

        getModel().addAssociatedWorkItemsChangedListener(listener);

        // Add a resize listener to limit the width of the AddWorkItemComposite.
        final TeamExplorerResizeListener resizeListener = new TeamExplorerResizeListener(addWorkItemComposite);
        context.getEvents().addListener(TeamExplorerEvents.FORM_RESIZED, resizeListener);

        // Handle disposal of this control.
        composite.addDisposeListener(new DisposeListener() {
            @Override
            public void widgetDisposed(final DisposeEvent e) {
                imageHelper.dispose();
                getModel().removeAssociatedWorkItemsChangedListener(listener);
                context.getEvents().removeListener(TeamExplorerEvents.FORM_RESIZED, resizeListener);
            }
        });

        return composite;
    }

    @Override
    public String getTitle() {
        final int count = getAssociatedWorkItemCount();

        // This method can be called before the section has been initialized, so
        // the context may be null
        if (getContext() == null || !getContext().isConnected() || count == 0) {
            return baseTitle;
        }

        final String format = Messages.getString("TeamExplorerCommon.TitleWithCountFormat"); //$NON-NLS-1$
        return MessageFormat.format(format, baseTitle, count);
    }

    private int getAssociatedWorkItemCount() {
        return getModel() == null ? 0 : getModel().getAssociatedWorkItemCount();
    }

    @Override
    public Object saveState() {
        if (addWorkItemComposite == null || idTextbox == null) {
            state = null;
        } else {
            state = new TeamExplorerPendingChangesWorkItemsSectionState(
                addWorkItemComposite.isVisible(),
                idTextbox.getText());
        }
        return state;
    }

    private void createTableViewer(final FormToolkit toolkit, final Composite parent) {
        tableViewer = new TableViewer(parent, SWT.MULTI | SWT.FULL_SELECTION | SWT.NO_SCROLL);

        // hook up tooltips
        final TableTooltipStyledTextManager tooltipManager =
            new TableTooltipStyledTextManager(tableViewer.getTable(), new MyTooltipProvider());

        tooltipManager.addTooltipManager();

        registerContextMenu(getContext(), tableViewer.getControl(), tableViewer);

        final TableColumnData[] columnData = new TableColumnData[] {
            new TableColumnData((Image) null, -1, 0.99F, null),
            new TableColumnData((Image) null, 100, -1F, "action"), //$NON-NLS-1$
        };

        TableViewerUtils.setupTableViewer(tableViewer, false, false, null, columnData);

        tableViewer.setContentProvider(new MyContentProvider());
        tableViewer.setLabelProvider(new MyLabelProvider());
        tableViewer.addDoubleClickListener(new MyDoubleClickListener(getContext().getServer()));

        new WorkItemActionCellEditor(tableViewer);
    }

    private Composite createAddWorkItemByIDComposite(final FormToolkit toolkit, final Composite parent) {
        final Composite composite = toolkit.createComposite(parent);

        // Text controls present in this composite, enable form-style borders,
        // must have at least 1 pixel margins
        toolkit.paintBordersFor(composite);
        SWTUtil.gridLayout(composite, 2, false, 5, 5);

        composite.setBackground(TeamExplorerHelpers.getDropCompositeBackground(parent));
        composite.setForeground(TeamExplorerHelpers.getDropCompositeForeground(parent));

        idTextbox = toolkit.createText(composite, ""); //$NON-NLS-1$
        if (state != null) {
            idTextbox.setText(state.getWorkItemId());
        }
        GridDataBuilder.newInstance().hAlignFill().hGrab().hSpan(2).applyTo(idTextbox);
        AutomationIDHelper.setWidgetID(idTextbox, WORKITEMID_TEXTBOX_ID);

        final Button addButton = toolkit.createButton(
            composite,
            Messages.getString("TeamExplorerPendingChangesWorkItemsSection.AddButtonText"), //$NON-NLS-1$
            SWT.PUSH);
        AutomationIDHelper.setWidgetID(addButton, ADD_BUTTON_ID);

        addButton.setEnabled(false);
        addButton.setBackground(composite.getBackground());
        addButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                addWorkItemByID(idTextbox);
            }

            @Override
            public void widgetDefaultSelected(final SelectionEvent e) {
                addWorkItemByID(idTextbox);
            }
        });

        idTextbox.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetDefaultSelected(final SelectionEvent e) {
                if (toWorkItemID(idTextbox.getText()) != -1) {
                    addWorkItemByID(idTextbox);
                }
            }
        });

        GridDataBuilder.newInstance().applyTo(addButton);

        final Button closeButton = toolkit.createButton(
            composite,
            Messages.getString("TeamExplorerPendingChangesWorkItemsSection.CloseButtonText"), //$NON-NLS-1$
            SWT.PUSH);

        closeButton.setBackground(composite.getBackground());
        closeButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                close();
            }

            @Override
            public void widgetDefaultSelected(final SelectionEvent e) {
                close();
            }

            private void close() {
                idTextbox.setText(""); //$NON-NLS-1$
                TeamExplorerHelpers.toggleCompositeVisibility(addWorkItemComposite);
                TeamExplorerHelpers.relayoutContainingScrolledComposite(composite.getParent());
            }
        });

        GridDataBuilder.newInstance().applyTo(closeButton);

        idTextbox.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(final ModifyEvent e) {
                addButton.setEnabled(toWorkItemID(idTextbox.getText()) != -1);
            }
        });

        return composite;
    }

    private void addWorkItemByID(final Text idTextbox) {
        // Convert the string to an ID and validate.
        final int workItemID = toWorkItemID(idTextbox.getText());
        Check.isTrue(workItemID != -1, "workItemID != -1"); //$NON-NLS-1$

        // Get the work item object for the specified ID.
        final WorkItemClient workItemClient = getContext().getServer().getConnection().getWorkItemClient();
        final GetWorkItemByIDCommand command = new GetWorkItemByIDCommand(workItemClient, workItemID);
        final ICommandExecutor executor = UICommandExecutorFactory.newUICommandExecutor(idTextbox.getShell());
        final IStatus status = executor.execute(command);

        if (status == Status.CANCEL_STATUS) {
            return;
        }

        final WorkItem workItem = command.getWorkItem();
        if (workItem == null) {
            WorkItemHelpers.showWorkItemDoesNotExistError(idTextbox.getShell(), workItemID);
        } else {
            getModel().associateWorkItem(workItem);
            idTextbox.setText(""); //$NON-NLS-1$
            TeamExplorerHelpers.toggleCompositeVisibility(addWorkItemComposite);
        }
    }

    private int toWorkItemID(final String text) {
        try {
            final int value = Integer.parseInt(text.trim());
            return value > 0 ? value : -1;
        } catch (final NumberFormatException e) {
            return -1;
        }
    }

    private void showOrHideTable(final boolean show) {
        final GridData gridDataTable = (GridData) tableViewer.getTable().getLayoutData();
        final GridData gridDataLabel = (GridData) emptyLabel.getLayoutData();

        if (show) {
            emptyLabel.setVisible(false);
            tableViewer.getTable().setVisible(true);

            gridDataLabel.exclude = true;
            gridDataTable.exclude = false;
        } else {
            emptyLabel.setVisible(true);
            tableViewer.getTable().setVisible(false);

            gridDataLabel.exclude = false;
            gridDataTable.exclude = true;
        }
    }

    private void refreshTable() {
        UIHelpers.runOnUIThread(true, new Runnable() {
            @Override
            public void run() {
                if (tableViewer.getTable().isDisposed()) {
                    return;
                }

                final boolean wasEmpty = tableViewer.getTable().getVisible() == false;

                final WorkItemCheckinInfo[] checkinInfos = getModel().getAssociatedWorkItems();

                final boolean isEmpty = checkinInfos.length == 0;

                if (wasEmpty != isEmpty) {
                    showOrHideTable(!isEmpty);
                }

                tableViewer.setInput(checkinInfos);

                TeamExplorerHelpers.updateContainingSectionTitle(tableViewer.getTable(), getTitle());
                TeamExplorerHelpers.relayoutContainingScrolledComposite(tableViewer.getTable());
            }
        });
    }

    private void fillQueryMenu(final IMenuManager menuManager) {
        final QueryDefinition[] myQueries = getMyQueries(getContext());
        final QueryFavoriteItem[] myFavorites = getMyFavorites(getContext());
        final QueryFavoriteItem[] teamFavorites = getTeamFavorites(getContext());

        if (myQueries.length > 0) {
            addQueryDefinitionsToMenu(menuManager, myQueries);
        }

        if (myFavorites.length > 0) {
            addQueryFavoriteActionsToMenu(menuManager, myFavorites);
        }

        if (teamFavorites.length > 0) {
            addQueryFavoriteActionsToMenu(menuManager, teamFavorites);
        }

        if (menuManager.getItems().length == 0) {
            menuManager.add(new QueryAction(null));
        }
    }

    private void addQueryFavoriteActionsToMenu(final IMenuManager menuManager, final QueryFavoriteItem[] favorites) {
        if (menuManager.getItems().length > 0) {
            menuManager.add(new Separator());
        }

        for (final QueryFavoriteItem favorite : favorites) {
            menuManager.add(new QueryAction(favorite.getQueryDefinition()));
        }
    }

    private void addQueryDefinitionsToMenu(final IMenuManager menuManager, final QueryDefinition[] definitions) {
        for (final QueryDefinition definition : definitions) {
            menuManager.add(new QueryAction(definition));
        }
    }

    private QueryDefinition[] getMyQueries(final TeamExplorerContext context) {
        final QueryFolder privateFolder = WorkItemHelpers.getMyQueriesFolder(context.getCurrentProject());
        if (privateFolder == null) {
            return new QueryDefinition[0];
        }

        final List<QueryDefinition> queries = new ArrayList<QueryDefinition>();
        for (final QueryItem privateItem : privateFolder.getItems()) {
            if (privateItem instanceof QueryDefinition) {
                queries.add((QueryDefinition) privateItem);
            }
        }

        return queries.toArray(new QueryDefinition[queries.size()]);
    }

    private QueryFavoriteItem[] getMyFavorites(final TeamExplorerContext context) {
        if (myFavorites == null) {
            final IFavoritesStore store = getFavoritesStore(context, true);
            myFavorites = getQueryFavoriteItems(store, context, true);
        }
        return myFavorites;
    }

    private QueryFavoriteItem[] getTeamFavorites(final TeamExplorerContext context) {
        if (teamFavorites == null) {
            final IFavoritesStore store = getFavoritesStore(context, false);
            teamFavorites = getQueryFavoriteItems(store, context, false);
        }
        return teamFavorites;
    }

    private QueryFavoriteItem[] getQueryFavoriteItems(
        final IFavoritesStore store,
        final TeamExplorerContext context,
        final boolean isPersonal) {
        if (store == null) {
            return new QueryFavoriteItem[0];
        } else {
            final FavoriteItem[] favorites = store.getFavorites();
            try {
                return QueryFavoriteItem.fromFavoriteItems(context.getCurrentProject(), favorites, isPersonal);
            } catch (final Exception e) {
                return new QueryFavoriteItem[0];
            }
        }
    }

    private IFavoritesStore getFavoritesStore(final TeamExplorerContext context, final boolean isPersonal) {
        return FavoritesStoreFactory.create(
            context.getServer().getConnection(),
            context.getCurrentProjectInfo(),
            context.getCurrentTeam(),
            QueryFavoriteItem.QUERY_DEFINITION_FEATURE_SCOPE,
            isPersonal);
    }

    private class MyTooltipProvider implements TableTooltipStyledTextProvider {
        private int tabWidth = 0;
        private final Map<String, String> map = new HashMap<String, String>();

        private MyTooltipProvider() {
            map.put(
                CoreFieldReferenceNames.CREATED_BY,
                Messages.getString("TeamExplorerPendingChangesWorkItemsSection.TipCreatedBy")); //$NON-NLS-1$

            map.put(
                CoreFieldReferenceNames.CREATED_DATE,
                Messages.getString("TeamExplorerPendingChangesWorkItemsSection.TipCratedDate")); //$NON-NLS-1$

            map.put(
                CoreFieldReferenceNames.STATE,
                Messages.getString("TeamExplorerPendingChangesWorkItemsSection.TipState")); //$NON-NLS-1$

            map.put(
                CoreFieldReferenceNames.ASSIGNED_TO,
                Messages.getString("TeamExplorerPendingChangesWorkItemsSection.TipAssignedTo")); //$NON-NLS-1$

            map.put(
                CoreFieldReferenceNames.CHANGED_DATE,
                Messages.getString("TeamExplorerPendingChangesWorkItemsSection.TipLastChanged")); //$NON-NLS-1$

            map.put(
                CoreFieldReferenceNames.AREA_PATH,
                Messages.getString("TeamExplorerPendingChangesWorkItemsSection.TipAreaPath")); //$NON-NLS-1$

            map.put(
                CoreFieldReferenceNames.ITERATION_PATH,
                Messages.getString("TeamExplorerPendingChangesWorkItemsSection.TipIterationPath")); //$NON-NLS-1$

            for (final String value : map.values()) {
                tabWidth = Math.max(tabWidth, value.length());
            }
        }

        @Override
        public TableTooltipStyledTextInfo getTooltipStyledTextInfo(final Object element) {
            if (element instanceof WorkItemCheckinInfo) {
                final TableTooltipStyledTextInfo info = new TableTooltipStyledTextInfo();
                final WorkItem workItem = ((WorkItemCheckinInfo) element).getWorkItem();
                final String title = workItem.getTitle();

                final StringBuilder sb = new StringBuilder();
                sb.append(title);

                final StyleRange titleRange = new StyleRange();
                titleRange.start = 0;
                titleRange.length = title.length();
                titleRange.fontStyle = SWT.BOLD;
                info.addStyleRange(titleRange);

                sb.append(NewlineUtils.PLATFORM_NEWLINE);
                sb.append(NewlineUtils.PLATFORM_NEWLINE);

                appendField(info, sb, workItem, CoreFieldReferenceNames.CREATED_BY);
                appendField(info, sb, workItem, CoreFieldReferenceNames.CREATED_DATE);

                sb.append(NewlineUtils.PLATFORM_NEWLINE);

                appendField(info, sb, workItem, CoreFieldReferenceNames.STATE);
                appendField(info, sb, workItem, CoreFieldReferenceNames.ASSIGNED_TO);
                appendField(info, sb, workItem, CoreFieldReferenceNames.CHANGED_DATE);
                appendField(info, sb, workItem, CoreFieldReferenceNames.AREA_PATH);
                appendField(info, sb, workItem, CoreFieldReferenceNames.ITERATION_PATH);

                info.setText(sb.toString());
                info.setTabWidth(tabWidth);
                return info;
            }

            return null;
        }

        private void appendField(
            final TableTooltipStyledTextInfo info,
            final StringBuilder sb,
            final WorkItem workItem,
            final String fieldName) {
            final String displayName = map.get(fieldName);
            Check.notNull(displayName, "displayName"); //$NON-NLS-1$

            sb.append(displayName);
            sb.append("\t"); //$NON-NLS-1$

            final Object value = workItem.getFields().getField(fieldName).getValue();
            if (value != null) {
                final int rangeStart = sb.length();
                sb.append(value.toString());
                final int rangeLen = sb.length() - rangeStart;

                final StyleRange range = new StyleRange();
                range.start = rangeStart;
                range.length = rangeLen;
                range.fontStyle = SWT.BOLD;
                info.addStyleRange(range);
            }
            sb.append(NewlineUtils.PLATFORM_NEWLINE);
        }
    }

    private class MyContentProvider extends ContentProviderAdapter {
        @Override
        public Object[] getElements(final Object inputElement) {
            return (Object[]) inputElement;
        }
    }

    private class MyLabelProvider extends LabelProvider implements ITableLabelProvider {
        @Override
        public Image getColumnImage(final Object element, final int columnIndex) {
            if (element instanceof WorkItemCheckinInfo && columnIndex == 0) {
                return imageHelper.getImage("/images/wit/workitem.gif"); //$NON-NLS-1$
            }

            return null;
        }

        @Override
        public String getColumnText(final Object element, final int columnIndex) {
            if (element instanceof WorkItemCheckinInfo) {
                final WorkItemCheckinInfo checkinInfo = (WorkItemCheckinInfo) element;

                if (columnIndex == 0) {
                    final WorkItem workItem = checkinInfo.getWorkItem();

                    return MessageFormat.format(
                        //@formatter:off
                        Messages.getString("TeamExplorerPendingChangesWorkItemsSection.AssociatedWorkItemDisplayFormat"), //$NON-NLS-1$
                        //@formatter:on
                        Integer.toString(workItem.getID()),
                        workItem.getTitle());
                } else if (columnIndex == 1) {
                    return checkinInfo.getAction().toUIString();
                }
            }

            return ""; //$NON-NLS-1$
        }
    }

    private class MyDoubleClickListener implements IDoubleClickListener {
        private final TFSServer server;

        public MyDoubleClickListener(final TFSServer server) {
            this.server = server;
        }

        @Override
        public void doubleClick(final DoubleClickEvent event) {
            final IStructuredSelection selection = (IStructuredSelection) event.getSelection();
            final Object element = selection.getFirstElement();

            if (element instanceof WorkItemCheckinInfo) {
                final WorkItemCheckinInfo checkinInfo = (WorkItemCheckinInfo) element;
                WorkItemEditorHelper.openEditor(server, checkinInfo.getWorkItem().getID());
            }
        }
    }

    private class AssociatedWorkItemsListener implements AssociatedWorkItemsChangedListener {
        @Override
        public void onAssociatedWorkItemsChanged() {
            refreshTable();
        }
    }

    private class WorkItemActionCellEditor extends ComboBoxCellEditorHelper {
        public WorkItemActionCellEditor(final TableViewer viewer) {
            super(viewer, 1);
        }

        @Override
        protected String[] getAvailableOptions(final Object element) {
            return ((WorkItemCheckinInfo) element).getAvailableActionStrings();
        }

        @Override
        protected String getSelectedOption(final Object element) {
            return ((WorkItemCheckinInfo) element).getActionString();
        }

        @Override
        protected void setSelectedOption(final Object element, final String option) {
            ((WorkItemCheckinInfo) element).setActionFromString(option);
        }

        @Override
        protected boolean shouldAllowEdit(final Object element) {
            return true;
        }
    }

    private class QueryAction extends Action {
        private final QueryDefinition queryDefinition;

        public QueryAction(final QueryDefinition queryDefinition) {
            this.queryDefinition = queryDefinition;

            setImageDescriptor(imageHelper.getImageDescriptor("/images/wit/query_normal.gif")); //$NON-NLS-1$
            setEnabled(queryDefinition != null);

            if (queryDefinition == null) {
                setText(Messages.getString("TeamExplorerPendingChangesWorkItemsSection.NoQueriesMenuText")); //$NON-NLS-1$
                setEnabled(false);
            } else {
                setText(queryDefinition.getName());
                setEnabled(true);
            }
        }

        @Override
        public void run() {
            final StoredQuery storedQuery = WorkItemHelpers.createStoredQueryFromDefinition(queryDefinition);

            WorkItemHelpers.runQuery(
                tableViewer.getTable().getShell(),
                getContext().getServer(),
                getContext().getCurrentProject(),
                storedQuery);
        }
    }
}
