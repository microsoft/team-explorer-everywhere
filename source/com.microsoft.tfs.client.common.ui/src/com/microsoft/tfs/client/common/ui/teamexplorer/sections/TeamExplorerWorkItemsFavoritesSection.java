// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teamexplorer.sections;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.forms.widgets.FormToolkit;

import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.TFSCommonUIClientPlugin;
import com.microsoft.tfs.client.common.ui.TeamExplorerEventArg;
import com.microsoft.tfs.client.common.ui.framework.helper.ContentProviderAdapter;
import com.microsoft.tfs.client.common.ui.framework.helper.SWTUtil;
import com.microsoft.tfs.client.common.ui.framework.helper.UIHelpers;
import com.microsoft.tfs.client.common.ui.framework.image.ImageHelper;
import com.microsoft.tfs.client.common.ui.framework.layout.GridDataBuilder;
import com.microsoft.tfs.client.common.ui.teamexplorer.TeamExplorerContext;
import com.microsoft.tfs.client.common.ui.teamexplorer.TeamExplorerEventListener;
import com.microsoft.tfs.client.common.ui.teamexplorer.TeamExplorerEvents;
import com.microsoft.tfs.client.common.ui.teamexplorer.events.QueryItemEventArg;
import com.microsoft.tfs.client.common.ui.teamexplorer.favorites.QueryFavoriteItem;
import com.microsoft.tfs.client.common.ui.teamexplorer.helpers.WorkItemHelpers;
import com.microsoft.tfs.client.common.ui.teamexplorer.internal.TeamExplorerHelpers;
import com.microsoft.tfs.core.clients.favorites.FavoritesStoreFactory;
import com.microsoft.tfs.core.clients.favorites.IFavoritesStore;
import com.microsoft.tfs.core.clients.workitem.query.StoredQuery;
import com.microsoft.tfs.core.clients.workitem.queryhierarchy.QueryDefinition;
import com.microsoft.tfs.core.clients.workitem.queryhierarchy.QueryHierarchy;
import com.microsoft.tfs.core.clients.workitem.queryhierarchy.QueryItem;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.GUID;

public abstract class TeamExplorerWorkItemsFavoritesSection extends TeamExplorerBaseSection {
    ImageHelper imageHelper = new ImageHelper(TFSCommonUIClientPlugin.PLUGIN_ID);

    private final QueryRenameListener queryRenameListener = new QueryRenameListener();
    private final QueryFolderUpdatedListener queryFolderUpdatedListener = new QueryFolderUpdatedListener();

    protected QueryFavoriteItem[] favoriteItems;

    private Label emptyLabel;
    private TableViewer tableViewer;
    private TeamExplorerContext context;

    @Override
    public boolean initializeInBackground(final TeamExplorerContext context) {
        return true;
    }

    protected static IFavoritesStore getFavoritesStore(final TeamExplorerContext context, final boolean isPersonal) {
        return FavoritesStoreFactory.create(
            context.getServer().getConnection(),
            context.getCurrentProjectInfo(),
            context.getCurrentTeam(),
            QueryFavoriteItem.QUERY_DEFINITION_FEATURE_SCOPE,
            isPersonal);
    }

    protected abstract void addFavoritesChangedListener(final TeamExplorerContext context);

    protected abstract void removeFavoritesChangedListener(final TeamExplorerContext context);

    @Override
    public Composite getSectionContent(
        final FormToolkit toolkit,
        final Composite parent,
        final int style,
        final TeamExplorerContext context) {
        this.context = context;

        final Composite composite = toolkit.createComposite(parent);
        SWTUtil.gridLayout(composite, 1, true, 0, 0);

        if (!context.isConnected()) {
            createDisconnectedContent(toolkit, composite);
        } else {
            // Create the empty label, which is visible when the table is empty.
            final String emptyListText = Messages.getString("TeamExplorerMyFavoritesSection.AddLabelText"); //$NON-NLS-1$

            emptyLabel = toolkit.createLabel(composite, emptyListText);
            GridDataBuilder.newInstance().hAlignFill().hGrab().applyTo(emptyLabel);

            // Create the table viewer, which is visible when the table is
            // non-empty.
            createTableViewer(toolkit, composite, context);
            tableViewer.setInput(favoriteItems);
            GridDataBuilder.newInstance().align(SWT.FILL, SWT.FILL).grab(true, true).applyTo(tableViewer.getTable());

            // Show either the table or the 'empty label'.
            showOrHideTable(favoriteItems.length > 0);
        }

        addFavoritesChangedListener(context);
        context.getEvents().addListener(TeamExplorerEvents.QUERY_ITEM_RENAMED, queryRenameListener);
        context.getEvents().addListener(TeamExplorerEvents.QUERY_FOLDER_CHILDREN_UPDATED, queryFolderUpdatedListener);

        // Handle disposal of this control.
        composite.addDisposeListener(new DisposeListener() {
            @Override
            public void widgetDisposed(final DisposeEvent e) {
                removeFavoritesChangedListener(context);
                context.getEvents().removeListener(TeamExplorerEvents.QUERY_ITEM_RENAMED, queryRenameListener);
                context.getEvents().removeListener(
                    TeamExplorerEvents.QUERY_FOLDER_CHILDREN_UPDATED,
                    queryFolderUpdatedListener);
                imageHelper.dispose();
            }
        });

        return composite;
    }

