// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.wit.dialogs;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.microsoft.tfs.client.common.server.TFSServer;
import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.controls.generic.compatibility.table.DoubleClickEvent;
import com.microsoft.tfs.client.common.ui.controls.generic.compatibility.table.DoubleClickListener;
import com.microsoft.tfs.client.common.ui.framework.command.UICommandExecutorFactory;
import com.microsoft.tfs.client.common.ui.framework.dialog.BaseDialog;
import com.microsoft.tfs.client.common.ui.framework.helper.MessageBoxHelpers;
import com.microsoft.tfs.client.common.ui.framework.helper.SWTUtil;
import com.microsoft.tfs.client.common.ui.framework.sizing.ControlSize;
import com.microsoft.tfs.client.common.ui.helpers.ComboHelper;
import com.microsoft.tfs.client.common.ui.teamexplorer.helpers.WorkItemHelpers;
import com.microsoft.tfs.client.common.ui.wit.results.QueryResultsControl;
import com.microsoft.tfs.client.common.ui.wit.results.data.QueryResultCommand;
import com.microsoft.tfs.client.common.ui.wit.results.data.QueryResultData;
import com.microsoft.tfs.core.clients.workitem.CoreFieldReferenceNames;
import com.microsoft.tfs.core.clients.workitem.WorkItem;
import com.microsoft.tfs.core.clients.workitem.WorkItemClient;
import com.microsoft.tfs.core.clients.workitem.WorkItemQueryUtils;
import com.microsoft.tfs.core.clients.workitem.form.WIFormLinksControlWITypeFilter;
import com.microsoft.tfs.core.clients.workitem.form.WIFormLinksControlWITypeFilterEnum;
import com.microsoft.tfs.core.clients.workitem.form.WIFormLinksControlWITypeFilters;
import com.microsoft.tfs.core.clients.workitem.internal.wiqlparse.Condition;
import com.microsoft.tfs.core.clients.workitem.internal.wiqlparse.Node;
import com.microsoft.tfs.core.clients.workitem.internal.wiqlparse.NodeAndOperator;
import com.microsoft.tfs.core.clients.workitem.internal.wiqlparse.NodeCondition;
import com.microsoft.tfs.core.clients.workitem.internal.wiqlparse.NodeFieldName;
import com.microsoft.tfs.core.clients.workitem.internal.wiqlparse.NodeOrOperator;
import com.microsoft.tfs.core.clients.workitem.internal.wiqlparse.NodeSelect;
import com.microsoft.tfs.core.clients.workitem.internal.wiqlparse.NodeString;
import com.microsoft.tfs.core.clients.workitem.internal.wiqlparse.Parser;
import com.microsoft.tfs.core.clients.workitem.link.WorkItemLinkUtils;
import com.microsoft.tfs.core.clients.workitem.project.Project;
import com.microsoft.tfs.core.clients.workitem.query.DisplayFieldList;
import com.microsoft.tfs.core.clients.workitem.query.Query;
import com.microsoft.tfs.core.clients.workitem.query.StoredQuery;
import com.microsoft.tfs.core.clients.workitem.queryhierarchy.QueryDefinition;
import com.microsoft.tfs.core.clients.workitem.queryhierarchy.QueryItemType;
import com.microsoft.tfs.core.clients.workitem.wittype.WorkItemType;
import com.microsoft.tfs.core.clients.workitem.wittype.WorkItemTypeCollection;
import com.microsoft.tfs.util.Check;

public class WorkItemPickerDialog extends BaseDialog {
    private static final Log log = LogFactory.getLog(WorkItemPickerDialog.class);

    private final TFSServer server;

    /*
     * the work item client used by this dialog
     */
    private final WorkItemClient workItemClient;

    /*
     * The initial project to select, or null if there is no initial project.
     */
    private final Project initialProject;

    /*
     * The work item type filters defined on a links control (can be null).
     */
    private final WIFormLinksControlWITypeFilters wiFilters;

    /*
     * widgets that need to be accessed after creation
     */
    private Combo projectCombo;
    private Composite savedQueryArea;
    private Text savedQueryText;
    private Button savedQueryButton;
    private Text idsText;
    private Text titleText;
    private Combo workItemTypeCombo;
    private Button findButton;
    private Button savedQueryModeButton;
    private Button idsModeButton;
    private Button titleModeButton;
    private QueryResultsControl resultsControl;
    private Composite resultsComposite;
    private Label resultsLabel;
    private Label filterLabel;
    private Font filterLabelFont;

