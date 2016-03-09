// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.wit.qe;

import java.io.File;
import java.text.MessageFormat;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

import com.microsoft.tfs.client.common.codemarker.CodeMarker;
import com.microsoft.tfs.client.common.codemarker.CodeMarkerDispatch;
import com.microsoft.tfs.client.common.server.ServerManager;
import com.microsoft.tfs.client.common.server.TFSServer;
import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.TFSCommonUIClientPlugin;
import com.microsoft.tfs.client.common.ui.framework.helper.MessageBoxHelpers;
import com.microsoft.tfs.client.common.ui.framework.helper.SWTUtil;
import com.microsoft.tfs.client.common.ui.framework.image.ImageHelper;
import com.microsoft.tfs.client.common.ui.framework.table.TableColumnWidthsPersistence;
import com.microsoft.tfs.client.common.ui.helpers.AutomationIDHelper;
import com.microsoft.tfs.client.common.ui.helpers.TFSTeamProjectCollectionFormatter;
import com.microsoft.tfs.client.common.ui.teamexplorer.helpers.WorkItemHelpers;
import com.microsoft.tfs.client.common.ui.wit.controls.EditorStatusControl;
import com.microsoft.tfs.client.common.ui.wit.qe.QuerySaveControl.SaveMode;
import com.microsoft.tfs.client.common.ui.wit.query.BaseQueryDocumentEditor;
import com.microsoft.tfs.client.common.ui.wit.query.QueryDocumentEditorInput;
import com.microsoft.tfs.client.common.ui.wit.results.QueryResultsEditor;
import com.microsoft.tfs.core.clients.workitem.WorkItemClient;
import com.microsoft.tfs.core.clients.workitem.exceptions.ValidationException;
import com.microsoft.tfs.core.clients.workitem.project.Project;
import com.microsoft.tfs.core.clients.workitem.query.QueryDocument;
import com.microsoft.tfs.core.clients.workitem.query.QueryScope;
import com.microsoft.tfs.core.clients.workitem.query.StoredQuery;
import com.microsoft.tfs.core.clients.workitem.query.qe.QEQuery;
import com.microsoft.tfs.core.clients.workitem.query.qe.QEQueryFactory;
import com.microsoft.tfs.core.clients.workitem.query.qe.QEQueryModifiedListener;
import com.microsoft.tfs.core.clients.workitem.queryhierarchy.QueryFolder;
import com.microsoft.tfs.core.clients.workitem.queryhierarchy.QueryItem;
import com.microsoft.tfs.core.clients.workitem.queryhierarchy.QueryType;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.GUID;

public class QueryEditor extends BaseQueryDocumentEditor {
    public static final CodeMarker CODEMARKER_SAVE_COMPLETE =
        new CodeMarker("com.microsoft.tfs.core.clients.workitem.query.QueryDocument#saveComplete"); //$NON-NLS-1$

    public static final String ID = "com.microsoft.tfs.client.common.ui.wit.qe.queryeditor"; //$NON-NLS-1$
    private static final String NEWLINE = System.getProperty("line.separator"); //$NON-NLS-1$
    private static final String COLUMN_WIDTHS_PERSIST_KEY = "query-editor-columns"; //$NON-NLS-1$
    private static final String ERROR_CANNOT_REPLACE_QUERY_WHILE_OPEN =
        Messages.getString("QueryEditor.CannotReplaceWhileOpen"); //$NON-NLS-1$
    private static final Log log = LogFactory.getLog(QueryEditor.class);
    private final ImageHelper imageHelper = new ImageHelper(TFSCommonUIClientPlugin.PLUGIN_ID);

    public static final String LIST_QUERY_BUTTON_ID = "QueryEditor.listQueryButtonId"; //$NON-NLS-1$
    public static final String LINK_QUERY_BUTTON_ID = "QueryEditor.linkQueryButtonId"; //$NON-NLS-1$
    public static final String TREE_QUERY_BUTTON_ID = "QueryEditor.treeQueryButtonId"; //$NON-NLS-1$

    public static void openEditor(final TFSServer server, final Project project, final StoredQuery storedQuery) {
        Check.notNull(server, "server"); //$NON-NLS-1$
        Check.notNull(storedQuery, "storedQuery"); //$NON-NLS-1$

        final QueryDocument queryDocument =
            server.getQueryDocumentService().getQueryDocumentForStoredQuery(project, storedQuery.getQueryGUID());
        openEditor(server, queryDocument);
    }

