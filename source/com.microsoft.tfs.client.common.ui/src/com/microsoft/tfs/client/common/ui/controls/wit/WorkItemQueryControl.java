// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.controls.wit;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import com.microsoft.tfs.client.common.commands.wit.RefreshStoredQueriesCommand;
import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.common.server.TFSServer;
import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.TFSCommonUIClientPlugin;
import com.microsoft.tfs.client.common.ui.controls.generic.BaseControl;
import com.microsoft.tfs.client.common.ui.dialogs.wit.InputWIQLDialog;
import com.microsoft.tfs.client.common.ui.dialogs.wit.InputWorkItemIDDialog;
import com.microsoft.tfs.client.common.ui.framework.command.UICommandExecutorFactory;
import com.microsoft.tfs.client.common.ui.framework.helper.MessageBoxHelpers;
import com.microsoft.tfs.client.common.ui.framework.layout.GridDataBuilder;
import com.microsoft.tfs.client.common.ui.helpers.AutomationIDHelper;
import com.microsoft.tfs.client.common.ui.teamexplorer.helpers.WorkItemHelpers;
import com.microsoft.tfs.client.common.ui.viewstate.ObjectSerializer;
import com.microsoft.tfs.client.common.ui.viewstate.TFSServerScope;
import com.microsoft.tfs.client.common.ui.viewstate.ViewState;
import com.microsoft.tfs.client.common.ui.wit.dialogs.SelectQueryItemDialog;
import com.microsoft.tfs.client.common.ui.wit.dialogs.WITSearchDialog;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.WorkItemCheckinInfo;
import com.microsoft.tfs.core.clients.workitem.WorkItemClient;
import com.microsoft.tfs.core.clients.workitem.WorkItemQueryUtils;
import com.microsoft.tfs.core.clients.workitem.exceptions.UnauthorizedAccessException;
import com.microsoft.tfs.core.clients.workitem.project.Project;
import com.microsoft.tfs.core.clients.workitem.project.ProjectCollection;
import com.microsoft.tfs.core.clients.workitem.query.BatchReadParameter;
import com.microsoft.tfs.core.clients.workitem.query.BatchReadParameterCollection;
import com.microsoft.tfs.core.clients.workitem.query.InvalidQueryTextException;
import com.microsoft.tfs.core.clients.workitem.query.Query;
import com.microsoft.tfs.core.clients.workitem.query.QueryUtils;
import com.microsoft.tfs.core.clients.workitem.query.StoredQuery;
import com.microsoft.tfs.core.clients.workitem.queryhierarchy.QueryDefinition;
import com.microsoft.tfs.core.clients.workitem.queryhierarchy.QueryItemType;
import com.microsoft.tfs.core.ws.runtime.exceptions.TransportRequestHandlerCanceledException;
import com.microsoft.tfs.util.GUID;
import com.microsoft.tfs.util.GUID.GUIDStringFormat;

public class WorkItemQueryControl extends BaseControl {
    private static final Log log = LogFactory.getLog(WorkItemQueryControl.class);

    private static final String NEWLINE = System.getProperty("line.separator"); //$NON-NLS-1$
    private static final String STORED_QUERIES_KEY = "queries-list"; //$NON-NLS-1$
    private static final String VIEW_STATE_KEY = "work-item-control"; //$NON-NLS-1$
    private static final int MAX_RECENT_QUERIES = 10;
    public static final String QUERY_DROPDOWN_ID = "WorkItemQueryControl.descriptions"; //$NON-NLS-1$

    private final Label queryLabel;
    private final Combo descriptions;
    private final Button selectQueryButton;
    private final Button refreshButton;

    private final List<QueryControlAction> actions = new ArrayList<QueryControlAction>();

    private TFSServer server;
    private ProjectCollection projects;
    private final int maxStoredQueryCount;
    private final String viewStateKey;

    private int[] lastIdsForIdsQuery = null;
    private String lastWiqlForWiqlQuery = null;
    private Project lastProjectForWiqlQuery = null;

    private boolean initialPopulation = true;
    private boolean suppressInitialPopulation = false;

    private final Set<QuerySelectedListener> listeners = new HashSet<QuerySelectedListener>();