    /*
     * the array of team projects used by this dialog - this array backs the
     * team projects shown in the team project combo
     */
    private Project[] projects;

    /*
     * the array of work item types used by this dialog - this array backs the
     * work item types shown in the work item type combo
     */
    private WorkItemType[] workItemTypes;

    /*
     * The default search mode to use when we are creating the control.
     */
    private static final SearchMode DEFAULT_SEARCH_MODE = SearchMode.SAVED_QUERY;

    /*
     * The WorkItem that the user has selected
     */
    private WorkItem[] selectedWorkItems;

    /*
     * The currently selected query definition.
     */
    private QueryDefinition selectedQueryDefinition;

    /*
     * A WHERE clause snippet which includes the allowed work item types based
     * on the filter. The form of this string is "([System.WorkItemType] = "
     * <type >" OR [System.WorkItemType] = "<type2>"...)". This member will be
     * null if no filter is supplied or no if no work item types are exluded by
     * the filter.
     */
    private String workItemTypeFilterWhereClause = null;

    /*
     * True if any work item types are excluded based on the work item type
     * filter.
     */
    private boolean haveFilters = false;

    /*
     * True if multi-select should be allowed on this dialog.
     */
    private boolean allowMultiSelect = true;

    /*
     * A simple enum-style class. Values indicate different search modes that
     * the dialog can be in.
     */
    private static class SearchMode {
        private static final SearchMode SAVED_QUERY = new SearchMode();
        private static final SearchMode IDS = new SearchMode();
        private static final SearchMode TITLE = new SearchMode();

        private SearchMode() {
        }
    }

    public WorkItemPickerDialog(
        final Shell parentShell,
        final TFSServer server,
        final WorkItemClient workItemClient,
        final Project initialProject,
        final WIFormLinksControlWITypeFilters wiFilters,
        final boolean allowMultiSelect) {
        super(parentShell);

        Check.notNull(server, "server"); //$NON-NLS-1$
        Check.notNull(workItemClient, "workItemClient"); //$NON-NLS-1$
        Check.notNull(initialProject, "initialProject"); //$NON-NLS-1$

        this.server = server;
        this.workItemClient = workItemClient;
        this.initialProject = initialProject;
        this.wiFilters = wiFilters;
        this.allowMultiSelect = allowMultiSelect;

        haveFilters = wiFilters != null && wiFilters.getFilter() != WIFormLinksControlWITypeFilterEnum.INCLUDEALL;
    }

    @Override
    protected String provideDialogTitle() {
        return Messages.getString("WorkItemPickerDialog.DialogTitle"); //$NON-NLS-1$
    }

