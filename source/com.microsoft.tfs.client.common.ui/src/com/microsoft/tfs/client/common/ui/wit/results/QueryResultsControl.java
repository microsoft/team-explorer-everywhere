// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.wit.results;

import java.lang.reflect.Method;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Layout;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;

import com.microsoft.tfs.client.common.codemarker.CodeMarker;
import com.microsoft.tfs.client.common.codemarker.CodeMarkerDispatch;
import com.microsoft.tfs.client.common.commands.wit.DestroyWorkItemCommand;
import com.microsoft.tfs.client.common.framework.command.ICommandExecutor;
import com.microsoft.tfs.client.common.server.TFSServer;
import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.TFSCommonUIClientPlugin;
import com.microsoft.tfs.client.common.ui.controls.generic.compatibility.table.CompatibilityVirtualTable;
import com.microsoft.tfs.client.common.ui.framework.action.keybinding.ActionKeyBindingSupport;
import com.microsoft.tfs.client.common.ui.framework.action.keybinding.ActionKeyBindingSupportFactory;
import com.microsoft.tfs.client.common.ui.framework.action.keybinding.CommandIDs;
import com.microsoft.tfs.client.common.ui.framework.command.UICommandExecutorFactory;
import com.microsoft.tfs.client.common.ui.framework.helper.MessageBoxHelpers;
import com.microsoft.tfs.client.common.ui.framework.helper.UIHelpers;
import com.microsoft.tfs.client.common.ui.framework.image.ImageHelper;
import com.microsoft.tfs.client.common.ui.framework.sizing.MeasureItemHeightListener;
import com.microsoft.tfs.client.common.ui.framework.table.TableSortIndicator;
import com.microsoft.tfs.client.common.ui.helpers.WorkItemEditorHelper;
import com.microsoft.tfs.client.common.ui.wit.OpenWorkItemWithAction;
import com.microsoft.tfs.client.common.ui.wit.form.WorkItemEditorInfo;
import com.microsoft.tfs.client.common.ui.wit.form.link.LinkDialog;
import com.microsoft.tfs.client.common.ui.wit.form.link.LinkUIRegistry;
import com.microsoft.tfs.client.common.ui.wit.query.UIQueryUtils;
import com.microsoft.tfs.client.common.ui.wit.results.data.LinkedQueryResultData;
import com.microsoft.tfs.client.common.ui.wit.results.data.QueryResultCommand;
import com.microsoft.tfs.client.common.ui.wit.results.data.QueryResultData;
import com.microsoft.tfs.core.artifact.LinkingFacade;
import com.microsoft.tfs.core.clients.workitem.CoreFieldReferenceNames;
import com.microsoft.tfs.core.clients.workitem.WorkItem;
import com.microsoft.tfs.core.clients.workitem.exceptions.WorkItemLinkValidationException;
import com.microsoft.tfs.core.clients.workitem.fields.FieldDefinition;
import com.microsoft.tfs.core.clients.workitem.fields.FieldDefinitionCollection;
import com.microsoft.tfs.core.clients.workitem.link.Link;
import com.microsoft.tfs.core.clients.workitem.query.DisplayFieldList;
import com.microsoft.tfs.core.clients.workitem.query.Query;
import com.microsoft.tfs.core.clients.workitem.query.SortFieldList;
import com.microsoft.tfs.core.clients.workitem.query.SortType;
import com.microsoft.tfs.core.clients.workitem.query.qe.DisplayField;
import com.microsoft.tfs.core.clients.workitem.query.qe.ResultOptions;
import com.microsoft.tfs.core.clients.workitem.query.qe.SortField;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.HTTPUtil;
import com.microsoft.tfs.util.NewlineUtils;
import com.microsoft.tfs.util.StringUtil;
import com.microsoft.tfs.util.listeners.ListenerList;
import com.microsoft.tfs.util.listeners.StandardListenerList;

public class QueryResultsControl extends CompatibilityVirtualTable {
    private static final String IGNORE_RESIZE_KEY = "ignore-resize"; //$NON-NLS-1$

    private static final Log log = LogFactory.getLog(QueryResultsControl.class);

    public static final CodeMarker CODEMARKER_LOAD_COMPLETE =
        new CodeMarker("com.microsoft.tfs.client.common.ui.wit.results.QueryResultsControl#loadComplete"); //$NON-NLS-1$