    protected void refresh() {
        UIHelpers.runOnUIThread(true, new Runnable() {
            @Override
            public void run() {
                // Sanity check.
                if (tableViewer == null || tableViewer.getTable() == null || tableViewer.getTable().isDisposed()) {
                    return;
                }

                // Remove any favorites whose definitions have been deleted.
                removeOrphanedFavorites();

                // Reset the input items.
                tableViewer.setInput(favoriteItems);
                showOrHideTable(favoriteItems.length > 0);
                TeamExplorerHelpers.relayoutContainingScrolledComposite(tableViewer.getTable());
            }
        });
    }

    protected void refreshItem(final QueryFavoriteItem queryFavorite) {
        UIHelpers.runOnUIThread(true, new Runnable() {
            @Override
            public void run() {
                if (tableViewer == null || tableViewer.getTable() == null || tableViewer.getTable().isDisposed()) {
                    return;
                }

                tableViewer.refresh(queryFavorite);
            }
        });
    }

    private void createTableViewer(
        final FormToolkit toolkit,
        final Composite parent,
        final TeamExplorerContext context) {
        tableViewer = new TableViewer(parent, SWT.NO_SCROLL | SWT.MULTI | SWT.FULL_SELECTION);
        tableViewer.setContentProvider(new MyContentProvider());
        tableViewer.setLabelProvider(new MyLabelProvider());
        tableViewer.addDoubleClickListener(new MyDoubleClickListener());

        registerContextMenu(context, tableViewer.getControl(), tableViewer);
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

    private void removeOrphanedFavorites() {
        // Nothing to do if there are no favorites.
        if (favoriteItems == null || favoriteItems.length == 0) {
            return;
        }

        // Get the query hierarchy from the context.
        final QueryHierarchy hierarchy = context.getCurrentProject().getQueryHierarchy();
        if (hierarchy == null) {
            return;
        }

        // Keep a map of IDs of query definitions of favorite items that have
        // been orphaned (their corresponding query definitions have been
        // removed).
        final Set<GUID> toDelete = new HashSet<GUID>();

        // Scan all favorites to see if their associated query definitions
        // still exist.
        for (final QueryFavoriteItem favoriteItem : favoriteItems) {
            final QueryDefinition favoriteDefinition = favoriteItem.getQueryDefinition();

            // See if the query still exists in the hierarchy.
            if (hierarchy.find(favoriteDefinition.getID()) == null) {
                toDelete.add(favoriteDefinition.getID());
            }
        }

        // Bail if there are none to delete.
        if (toDelete.size() == 0) {
            return;
        }

        // Remove the deleted items from favorites.
        final List<QueryFavoriteItem> list = new ArrayList<QueryFavoriteItem>();
        for (final QueryFavoriteItem favoriteItem : favoriteItems) {
            if (!toDelete.contains(favoriteItem.getQueryDefinition().getID())) {
                list.add(favoriteItem);
            }
        }

        // Reset the favorites to the reduced list
        favoriteItems = list.toArray(new QueryFavoriteItem[list.size()]);
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
            if (element instanceof QueryFavoriteItem && columnIndex == 0) {
                final QueryDefinition queryDefinition = ((QueryFavoriteItem) element).getQueryDefinition();
                return WorkItemHelpers.getImageForWorkItemQueryType(imageHelper, queryDefinition.getQueryType());
            }
            return null;
        }

        @Override
        public String getColumnText(final Object element, final int columnIndex) {
            if (element instanceof QueryFavoriteItem && columnIndex == 0) {
                final QueryFavoriteItem queryFavoriteItem = (QueryFavoriteItem) element;
                return queryFavoriteItem.getQueryDefinition().getName();
            }
            return null;
        }
    }

    private class MyDoubleClickListener implements IDoubleClickListener {
        @Override
        public void doubleClick(final DoubleClickEvent event) {
            final IStructuredSelection selection = (IStructuredSelection) event.getSelection();
            final Object element = selection.getFirstElement();

            if (element instanceof QueryFavoriteItem) {
                final Shell shell = event.getViewer().getControl().getShell();
                final QueryFavoriteItem favoriteItem = (QueryFavoriteItem) element;
                final QueryDefinition queryDefinition = favoriteItem.getQueryDefinition();
                final StoredQuery storedQuery = WorkItemHelpers.createStoredQueryFromDefinition(queryDefinition);

                WorkItemHelpers.runQuery(shell, context.getServer(), context.getCurrentProject(), storedQuery);
            }
        }
    }

    private class QueryRenameListener implements TeamExplorerEventListener {
        @Override
        public void onEvent(final TeamExplorerEventArg arg) {
            Check.isTrue(arg instanceof QueryItemEventArg, "arg instanceof QueryItemEventArg"); //$NON-NLS-1$

            final QueryItem queryItem = ((QueryItemEventArg) arg).getQueryItem();
            for (final QueryFavoriteItem queryFavorite : favoriteItems) {
                final QueryItem favoriteItem = queryFavorite.getQueryDefinition();
                if (favoriteItem.getID().equals(queryItem.getID())) {
                    refreshItem(queryFavorite);
                    return;
                }
            }
        }
    }

    private class QueryFolderUpdatedListener implements TeamExplorerEventListener {
        @Override
        public void onEvent(final TeamExplorerEventArg arg) {
            refresh();
        }
    }
}
