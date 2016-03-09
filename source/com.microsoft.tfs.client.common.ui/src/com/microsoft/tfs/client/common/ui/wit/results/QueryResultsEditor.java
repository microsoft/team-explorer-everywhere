// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.wit.results;

import java.text.MessageFormat;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

import com.microsoft.tfs.client.common.codemarker.CodeMarker;
import com.microsoft.tfs.client.common.codemarker.CodeMarkerDispatch;
import com.microsoft.tfs.client.common.framework.command.ICommand;
import com.microsoft.tfs.client.common.framework.command.ICommandExecutor;
import com.microsoft.tfs.client.common.framework.command.ICommandFinishedCallback;
import com.microsoft.tfs.client.common.framework.command.MultiCommandFinishedCallback;
import com.microsoft.tfs.client.common.server.TFSServer;
import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.controls.generic.compatibility.table.DoubleClickEvent;
import com.microsoft.tfs.client.common.ui.controls.generic.compatibility.table.DoubleClickListener;
import com.microsoft.tfs.client.common.ui.dialogs.generic.TextDisplayDialog;
import com.microsoft.tfs.client.common.ui.framework.command.UICommandExecutorFactory;
import com.microsoft.tfs.client.common.ui.framework.helper.MessageBoxHelpers;
import com.microsoft.tfs.client.common.ui.helpers.WorkItemEditorHelper;
import com.microsoft.tfs.client.common.ui.teamexplorer.helpers.WorkItemHelpers;
import com.microsoft.tfs.client.common.ui.wit.controls.EditorStatusControl;
import com.microsoft.tfs.client.common.ui.wit.qe.QueryEditor;
import com.microsoft.tfs.client.common.ui.wit.query.BaseQueryDocumentEditor;
import com.microsoft.tfs.client.common.ui.wit.query.QueryDocumentEditorInput;
import com.microsoft.tfs.client.common.ui.wit.query.UIQueryUtils;
import com.microsoft.tfs.client.common.ui.wit.results.data.LinkedQueryResultData;
import com.microsoft.tfs.client.common.ui.wit.results.data.QueryResultCommand;
import com.microsoft.tfs.client.common.ui.wit.results.data.QueryResultData;
import com.microsoft.tfs.core.clients.workitem.WorkItem;
import com.microsoft.tfs.core.clients.workitem.WorkItemQueryUtils;
import com.microsoft.tfs.core.clients.workitem.fields.FieldDefinition;
import com.microsoft.tfs.core.clients.workitem.project.Project;
import com.microsoft.tfs.core.clients.workitem.query.InvalidQueryTextException;
import com.microsoft.tfs.core.clients.workitem.query.QueryDocument;
import com.microsoft.tfs.core.clients.workitem.query.StoredQuery;
import com.microsoft.tfs.core.clients.workitem.query.qe.DisplayField;
import com.microsoft.tfs.core.clients.workitem.query.qe.ResultOptions;
import com.microsoft.tfs.core.clients.workitem.query.qe.SortField;
import com.microsoft.tfs.core.clients.workitem.query.qe.SortFieldCollection;

public class QueryResultsEditor extends BaseQueryDocumentEditor {
    public static final String ID = "com.microsoft.tfs.client.common.ui.wit.results.queryresultseditor"; //$NON-NLS-1$

    public static CodeMarker CODEMARKER_RUN_COMPLETE =
        new CodeMarker("com.microsoft.tfs.client.common.ui.wit.results.QueryResultsEditor#runComplete"); //$NON-NLS-1$

    private static final Log log = LogFactory.getLog(QueryResultsEditor.class);
    private static final String NEWLINE = System.getProperty("line.separator"); //$NON-NLS-1$

    public static void openEditor(final TFSServer server, final Project project, final StoredQuery storedQuery) {
        final QueryDocument queryDocument =
            server.getQueryDocumentService().getQueryDocumentForStoredQuery(project, storedQuery.getQueryGUID());
        openEditor(server, queryDocument);
    }

    public static void openEditor(final TFSServer server, final QueryDocument queryDocument) {
        final IEditorInput editorInput = new QueryDocumentEditorInput(server, queryDocument, ID);

        final IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
        try {
            final QueryResultsEditor editor = (QueryResultsEditor) page.openEditor(editorInput, ID);
            editor.run();
        } catch (final PartInitException e) {
            throw new RuntimeException(e);
        }
    }