    private int lastRunActionIndex = -1;

    public static interface QuerySelectedListener {
        public void querySelected(Query query);
    }

    public WorkItemQueryControl(final Composite parent, final int style) {
        this(parent, style, MAX_RECENT_QUERIES, VIEW_STATE_KEY);
    }

    public WorkItemQueryControl(
        final Composite parent,
        final int style,
        final int maxStoredQueryCount,
        final String viewStateKey) {
        super(parent, style);

        this.maxStoredQueryCount = maxStoredQueryCount;
        this.viewStateKey = viewStateKey;

        final GridLayout layout = new GridLayout(4, false);
        layout.marginWidth = 0;
        layout.marginHeight = 0;
        layout.horizontalSpacing = getHorizontalSpacing();
        layout.verticalSpacing = getVerticalSpacing();
        setLayout(layout);

        queryLabel = new Label(this, SWT.NONE);
        queryLabel.setText(Messages.getString("WorkItemQueryControl.QueryLabelText")); //$NON-NLS-1$

        descriptions = new Combo(this, SWT.NONE | SWT.READ_ONLY);
        AutomationIDHelper.setWidgetID(descriptions, QUERY_DROPDOWN_ID);

        descriptions.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                runSelectedAction();
            }
        });
        GridDataBuilder.newInstance().hGrab().hFill().applyTo(descriptions);

        selectQueryButton = new Button(this, SWT.NONE);
        selectQueryButton.setText(Messages.getString("WorkItemQueryControl.SelectQueryButtonText")); //$NON-NLS-1$
        selectQueryButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                openSelectQueryDialog();
            }
        });

        refreshButton = new Button(this, SWT.NONE);
        refreshButton.setText(Messages.getString("WorkItemQueryControl.RefreshButtonText")); //$NON-NLS-1$
        refreshButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                refresh();
            }
        });

        setRepository(null);
    }

    public void setRepository(final TFSRepository repository) {
        if (repository == null) {
            descriptions.setItems(new String[0]);
            setEnabled(false);
        } else {
            setEnabled(true);

            /* TODO: hook repositories <-> servers, get that way. */
            server = TFSCommonUIClientPlugin.getDefault().getProductPlugin().getServerManager().getServer(
                repository.getWorkspace().getClient().getConnection());

            /*
             * Pass null for project collection. The new set of projects will be
             * retrieved on initial access.
             */
            populate(null, null);
        }
    }

    private void refresh() {
        /*
         * The first thing we do when the user clicks refresh is to get all the
         * latest stored queries from the server. This handles the case where a
         * query has been modified since the last time we got it.
         */

        final RefreshStoredQueriesCommand refreshCommand =
            new RefreshStoredQueriesCommand(server, getProjectCollection());
        final IStatus refreshStatus = UICommandExecutorFactory.newUICommandExecutor(getShell()).execute(refreshCommand);

        if (!refreshStatus.isOK()) {
            return;
        }

        /*
         * Test whether a stored query is currently selected, and store this
         * state for later use.
         */
        final boolean wasStoredQuerySelected =
            (actions.get(descriptions.getSelectionIndex()) instanceof StoredQueryAction);

        /*
         * Loop over all our stored queries actions, updating each one with the
         * new information from the server.
         */
        for (int i = 0; i < descriptions.getItemCount(); i++) {
            final QueryControlAction action = actions.get(i);

            if (action instanceof StoredQueryAction) {
                final StoredQueryAction sqa = (StoredQueryAction) action;
                sqa.updateAfterRefresh();

                descriptions.remove(i);
                descriptions.add(QueryUtils.getExtendedDescription(sqa.storedQuery), i);
            }
        }

        /*
         * Pack and relayout the descriptions combo, as some of the query
         * descriptions may have changed.
         */
        descriptions.pack();
        layout();
        getParent().layout();

        /*
         * If a stored query action was previously selected, we need to reselect
         * it since the refresh algorithm removed and re-added the item to the
         * descriptions combo.
         */
        if (wasStoredQuerySelected) {
            /*
             * Select the first item - because of the way items are inserted we
             * can ensure that the selected item is always first.
             */
            descriptions.select(0);
        }

        refreshSelectedAction();
    }

    public void addQuerySelectedListener(final QuerySelectedListener listener) {
        synchronized (listeners) {
            listeners.add(listener);
        }
    }

    public void removeQuerySelectedListener(final QuerySelectedListener listener) {
        synchronized (listeners) {
            listeners.remove(listener);
        }
    }

    public void setSuppressInitialPopulation(final boolean suppressInitialPopulation) {
        this.suppressInitialPopulation = suppressInitialPopulation;
    }

    private void runSelectedAction() {
        int ix = descriptions.getSelectionIndex();

        /*
         * Remove any null actions that were not run (those exist only as
         * placeholders for set work items.
         */
        if (actions.size() > 0 && actions.get(0) instanceof SelectedItemsAction && ix > 0) {
            descriptions.remove(0);
            actions.remove(0);

            ix--;

            descriptions.select(ix);
        }

        if (ix == -1) {
            return;
        }

        final QueryControlAction action = actions.get(ix);

        try {
            action.run();
        } catch (final TransportRequestHandlerCanceledException e) {
            // The user didn't want to do federated auth or supply credentials;
            // ignore these
        } catch (final Exception e) {
            MessageDialog.openError(
                getShell(),
                Messages.getString("WorkItemQueryControl.ExecQueryErrorDialogTitle"), //$NON-NLS-1$
                e.getLocalizedMessage());
        }

        lastRunActionIndex = descriptions.getSelectionIndex();
    }

    private void refreshSelectedAction() {
        final int ix = descriptions.getSelectionIndex();

        if (ix == -1) {
            return;
        }

        final QueryControlAction action = actions.get(ix);

        try {
            action.refresh();
        } catch (final Exception e) {
            MessageDialog.openError(
                getShell(),
                Messages.getString("WorkItemQueryControl.ExecQueryErrorDialogTitle"), //$NON-NLS-1$
                e.getLocalizedMessage());
        }

        lastRunActionIndex = descriptions.getSelectionIndex();
    }

    /**
     * Sets the query text when overriding the work items configured.
     *
     * @param queryText
     */
    public void setQueryText(final String queryText) {
        setSelectedItems(queryText, new int[0]);
    }

    public void setSelectedItems(final String queryText, int[] ids) {
        if (ids == null) {
            ids = new int[0];
        }

        /* If there was already a selected items action, remove it. */
        if (actions.size() > 0 && actions.get(0) instanceof SelectedItemsAction) {
            descriptions.remove(0);
            actions.remove(0);
        }

        descriptions.add(queryText, 0);
        actions.add(0, new SelectedItemsAction(ids));

        descriptions.select(0);
    }

    public void setSelectedItems(final String queryText, WorkItemCheckinInfo[] checkinInfo) {
        if (checkinInfo == null) {
            checkinInfo = new WorkItemCheckinInfo[0];
        }

        final int[] ids = new int[checkinInfo.length];

        for (int i = 0; i < checkinInfo.length; i++) {
            ids[i] = checkinInfo[i].getWorkItem().getID();
        }

        setSelectedItems(queryText, ids);
    }

    public void populate(final ProjectCollection projects, final String pendingChangeProject) {
        this.projects = projects;

        descriptions.removeAll();
        actions.clear();

        /*
         * This is when a container (eg, ShelveDialog) wants to set the work
         * items itself, for example, the selected work items from pending
         * changes view.
         */
        if (initialPopulation == true && suppressInitialPopulation == true) {
            descriptions.add(Messages.getString("WorkItemQueryControl.SelectedWorkItemsChoice")); //$NON-NLS-1$
            actions.add(new SelectedItemsAction());
        }

        restoreSavedStoredQueries();

        /*
         * handle the case where no team project is active in team explorer
         */
        if (descriptions.getItemCount() == 0) {
            descriptions.add(""); //$NON-NLS-1$
            actions.add(new EmptyQueryAction());
        }

        descriptions.add(Messages.getString("WorkItemQueryControl.SelectQueryChoice")); //$NON-NLS-1$
        actions.add(new SelectQueryAction());

        descriptions.add(Messages.getString("WorkItemQueryControl.EnterIDChoice")); //$NON-NLS-1$
        actions.add(new EnterIDsAction());

        descriptions.add(Messages.getString("WorkItemQueryControl.EnterWiqlChoice")); //$NON-NLS-1$
        actions.add(new EnterWIQLQuery());

        descriptions.add(Messages.getString("WorkItemQueryControl.SearchChoice")); //$NON-NLS-1$
        actions.add(new WIQLSearchDialogAction());

        descriptions.select(0);
        descriptions.pack();
        layout();
        getParent().layout();

        if (initialPopulation == false || suppressInitialPopulation == false) {
            runSelectedAction();
        }

        initialPopulation = false;
    }

    private ProjectCollection getProjectCollection() {
        if (projects == null) {
            projects = server.getConnection().getWorkItemClient().getProjects();
        }

        return projects;
    }

    private WorkItemClient getWorkItemClient() {
        return server.getConnection().getWorkItemClient();
    }

    public boolean isPopulated() {
        return actions.size() > 0;
    }

    private ViewState getViewState() {
        final ViewState viewState = new ViewState(new TFSServerScope(viewStateKey, server));
        viewState.addObjectSerializer(StoredQuery.class, new WorkItemQuerySerializer());
        return viewState;
    }

    private void persistStoredQueries() {
        final ViewState viewState = getViewState();
        final List<StoredQuery> queries = new ArrayList<StoredQuery>();

        for (final Iterator<QueryControlAction> it = actions.iterator(); it.hasNext();) {
            final QueryControlAction action = it.next();

            if (action instanceof StoredQueryAction) {
                queries.add(((StoredQueryAction) action).storedQuery);
            }
        }

        viewState.persistList(queries, STORED_QUERIES_KEY, StoredQuery.class);
        viewState.commit();
    }

    private void restoreSavedStoredQueries() {
        final ViewState viewState = getViewState();
        final List<StoredQuery> queries = viewState.restoreList(STORED_QUERIES_KEY, StoredQuery.class);

        if (queries == null) {
            return;
        } else {
            for (final Iterator<StoredQuery> it = queries.iterator(); it.hasNext();) {
                final StoredQuery query = it.next();
                descriptions.add(QueryUtils.getExtendedDescription(query));
                actions.add(new StoredQueryAction(query));
            }
        }
    }

    private boolean openSelectQueryDialog() {
        if (!isPopulated()) {
            return false;
        }

        final StoredQuery lastRunQuery = getLastRunStoredQuery();
        QueryDefinition lastRunQueryDefinition = null;

        if (lastRunQuery != null) {
            lastRunQueryDefinition =
                (QueryDefinition) lastRunQuery.getProject().getQueryHierarchy().find(lastRunQuery.getQueryGUID());
        }

        final SelectQueryItemDialog queryDefinitionDialog = new SelectQueryItemDialog(
            getShell(),
            server,
            getProjectCollection().getProjects(),
            lastRunQueryDefinition,
            QueryItemType.QUERY_DEFINITION);

        if (queryDefinitionDialog.open() == IDialogConstants.OK_ID) {
            final QueryDefinition queryDefinition = (QueryDefinition) queryDefinitionDialog.getSelectedQueryItem();
            final StoredQuery storedQuery = getProjectCollection().getClient().getStoredQuery(queryDefinition.getID());

            storedQuerySelected(storedQuery);
            return true;
        } else {
            return false;
        }
    }

    private void storedQuerySelected(final StoredQuery query) {
        final int existingIx = getStoredQueryActionIndex(query);
        if (existingIx != -1) {
            descriptions.remove(existingIx);
            actions.remove(existingIx);
        }

        /*
         * remove the blank line (see populate())
         */
        if (actions.get(0) instanceof EmptyQueryAction || actions.get(0) instanceof SelectedItemsAction) {
            descriptions.remove(0);
            actions.remove(0);
        }

        descriptions.add(QueryUtils.getExtendedDescription(query), 0);
        actions.add(0, new StoredQueryAction(query));
        descriptions.select(0);

        if (getStoredQueryActionCount() > maxStoredQueryCount) {
            removeLastStoredQueryAction();
        }

        descriptions.pack();
        layout();
        getParent().layout();

        runSelectedAction();
    }

    private StoredQuery getLastRunStoredQuery() {
        if (lastRunActionIndex == -1) {
            return null;
        }
        final QueryControlAction action = actions.get(lastRunActionIndex);
        if (action instanceof StoredQueryAction) {
            return ((StoredQueryAction) action).storedQuery;
        }
        return null;
    }

    private int getStoredQueryActionIndex(final StoredQuery query) {
        for (int i = 0; i < actions.size(); i++) {
            final QueryControlAction action = actions.get(i);
            if (action instanceof StoredQueryAction) {
                final StoredQueryAction sqa = (StoredQueryAction) action;
                if (sqa.storedQuery.equals(query)) {
                    return i;
                }
            }
        }
        return -1;
    }

    private int getStoredQueryActionCount() {
        int count = 0;
        for (final Iterator<QueryControlAction> it = actions.iterator(); it.hasNext();) {
            if (it.next() instanceof StoredQueryAction) {
                ++count;
            }
        }
        return count;
    }

    private void removeLastStoredQueryAction() {
        for (int i = actions.size() - 1; i >= 0; i--) {
            if (actions.get(i) instanceof StoredQueryAction) {
                actions.remove(i);
                descriptions.remove(i);
                return;
            }
        }
    }

    private class WorkItemQuerySerializer implements ObjectSerializer {
        @Override
        public String toString(final Object object) {
            return ((StoredQuery) object).getQueryGUID().getGUIDString(GUIDStringFormat.NONE);
        }

        @Override
        public Object fromString(final String string) {
            try {
                return getWorkItemClient().getStoredQuery(new GUID(string));
            } catch (final UnauthorizedAccessException ex) {
                if (log.isTraceEnabled()) {
                    final String messageFormat = "UnauthorizedAccessException getting query [{0}]"; //$NON-NLS-1$
                    final String message = MessageFormat.format(messageFormat, string);
                    log.trace(message);
                }
                return null;
            }
        }
    }

    @Override
    public void setEnabled(final boolean enabled) {
        super.setEnabled(enabled);
        queryLabel.setEnabled(enabled);
        descriptions.setEnabled(enabled);
        selectQueryButton.setEnabled(enabled);
        refreshButton.setEnabled(enabled);
    }

    private void fireListeners(final Query query) {
        for (final Iterator<QuerySelectedListener> it = listeners.iterator(); it.hasNext();) {
            final QuerySelectedListener listener = it.next();
            listener.querySelected(query);
        }
    }

    private interface QueryControlAction {
        /**
         * Runs the action - this may provide UI if required.
         */
        public void run();

        /**
         * Refreshes to results of the previous run. Must not provide any
         * interactive results.
         */
        public void refresh();
    }

    private class StoredQueryAction implements QueryControlAction {
        private StoredQuery storedQuery;

        public StoredQueryAction(final StoredQuery storedQuery) {
            this.storedQuery = storedQuery;
        }

        public void updateAfterRefresh() {
            storedQuery = getProjectCollection().getClient().getStoredQuery(storedQuery.getQueryGUID());
        }

        @Override
        public void run() {
            final int ix = actions.indexOf(this);
            if (ix != 0) {
                actions.remove(ix);
                descriptions.remove(ix);

                actions.add(0, this);
                descriptions.add(QueryUtils.getExtendedDescription(storedQuery), 0);

                descriptions.select(0);
            }

            persistStoredQueries();

            try {
                fireListeners(storedQuery.createQuery(WorkItemQueryUtils.makeContext(
                    lastProjectForWiqlQuery == null ? storedQuery.getProject() : lastProjectForWiqlQuery,
                    WorkItemHelpers.getCurrentTeamName())));
            } catch (final InvalidQueryTextException ex) {
                final String messageFormat = Messages.getString("WorkItemQueryControl.QueryCannotBeRunFormat"); //$NON-NLS-1$
                final String message = MessageFormat.format(messageFormat, storedQuery.getName());

                MessageBoxHelpers.errorMessageBox(
                    getShell(),
                    Messages.getString("WorkItemQueryControl.UnableToRunDialogTitle"), //$NON-NLS-1$
                    message + NEWLINE + NEWLINE + ex.getMessage());

                fireListeners(getWorkItemClient().createEmptyQuery());
            }
        }

        @Override
        public void refresh() {
            /* Refreshing a stored query action simply runs the action again. */
            run();
        }
    }

    private class SelectQueryAction implements QueryControlAction {
        @Override
        public void run() {
            if (!openSelectQueryDialog()) {
                descriptions.select(lastRunActionIndex);
            }
        }

        @Override
        public void refresh() {
            /*
             * Noop - the select query action unfocuses itself as soon as it is
             * selected. (If the user selects a query from the dialog, the
             * selected query is selected in the combo - if the user cancels,
             * combo selection is returned to the previous selection.
             */
        }
    }

    private class EnterIDsAction implements QueryControlAction {
        @Override
        public void run() {
            final InputWorkItemIDDialog dlg = new InputWorkItemIDDialog(getShell(), lastIdsForIdsQuery);

            if (dlg.open() == IDialogConstants.OK_ID) {
                lastIdsForIdsQuery = dlg.getIDs();

                query();
            } else {
                descriptions.select(lastRunActionIndex);
            }
        }

        @Override
        public void refresh() {
            /* Simply requery the last queried IDs */
            query();
        }

        private void query() {
            final BatchReadParameterCollection batchReadParams = new BatchReadParameterCollection();
            for (int i = 0; i < lastIdsForIdsQuery.length; i++) {
                batchReadParams.add(new BatchReadParameter(lastIdsForIdsQuery[i]));
            }

            fireListeners(getWorkItemClient().createQuery("select [System.Id] from workitems", batchReadParams)); //$NON-NLS-1$
        }
    }

    private class EnterWIQLQuery implements QueryControlAction {
        @Override
        public void run() {
            final InputWIQLDialog dlg = new InputWIQLDialog(
                getShell(),
                server,
                getProjectCollection(),
                lastProjectForWiqlQuery,
                lastWiqlForWiqlQuery);

            if (dlg.open() == IDialogConstants.OK_ID) {
                lastWiqlForWiqlQuery = dlg.getWIQL();
                lastProjectForWiqlQuery = dlg.getSelectedProject();

                query();
            } else {
                descriptions.select(lastRunActionIndex);
            }
        }

        @Override
        public void refresh() {
            query();
        }

        private void query() {
            fireListeners(
                getWorkItemClient().createQuery(
                    lastWiqlForWiqlQuery,
                    WorkItemQueryUtils.makeContext(lastProjectForWiqlQuery, WorkItemHelpers.getCurrentTeamName())));
        }
    }

    private class WIQLSearchDialogAction implements QueryControlAction {
        private Query query;

        @Override
        public void run() {
            final WITSearchDialog dlg = new WITSearchDialog(getShell(), getWorkItemClient(), null);

            if (dlg.open() == IDialogConstants.OK_ID) {
                query = dlg.createQuery();
                fireListeners(query);
            } else {
                descriptions.select(lastRunActionIndex);
            }
        }

        @Override
        public void refresh() {
            fireListeners(query);
        }
    }

    private class EmptyQueryAction implements QueryControlAction {
        @Override
        public void run() {
            fireListeners(getWorkItemClient().createEmptyQuery());
        }

        @Override
        public void refresh() {
            /* Noop */
        }
    }

    private class SelectedItemsAction implements QueryControlAction {
        private final int[] ids;

        public SelectedItemsAction() {
            this(new int[0]);
        }

        public SelectedItemsAction(final int[] ids) {
            this.ids = ids;
        }

        @Override
        public void run() {
            query();
        }

        @Override
        public void refresh() {
            query();
        }

        private void query() {
            if (ids.length == 0) {
                return;
            }

            final BatchReadParameterCollection batchReadParams = new BatchReadParameterCollection();
            for (int i = 0; i < ids.length; i++) {
                batchReadParams.add(new BatchReadParameter(ids[i]));
            }

            fireListeners(getWorkItemClient().createQuery("select [System.Id] from workitems", batchReadParams)); //$NON-NLS-1$
        }
    }
}
