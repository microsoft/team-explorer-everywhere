// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teamexplorer.sections;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.forms.widgets.FormToolkit;

import com.microsoft.tfs.client.common.ui.TFSCommonUIClientPlugin;
import com.microsoft.tfs.client.common.ui.TeamExplorerEventArg;
import com.microsoft.tfs.client.common.ui.framework.helper.SWTUtil;
import com.microsoft.tfs.client.common.ui.framework.image.ImageHelper;
import com.microsoft.tfs.client.common.ui.framework.layout.GridDataBuilder;
import com.microsoft.tfs.client.common.ui.framework.tree.TreeContentProvider;
import com.microsoft.tfs.client.common.ui.helpers.AutomationIDHelper;
import com.microsoft.tfs.client.common.ui.teamexplorer.TeamExplorerContext;
import com.microsoft.tfs.client.common.ui.teamexplorer.TeamExplorerEventListener;
import com.microsoft.tfs.client.common.ui.teamexplorer.TeamExplorerEvents;
import com.microsoft.tfs.client.common.ui.teamexplorer.events.QueryFolderEventArg;
import com.microsoft.tfs.client.common.ui.teamexplorer.events.QueryItemEventArg;
import com.microsoft.tfs.client.common.ui.teamexplorer.helpers.WorkItemHelpers;
import com.microsoft.tfs.core.clients.workitem.project.Project;
import com.microsoft.tfs.core.clients.workitem.query.StoredQuery;
import com.microsoft.tfs.core.clients.workitem.queryhierarchy.QueryDefinition;
import com.microsoft.tfs.core.clients.workitem.queryhierarchy.QueryFolder;
import com.microsoft.tfs.core.clients.workitem.queryhierarchy.QueryHierarchy;
import com.microsoft.tfs.core.clients.workitem.queryhierarchy.QueryType;
import com.microsoft.tfs.util.Check;

public class TeamExplorerWorkItemsQueriesSection extends TeamExplorerBaseSection {

    public static final String QUERIES_TREE_ID =
        "com.microsoft.tfs.client.common.ui.teamexplorer.sections.TeamExplorerWorkItemsQueriesSection.QueriesTree"; //$NON-NLS-1$

    ImageHelper imageHelper = new ImageHelper(TFSCommonUIClientPlugin.PLUGIN_ID);

    private TreeViewer treeViewer;
    private QueryHierarchy queryHierarchy;
    private TeamExplorerWorkItemsQueriesState state;

    private final QueryItemUpdatedListener queryItemUpdatedListener = new QueryItemUpdatedListener();
    private final QueryItemDeletedListener queryItemDeletedListener = new QueryItemDeletedListener();
    private final QueryFolderContentChangedListener queryFolderContentChangedListener =
        new QueryFolderContentChangedListener();

    @Override
    public boolean initializeInBackground(final TeamExplorerContext context) {
        return true;
    }

    @Override
    public void initialize(final IProgressMonitor monitor, final TeamExplorerContext context) {
        final Project project = context.getCurrentProject();
        if (project == null) {
            return;
        }

        queryHierarchy = project.getQueryHierarchy();
    }

    @Override
    public void initialize(final IProgressMonitor monitor, final TeamExplorerContext context, final Object state) {
        initialize(monitor, context);
        if (state instanceof TeamExplorerWorkItemsQueriesState) {
            this.state = (TeamExplorerWorkItemsQueriesState) state;
        }
    }

    @Override
    public Composite getSectionContent(
        final FormToolkit toolkit,
        final Composite parent,
        final int style,
        final TeamExplorerContext context) {
        final Composite composite = toolkit.createComposite(parent);

        // Form-style border painting not enabled (0 pixel margins OK) because
        // no applicable controls in this composite
        SWTUtil.gridLayout(composite, 1, true, 0, 5);

        if (context.isConnected()) {
            treeViewer = new TreeViewer(composite, SWT.MULTI | SWT.NO_SCROLL);
            treeViewer.setContentProvider(new QueryContentProvider());
            treeViewer.setLabelProvider(new QueryLabelProvider());
            treeViewer.addDoubleClickListener(new QueryDoubleClickListener(context));
            treeViewer.addTreeListener(new SectionTreeViewerListener());

            if (state == null) {
                treeViewer.setAutoExpandLevel(2);
                treeViewer.setInput(queryHierarchy);
            } else {
                treeViewer.setInput(queryHierarchy);
                restoreState();
            }

            AutomationIDHelper.setWidgetID(treeViewer.getTree(), QUERIES_TREE_ID);

            GridDataBuilder.newInstance().align(SWT.FILL, SWT.FILL).grab(true, true).applyTo(treeViewer.getControl());

            registerContextMenu(context, treeViewer.getControl(), treeViewer);
        } else {
            createDisconnectedContent(toolkit, composite);
        }

        context.getEvents().addListener(TeamExplorerEvents.QUERY_ITEM_UPDATED, queryItemUpdatedListener);
        context.getEvents().addListener(TeamExplorerEvents.QUERY_ITEM_DELETED, queryItemDeletedListener);
        context.getEvents().addListener(
            TeamExplorerEvents.QUERY_FOLDER_CHILDREN_UPDATED,
            queryFolderContentChangedListener);

        composite.addDisposeListener(new DisposeListener() {
            @Override
            public void widgetDisposed(final DisposeEvent e) {
                imageHelper.dispose();
                context.getEvents().removeListener(TeamExplorerEvents.QUERY_ITEM_UPDATED, queryItemUpdatedListener);
                context.getEvents().removeListener(TeamExplorerEvents.QUERY_ITEM_DELETED, queryItemDeletedListener);
                context.getEvents().removeListener(
                    TeamExplorerEvents.QUERY_FOLDER_CHILDREN_UPDATED,
                    queryFolderContentChangedListener);
            }
        });

        return composite;
    }