    public static void openEditor(final TFSServer server, final QueryDocument queryDocument) {
        final IEditorInput editorInput = new QueryDocumentEditorInput(server, queryDocument, ID);

        final IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
        try {
            page.openEditor(editorInput, ID);
        } catch (final PartInitException e) {
            throw new RuntimeException(e);
        }
    }

    private EditorStatusControl statusControl;
    private QueryEditorControl queryControl;
    private Button buttonQueryTypeFlat;
    private Button buttonQueryTypeLink;
    private Button buttonQueryTypeTree;

    private TableColumnWidthsPersistence tableColumnWidthsPersistence;
    private QEQuery qeQuery;

    private RunQueryAction runQueryAction;
    private ColumnOptionsAction columnOptionsAction;
    private IAction debugInfoAction;

    private boolean shiftKeyPressed = false;

    public IAction getAction(final String actionId) {
        return queryControl.getAction(actionId);
    }

    @Override
    protected String getID() {
        return ID;
    }

    @Override
    protected String getTitleSuffix() {
        return Messages.getString("QueryEditor.TitleSuffix"); //$NON-NLS-1$
    }

    @Override
    public void dispose() {
        tableColumnWidthsPersistence.persist();

        super.dispose();
    }

    @Override
    public void doCreatePartControl(final Composite parent, final QueryDocument queryDocument) {
        qeQuery = QEQueryFactory.createQueryFromWIQL(
            getQueryDocument().getQueryText(),
            getQueryDocument().getWorkItemClient(),
            getQueryDocument().getQueryMode());

        qeQuery.addModifiedListener(new QEQueryModifiedListener() {
            @Override
            public void onQueryModified(final QEQuery query) {
                if (qeQuery.isValid()) {
                    getQueryDocument().setFilterExpression(qeQuery.getFilterExpression());
                    getQueryDocument().setQueryType(qeQuery.getQueryType());
                    getQueryDocument().setQueryMode(qeQuery.getLinkQueryMode());
                    statusControl.setStatus(Messages.getString("QueryEditor.RunTheQueryToSeeResults")); //$NON-NLS-1$
                } else {
                    statusControl.setStatus(qeQuery.getInvalidMessage(), false);
                }
            }
        });

        final Composite composite = new Composite(parent, SWT.NONE);
        final GridLayout gridLayout = SWTUtil.gridLayout(composite, 1, false, 0, 0);
        gridLayout.verticalSpacing = 2;

        statusControl = new EditorStatusControl(composite, SWT.NONE);
        statusControl.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        statusControl.setStatus(Messages.getString("QueryEditor.RunTheQueryToSeeResults")); //$NON-NLS-1$

        if (queryDocument.getWorkItemClient().supportsLinkQueries()) {
            final Composite queryTypeComposite = new Composite(composite, SWT.NONE);
            SWTUtil.gridLayout(queryTypeComposite, 3, false, 10, 10);
            queryTypeComposite.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
            queryTypeComposite.setBackground(composite.getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND));

            buttonQueryTypeFlat = new Button(queryTypeComposite, SWT.RADIO);
            buttonQueryTypeLink = new Button(queryTypeComposite, SWT.RADIO);
            buttonQueryTypeTree = new Button(queryTypeComposite, SWT.RADIO);

            buttonQueryTypeFlat.setText(Messages.getString("QueryEditor.FlatQueryButtonText")); //$NON-NLS-1$
            buttonQueryTypeLink.setText(Messages.getString("QueryEditor.LinkQueryButtonText")); //$NON-NLS-1$
            buttonQueryTypeTree.setText(Messages.getString("QueryEditor.TreeQueryButtonText")); //$NON-NLS-1$

            buttonQueryTypeFlat.setImage(imageHelper.getImage("images/wit/query_type_flat.gif")); //$NON-NLS-1$
            buttonQueryTypeLink.setImage(imageHelper.getImage("images/wit/query_type_onehop.gif")); //$NON-NLS-1$
            buttonQueryTypeTree.setImage(imageHelper.getImage("images/wit/query_type_tree.gif")); //$NON-NLS-1$

            buttonQueryTypeFlat.setBackground(composite.getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
            buttonQueryTypeLink.setBackground(composite.getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
            buttonQueryTypeTree.setBackground(composite.getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND));

            buttonQueryTypeFlat.setSelection(queryDocument.getQueryType() == QueryType.LIST);
            buttonQueryTypeLink.setSelection(queryDocument.getQueryType() == QueryType.ONE_HOP);
            buttonQueryTypeTree.setSelection(queryDocument.getQueryType() == QueryType.TREE);

            AutomationIDHelper.setWidgetID(buttonQueryTypeFlat, LIST_QUERY_BUTTON_ID);
            AutomationIDHelper.setWidgetID(buttonQueryTypeLink, LINK_QUERY_BUTTON_ID);
            AutomationIDHelper.setWidgetID(buttonQueryTypeTree, TREE_QUERY_BUTTON_ID);

            final SelectionAdapter clickHandler = new SelectionAdapter() {
                @Override
                public void widgetSelected(final SelectionEvent e) {
                    final Button button = (Button) e.widget;

                    if (button.getSelection()) {
                        if (button.equals(buttonQueryTypeFlat)) {
                            setQueryType(QueryType.LIST);
                        } else if (button.equals(buttonQueryTypeLink)) {
                            setQueryType(QueryType.ONE_HOP);
                        } else if (button.equals(buttonQueryTypeTree)) {
                            setQueryType(QueryType.TREE);
                        }
                    }
                }
            };

            buttonQueryTypeFlat.addSelectionListener(clickHandler);
            buttonQueryTypeLink.addSelectionListener(clickHandler);
            buttonQueryTypeTree.addSelectionListener(clickHandler);
        }

        final Project project = queryDocument.getWorkItemClient().getProjects().get(queryDocument.getProjectName());
        queryControl = new QueryEditorControl(composite, SWT.NONE, qeQuery, project);
        queryControl.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        tableColumnWidthsPersistence =
            new TableColumnWidthsPersistence(queryControl.getTableViewer().getTable(), COLUMN_WIDTHS_PERSIST_KEY);

        tableColumnWidthsPersistence.addMapping(
            QueryEditorControl.LOGICAL_OPERATOR_COLUMN,
            QueryEditorControl.columnIndexOf(QueryEditorControl.LOGICAL_OPERATOR_COLUMN));

        tableColumnWidthsPersistence.addMapping(
            QueryEditorControl.FIELD_NAME_COLUMN,
            QueryEditorControl.columnIndexOf(QueryEditorControl.FIELD_NAME_COLUMN));

        tableColumnWidthsPersistence.addMapping(
            QueryEditorControl.OPERATOR_COLUMN,
            QueryEditorControl.columnIndexOf(QueryEditorControl.OPERATOR_COLUMN));

        tableColumnWidthsPersistence.restore();

        queryControl.getTableViewer().getTable().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseDown(final MouseEvent e) {
                shiftKeyPressed = ((e.stateMask & SWT.SHIFT) > 0);
            }
        });

        queryControl.getMenuManager().addMenuListener(new IMenuListener() {
            @Override
            public void menuAboutToShow(final IMenuManager manager) {
                fillContextMenu(manager);
            }
        });

        getSite().registerContextMenu(queryControl.getMenuManager(), queryControl.getTableViewer());
        getSite().setSelectionProvider(queryControl.getTableViewer());

        createActions();
    }

    @Override
    public void setEnabled(final boolean enabled) {
        statusControl.setEnabled(enabled);
        queryControl.setEnabled(enabled);
        fireEnabledChanged(enabled);
    }

    @Override
    public void setDisconnected(final boolean disconnected) {
        statusControl.setDisconnected(disconnected);
        setEnabled(!disconnected);
    }

    private void fillContextMenu(final IMenuManager manager) {
        runQueryAction.setEnabled(true);
        columnOptionsAction.setEnabled(true);

        manager.appendToGroup(IWorkbenchActionConstants.MB_ADDITIONS, runQueryAction);
        manager.appendToGroup(IWorkbenchActionConstants.MB_ADDITIONS, new Separator());
        manager.appendToGroup(IWorkbenchActionConstants.MB_ADDITIONS, columnOptionsAction);

        if (shiftKeyPressed) {
            manager.add(new Separator());
            manager.add(debugInfoAction);
        }
    }

    private void createActions() {
        runQueryAction = new RunQueryAction(this);
        runQueryAction.setText(Messages.getString("QueryEditor.RunQueryActionText")); //$NON-NLS-1$

        columnOptionsAction = new ColumnOptionsAction(this);
        columnOptionsAction.setText(Messages.getString("QueryEditor.ColumnOptionsActionText")); //$NON-NLS-1$
    }

    public void setQueryType(final QueryType queryType) {
        queryControl.setQueryType(queryType);
    }

    @Override
    public void setFocus() {
        queryControl.setFocus();
    }

    public void runQuery() {
        if (!qeQuery.isValid()) {
            MessageBoxHelpers.errorMessageBox(
                getSite().getShell(),
                Messages.getString("QueryEditor.CannotRunDialogTitle"), //$NON-NLS-1$
                qeQuery.getInvalidMessage());
            return;
        }

        QueryResultsEditor.openEditor(getServer(), getQueryDocument());
    }

    private boolean checkQueryComplete() {
        if (!qeQuery.isValid()) {
            final StringBuffer sb = new StringBuffer();
            sb.append(Messages.getString("QueryEditor.QueryNotComplete")); //$NON-NLS-1$
            sb.append("\n\n"); //$NON-NLS-1$
            sb.append(qeQuery.getInvalidMessage());

            MessageBoxHelpers.errorMessageBox(
                getSite().getShell(),
                Messages.getString("QueryEditor.QueryNotCompleteDialogTitle"), //$NON-NLS-1$
                sb.toString());
            return false;
        }
        return true;
    }

    @Override
    public void doSave(final IProgressMonitor monitor) {
        if (!checkQueryComplete()) {
            monitor.setCanceled(true);
            return;
        }

        final TFSServer server = getServer();
        final ServerManager serverManager = TFSCommonUIClientPlugin.getDefault().getProductPlugin().getServerManager();
        final TFSServer defaultServer = serverManager.getDefaultServer();

        if (defaultServer != server) {
            final String message = Messages.getString("QueryEditor.NotConnectedToTfsError"); //$NON-NLS-1$
            MessageBoxHelpers.errorMessageBox(
                getSite().getShell(),
                Messages.getString("QueryEditor.UnableToSaveDialogTitle"), //$NON-NLS-1$
                message);
            monitor.setCanceled(true);
            return;
        }

        final QueryDocument queryDocument = getQueryDocument();
        queryDocument.setTeamName(WorkItemHelpers.getCurrentTeamName());

        if (queryDocument.getGUID() == null && queryDocument.getFile() == null) {
            final boolean success = doSaveAsInternal();
            if (!success) {
                monitor.setCanceled(true);
            }
        } else {
            try {
                queryDocument.save();
                CodeMarkerDispatch.dispatch(CODEMARKER_SAVE_COMPLETE);
            } catch (final Exception e) {
                monitor.setCanceled(true);

                log.warn("error saving query document", e); //$NON-NLS-1$

                final String messageFormat = Messages.getString("QueryEditor.UnableToSaveFormat"); //$NON-NLS-1$
                final String message =
                    MessageFormat.format(messageFormat, getEditorInput().getName(), getTitleSuffix());

                MessageBoxHelpers.errorMessageBox(
                    getSite().getShell(),
                    null,
                    message + NEWLINE + NEWLINE + e.getMessage());
            }
        }
    }

    @Override
    public void doSaveAs() {
        doSaveAsInternal();
    }

    private boolean doSaveAsInternal() {
        if (!checkQueryComplete()) {
            return false;
        }

        final TFSServer server = getServer();
        final QueryDocument queryDocument = getQueryDocument();

        final WorkItemClient workItemClient = queryDocument.getWorkItemClient();
        SaveMode saveMode = null;
        if (queryDocument.getFile() != null) {
            saveMode = SaveMode.FILE;
        } else {
            saveMode = SaveMode.SERVER;
        }

        final String displayURL = TFSTeamProjectCollectionFormatter.getLabel(workItemClient.getConnection());

        /*
         * TODO: what if the query document's project is null?
         */
        final Project project = workItemClient.getProjects().get(queryDocument.getProjectName());
        QueryFolder parentFolder = (QueryFolder) project.getQueryHierarchy().find(queryDocument.getParentGUID());
        if (parentFolder == null) {
            parentFolder = project.getQueryHierarchy();
        }

        final QuerySaveDialog dialog = new QuerySaveDialog(
            getSite().getShell(),
            workItemClient.getProjects(),
            parentFolder,
            displayURL,
            project,
            saveMode,
            queryDocument.getName(),

            /*
             * TODO: need to save/restore this directory
             */
            new File(System.getProperty("user.home"))); //$NON-NLS-1$

        if (dialog.open() != IDialogConstants.OK_ID) {
            return false;
        }

        if (dialog.getSaveMode() == SaveMode.FILE) {
            if (dialog.getOverwriteExisting()) {
                final File saveLocation = dialog.getSaveLocation();
                if (queryDocument.getFile() == null || !queryDocument.getFile().equals(saveLocation)) {
                    if (server.getQueryDocumentService().hasQueryDocumentForFile(saveLocation)) {
                        MessageBoxHelpers.errorMessageBox(
                            getSite().getShell(),
                            Messages.getString("QueryEditor.CannotSaveQueryDialogTitle"), //$NON-NLS-1$
                            ERROR_CANNOT_REPLACE_QUERY_WHILE_OPEN);
                        return false;
                    }
                }
            }
        } else {
            if (dialog.getOverwriteExisting()) {
                final QueryItem existingQuery = queryDocument.getExistingQueryByName(dialog.getQueryName());
                if (existingQuery != null && existingQuery.getID() != queryDocument.getGUID()) {
                    if (server.getQueryDocumentService().hasQueryDocumentForStoredQuery(existingQuery.getID())) {
                        MessageBoxHelpers.errorMessageBox(
                            getSite().getShell(),
                            Messages.getString("QueryEditor.CannotSaveQueryDialogTitle"), //$NON-NLS-1$
                            ERROR_CANNOT_REPLACE_QUERY_WHILE_OPEN);
                        return false;
                    }
                }
            }
        }

        final boolean originalDirty = queryDocument.isDirty();
        final GUID originalGuid = queryDocument.getGUID();
        final GUID originalParentGuid = queryDocument.getParentGUID();
        final File originalFile = queryDocument.getFile();
        final String originalName = queryDocument.getName();
        final String originalProject = queryDocument.getProjectName();
        final QueryScope originalScope = queryDocument.getQueryScope();
        boolean saveSuccessful = false;

        try {
            queryDocument.setName(dialog.getQueryName());
            queryDocument.setParentGUID(dialog.getParentGUID());
            queryDocument.setProjectName(dialog.getProject().getName());

            if (dialog.getSaveMode() == SaveMode.FILE) {
                queryDocument.setFile(dialog.getSaveLocation());
            } else {
                if (queryDocument.getFile() != null) {
                    queryDocument.setFile(null);
                }

                if (queryDocument.getGUID() != null) {
                    if (originalScope != queryDocument.getQueryScope()
                        || !originalProject.equals(queryDocument.getProjectName())
                        || !originalParentGuid.equals(queryDocument.getParentGUID())
                        || !originalName.equals(queryDocument.getName())) {
                        queryDocument.setGUID(null);
                    }
                }
            }

            queryDocument.save();
            saveSuccessful = true;
            return true;
        } catch (final Exception ex) {
            logIfNeeded(ex);
            MessageBoxHelpers.errorMessageBox(
                getSite().getShell(),
                Messages.getString("QueryEditor.CannotSaveQueryDialogTitle"), //$NON-NLS-1$
                ex.getMessage());
            return false;
        } finally {
            if (saveSuccessful) {
                if (queryDocument.getGUID() == null && dialog.getSaveMode() == SaveMode.SERVER) {
                    try {
                        final GUID targetFolderGuid = queryDocument.getParentGUID();
                        final QueryFolder targetFolder =
                            (QueryFolder) dialog.getProject().getQueryHierarchy().find(targetFolderGuid);
                        final QueryItem queryItem = targetFolder.getItemByName(queryDocument.getName());
                        queryDocument.setGUID(queryItem.getID());
                    } catch (final IllegalArgumentException e) {
                        final String messageFormat = "Did not find query item {0} in the hierarchy."; //$NON-NLS-1$
                        final String message = MessageFormat.format(messageFormat, queryDocument.getName());
                        log.warn(message);
                    }
                }
            } else {
                queryDocument.restoreGUIDAndFile(originalGuid, originalFile);
                queryDocument.setParentGUID(originalParentGuid);
                queryDocument.setName(originalName);
                queryDocument.setProjectName(originalProject);
                queryDocument.setQueryScope(originalScope);
                if (!originalDirty) {
                    queryDocument.clearDirty();
                }
            }
        }
    }

    private void logIfNeeded(final Exception ex) {
        if (ex instanceof ValidationException) {
            final ValidationException validationException = (ValidationException) ex;
            if (validationException.getType() == ValidationException.Type.NOT_UNIQUE_STORED_QUERY) {
                return;
            }
        }

        log.warn("error encountered during save", ex); //$NON-NLS-1$
    }

    @Override
    public boolean isSaveAsAllowed() {
        return true;
    }
}
