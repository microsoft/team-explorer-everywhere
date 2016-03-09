// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.wit.controls;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;

import com.microsoft.tfs.client.common.server.TFSServer;
import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.TFSCommonUIClientPlugin;
import com.microsoft.tfs.client.common.ui.framework.helper.UIHelpers;
import com.microsoft.tfs.client.common.ui.framework.image.ImageHelper;
import com.microsoft.tfs.client.common.ui.framework.tree.TreeContentProvider;
import com.microsoft.tfs.client.common.ui.framework.tree.TreeViewerDoubleClickListener;
import com.microsoft.tfs.core.clients.commonstructure.internal.ProjectInfoHelper;
import com.microsoft.tfs.core.clients.workitem.internal.project.ProjectImpl;
import com.microsoft.tfs.core.clients.workitem.internal.query.StoredQueryImpl;
import com.microsoft.tfs.core.clients.workitem.project.Project;
import com.microsoft.tfs.core.clients.workitem.query.QueryScope;
import com.microsoft.tfs.core.clients.workitem.query.StoredQuery;
import com.microsoft.tfs.core.clients.workitem.queryhierarchy.QueryDefinition;
import com.microsoft.tfs.core.clients.workitem.queryhierarchy.QueryFolder;
import com.microsoft.tfs.core.clients.workitem.queryhierarchy.QueryHierarchy;
import com.microsoft.tfs.core.clients.workitem.queryhierarchy.QueryItem;
import com.microsoft.tfs.core.clients.workitem.queryhierarchy.QueryItemType;
import com.microsoft.tfs.core.clients.workitem.queryhierarchy.QueryType;
import com.microsoft.tfs.util.GUID;

public class QueryItemTreeControl extends Composite {
    public static interface QueryItemDoubleClickedListener {
        public void queryItemDoubleClicked(QueryItem queryItem);
    }

    public static interface QueryItemSelectionListener {
        public void queryItemSelected(QueryItem queryItem);
    }

    /*
     * a reference to all the projects on the server
     */
    private final Project[] projects;

    /*
     * a sorted array of the names of the currently "active" projects, where
     * active means the user has added the project to team explorer
     */
    private final String[] activeProjectNames;

    /*
     * the tree viewer this composite is based around
     */
    private TreeViewer treeViewer;

    /*
     * used to track the currently selected query in the tree
     */
    private QueryItem selectedQueryItem;

    private final QueryItemType itemTypes;

    /*
     * listener set
     */
    private final Set<QueryItemDoubleClickedListener> queryDoubleClickListeners =
        new HashSet<QueryItemDoubleClickedListener>();
    private final Set<QueryItemSelectionListener> querySelectionListeners = new HashSet<QueryItemSelectionListener>();

    public QueryItemTreeControl(
        final Composite parent,
        final int style,
        final TFSServer server,
        final Project[] projects,
        final QueryItem initialQueryItem,
        final QueryItemType itemTypes) {
        this(
            parent,
            style,
            projects,
            ProjectInfoHelper.getProjectNames(server.getProjectCache().getActiveTeamProjects()),
            initialQueryItem,
            itemTypes);
    }

    public QueryItemTreeControl(
        final Composite parent,
        final int style,
        final Project[] projects,
        final String[] activeProjects,
        final QueryItem initialQueryItem,
        final QueryItemType itemTypes) {
        super(parent, style);
        this.projects = projects;
        selectedQueryItem = initialQueryItem;
        this.itemTypes = itemTypes;

        activeProjectNames = activeProjects;
        Arrays.sort(activeProjectNames);

        if (activeProjectNames.length > 0) {
            /*
             * set up the tree control in this composite
             */
            createUI();
        } else {
            createNoProjectsUI();
        }
    }

    public QueryItem getSelectedQueryItem() {
        return selectedQueryItem;
    }

    public void addQueryItemDoubleClickedListener(final QueryItemDoubleClickedListener listener) {
        synchronized (queryDoubleClickListeners) {
            queryDoubleClickListeners.add(listener);
        }
    }

    public void removeQueryItemDoubleClickedListener(final QueryItemDoubleClickedListener listener) {
        synchronized (queryDoubleClickListeners) {
            queryDoubleClickListeners.remove(listener);
        }
    }

    public void addQueryItemSelectionListener(final QueryItemSelectionListener listener) {
        synchronized (querySelectionListeners) {
            querySelectionListeners.add(listener);
        }
    }

    public void removeQueryItemSelectionListener(final QueryItemSelectionListener listener) {
        synchronized (querySelectionListeners) {
            querySelectionListeners.remove(listener);
        }
    }