    @Override
    protected void hookAddToDialogArea(final Composite dialogArea) {
        final GridLayout layout = new GridLayout(2, false);
        dialogArea.setLayout(layout);

        Label label = new Label(dialogArea, SWT.NONE);
        label.setText(Messages.getString("WorkItemPickerDialog.ProjectLabelText")); //$NON-NLS-1$

        projectCombo = new Combo(dialogArea, SWT.READ_ONLY);
        projectCombo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                projectChanged();
            }
        });
        populateProjects();

        label = new Label(dialogArea, SWT.NONE);
        label.setText(Messages.getString("WorkItemPickerDialog.SelectMethodLabelText")); //$NON-NLS-1$
        label.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false, layout.numColumns, 1));

        savedQueryModeButton = new Button(dialogArea, SWT.RADIO);
        savedQueryModeButton.setText(Messages.getString("WorkItemPickerDialog.SaveQueryButtonText")); //$NON-NLS-1$
        savedQueryModeButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                searchModeChanged();
            }
        });
        savedQueryModeButton.setSelection(DEFAULT_SEARCH_MODE == SearchMode.SAVED_QUERY);

        savedQueryArea = new Composite(dialogArea, SWT.NONE);
        final GridLayout queryAreaLayout = new GridLayout(2, false);
        queryAreaLayout.marginHeight = 0;
        queryAreaLayout.marginWidth = 0;
        savedQueryArea.setLayout(queryAreaLayout);
        savedQueryArea.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

        savedQueryText = new Text(savedQueryArea, SWT.BORDER | SWT.READ_ONLY);
        savedQueryText.setText(Messages.getString("WorkItemPickerDialog.SavedQueryLabelText")); //$NON-NLS-1$
        savedQueryText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

        savedQueryButton = new Button(savedQueryArea, SWT.NONE);
        savedQueryButton.setText(Messages.getString("WorkItemPickerDialog.SavedQueryButtonText")); //$NON-NLS-1$
        savedQueryButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                selectQuery();
            }
        });

        idsModeButton = new Button(dialogArea, SWT.RADIO);
        idsModeButton.setText(Messages.getString("WorkItemPickerDialog.IdsButtonText")); //$NON-NLS-1$
        idsModeButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                searchModeChanged();
            }
        });
        idsModeButton.setSelection(DEFAULT_SEARCH_MODE == SearchMode.IDS);

        idsText = new Text(dialogArea, SWT.BORDER);
        idsText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
        idsText.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(final ModifyEvent e) {
                idsTextModified();
            }
        });

        titleModeButton = new Button(dialogArea, SWT.RADIO);
        titleModeButton.setText(Messages.getString("WorkItemPickerDialog.TitleModeButtonText")); //$NON-NLS-1$
        titleModeButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                searchModeChanged();
            }
        });
        titleModeButton.setSelection(DEFAULT_SEARCH_MODE == SearchMode.TITLE);

        titleText = new Text(dialogArea, SWT.BORDER);
        titleText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
        titleText.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(final ModifyEvent e) {
                titleTextModified();
            }
        });

        label = new Label(dialogArea, SWT.NONE);
        label.setText(Messages.getString("WorkItemPickerDialog.AndTypeLabelText")); //$NON-NLS-1$

        workItemTypeCombo = new Combo(dialogArea, SWT.READ_ONLY);
        workItemTypeCombo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
        populateWorkItemTypes();

        findButton = new Button(dialogArea, SWT.NONE);
        findButton.setText(Messages.getString("WorkItemPickerDialog.FindButtonText")); //$NON-NLS-1$
        findButton.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, layout.numColumns, 1));
        findButton.setEnabled(false); // find is disabled by default
        findButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                find();
            }
        });

        label = new Label(dialogArea, SWT.NONE);
        label.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false, layout.numColumns, 1));
        label.setText(Messages.getString("WorkItemPickerDialog.SelectItemLabelText")); //$NON-NLS-1$

        resultsControl = new QueryResultsControl(dialogArea, allowMultiSelect ? SWT.MULTI : SWT.NONE);
        resultsControl.addSimpleSortingBehavior();
        resultsControl.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, layout.numColumns, 1));
        ControlSize.setCharHeightHint(resultsControl, 10);
        resultsControl.addSelectionChangedListener(new ISelectionChangedListener() {
            @Override
            public void selectionChanged(final SelectionChangedEvent event) {
                resultsControlSelectionChanged(event.getSelection());
            }
        });
        resultsControl.addDoubleClickListener(new DoubleClickListener() {
            @Override
            public void doubleClick(final DoubleClickEvent event) {
                final WorkItem workItem = (WorkItem) ((IStructuredSelection) event.getSelection()).getFirstElement();
                doubleClicked(workItem);
            }
        });

        resultsComposite = new Composite(dialogArea, SWT.NONE);
        resultsComposite.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false, layout.numColumns, 1));
        SWTUtil.gridLayout(resultsComposite, 2, false, 0, 0);

        resultsLabel = new Label(resultsComposite, SWT.NONE);
        resultsLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1));

        filterLabel = new Label(resultsComposite, SWT.NONE);
        filterLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 1, 1));
        filterLabel.setVisible(false);
        filterLabel.setText(getFilterLabelText());
        filterLabelFont = styleFont(filterLabel, SWT.ITALIC);
        filterLabel.setFont(filterLabelFont);

        final Button button = new Button(dialogArea, SWT.NONE);
        button.setText(Messages.getString("WorkItemPickerDialog.ResetButtonText")); //$NON-NLS-1$
        button.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, layout.numColumns, 1));
        button.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                reset();
            }
        });

        /*
         * set initial enablement
         */
        setEnablementBasedOnSearchMode();
        setFindEnablement();

        reset();
    }

    @Override
    protected void hookDialogAboutToClose() {
        filterLabelFont.dispose();
    }

    public WorkItem[] getSelectedWorkItems() {
        return selectedWorkItems;
    }

    private void resultsControlSelectionChanged(final ISelection selection) {
        if (selection.isEmpty()) {
            selectedWorkItems = null;
        } else {
            final ArrayList selections = new ArrayList();
            final IStructuredSelection structuredSelection = (IStructuredSelection) selection;
            for (final Iterator it = structuredSelection.iterator(); it.hasNext();) {
                selections.add(it.next());
            }
            selectedWorkItems = (WorkItem[]) selections.toArray(new WorkItem[selections.size()]);
        }
        setOKEnablement();
    }

    private void doubleClicked(final WorkItem workItem) {
        okPressed();
    }

    /**
     * The title text control has had its contents changed.
     */
    private void titleTextModified() {
        setFindEnablement();
    }

    /**
     * The IDs text control has had its contents changed.
     */
    private void idsTextModified() {
        setFindEnablement();
    }

    /**
     * Sets the enablement state of controls on the dialog based on the search
     * mode that is currently active.
     */
    private void setEnablementBasedOnSearchMode() {
        final SearchMode searchMode = getSearchMode();

        savedQueryText.setEnabled(searchMode == SearchMode.SAVED_QUERY);
        savedQueryButton.setEnabled(searchMode == SearchMode.SAVED_QUERY);

        idsText.setEnabled(searchMode == SearchMode.IDS);
        titleText.setEnabled(searchMode == SearchMode.TITLE);
        workItemTypeCombo.setEnabled(searchMode == SearchMode.TITLE);
    }

    /**
     * Sets enablement of the Find button based on the data provided by the user
     * for the current search mode.
     */
    private void setFindEnablement() {
        final SearchMode searchMode = getSearchMode();

        if (searchMode == SearchMode.SAVED_QUERY) {
            findButton.setEnabled(selectedQueryDefinition != null);
        } else if (searchMode == SearchMode.IDS) {
            findButton.setEnabled(idsText.getText().trim().length() > 0);
        } else if (searchMode == SearchMode.TITLE) {
            findButton.setEnabled(titleText.getText().trim().length() > 0);
        } else {
            throw new IllegalStateException();
        }
    }

    @Override
    protected void createButtonsForButtonBar(final Composite parent) {
        super.createButtonsForButtonBar(parent);
        setOKEnablement();
    }

    private void setOKEnablement() {
        final Button button = getButton(IDialogConstants.OK_ID);

        /*
         * guard for when we are called from reset() before the buttons have
         * been created
         */
        if (button != null) {
            button.setEnabled(selectedWorkItems != null && selectedWorkItems.length > 0);
        }
    }

    /**
     * The select query button was clicked by the user.
     */
    private void selectQuery() {
        final SelectQueryItemDialog queryDefinitionDialog = new SelectQueryItemDialog(
            getShell(),
            server,
            workItemClient.getProjects().getProjects(),
            selectedQueryDefinition,
            QueryItemType.QUERY_DEFINITION);

        if (queryDefinitionDialog.open() == IDialogConstants.OK_ID) {
            selectedQueryDefinition = (QueryDefinition) queryDefinitionDialog.getSelectedQueryItem();
            final StoredQuery storedQuery = workItemClient.getStoredQuery(selectedQueryDefinition.getID());
            savedQueryText.setText(storedQuery.getName());
            setFindEnablement();
        }
    }

    /**
     * The find button was clicked by the user.
     */
    private void find() {
        final Query query = makeQuery();
        if (query == null) {
            /*
             * some sort of validation error - return without filling the
             * results
             */
            return;
        }
        fillResultsWithQuery(query, true);
    }

    /**
     * Make a query to run based on the current search mode. Called on the UI
     * thread.
     *
     * @return a Query to run, or null to indicate a validation error or
     *         cancellation occurred
     */
    private Query makeQuery() {
        final SearchMode searchMode = getSearchMode();

        if (searchMode == SearchMode.SAVED_QUERY) {
            return makeQueryQuery();
        } else if (searchMode == SearchMode.IDS) {
            return makeIDsQuery();
        } else if (searchMode == SearchMode.TITLE) {
            return makeTitleQuery();
        }

        throw new UnsupportedOperationException();
    }

    private Query makeIDsQuery() {

        int[] workItemIds = null;

        try {
            workItemIds = WorkItemLinkUtils.buildWorkItemIDListFromText(idsText.getText());
        } catch (final NumberFormatException e) {
            MessageBoxHelpers.errorMessageBox(
                getShell(),
                Messages.getString("WorkItemPickerDialog.ErrorDialogTitle"), //$NON-NLS-1$
                e.getLocalizedMessage());
            return null;
        } catch (final RuntimeException e) {
        }

        if (workItemIds == null || workItemIds.length == 0) {
            MessageBoxHelpers.errorMessageBox(
                getShell(),
                Messages.getString("WorkItemPickerDialog.ErrorDialogTitle"), //$NON-NLS-1$
                Messages.getString("WorkItemPickerDialog.IdErrorDialogText")); //$NON-NLS-1$
            return null;
        }

        final StringBuffer sb = new StringBuffer();

        sb.append("SELECT ["); //$NON-NLS-1$
        sb.append(CoreFieldReferenceNames.ID);
        sb.append("] From WorkItem WHERE ["); //$NON-NLS-1$

        sb.append(CoreFieldReferenceNames.ID);
        sb.append("] IN ("); //$NON-NLS-1$

        for (int i = 0; i < workItemIds.length; i++) {
            if (i > 0) {
                sb.append(","); //$NON-NLS-1$
            }
            sb.append(workItemIds[i]);
        }
        sb.append(")"); //$NON-NLS-1$

        final Project project = getSelectedProject();
        if (project != null) {
            sb.append(" AND ["); //$NON-NLS-1$
            sb.append(CoreFieldReferenceNames.AREA_PATH);
            sb.append("] UNDER \""); //$NON-NLS-1$
            sb.append(project.getName());
            sb.append("\""); //$NON-NLS-1$
        }

        if (workItemTypeFilterWhereClause != null) {
            sb.append(" AND "); //$NON-NLS-1$
            sb.append(workItemTypeFilterWhereClause);
        }

        sb.append(" ORDER BY ["); //$NON-NLS-1$
        sb.append(CoreFieldReferenceNames.ID);
        sb.append("]"); //$NON-NLS-1$

        final String wiql = sb.toString();
        if (log.isTraceEnabled()) {
            log.trace("ids query: [" + wiql + "]"); //$NON-NLS-1$ //$NON-NLS-2$
        }

        return workItemClient.createQuery(wiql);
    }

    private Query makeTitleQuery() {
        String containsText = titleText.getText().trim();
        containsText = containsText.replaceAll("\"", "\"\""); //$NON-NLS-1$ //$NON-NLS-2$

        final StringBuffer sb = new StringBuffer();

        sb.append("SELECT ["); //$NON-NLS-1$
        sb.append(CoreFieldReferenceNames.ID);
        sb.append("] From WorkItem WHERE ["); //$NON-NLS-1$
        sb.append(CoreFieldReferenceNames.TITLE);
        sb.append("] CONTAINS \""); //$NON-NLS-1$
        sb.append(containsText);
        sb.append("\""); //$NON-NLS-1$

        final WorkItemType workItemType = getSelectedWorkItemType();
        if (workItemType != null) {
            sb.append(" AND ["); //$NON-NLS-1$
            sb.append(CoreFieldReferenceNames.WORK_ITEM_TYPE);
            sb.append("] = \""); //$NON-NLS-1$
            sb.append(workItemType.getName());
            sb.append("\""); //$NON-NLS-1$
        } else if (workItemTypeFilterWhereClause != null) {
            sb.append(" AND "); //$NON-NLS-1$
            sb.append(workItemTypeFilterWhereClause);
        }

        final Project project = getSelectedProject();
        if (project != null) {
            sb.append(" AND ["); //$NON-NLS-1$
            sb.append(CoreFieldReferenceNames.AREA_PATH);
            sb.append("] UNDER \""); //$NON-NLS-1$
            sb.append(project.getName());
            sb.append("\""); //$NON-NLS-1$
        }

        sb.append(" ORDER BY ["); //$NON-NLS-1$
        sb.append(CoreFieldReferenceNames.ID);
        sb.append("]"); //$NON-NLS-1$

        final String wiql = sb.toString();

        if (log.isTraceEnabled()) {
            final String messageFormat = "title query: [{0}]"; //$NON-NLS-1$
            final String message = MessageFormat.format(messageFormat, wiql);
            log.trace(message);
        }

        return workItemClient.createQuery(wiql);
    }

    /**
     * Create a new query based of an existing query, which was selected by the
     * user, with the addition of work item type filtering. If filtering is
     * required, the WHERE clause of the existing query needs to be updated to
     * include on the valid work item types. To do this, the existing query is
     * parsed by the WIQL parser and the WIQL DOM is modified to include the
     * additional conditions on the WHERE clause.
     *
     *
     * @return A new query which contains required work item filtering.
     */
    private Query makeQueryQuery() {
        // Get the existing WIQL query chosen by the user.
        String wiql = selectedQueryDefinition.getQueryText();

        if (haveFilters && workItemTypes.length > 0) {
            // Parse the existing WIQL.
            final NodeSelect select = Parser.parseSyntax(wiql);
            final Node where = select.getWhere();
            Node addition;

            // Build the conditions that will produce filtering.
            if (workItemTypes.length == 1) {
                addition = new NodeCondition(
                    Condition.EQUALS,
                    new NodeFieldName("System.WorkItemType"), //$NON-NLS-1$
                    new NodeString(workItemTypes[0].getName()));
            } else {
                final NodeOrOperator nodeOr = new NodeOrOperator();
                for (int i = 0; i < workItemTypes.length; i++) {
                    nodeOr.add(new NodeCondition(
                        Condition.EQUALS,
                        new NodeFieldName("System.WorkItemType"), //$NON-NLS-1$
                        new NodeString(workItemTypes[i].getName())));
                }
                addition = nodeOr;
            }

            // Update the WHERE clause.
            if (where == null) {
                select.setWhere(addition);
            } else {
                final NodeAndOperator nodeAnd = new NodeAndOperator();
                nodeAnd.add(where);
                nodeAnd.add(addition);
                select.setWhere(nodeAnd);
            }

            wiql = select.toString();
        }

        // Create a query based on the potentially modified WIQL.
        return workItemClient.createQuery(
            wiql,
            WorkItemQueryUtils.makeContext(initialProject, WorkItemHelpers.getCurrentTeamName()));
    }

    /**
     * Builds a snippet of a WHERE clause that can be included when building a
     * query to support the ID and TITLE search modes.
     *
     *
     * @param workItemTypes
     *        The work item types to include.
     *
     * @return The snippet for a WIQL where clause. The string is in the format
     *         ([System.WorkItemType] = "type1" OR [System.WorkItemType] =
     *         "type2"...)
     */
    private String makeWorkItemTypeFilterWhereClause(final WorkItemType[] workItemTypes) {
        if (workItemTypes.length == 0) {
            return null;
        }

        final StringBuffer sb = new StringBuffer();
        sb.append("("); //$NON-NLS-1$

        for (int i = 0; i < workItemTypes.length; i++) {
            if (i > 0) {
                sb.append(" OR "); //$NON-NLS-1$
            }
            sb.append("["); //$NON-NLS-1$
            sb.append(CoreFieldReferenceNames.WORK_ITEM_TYPE);
            sb.append("] = \""); //$NON-NLS-1$
            sb.append(workItemTypes[i].getName());
            sb.append("\""); //$NON-NLS-1$
        }

        sb.append(")"); //$NON-NLS-1$
        return sb.toString();
    }

    /**
     * Fill the results control with the results made from running the given
     * Query.
     *
     * @param query
     *        the Query to run
     * @param updateResultsLabel
     *        true to update the results label
     */
    private void fillResultsWithQuery(final Query query, final boolean updateResultsLabel) {
        /*
         * modify the DisplayFieldList on the Query to have the fields we want
         * to display in this control
         */
        final DisplayFieldList displayFieldList = query.getDisplayFieldList();
        displayFieldList.clear();
        displayFieldList.add(CoreFieldReferenceNames.ID);
        displayFieldList.add(CoreFieldReferenceNames.WORK_ITEM_TYPE);
        displayFieldList.add(CoreFieldReferenceNames.TITLE);
        displayFieldList.add(CoreFieldReferenceNames.STATE);

        /*
         * run the query
         */
        final String projectName = (getSelectedProject() != null) ? getSelectedProject().getName() : null;
        final String queryName = (selectedQueryDefinition != null) ? selectedQueryDefinition.getName() : null;
        final QueryResultCommand command = new QueryResultCommand(query, projectName, queryName);

        final IStatus status = UICommandExecutorFactory.newBusyIndicatorCommandExecutor(getShell()).execute(command);
        if (status.getSeverity() != Status.OK) {
            return;
        }
        final QueryResultData results = command.getQueryResultData();

        /*
         * pass the results off to the results control
         */
        resultsControl.setServer(server);
        resultsControl.setWorkItems(results);

        /*
         * enable the results control if there is at least one result
         */
        resultsControl.setEnabled(results.getCount() > 0);

        /*
         * update the results label if needed
         */
        if (updateResultsLabel) {
            final String message =
                MessageFormat.format(Messages.getString("WorkItemPickerDialog.ResultsLabelText"), new Object[] //$NON-NLS-1$
            {
                new Integer(results.getCount())
            });
            resultsLabel.setText(message);
            filterLabel.setVisible(true);
        } else {
            resultsLabel.setText(""); //$NON-NLS-1$
            filterLabel.setVisible(false);
        }

        resultsComposite.layout(true);
    }

    /**
     * The reset button was clicked by the user.
     */
    private void reset() {
        // 1) does not change the selected search mode or team project
        // 2) clears any entered or selected data
        // 3) clears any results
        savedQueryText.setText(Messages.getString("WorkItemPickerDialog.SavedQueryLabelText")); //$NON-NLS-1$
        idsText.setText(""); //$NON-NLS-1$
        titleText.setText(""); //$NON-NLS-1$
        workItemTypeCombo.select(0);

        fillResultsWithQuery(workItemClient.createEmptyQuery(), false);
        selectedWorkItems = null;
        selectedQueryDefinition = null;

        setFindEnablement();
        setOKEnablement();
    }

    /**
     * The search mode has been changed by the user. Note: the new search mode
     * is available by calling the getSearchMode() method.
     */
    private void searchModeChanged() {
        setEnablementBasedOnSearchMode();
        reset();
    }

    /**
     * The selected project has been changed by the user. Note: The newly
     * selected project can be obtained by calling getSelectedProject().
     */
    private void projectChanged() {
        populateWorkItemTypes();
        reset();
    }

    /**
     * This method: 1) populates the "projects" field of this class 2) populates
     * the projects combo with project names 3) selects the initial choice in
     * the projects combo ("Any Project")
     */
    private void populateProjects() {
        projects = workItemClient.getProjects().getProjects();

        projectCombo.add(Messages.getString("WorkItemPickerDialog.AnyProjectChoice")); //$NON-NLS-1$
        int initialSelectIx = 0;

        for (int i = 0; i < projects.length; i++) {
            projectCombo.add(projects[i].getName());
            if (projects[i] == initialProject) {
                initialSelectIx = (i + 1);
            }
        }

        /*
         * select "Any Project" by default
         */
        projectCombo.select(initialSelectIx);
    }

    /**
     * Obtains the currently selected team project in the projects combo. This
     * method will return null to indicate that the "Any Project" choice is
     * currently selected.
     *
     * @return the current project or null
     */
    private Project getSelectedProject() {
        final int ix = projectCombo.getSelectionIndex();
        if (ix == 0) {
            return null;
        }
        return projects[ix - 1];
    }

    /**
     * @return the current SearchMode, as determined by the search mode radio
     *         buttons
     */
    private SearchMode getSearchMode() {
        if (savedQueryModeButton.getSelection()) {
            return SearchMode.SAVED_QUERY;
        } else if (idsModeButton.getSelection()) {
            return SearchMode.IDS;
        } else if (titleModeButton.getSelection()) {
            return SearchMode.TITLE;
        }
        throw new IllegalStateException();
    }

    /**
     * Populates the saved query combo based on the currently selected project
     * in the project combo and the specified work item type filters.
     */
    private void populateWorkItemTypes() {
        workItemTypeCombo.removeAll();
        workItemTypeCombo.add(Messages.getString("WorkItemPickerDialog.AllWorkItemTypesChoice")); //$NON-NLS-1$
        workItemTypeCombo.select(0);

        // Get the set of filtered work item types based on the project
        // selection.
        final Project selectedProject = getSelectedProject();
        if (selectedProject != null) {
            workItemTypes = getFilteredWorkItemTypes(selectedProject.getWorkItemTypes().getTypes());
        } else {
            workItemTypes = getFilteredWorkItemTypes(getAllProjectWorkItemTypes());
        }

        // Build the WHERE clause that would apply for the filtered work item
        // types.
        workItemTypeFilterWhereClause = null;
        if (haveFilters && workItemTypes != null && workItemTypes.length > 0) {
            workItemTypeFilterWhereClause = makeWorkItemTypeFilterWhereClause(workItemTypes);
        }

        // Populate the combobox.
        if (workItemTypes != null) {
            for (int i = 0; i < workItemTypes.length; i++) {
                workItemTypeCombo.add(workItemTypes[i].getName());
            }
        }

        // Set the minimum size of the drop down.
        ComboHelper.setVisibleItemCount(workItemTypeCombo);
    }

    /**
     * Iterate all projects and determine the union of work item types across
     * all projects.
     *
     * @return Array of work-item types representing the union of all work item
     *         types across all projects.
     */
    private WorkItemType[] getAllProjectWorkItemTypes() {
        final HashMap map = new HashMap();
        for (int i = 0; i < projects.length; i++) {
            final Project project = projects[i];
            final WorkItemTypeCollection collection = project.getWorkItemTypes();

            for (final Iterator it = collection.iterator(); it.hasNext();) {
                final WorkItemType type = (WorkItemType) it.next();
                final String typeName = type.getName();

                if (!map.containsKey(typeName)) {
                    map.put(typeName, type);
                }
            }
        }

        return (WorkItemType[]) map.values().toArray(new WorkItemType[map.size()]);
    }

    /**
     * Filters the specified array of work-item types based on the filter
     * options suppled to this class.
     *
     *
     * @param allWorkItemTypes
     * @return
     */
    private WorkItemType[] getFilteredWorkItemTypes(final WorkItemType[] allWorkItemTypes) {
        if (!haveFilters) {
            return allWorkItemTypes;
        }

        final ArrayList list = new ArrayList();
        for (int i = 0; i < allWorkItemTypes.length; i++) {
            if (wiFilters.includes(allWorkItemTypes[i].getName())) {
                list.add(allWorkItemTypes[i]);
            }
        }

        return (WorkItemType[]) list.toArray(new WorkItemType[list.size()]);
    }

    /**
     * Obtains the currently selected work item type in the work item types
     * combo. This method will return null to indicate that the
     * "All Work Item Types" choice is currently selected.
     *
     * @return the current work item type or null
     */
    private WorkItemType getSelectedWorkItemType() {
        final int ix = workItemTypeCombo.getSelectionIndex();
        if (ix == 0) {
            return null;
        }
        return workItemTypes[ix - 1];
    }

    /**
     * Returns the text to display when work items are being filtered by type.
     * An empty string is returned if there are no filters applied to this
     * dialog.
     */
    private String getFilterLabelText() {
        if (wiFilters != null && wiFilters.getFilter() != WIFormLinksControlWITypeFilterEnum.INCLUDEALL) {
            final WIFormLinksControlWITypeFilter[] filters = wiFilters.getFilters();

            if (filters.length > 0) {
                String messageFormat;
                if (wiFilters.getFilter() == WIFormLinksControlWITypeFilterEnum.EXCLUDE) {
                    messageFormat = Messages.getString("WorkItemPickerDialog.ExcludingWorkItemTypesFormat"); //$NON-NLS-1$
                } else {
                    messageFormat = Messages.getString("WorkItemPickerDialog.IncludingWorkItemTypesFormat"); //$NON-NLS-1$
                }

                final StringBuffer sb = new StringBuffer();
                for (int i = 0; i < filters.length; i++) {
                    if (i > 0) {
                        sb.append(Messages.getString("WorkItemPickerDialog.WorkItemTypeNameSeparator")); //$NON-NLS-1$
                    }
                    sb.append(filters[i].getWorkItemType());
                }

                return MessageFormat.format(messageFormat, sb.toString());
            }
        }

        return ""; //$NON-NLS-1$
    }

    /**
     * Create a new font based of the existing font of the specified label. Add
     * the additional style to the new font.
     *
     *
     * @param label
     *        The label whose font we want to use as a base.
     * @param style
     *        The styles to add to the base font.
     * @return A newly allocated font.
     */
    private Font styleFont(final Label label, final int style) {
        final FontData[] fontData = label.getFont().getFontData();
        for (int i = 0; i < fontData.length; i++) {
            fontData[i].setStyle(fontData[i].getStyle() | style);
        }

        return new Font(label.getDisplay(), fontData);
    }
}