    private EditorStatusControl statusControl;
    private QueryResultsControl resultsControl;
    private RunQueryAction runQueryAction;
    private ViewQueryAction viewQueryAction;
    private ColumnOptionsAction columnOptionsAction;
    private IAction debugInfoAction;
    private String wiqlBeingDisplayed;
    private boolean shiftKeyPressed = false;
    private QueryResultData queryResultData;

    @Override
    protected String getID() {
        return ID;
    }

    @Override
    protected String getTitleSuffix() {
        return Messages.getString("QueryResultsEditor.TitleSuffix"); //$NON-NLS-1$
    }

    @Override
    public void doSave(final IProgressMonitor monitor) {
        try {
            final QueryDocument queryDocument = getQueryDocument();
            queryDocument.setTeamName(WorkItemHelpers.getCurrentTeamName());

            queryDocument.save();
        } catch (final Exception e) {
            log.warn("update to save querydocument", e); //$NON-NLS-1$

            final String messageFormat = Messages.getString("QueryResultsEditor.ErrorDialogTextFormat"); //$NON-NLS-1$
            final String message = MessageFormat.format(messageFormat, getEditorInput().getName(), getTitleSuffix());
            MessageBoxHelpers.errorMessageBox(getSite().getShell(), null, message + NEWLINE + NEWLINE + e.getMessage());
        }

    }

    @Override
    public void doSaveAs() {

    }

    /**
     * Override to ignore save if the query document hosted by this result
     * editor is also open in the query editor.
     */
    @Override
    public boolean isSaveOnCloseNeeded() {
        if (isDirty()) {
            final IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
            if (page != null) {
                for (final IEditorPart editor : page.getDirtyEditors()) {
                    if (editor instanceof QueryEditor) {
                        final QueryEditor queryEditor = (QueryEditor) editor;
                        if (getQueryDocument().equals(queryEditor.getQueryDocument())) {
                            return false;
                        }
                    }
                }
            }
        }

        return super.isSaveOnCloseNeeded();
    }

    @Override
    public boolean isSaveAsAllowed() {
        return false;
    }