    private void createUI() {
        setLayout(new FillLayout());

        treeViewer = new TreeViewer(this, SWT.BORDER);
        treeViewer.setContentProvider(new ContentProvider(activeProjectNames));
        treeViewer.setLabelProvider(new LabelProvider());
        treeViewer.addDoubleClickListener(new DoubleClickListener(treeViewer, queryDoubleClickListeners));
        treeViewer.addSelectionChangedListener(new SelectionChangedListener(querySelectionListeners));

        addContextMenu();

        treeViewer.setInput(projects);

        /*
         * set the initial selection if applicable
         */
        if (selectedQueryItem != null) {
            treeViewer.setSelection(new StructuredSelection(selectedQueryItem), true);
        }
    }

    private void createNoProjectsUI() {
        setLayout(new FillLayout());

        final Label label = new Label(this, SWT.WRAP);
        label.setText(Messages.getString("QueryItemTreeControl.NoTeamProjectsLabelText")); //$NON-NLS-1$
    }

    private void addContextMenu() {
        final MenuManager menuMgr = new MenuManager("#PopUp"); //$NON-NLS-1$
        final IAction copyToClipboardAction = new Action() {
            @Override
            public void run() {
                final IStructuredSelection selection = (IStructuredSelection) treeViewer.getSelection();
                final QueryDefinition queryDefinition = (QueryDefinition) selection.getFirstElement();
                UIHelpers.copyToClipboard(queryDefinition.getQueryText());
            }
        };
        copyToClipboardAction.setText(Messages.getString("QueryItemTreeControl.CopyWiqlToClipboard")); //$NON-NLS-1$
        copyToClipboardAction.setEnabled(false);
        menuMgr.add(copyToClipboardAction);

        treeViewer.getControl().setMenu(menuMgr.createContextMenu(treeViewer.getControl()));

        treeViewer.addSelectionChangedListener(new ISelectionChangedListener() {
            @Override
            public void selectionChanged(final SelectionChangedEvent event) {
                final IStructuredSelection selection = (IStructuredSelection) event.getSelection();
                final boolean enable = (selection.getFirstElement() instanceof QueryDefinition);
                copyToClipboardAction.setEnabled(enable);
            }
        });
    }

    private class SelectionChangedListener implements ISelectionChangedListener {
        private final Set<QueryItemSelectionListener> listeners;

        public SelectionChangedListener(final Set<QueryItemSelectionListener> listeners) {
            this.listeners = listeners;
        }

        @Override
        public void selectionChanged(final SelectionChangedEvent event) {
            final Object selected = ((IStructuredSelection) event.getSelection()).getFirstElement();
            if (selected instanceof QueryItem && itemTypes.contains(((QueryItem) selected).getType())) {
                selectedQueryItem = (QueryItem) selected;
            } else {
                selectedQueryItem = null;
            }

            synchronized (listeners) {
                for (final QueryItemSelectionListener listener : listeners) {
                    listener.queryItemSelected(selectedQueryItem);
                }
            }
        }
    }

    private static class DoubleClickListener extends TreeViewerDoubleClickListener {
        private final Set<QueryItemDoubleClickedListener> listeners;

        public DoubleClickListener(final TreeViewer treeViewer, final Set<QueryItemDoubleClickedListener> listeners) {
            super(treeViewer);
            this.listeners = listeners;
        }

        @Override
        public void doubleClick(final DoubleClickEvent event) {
            super.doubleClick(event);

            final Object element = ((IStructuredSelection) event.getSelection()).getFirstElement();
            if (element instanceof QueryDefinition) {
                final QueryDefinition queryDefinition = (QueryDefinition) element;
                synchronized (listeners) {
                    for (final QueryItemDoubleClickedListener listener : listeners) {
                        listener.queryItemDoubleClicked(queryDefinition);
                    }
                }
            }
        }
    }

    private class ContentProvider extends TreeContentProvider {
        private final String[] activeProjectNames;

        public ContentProvider(final String[] activeProjectNames) {
            this.activeProjectNames = activeProjectNames;
        }

        @Override
        public Object getParent(final Object element) {
            if (element instanceof QueryHierarchy) {
                return null;
            }

            return ((QueryItem) element).getParent();
        }

        @Override
        public Object[] getChildren(final Object parentElement) {
            final QueryItemType displayTypes = getDisplayTypes();

            if (parentElement instanceof QueryFolder) {
                final List<QueryItem> childList = new ArrayList<QueryItem>();
                final QueryItem[] children = ((QueryFolder) parentElement).getItems();

                for (final QueryItem child : children) {
                    if (displayTypes.contains(child.getType())) {
                        childList.add(child);
                    }
                }

                return childList.toArray(new QueryItem[childList.size()]);
            }

            return null;
        }

