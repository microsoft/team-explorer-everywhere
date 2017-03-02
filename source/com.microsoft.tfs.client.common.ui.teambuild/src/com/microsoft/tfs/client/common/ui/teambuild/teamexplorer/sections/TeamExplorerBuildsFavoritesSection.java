// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teambuild.teamexplorer.sections;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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
import org.eclipse.ui.forms.widgets.FormToolkit;

import com.microsoft.alm.client.TeeClientHandler;
import com.microsoft.alm.teamfoundation.build.webapi.BuildDefinitionReference;
import com.microsoft.alm.teamfoundation.build.webapi.BuildHttpClient;
import com.microsoft.alm.teamfoundation.build.webapi.DefinitionReference;
import com.microsoft.alm.teamfoundation.build.webapi.DefinitionType;
import com.microsoft.tfs.client.common.ui.TeamExplorerEventArg;
import com.microsoft.tfs.client.common.ui.framework.helper.ContentProviderAdapter;
import com.microsoft.tfs.client.common.ui.framework.helper.SWTUtil;
import com.microsoft.tfs.client.common.ui.framework.helper.UIHelpers;
import com.microsoft.tfs.client.common.ui.framework.layout.GridDataBuilder;
import com.microsoft.tfs.client.common.ui.tasks.OpenBuildDefinitionVNextTask;
import com.microsoft.tfs.client.common.ui.teambuild.Messages;
import com.microsoft.tfs.client.common.ui.teambuild.TeamBuildImageHelper;
import com.microsoft.tfs.client.common.ui.teambuild.teamexplorer.events.BuildDefinitionEventArg;
import com.microsoft.tfs.client.common.ui.teambuild.teamexplorer.favorites.BuildFavoriteItem;
import com.microsoft.tfs.client.common.ui.teambuild.teamexplorer.helpers.BuildHelpers;
import com.microsoft.tfs.client.common.ui.teamexplorer.TeamExplorerContext;
import com.microsoft.tfs.client.common.ui.teamexplorer.TeamExplorerEventListener;
import com.microsoft.tfs.client.common.ui.teamexplorer.TeamExplorerEvents;
import com.microsoft.tfs.client.common.ui.teamexplorer.internal.TeamExplorerHelpers;
import com.microsoft.tfs.client.common.ui.teamexplorer.sections.TeamExplorerBaseSection;
import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.core.clients.build.IBuildDefinition;
import com.microsoft.tfs.core.clients.favorites.FavoritesStoreFactory;
import com.microsoft.tfs.core.clients.favorites.IFavoritesStore;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.GUID;

public abstract class TeamExplorerBuildsFavoritesSection extends TeamExplorerBaseSection {
    protected TeamExplorerContext context;
    TeamBuildImageHelper imageHelper = new TeamBuildImageHelper();

    protected BuildFavoriteItem[] favoriteItems;
    final protected Map<String, DefinitionReference> definitions = new HashMap<String, DefinitionReference>();

    private Label emptyLabel;
    private TableViewer tableViewer;

    private final BuildDefinitionChangedListener buildDefinitionChangedListener = new BuildDefinitionChangedListener();
    private final BuildDefinitionDeletedListener buildDefinitionDeletedListener = new BuildDefinitionDeletedListener();

    @Override
    public boolean initializeInBackground(final TeamExplorerContext context) {
        return true;
    }

    protected static IFavoritesStore getFavoritesStore(final TeamExplorerContext context, final boolean isPersonal) {
        if (context.getServer() == null || context.getServer().getConnection() == null) {
            return null;
        }
        return FavoritesStoreFactory.create(
            context.getServer().getConnection(),
            context.getCurrentProjectInfo(),
            context.getCurrentTeam(),
            BuildFavoriteItem.BUILD_DEFINITION_FEATURE_SCOPE,
            isPersonal);
    }

    protected void loadDefinitions(final TeamExplorerContext context) {
        definitions.clear();

        if (!context.isConnected() || context.getServer() == null || !BuildHelpers.isBuildVNextSupported(context)) {
            return;
        }

        final TFSTeamProjectCollection connection = context.getServer().getConnection();
        final BuildHttpClient buildClient =
            new BuildHttpClient(new TeeClientHandler(connection.getHTTPClient()), connection.getBaseURI());

        final UUID projectId = UUID.fromString(context.getCurrentProjectInfo().getGUID());
        final List<BuildDefinitionReference> rawDefinitions = buildClient.getDefinitions(projectId);

        for (final DefinitionReference definition : rawDefinitions) {
            definitions.put(definition.getUri().toString(), definition);
        }

        return;
    }

    protected abstract void loadFavorites(final TeamExplorerContext context);

    protected abstract void addFavoritesChangedListener(final TeamExplorerContext context);

    protected abstract void removeFavoritesChangedListener(final TeamExplorerContext context);