    @Override
    public Object saveState() {
        if (state == null) {
            state = new TeamExplorerWorkItemsQueriesState(treeViewer);
        } else {
            state.updateTreeState(treeViewer);
        }
        return state;
    }

    private void restoreState() {
        if (treeViewer == null) {
            return;
        }
        if (state == null) {
            treeViewer.expandAll();
        } else {
            state.restoreState(treeViewer);
        }
    }

    private class QueryContentProvider extends TreeContentProvider {
        @Override
        public Object[] getElements(final Object inputElement) {
            Check.isTrue(inputElement instanceof QueryHierarchy, "inputElement instanceof QueryHierarchy"); //$NON-NLS-1$
            return ((QueryHierarchy) inputElement).getItems();
        }

        @Override
        public Object[] getChildren(final Object parentElement) {
            if (parentElement instanceof QueryFolder) {
                return ((QueryFolder) parentElement).getItems();
            } else {
                throw new IllegalArgumentException("parentElement instanceof Query item"); //$NON-NLS-1$
            }
        }

        @Override
        public boolean hasChildren(final Object element) {
            if (element instanceof QueryFolder) {
                return ((QueryFolder) element).getItems().length > 0;
            } else if (element instanceof QueryDefinition) {
                return false;
            } else {
                throw new IllegalArgumentException("element instanceof Query item"); //$NON-NLS-1$
            }
        }
    }

    private class QueryLabelProvider extends LabelProvider {
        @Override
        public String getText(final Object element) {
            if (element instanceof QueryFolder) {
                return ((QueryFolder) element).getName();
            } else if (element instanceof QueryDefinition) {
                return ((QueryDefinition) element).getName();
            } else {
                throw new IllegalArgumentException("element instanceof Query item"); //$NON-NLS-1$
            }
        }

        @Override
        public Image getImage(final Object element) {
            if (element instanceof QueryFolder) {
                final QueryFolder queryFolder = (QueryFolder) element;
                return WorkItemHelpers.getImageForWorkItemQueryFolder(imageHelper, queryFolder);
            } else if (element instanceof QueryDefinition) {
                final QueryType type = ((QueryDefinition) element).getQueryType();
                return WorkItemHelpers.getImageForWorkItemQueryType(imageHelper, type);
            } else {
                throw new IllegalArgumentException("element instanceof Query item"); //$NON-NLS-1$
            }
        }
    }

    private class QueryDoubleClickListener implements IDoubleClickListener {
        private final TeamExplorerContext context;

        public QueryDoubleClickListener(final TeamExplorerContext context) {
            this.context = context;
        }

        @Override
        public void doubleClick(final DoubleClickEvent event) {
            final IStructuredSelection selection = (IStructuredSelection) event.getSelection();
            final Object element = selection.getFirstElement();

            if (element instanceof QueryFolder) {
                final boolean expanded = treeViewer.getExpandedState(element);
                treeViewer.setExpandedState(element, !expanded);
            } else if (element instanceof QueryDefinition) {
                final Shell shell = treeViewer.getControl().getShell();
                final QueryDefinition queryDefinition = (QueryDefinition) element;
                final StoredQuery storedQuery = WorkItemHelpers.createStoredQueryFromDefinition(queryDefinition);

                WorkItemHelpers.runQuery(shell, context.getServer(), context.getCurrentProject(), storedQuery);
            }
        }
    }

    private class QueryFolderContentChangedListener implements TeamExplorerEventListener {
        @Override
        public void onEvent(final TeamExplorerEventArg arg) {
            Check.isTrue(arg instanceof QueryFolderEventArg, "arg instanceof QueryFolderEventArg"); //$NON-NLS-1$

            final QueryFolderEventArg queryFolderArg = (QueryFolderEventArg) arg;
            treeViewer.refresh(queryFolderArg.getQueryFolder());
        }
    }

    private class QueryItemUpdatedListener implements TeamExplorerEventListener {
        @Override
        public void onEvent(final TeamExplorerEventArg arg) {
            Check.isTrue(arg instanceof QueryItemEventArg, "arg instanceof QueryItemEventArg"); //$NON-NLS-1$

            final QueryItemEventArg queryItemArg = (QueryItemEventArg) arg;
            treeViewer.refresh(queryItemArg.getQueryItem());
        }
    }

    private class QueryItemDeletedListener implements TeamExplorerEventListener {
        @Override
        public void onEvent(final TeamExplorerEventArg arg) {
            Check.isTrue(arg instanceof QueryItemEventArg, "arg instanceof QueryItemEventArg"); //$NON-NLS-1$

            final QueryItemEventArg queryItemArg = (QueryItemEventArg) arg;
            treeViewer.remove(queryItemArg.getQueryItem());
        }
    }
}