        @Override
        public boolean hasChildren(final Object element) {
            final QueryItemType displayTypes = getDisplayTypes();

            if (element instanceof QueryFolder) {
                final QueryItem[] children = ((QueryFolder) element).getItems();

                for (int i = 0; i < children.length; i++) {
                    if (displayTypes.contains(children[i].getType())) {
                        return true;
                    }
                }
            }

            return false;
        }

        private QueryItemType getDisplayTypes() {
            if (itemTypes.contains(QueryItemType.QUERY_DEFINITION)) {
                return QueryItemType.ALL;
            } else if (itemTypes.contains(QueryItemType.QUERY_FOLDER)) {
                return QueryItemType.ALL_FOLDERS;
            }

            return itemTypes;
        }

        @Override
        public Object[] getElements(final Object inputElement) {
            final Project[] projects = (Project[]) inputElement;
            final List<QueryHierarchy> queryHierarchies = new ArrayList<QueryHierarchy>();

            final Map<String, Project> availableProjects = new HashMap<String, Project>();
            for (final Project project : projects) {
                availableProjects.put(project.getName(), project);
            }

            for (final String activeProjectName : activeProjectNames) {
                final Project project = availableProjects.get(activeProjectName);
                if (project != null) {
                    queryHierarchies.add(project.getQueryHierarchy());
                }
            }

            return queryHierarchies.toArray(new QueryHierarchy[queryHierarchies.size()]);
        }
    }

    private static class LabelProvider extends org.eclipse.jface.viewers.LabelProvider {
        private final Map<QueryDefinition, StoredQuery> definitionToQueryMap =
            new HashMap<QueryDefinition, StoredQuery>();
        private final ImageHelper imageHelper = new ImageHelper(TFSCommonUIClientPlugin.PLUGIN_ID);

        public LabelProvider() {
        }

        @Override
        public Image getImage(final Object element) {
            if (element instanceof QueryHierarchy) {
                return imageHelper.getImage("images/common/team_project.gif"); //$NON-NLS-1$
            }
            if (element instanceof QueryFolder) {
                final QueryFolder queryFolder = (QueryFolder) element;

                if (GUID.EMPTY.getGUIDString().replaceAll("-", "").equals(queryFolder.getParent().getID())) //$NON-NLS-1$ //$NON-NLS-2$
                {
                    // This is a top level "Team Queries" / "My Queries" folder
                    if (queryFolder.isPersonal()) {
                        return imageHelper.getImage("images/wit/query_group_my.gif"); //$NON-NLS-1$
                    }
                    return imageHelper.getImage("images/wit/query_group_team.gif"); //$NON-NLS-1$
                }

                return PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJ_FOLDER);
            }
            if (element instanceof QueryDefinition) {
                final QueryDefinition queryDefinition = (QueryDefinition) element;

                StoredQuery query = definitionToQueryMap.get(queryDefinition);

                if (query == null) {
                    query = new StoredQueryImpl(
                        queryDefinition.getID(),
                        queryDefinition.getName(),
                        queryDefinition.getQueryText(),
                        queryDefinition.isPersonal() ? QueryScope.PRIVATE : QueryScope.PUBLIC,
                        queryDefinition.getProject().getID(),
                        (ProjectImpl) queryDefinition.getProject(),
                        queryDefinition.isDeleted(),
                        queryDefinition.getProject().getWITContext());

                    definitionToQueryMap.put(queryDefinition, query);
                }

                if (QueryType.LIST.equals(queryDefinition.getQueryType())) {
                    return imageHelper.getImage("images/wit/query_type_flat.gif"); //$NON-NLS-1$
                } else if (QueryType.TREE.equals(queryDefinition.getQueryType())) {
                    return imageHelper.getImage("images/wit/query_type_tree.gif"); //$NON-NLS-1$
                } else if (QueryType.ONE_HOP.equals(queryDefinition.getQueryType())) {
                    return imageHelper.getImage("images/wit/query_type_onehop.gif"); //$NON-NLS-1$
                }

                return imageHelper.getImage("images/wit/query_type_flat_error.gif"); //$NON-NLS-1$
            }

            return imageHelper.getImage("images/wit/query.gif"); //$NON-NLS-1$
        }

        @Override
        public String getText(final Object element) {
            return ((QueryItem) element).getName();
        }

        @Override
        public void dispose() {
            imageHelper.dispose();
        }
    }
}