    private final ImageHelper imageHelper = new ImageHelper(TFSCommonUIClientPlugin.PLUGIN_ID);

    public static interface ColumnClickedListener {
        public void onColumnClicked(DisplayField displayField, int index);
    }

    public static interface ColumnResizedListener {
        public void onColumnResized(DisplayField displayField, int width);
    }

    private TFSServer server;

    /*
     * The 3 "model" objects for the results control
     */
    private QueryResultData results;
    private DisplayField[] displayFields;
    private SortField[] sortFields;

    /* Two optional objects: project and query name, used only for logging */
    private String projectName;
    private String queryName;

    /*
     * context menu actions defined by the results control
     */
    private IAction openAction;
    private IAction[] openWithActions;
    private IAction linkExistingWorkItemAction;
    private IAction copyUrlAction;
    private IAction copyIdAction;
    private IAction destroyAction;
    private IAction copySelectionAction;
    private IAction copyAllAction;
    private IAction associateWithPendingChangesAction;

    private boolean shiftKeyPressed;
    private ActionKeyBindingSupport actionCommandSupport;

    /*
     * listener collections
     */
    private final ListenerList columnClickedListeners = new StandardListenerList();
    private final ListenerList columnResizedListeners = new StandardListenerList();

    /*
     * The DateFormat used to format field values that are dates.
     *
     * I18N: need to use a specified Locale instead of the default Locale
     */
    private final DateFormat resultsDateFormat = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM);

    /*
     * The number format to use in query results.
     */
    private final NumberFormat resultNumberFormat = NumberFormat.getNumberInstance();

    public QueryResultsControl(final Composite parent, final int style) {
        super(parent, style | SWT.FULL_SELECTION, WorkItem.class);

        setupTable(getTable());

        createActions();

        createTransfers();

        addDisposeListener(new DisposeListener() {
            @Override
            public void widgetDisposed(final DisposeEvent e) {
                imageHelper.dispose();
                QueryResultsControl.this.onTableDisposed(e);
            }
        });

        // VS, Web Access and Excel do not display grouping in their number
        // formats despite what locale says, so we do the same.
        resultNumberFormat.setGroupingUsed(false);
    }

    private void createTransfers() {
        final List<Transfer> transferList = new ArrayList<Transfer>();
        transferList.add(TextTransfer.getInstance());

        /*
         * Use reflection to try to create an html transfer - new in Eclipse 3.2
         */
        Transfer htmlTransfer = null;

        try {
            final Class htmlTransferClass = Class.forName("org.eclipse.swt.dnd.HTMLTransfer"); //$NON-NLS-1$
            final Method instanceMethod = htmlTransferClass.getMethod("getInstance", new Class[0]); //$NON-NLS-1$

            htmlTransfer = (Transfer) instanceMethod.invoke(htmlTransferClass, new Object[0]);
        } catch (final Exception e) {
            /* Suppress */
        }

        if (htmlTransfer != null) {
            transferList.add(htmlTransfer);
        }

        setClipboardTransferTypes(transferList.toArray(new Transfer[transferList.size()]));
    }

    public void addColumnClickedListener(final ColumnClickedListener listener) {
        columnClickedListeners.addListener(listener);
    }

    public void removeColumnClickedListener(final ColumnClickedListener listener) {
        columnClickedListeners.removeListener(listener);
    }

    public void addColumnResizedListener(final ColumnResizedListener listener) {
        columnResizedListeners.addListener(listener);
    }

    public void removeColumnResizedListener(final ColumnResizedListener listener) {
        columnResizedListeners.removeListener(listener);
    }

    public MenuManager getMenuManager() {
        return getContextMenu();
    }

    public void setServer(final TFSServer server) {
        Check.notNull(server, "server"); //$NON-NLS-1$

        this.server = server;
    }

    public void setWorkItems(final QueryResultData dataProvider) {
        Check.notNull(dataProvider, "dataProvider"); //$NON-NLS-1$

        final DisplayFieldList dfl = dataProvider.getQuery().getDisplayFieldList();
        final DisplayField[] displayFields = new DisplayField[dfl.getSize()];
        for (int i = 0; i < displayFields.length; i++) {
            displayFields[i] =
                new DisplayField(dfl.getField(i).getName(), ResultOptions.getDefaultColumnWidth(dfl.getField(i)));
        }

        final SortFieldList sfl = dataProvider.getQuery().getSortFieldList();
        final SortField[] sortFields = new SortField[sfl.getSize()];
        for (int i = 0; i < sortFields.length; i++) {
            sortFields[i] = new SortField(
                sfl.get(i).getFieldDefinition().getName(),
                SortType.ASCENDING == sfl.get(i).getSortType());
        }

        setWorkItems(dataProvider, displayFields, sortFields, null, null);
    }

    public void setWorkItems(
        final QueryResultData dataProvider,
        final DisplayField[] displayFields,
        final SortField[] sortFields,
        final String projectName,
        final String queryName) {
        Check.notNull(dataProvider, "dataProvider"); //$NON-NLS-1$
        Check.notNull(displayFields, "displayFields"); //$NON-NLS-1$
        Check.notNull(sortFields, "sortFields"); //$NON-NLS-1$

        results = dataProvider;
        this.displayFields = displayFields;
        this.sortFields = sortFields;
        this.projectName = projectName; /* may be null */
        this.queryName = queryName; /* may be null */

        createTableColumns();

        setItemCountAndClearAll(results.getCount());
        CodeMarkerDispatch.dispatch(CODEMARKER_LOAD_COMPLETE);
    }

    public void clear() {
        setItemCountAndClearAll(0);
    }

    public WorkItem getSelectedWorkItem() {
        final int index = getTable().getSelectionIndex();
        if (index == -1) {
            return null;
        }
        return results.getItem(index);
    }

    public WorkItem[] getSelectedWorkItems() {
        final List<WorkItem> list = new ArrayList<WorkItem>();
        final int[] indicies = getSelectedIndices();

        for (final int index : indicies) {
            list.add(results.getItem(index));
        }

        return list.toArray(new WorkItem[list.size()]);
    }

    public QueryResultData getDataProvider() {
        return results;
    }

    private void setItemCountAndClearAll(final int itemCount) {
        getTable().setItemCount(itemCount);
        clearSelection();
    }

    private void createActions() {
        openAction = new Action() {
            @Override
            public void run() {
                final WorkItem selectedWorkItem = getSelectedWorkItem();
                if (UIQueryUtils.verifyAccessToWorkItem(selectedWorkItem)) {
                    WorkItemEditorHelper.openEditor(server, selectedWorkItem);
                }
            }
        };
        openAction.setText(Messages.getString("QueryResultsControl.OpenActionText")); //$NON-NLS-1$

        final List<WorkItemEditorInfo> editors = WorkItemEditorHelper.getWorkItemEditors();
        if (editors != null && editors.size() > 0) {
            int count = 0;
            openWithActions = new OpenWorkItemWithAction[editors.size()];

            for (final WorkItemEditorInfo editor : editors) {
                openWithActions[count++] = new OpenWorkItemWithAction(editor.getDisplayName(), editor.getEditorID()) {
                    @Override
                    public void run() {
                        final WorkItem selectedWorkItem = getSelectedWorkItem();
                        if (UIQueryUtils.verifyAccessToWorkItem(selectedWorkItem)) {
                            WorkItemEditorHelper.openEditor(server, selectedWorkItem, getEditorID());
                        }
                    }
                };
            }
        }

        associateWithPendingChangesAction = new Action() {
            @Override
            public void run() {
                final TFSCommonUIClientPlugin plugin = TFSCommonUIClientPlugin.getDefault();
                plugin.getPendingChangesViewModel().associateWorkItems(getSelectedWorkItems());
            }
        };
        associateWithPendingChangesAction.setText(
            Messages.getString("QueryResultsControl.AssocWithPendingChangeActionText")); //$NON-NLS-1$

        linkExistingWorkItemAction = new Action() {
            @Override
            public void run() {
                final WorkItem selectedWorkItem = getSelectedWorkItem();
                if (UIQueryUtils.verifyAccessToWorkItem(selectedWorkItem)) {
                    try {
                        final LinkDialog dialog = new LinkDialog(
                            getShell(),
                            selectedWorkItem,
                            new LinkUIRegistry(server, selectedWorkItem, null),
                            null);

                        if (dialog.open() == IDialogConstants.OK_ID) {
                            selectedWorkItem.open();
                            final Link[] links = dialog.getLinks();
                            for (int i = 0; i < links.length; i++) {
                                selectedWorkItem.getLinks().add(links[i]);
                            }

                            WorkItemEditorHelper.openEditor(server, selectedWorkItem);
                        }
                    } catch (final WorkItemLinkValidationException e) {
                        MessageDialog.openError(
                            getShell(),
                            Messages.getString("QueryResultsControl.ErrorDialogTitle"), //$NON-NLS-1$
                            e.getLocalizedMessage());
                    }
                }
            }
        };
        linkExistingWorkItemAction.setText(Messages.getString("QueryResultsControl.LinkToExistingActionText")); //$NON-NLS-1$

        destroyAction = new Action() {
            @Override
            public void run() {
                final WorkItem selectedWorkItem = getSelectedWorkItem();
                if (UIQueryUtils.verifyAccessToWorkItem(selectedWorkItem)) {
                    final int destroyId = selectedWorkItem.getFields().getID();

                    final String messageFormat =
                        Messages.getString("QueryResultsControl.ConfirmDestroyDialogTextFormat"); //$NON-NLS-1$
                    final String message = MessageFormat.format(messageFormat, Integer.toString(destroyId));

                    if (MessageDialog.openQuestion(
                        getShell(),
                        Messages.getString("QueryResultsControl.DestroyDialogTitle"), //$NON-NLS-1$
                        message)) {
                        final DestroyWorkItemCommand command = new DestroyWorkItemCommand(selectedWorkItem);
                        final ICommandExecutor executor = UICommandExecutorFactory.newUICommandExecutor(getShell());
                        executor.execute(command);
                    }
                }
            }
        };
        destroyAction.setText(Messages.getString("QueryResultsControl.DestroyActionText")); //$NON-NLS-1$

        copyUrlAction = new Action() {
            @Override
            public void run() {
                final WorkItem workItem = getSelectedWorkItem();
                final String url = LinkingFacade.getExternalURL(workItem, workItem.getClient().getConnection());
                UIHelpers.copyToClipboard(url);
            }
        };
        copyUrlAction.setText(Messages.getString("QueryResultsControl.CopyUrlActionText")); //$NON-NLS-1$

        copyIdAction = new Action() {
            @Override
            public void run() {
                final WorkItem[] workItems = getSelectedWorkItems();
                final String gitCommitWorkItemsLink = WorkItemEditorHelper.createGitCommitWorkItemsLink(workItems);
                UIHelpers.copyToClipboard(gitCommitWorkItemsLink);
            }
        };
        copyIdAction.setText(Messages.getString("QueryResultsControl.CopyIdActionText")); //$NON-NLS-1$

        copySelectionAction = new Action() {
            @Override
            public void run() {
                QueryResultsControl.this.copySelectionToClipboard();
            }
        };
        copySelectionAction.setText(Messages.getString("QueryResultsControl.CopyResultsToClipActionText")); //$NON-NLS-1$
        copySelectionAction.setImageDescriptor(
            PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(ISharedImages.IMG_TOOL_COPY));
        copySelectionAction.setActionDefinitionId(CommandIDs.COPY);

        copyAllAction = new Action() {
            @Override
            public void run() {
                QueryResultsControl.this.copyAllToClipboard();
            }
        };
        copyAllAction.setText(Messages.getString("QueryResultsControl.CopyAllToClipActionText")); //$NON-NLS-1$
    }

    public void bindActionsToParentWorkbenchPart(final IWorkbenchPart part) {
        disposeExistingActionCommandSupport();

        actionCommandSupport = ActionKeyBindingSupportFactory.newInstance(part);
        actionCommandSupport.addAction(copySelectionAction);
    }

    public void bindActionsToParentShell(final Shell shell) {
        disposeExistingActionCommandSupport();

        actionCommandSupport = ActionKeyBindingSupportFactory.newInstance(shell);
        actionCommandSupport.addAction(copySelectionAction);
    }

    private void disposeExistingActionCommandSupport() {
        if (actionCommandSupport != null) {
            actionCommandSupport.dispose();
            actionCommandSupport = null;
        }
    }

    private void onTableDisposed(final DisposeEvent e) {
        disposeExistingActionCommandSupport();
    }

    private void setupTable(final Table table) {
        table.setHeaderVisible(true);
        table.setLinesVisible(true);
        table.setLayout(new TableLayout());

        /* Pad table height by four pixels to increase readability */
        table.addListener(/* SWT.MeasureItem */41, new MeasureItemHeightListener(4));
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseDown(final MouseEvent e) {
                shiftKeyPressed = ((e.stateMask & SWT.SHIFT) > 0);
            }
        });
    }

    @Override
    protected void fillMenu(final IMenuManager manager) {
        final boolean itemSelected = getTable().getSelectionCount() > 0;
        final boolean singleItemSelected = getTable().getSelectionCount() == 1;

        openAction.setEnabled(singleItemSelected);
        destroyAction.setEnabled(singleItemSelected);
        copyUrlAction.setEnabled(singleItemSelected);
        copyIdAction.setEnabled(itemSelected);
        copySelectionAction.setEnabled(itemSelected);
        linkExistingWorkItemAction.setEnabled(singleItemSelected);
        associateWithPendingChangesAction.setEnabled(itemSelected);

        manager.add(openAction);
        if (openWithActions != null && singleItemSelected) {
            final IMenuManager subMenu = new MenuManager(Messages.getString("QueryResultsControl.OpenWithCommandText")); //$NON-NLS-1$
            manager.add(subMenu);

            for (final IAction action : openWithActions) {
                subMenu.add(action);
            }
        }

        manager.add(new Separator());
        manager.add(associateWithPendingChangesAction);

        manager.add(new Separator());
        manager.add(linkExistingWorkItemAction);

        if (shiftKeyPressed) {
            manager.add(destroyAction);
        }

        manager.add(new Separator());
        manager.add(copySelectionAction);
        manager.add(copyAllAction);

        manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
        manager.add(new Separator());
        manager.add(copyUrlAction);
        manager.add(copyIdAction);
    }

    private void createTableColumns() {
        final Table table = getTable();

        table.setRedraw(false);

        final TableColumn[] existingColumns = table.getColumns();
        for (int i = 0; i < existingColumns.length; i++) {
            existingColumns[i].dispose();
        }

        for (int i = 0; i < displayFields.length; i++) {
            final DisplayField displayField = displayFields[i];

            final TableColumn tableColumn = new TableColumn(table, SWT.NONE);
            tableColumn.setText(displayField.getFieldName());
            tableColumn.setResizable(true);
            tableColumn.setWidth(displayField.getWidth());
            tableColumn.setData(displayField);

            tableColumn.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(final SelectionEvent e) {
                    final TableColumn column = (TableColumn) e.widget;
                    final int columnIx = column.getParent().indexOf(column);
                    final DisplayField displayField = (DisplayField) column.getData();

                    final ColumnClickedListener[] listeners =
                        (ColumnClickedListener[]) columnClickedListeners.getListeners(new ColumnClickedListener[] {});
                    for (int i = 0; i < listeners.length; i++) {
                        listeners[i].onColumnClicked(displayField, columnIx);
                    }
                }
            });

            tableColumn.addControlListener(new ControlAdapter() {
                @Override
                public void controlResized(final ControlEvent e) {
                    final TableColumn column = (TableColumn) e.widget;

                    if (column.getData(IGNORE_RESIZE_KEY) != null) {
                        return;
                    }

                    final DisplayField displayField = (DisplayField) column.getData();

                    final ColumnResizedListener[] listeners =
                        (ColumnResizedListener[]) columnResizedListeners.getListeners(new ColumnResizedListener[] {});
                    for (int i = 0; i < listeners.length; i++) {
                        listeners[i].onColumnResized(displayField, column.getWidth());
                    }

                    ((TableColumn) e.widget).getParent().layout();
                }
            });
        }

        if (results.getCount() > 0) {
            if (sortFields.length > 0) {
                final SortField sortField = sortFields[0];
                int ix = -1;

                for (int i = 0; i < displayFields.length; i++) {
                    if (displayFields[i].getFieldName().equals(sortField.getFieldName())) {
                        ix = i;
                        break;
                    }
                }

                if (ix != -1) {
                    final TableColumn tableColumn = table.getColumn(ix);
                    final int sortDirection = (sortField.isAscending() ? SWT.UP : SWT.DOWN);
                    TableSortIndicator.setSortIndicator(table, tableColumn, sortDirection);
                }
            }
        }

        table.setRedraw(true);
    }

    public void addSimpleSortingBehavior() {
        addColumnClickedListener(new ColumnClickedListener() {
            @Override
            public void onColumnClicked(final DisplayField displayField, final int colIndex) {
                final FieldDefinition fieldDefinition = results.getQuery().getDisplayFieldList().getField(colIndex);

                if (!fieldDefinition.isSortable()) {
                    final String messageFormat = Messages.getString("QueryResultsControl.CantSortDialogTextFormat"); //$NON-NLS-1$
                    final String message = MessageFormat.format(messageFormat, fieldDefinition.getName());
                    MessageBoxHelpers.errorMessageBox(
                        getShell(),
                        Messages.getString("QueryResultsControl.CantSortDialogTitle"), //$NON-NLS-1$
                        message);
                    return;
                }

                final Query query = results.getQuery();
                final SortFieldList sortFieldList = query.getSortFieldList();

                final int ix = sortFieldList.indexOf(fieldDefinition);
                if (ix == 0) {
                    final com.microsoft.tfs.core.clients.workitem.query.SortField sortField = sortFieldList.get(0);
                    final SortType sortType =
                        sortField.getSortType() == SortType.ASCENDING ? SortType.DESCENDING : SortType.ASCENDING;
                    sortField.setSortType(sortType);
                } else {
                    sortFieldList.clear();
                    sortFieldList.insert(0, fieldDefinition, SortType.ASCENDING);
                }

                final QueryResultCommand command = new QueryResultCommand(query, projectName, queryName);
                final IStatus status =
                    UICommandExecutorFactory.newBusyIndicatorCommandExecutor(getShell()).execute(command);

                if (status.getSeverity() == Status.OK) {
                    setWorkItems(command.getQueryResultData());
                }
            }
        });
    }

    @Override
    protected void populateTableItem(final TableItem tableItem) {
        final int index = getTable().indexOf(tableItem);

        final WorkItem workItem = results.getItem(index);
        tableItem.setData(workItem);
        final String[] data = getWorkItemColumnData(workItem, index);
        final int linkColumnIndex = getLinkTypeColumnIndex(workItem);

        for (int i = 0; i < data.length; i++) {
            tableItem.setText(i, data[i]);
            if (i == linkColumnIndex) {
                tableItem.setImage(i, getLinkTypeImage(index));
            }
        }
    }

    private String[] getWorkItemColumnData(final WorkItem workItem, final int tableIndex) {
        final String indentColumnName = getIndentColumnName(workItem);
        final String linkTypeFieldName = getLinkTypeFieldName(workItem);

        final String[] columnData = new String[displayFields.length];
        for (int i = 0; i < displayFields.length; i++) {
            final String fieldName = displayFields[i].getFieldName();
            Object value = null;
            try {
                if (linkTypeFieldName != null
                    && linkTypeFieldName.equals(fieldName)
                    && results instanceof LinkedQueryResultData) {
                    // TODO - We get the link type from the work item info, not
                    // the
                    // work item field.
                    value = ((LinkedQueryResultData) results).getLinkTypeName(tableIndex);
                } else if (indentColumnName.equals(fieldName)) {
                    value = workItem.getFields().getField(fieldName).getValue();
                    if (value != null) {
                        // Make the title indented depending on level.
                        final StringBuffer padding = new StringBuffer();
                        final int level = results.getLevel(tableIndex);
                        for (int j = 0; j < level; j++) {
                            padding.append("    "); //$NON-NLS-1$
                        }
                        value = padding.toString() + value.toString();
                    }
                } else {
                    value = workItem.getFields().getField(fieldName).getValue();
                }

            } catch (final Exception e) {
                // If we encountered any errors populating field data (such as a
                // permissions issue etc)
                // then just log, but return an empty cell value.
                log.warn(MessageFormat.format("Exception detected getting field value:\"{0}\"", fieldName), e); //$NON-NLS-1$
            }
            columnData[i] = getStringValueForFieldValue(value);
        }

        return columnData;
    }

    /**
     * Return the column name to indent on. This is the title field if present,
     * otherwise the first field.
     *
     */
    private String getIndentColumnName(final WorkItem workItem) {
        if (displayFields == null || displayFields.length == 0) {
            return ""; //$NON-NLS-1$
        }
        final String titleFieldName =
            workItem.getClient().getFieldDefinitions().get(CoreFieldReferenceNames.TITLE).getName();
        for (int i = 0; i < displayFields.length; i++) {
            if (titleFieldName.equals(displayFields[i].getFieldName())) {
                return titleFieldName;
            }
        }
        return displayFields[0].getFieldName();
    }

    private String getLinkTypeFieldName(final WorkItem workItem) {
        String linkTypeFieldName = null;

        final FieldDefinitionCollection fieldDefinitions = workItem.getClient().getFieldDefinitions();
        if (fieldDefinitions.contains(CoreFieldReferenceNames.LINK_TYPE)) {
            linkTypeFieldName = fieldDefinitions.get(CoreFieldReferenceNames.LINK_TYPE).getName();
        }

        return linkTypeFieldName;
    }

    private int getLinkTypeColumnIndex(final WorkItem workItem) {
        if (displayFields != null) {
            final String linkTypeColumnName = getLinkTypeFieldName(workItem);

            if (linkTypeColumnName != null) {
                for (int i = 0; i < displayFields.length; i++) {
                    if (linkTypeColumnName.equalsIgnoreCase(displayFields[i].getFieldName())) {
                        return i;
                    }
                }
            }
        }
        return -1;
    }

    private Image getLinkTypeImage(final int tableIndex) {
        if (results instanceof LinkedQueryResultData) {
            if (((LinkedQueryResultData) results).isLinkLocked(tableIndex)) {
                return imageHelper.getImage("/images/wit/LockedLink.gif"); //$NON-NLS-1$
            }
        }
        return null;
    }

    private String getStringValueForFieldValue(final Object fieldValue) {
        if (fieldValue == null) {
            return ""; //$NON-NLS-1$
        }

        if (fieldValue instanceof Date) {
            return resultsDateFormat.format((Date) fieldValue);
        }

        if (fieldValue instanceof Double) {
            return resultNumberFormat.format(fieldValue);
        }

        String s = fieldValue.toString();
        s = s.replace('\n', ' ');
        s = s.replace('\r', ' ');
        return s;
    }

    @Override
    protected Object getTransferData(
        final Transfer transferType,
        final int[] selectedIndices,
        final IProgressMonitor progressMonitor) {
        Class htmlTransferClass = null;

        try {
            htmlTransferClass = Class.forName("org.eclipse.swt.dnd.HTMLTransfer"); //$NON-NLS-1$
        } catch (final Exception e) {
            /* Suppress: not supported on Eclipse < 3.2 */
        }

        if (transferType == TextTransfer.getInstance()) {
            final StringBuffer sb = new StringBuffer();

            // Write column header row
            for (int i = 0; i < displayFields.length; i++) {
                if (i > 0) {
                    sb.append("\t"); //$NON-NLS-1$
                }
                sb.append(escapeClipboardTextCell(displayFields[i].getFieldName()));
            }

            // Write work item rows
            for (int i = 0; i < selectedIndices.length; i++) {
                final WorkItem workItem = results.getItem(selectedIndices[i]);
                sb.append(NewlineUtils.PLATFORM_NEWLINE);

                final String[] data = getWorkItemColumnData(workItem, selectedIndices[i]);
                for (int col = 0; col < data.length; col++) {
                    if (col > 0) {
                        sb.append("\t"); //$NON-NLS-1$
                    }
                    sb.append(escapeClipboardTextCell(data[col]));
                }
                progressMonitor.worked(1);
                if (progressMonitor.isCanceled()) {
                    return null;
                }
            }
            return sb.toString();
        } else if (htmlTransferClass != null && htmlTransferClass.isInstance(transferType)) {
            final StringBuffer sb = new StringBuffer();

            sb.append("<table border=0 cellspacing=0 cellpadding=0 style='border-collapse:collapse;-size:11pt'>"); //$NON-NLS-1$
            sb.append("<tr>"); //$NON-NLS-1$

            // Write column header row
            for (int i = 0; i < displayFields.length; i++) {
                sb.append(
                    "<td valign=top style='border:none;border-right:solid white 1.0pt;background:#3D5277;padding:1.45pt .05in 1.45pt .05in'>"); //$NON-NLS-1$
                sb.append("<p align=center style='text-align:center'>"); //$NON-NLS-1$
                sb.append("<span style='font-family:\"Calibri\",\"sans-serif\";color:white'>"); //$NON-NLS-1$
                sb.append("<b>" + HTTPUtil.escapeHTMLCharacters(displayFields[i].getFieldName()) + "</b>"); //$NON-NLS-1$ //$NON-NLS-2$
                sb.append("</span></p></td>"); //$NON-NLS-1$
            }
            sb.append("</tr>"); //$NON-NLS-1$

            // Write work item rows
            for (int i = 0; i < selectedIndices.length; i++) {
                final String bgColor = (i % 2 == 0) ? "#F0F4FA;" : "#DEE8F2"; //$NON-NLS-1$ //$NON-NLS-2$

                sb.append("<tr>"); //$NON-NLS-1$

                final WorkItem workItem = results.getItem(selectedIndices[i]);
                final String[] data = getWorkItemColumnData(workItem, selectedIndices[i]);
                for (int col = 0; col < data.length; col++) {
                    String cellData = HTTPUtil.escapeHTMLCharacters(data[col]);
                    if (col == 0) {
                        final String url = LinkingFacade.getExternalURL(workItem, workItem.getClient().getConnection());
                        cellData = "<a href=\"" + url + "\">" + cellData + "</a>"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                    }

                    sb.append("<td valign=top style='border:none;border-right:solid white 1.0pt;background:" //$NON-NLS-1$
                        + bgColor
                        + ";padding:1.45pt .05in 1.45pt .05in'>"); //$NON-NLS-1$
                    sb.append("<p><span style='font-family:\"Calibri\",\"sans-serif\"'>"); //$NON-NLS-1$
                    sb.append(cellData);
                    sb.append("</span></p></td>"); //$NON-NLS-1$
                }
                sb.append("</tr>"); //$NON-NLS-1$
                progressMonitor.worked(1);
                if (progressMonitor.isCanceled()) {
                    return null;
                }
            }
            sb.append("</table>"); //$NON-NLS-1$
            return sb.toString();
        }

        final String messageFormat = Messages.getString("QueryResultsControl.UnsupportedTransferTypeFormat"); //$NON-NLS-1$
        final String message = MessageFormat.format(messageFormat, transferType);
        throw new IllegalArgumentException(message);
    }

    private static String escapeClipboardTextCell(String text) {
        if (text != null) {
            text = StringUtil.replace(text, "\t", "    "); //$NON-NLS-1$ //$NON-NLS-2$
            text = text.replace('\n', ' ');
            text = text.replace('\r', ' ');
        }
        return text;
    }

    private static class TableLayout extends Layout {
        @Override
        protected Point computeSize(
            final Composite composite,
            final int wHint,
            final int hHint,
            final boolean flushCache) {
            composite.setLayout(null);
            final Point size = composite.computeSize(wHint, hHint, flushCache);
            composite.setLayout(this);
            return size;
        }

        @Override
        protected void layout(final Composite composite, final boolean flushCache) {
            final Rectangle clientArea = composite.getClientArea();

            final Table table = (Table) composite;
            final TableColumn[] columns = table.getColumns();

            if (columns.length == 0) {
                return;
            }

            int totalColumnWidth = 0;
            for (int i = 0; i < columns.length; i++) {
                totalColumnWidth += columns[i].getWidth();
            }

            table.setLayout(null);

            if (totalColumnWidth < clientArea.width) {
                final TableColumn lastColumn = columns[columns.length - 1];
                int lastColumnWidth = lastColumn.getWidth();
                lastColumnWidth += (clientArea.width - totalColumnWidth);

                lastColumn.setData(IGNORE_RESIZE_KEY, Boolean.TRUE);
                lastColumn.setWidth(lastColumnWidth);
                lastColumn.setData(IGNORE_RESIZE_KEY, null);
            } else if (totalColumnWidth > clientArea.width) {
                final TableColumn lastColumn = columns[columns.length - 1];
                int lastColumnWidth = lastColumn.getWidth();

                final int otherColumnWidths = totalColumnWidth - lastColumnWidth;
                if (clientArea.width - otherColumnWidths > 75) {
                    lastColumnWidth -= (totalColumnWidth - clientArea.width);

                    lastColumn.setData(IGNORE_RESIZE_KEY, Boolean.TRUE);
                    lastColumn.setWidth(lastColumnWidth);
                    lastColumn.setData(IGNORE_RESIZE_KEY, null);
                }
            }

            table.setLayout(this);
        }
    }
}