    @Override
    public Composite getSectionContent(
        final FormToolkit toolkit,
        final Composite parent,
        final int style,
        final TeamExplorerContext context) {
        final Composite composite = toolkit.createComposite(parent);

        if (!context.isConnected() || context.getServer() == null) {
            SWTUtil.gridLayout(composite, 1, true, 1, 5);
            createDisconnectedContent(toolkit, composite);
            return composite;
        } else {
            SWTUtil.gridLayout(composite, 1, true, 0, 5);
            // Create the empty label, which is visible when the table is empty.
            final String emptyListText =
                Messages.getString("TeamExplorerBuildsFavoritesSection.BuildFavoriteWatermark"); //$NON-NLS-1$

            emptyLabel = toolkit.createLabel(composite, emptyListText);
            GridDataBuilder.newInstance().hAlignFill().hGrab().applyTo(emptyLabel);

            // Create the table viewer, which is visible when the table is
            // non-empty.
            createTableViewer(toolkit, composite, context);
            tableViewer.setInput(favoriteItems);
            GridDataBuilder.newInstance().align(SWT.FILL, SWT.FILL).grab(true, true).applyTo(tableViewer.getTable());

            // Show either the table or the 'empty label'.
            showOrHideTable(favoriteItems.length > 0);

            addFavoritesChangedListener(context);

            composite.addDisposeListener(new DisposeListener() {
                @Override
                public void widgetDisposed(final DisposeEvent e) {
                    removeFavoritesChangedListener(context);
                }
            });
        }

        context.getEvents().addListener(TeamExplorerEvents.BUILD_DEFINITION_CHANGED, buildDefinitionChangedListener);
        context.getEvents().addListener(TeamExplorerEvents.BUILD_DEFINITION_DELETED, buildDefinitionDeletedListener);

        // Handle disposal of this control.
        composite.addDisposeListener(new DisposeListener() {
            @Override
            public void widgetDisposed(final DisposeEvent e) {
                imageHelper.dispose();

                context.getEvents().removeListener(
                    TeamExplorerEvents.BUILD_DEFINITION_CHANGED,
                    buildDefinitionChangedListener);
                context.getEvents().removeListener(
                    TeamExplorerEvents.BUILD_DEFINITION_DELETED,
                    buildDefinitionDeletedListener);
            }
        });

        return composite;
    }

    protected void refresh() {
        UIHelpers.runOnUIThread(false, new Runnable() {
            @Override
            public void run() {
                tableViewer.setInput(favoriteItems);
                showOrHideTable(favoriteItems.length > 0);
                TeamExplorerHelpers.relayoutContainingScrolledComposite(tableViewer.getTable());
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

    private boolean removeFromFavorites(final BuildFavoriteItem itemToRemove) {
        if (itemToRemove == null || favoriteItems == null) {
            return false;
        }

        boolean removed = false;
        final GUID idToRemove = itemToRemove.getFavoriteItem().getID();

        final List<BuildFavoriteItem> list = new ArrayList<BuildFavoriteItem>();
        for (final BuildFavoriteItem favoriteItem : favoriteItems) {
            if (!favoriteItem.getFavoriteItem().getID().equals(idToRemove)) {
                list.add(favoriteItem);
            } else {
                removed = true;
            }
        }

        if (removed) {
            favoriteItems = list.toArray(new BuildFavoriteItem[list.size()]);
        }

        return removed;
    }

    private BuildFavoriteItem getFavoriteForDefinition(final IBuildDefinition buildDefinition) {
        for (final BuildFavoriteItem favoriteItem : favoriteItems) {
            if (favoriteItem.getBuildDefinitionUri().equals(buildDefinition.getURI())) {
                return favoriteItem;
            }
        }
        return null;
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
            if (element instanceof BuildFavoriteItem && columnIndex == 0) {
                final BuildFavoriteItem favorite = (BuildFavoriteItem) element;
                if (favorite.getBuildDefinitionType() == DefinitionType.XAML) {
                    return imageHelper.getBuildDefinitionImage((IBuildDefinition) favorite.getBuildDefinition());
                } else {
                    return imageHelper.getBuildDefinitionImage(
                        (BuildDefinitionReference) favorite.getBuildDefinition());
                }
            }
            return null;
        }

        @Override
        public String getColumnText(final Object element, final int columnIndex) {
            if (element instanceof BuildFavoriteItem && columnIndex == 0) {
                final BuildFavoriteItem favorite = (BuildFavoriteItem) element;
                return favorite.getBuildDefinitionName();
            }
            return null;
        }
    }

    private class MyDoubleClickListener implements IDoubleClickListener {
        @Override
        public void doubleClick(final DoubleClickEvent event) {
            final IStructuredSelection selection = (IStructuredSelection) event.getSelection();
            final Object element = selection.getFirstElement();

            if (element instanceof BuildFavoriteItem) {
                final BuildFavoriteItem favorite = (BuildFavoriteItem) element;

                if (favorite.getBuildDefinitionType() == DefinitionType.XAML) {
                    BuildHelpers.viewTodaysBuildsForDefinition((IBuildDefinition) favorite.getBuildDefinition());
                } else {
                    new OpenBuildDefinitionVNextTask(
                        context.getWorkbenchPart().getSite().getShell(),
                        context.getServer().getConnection(),
                        (BuildDefinitionReference) favorite.getBuildDefinition()).run();
                }
            }
        }
    }

    private class BuildDefinitionChangedListener implements TeamExplorerEventListener {
        @Override
        public void onEvent(final TeamExplorerEventArg arg) {
            Check.isTrue(arg instanceof BuildDefinitionEventArg, "arg instanceof BuildDefinitionEventArg"); //$NON-NLS-1$
            final BuildFavoriteItem item =
                getFavoriteForDefinition(((BuildDefinitionEventArg) arg).getBuildDefinition());

            if (item != null) {
                item.refresh();
                tableViewer.refresh(item);
            }
        }
    }

    private class BuildDefinitionDeletedListener implements TeamExplorerEventListener {
        @Override
        public void onEvent(final TeamExplorerEventArg arg) {
            Check.isTrue(arg instanceof BuildDefinitionEventArg, "arg instanceof BuildDefinitionEventArg"); //$NON-NLS-1$
            final BuildFavoriteItem item =
                getFavoriteForDefinition(((BuildDefinitionEventArg) arg).getBuildDefinition());

            if (item != null) {
                final boolean removed = removeFromFavorites(item);
                if (removed) {
                    refresh();
                }
            }
        }
    }
}