    @Override
    public void doCreatePartControl(final Composite parent, final QueryDocument queryDocument) {
        final Composite composite = new Composite(parent, SWT.NONE);
        composite.setLayout(new GridLayout(1, false));

        statusControl = new EditorStatusControl(composite, SWT.NONE);
        statusControl.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        resultsControl = new QueryResultsControl(composite, SWT.MULTI);
        resultsControl.bindActionsToParentWorkbenchPart(this);
        resultsControl.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        resultsControl.setServer(getServer());

        resultsControl.getMenuManager().addMenuListener(new IMenuListener() {
            @Override
            public void menuAboutToShow(final IMenuManager manager) {
                fillContextMenu(manager);
            }
        });

        resultsControl.addSelectionChangedListener(new ISelectionChangedListener() {
            @Override
            public void selectionChanged(final SelectionChangedEvent event) {
                updateStatusControlCounts();
            }
        });

        resultsControl.addDoubleClickListener(new DoubleClickListener() {
            @Override
            public void doubleClick(final DoubleClickEvent event) {
                final WorkItem selectedWorkItem =
                    (WorkItem) ((IStructuredSelection) event.getSelection()).getFirstElement();
                if (UIQueryUtils.verifyAccessToWorkItem(selectedWorkItem)) {
                    WorkItemEditorHelper.openEditor(getServer(), selectedWorkItem);
                }
            }
        });

        resultsControl.getTable().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseDown(final MouseEvent e) {
                shiftKeyPressed = ((e.stateMask & SWT.SHIFT) > 0);
            }
        });

        resultsControl.addColumnClickedListener(new QueryResultsControl.ColumnClickedListener() {
            @Override
            public void onColumnClicked(final DisplayField displayField, final int index) {
                handleColumnClicked(displayField);
            }
        });

        resultsControl.addColumnResizedListener(new QueryResultsControl.ColumnResizedListener() {
            @Override
            public void onColumnResized(final DisplayField displayField, final int width) {
                handleColumnResized(displayField, width);
            }
        });

        getSite().registerContextMenu(resultsControl.getMenuManager(), resultsControl);
        getSite().setSelectionProvider(resultsControl);

        createActions();
        // run(); - run is now called by the openEditor method.
    }

    @Override
    public void setEnabled(final boolean enabled) {
        statusControl.setEnabled(enabled);
        resultsControl.setEnabled(enabled);
        fireEnabledChanged(enabled);
    }

    @Override
    public void setDisconnected(final boolean disconnected) {
        statusControl.setDisconnected(disconnected);
        setEnabled(!disconnected);
    }

    public void viewQuery() {
        QueryEditor.openEditor(getServer(), getQueryDocument());
    }

    private void handleColumnResized(final DisplayField displayField, final int width) {
        final QueryDocument queryDocument = getQueryDocument();

        if (!queryDocument.getResultOptions().getDisplayFields().contains(displayField.getFieldName())) {
            return;
        }

        final ResultOptions newResultOptions = new ResultOptions(queryDocument.getResultOptions(), true, queryDocument);
        newResultOptions.getDisplayFields().get(displayField.getFieldName()).setWidth(width);

        queryDocument.setResultOptions(newResultOptions);
    }

    private void handleColumnClicked(final DisplayField displayField) {
        final QueryDocument queryDocument = getQueryDocument();

        final FieldDefinition fd =
            queryDocument.getWorkItemClient().getFieldDefinitions().get(displayField.getFieldName());
        if (!fd.isSortable()) {
            final String messageFormat = Messages.getString("QueryResultsEditor.CannotSortFormat"); //$NON-NLS-1$
            final String message = MessageFormat.format(messageFormat, displayField.getFieldName());
            MessageBoxHelpers.errorMessageBox(
                getSite().getShell(),
                Messages.getString("QueryResultsEditor.CantSortDialogTitle"), //$NON-NLS-1$
                message);
            return;
        }

        SortField field = null;

        if (queryDocument.getResultOptions().getSortFields().getCount() > 0) {
            final SortField currentFirstField = queryDocument.getResultOptions().getSortFields().get(0);
            if (currentFirstField.getFieldName().equals(displayField.getFieldName())) {
                field = new SortField(currentFirstField.getFieldName(), !currentFirstField.isAscending());
            }
        }
        if (field == null) {
            field = new SortField(displayField.getFieldName(), true);
        }

        final SortFieldCollection sortFieldCollection = new SortFieldCollection();
        sortFieldCollection.add(field);

        final ResultOptions newResultOptions =
            new ResultOptions(queryDocument.getResultOptions().getDisplayFields(), sortFieldCollection, queryDocument);

        queryDocument.setResultOptions(newResultOptions);
        run();
    }

    private void fillContextMenu(final IMenuManager manager) {
        manager.prependToGroup(IWorkbenchActionConstants.MB_ADDITIONS, viewQueryAction);
        manager.prependToGroup(IWorkbenchActionConstants.MB_ADDITIONS, new Separator());

        manager.prependToGroup(IWorkbenchActionConstants.MB_ADDITIONS, columnOptionsAction);
        manager.prependToGroup(IWorkbenchActionConstants.MB_ADDITIONS, new Separator());

        manager.prependToGroup(IWorkbenchActionConstants.MB_ADDITIONS, runQueryAction);

        if (shiftKeyPressed) {
            manager.add(new Separator());
            manager.add(debugInfoAction);
        }
    }

    private void createActions() {
        runQueryAction = new RunQueryAction(this);
        runQueryAction.setText(Messages.getString("QueryResultsEditor.RunQueryActionText")); //$NON-NLS-1$

        viewQueryAction = new ViewQueryAction(this);
        viewQueryAction.setText(Messages.getString("QueryResultsEditor.EditQueryActionText")); //$NON-NLS-1$

        columnOptionsAction = new ColumnOptionsAction(this);
        columnOptionsAction.setText(Messages.getString("QueryResultsEditor.ColumnOptionsActionText")); //$NON-NLS-1$

        debugInfoAction = new Action() {
            @Override
            public void run() {
                final StringBuffer info = new StringBuffer();

                info.append("editor input: " + getEditorInput().toString()).append(NEWLINE); //$NON-NLS-1$
                info.append(NEWLINE);

                UIQueryUtils.generateDebugInfo(getQueryDocument(), info);
                info.append(NEWLINE);

                info.append("WIQL being displayed:").append(NEWLINE); //$NON-NLS-1$
                info.append(wiqlBeingDisplayed).append(NEWLINE);

                final TextDisplayDialog dlg = new TextDisplayDialog(getSite().getShell(), "Query Editor Debug Info: " //$NON-NLS-1$
                    + getPartName(), info.toString(), getClass().getName());

                dlg.open();
            }
        };
        debugInfoAction.setText(Messages.getString("QueryResultsEditor.DebugInfoActionText")); //$NON-NLS-1$
    }

    public void run() {
        resultsControl.clear();
        queryResultData = null;

        statusControl.setStatus(Messages.getString("QueryResultsEditor.RunningStatus")); //$NON-NLS-1$

        final QueryDocument queryDocument = getQueryDocument();

        try {
            final Map<String, Object> queryContext =
                WorkItemQueryUtils.makeContext(queryDocument.getProjectName(), WorkItemHelpers.getCurrentTeamName());
            wiqlBeingDisplayed = queryDocument.getQueryText();

            final QueryResultCommand command = new QueryResultCommand(
                queryDocument.getWorkItemClient(),
                wiqlBeingDisplayed,
                queryContext,
                queryDocument.getProjectName(),
                queryDocument.getName());

            final ICommandExecutor executor = UICommandExecutorFactory.newUIJobCommandExecutor(getSite().getShell());
            final ICommandFinishedCallback defaultCallback = executor.getCommandFinishedCallback();

            final ICommandFinishedCallback mutliCallback =
                MultiCommandFinishedCallback.combine(defaultCallback, new ICommandFinishedCallback() {
                    @Override
                    public void onCommandFinished(final ICommand command, final IStatus status) {
                        if (statusControl.isDisposed() || resultsControl.isDisposed()) {
                            return;
                        }

                        if (status.getSeverity() != Status.OK) {
                            statusControl.setStatus(Messages.getString("QueryResultsEditor.InvalidQueryStatus"), false); //$NON-NLS-1$
                            return;
                        }

                        queryResultData = ((QueryResultCommand) command).getQueryResultData();
                        updateStatusControlCounts();

                        Display.getDefault().asyncExec(new Runnable() {
                            @Override
                            public void run() {
                                // Result data could have been invalidated by
                                // another run
                                if (queryResultData == null || resultsControl.isDisposed()) {
                                    return;
                                }

                                resultsControl.setWorkItems(
                                    queryResultData,
                                    queryDocument.getResultOptions().getDisplayFields().toArray(),
                                    queryDocument.getResultOptions().getSortFields().toArray(),
                                    queryDocument.getProjectName(),
                                    queryDocument.getName());
                            }
                        });
                    }
                });

            executor.setCommandFinishedCallback(mutliCallback);

            executor.execute(command);

            CodeMarkerDispatch.dispatch(CODEMARKER_RUN_COMPLETE);
        } catch (final InvalidQueryTextException ex) {
            statusControl.setStatus(Messages.getString("QueryResultsEditor.InvalidQueryStatus"), false); //$NON-NLS-1$
            MessageBoxHelpers.errorMessageBox(
                getSite().getShell(),
                Messages.getString("QueryResultsEditor.ErrorDialogTitle"), //$NON-NLS-1$
                ex.getMessage());
            return;
        }
    }

    @Override
    public void setFocus() {
        resultsControl.setFocus();
    }

    private void updateStatusControlCounts() {
        int itemCount = 0;
        int selectedCount = 0;

        if (queryResultData != null) {
            itemCount = queryResultData.getCount();
            selectedCount = resultsControl.getSelectedIndices().length;
        }

        String message;
        if (queryResultData instanceof LinkedQueryResultData) {
            int linkedItemCount = 0;
            for (int i = 0; i < itemCount; i++) {
                if (queryResultData.getLevel(i) > 0) {
                    linkedItemCount++;
                }
            }
            final int topLevelCount = itemCount - linkedItemCount;

            final String messageFormat = Messages.getString("QueryResultsEditor.LinkedResultsFoundFormat"); //$NON-NLS-1$
            message = MessageFormat.format(messageFormat, itemCount, topLevelCount, linkedItemCount, selectedCount);
        } else {
            final String messageFormat = Messages.getString("QueryResultsEditor.ResultsFoundFormat"); //$NON-NLS-1$
            message = MessageFormat.format(messageFormat, itemCount, selectedCount);
        }

        statusControl.setStatus(message);
    }
}
