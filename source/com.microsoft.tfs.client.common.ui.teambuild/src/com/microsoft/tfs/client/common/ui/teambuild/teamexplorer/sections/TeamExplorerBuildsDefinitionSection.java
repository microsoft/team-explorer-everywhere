// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teambuild.teamexplorer.sections;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.eclipse.ui.forms.widgets.ImageHyperlink;

import com.microsoft.tfs.client.common.codemarker.CodeMarker;
import com.microsoft.tfs.client.common.codemarker.CodeMarkerDispatch;
import com.microsoft.tfs.client.common.server.TFSServer;
import com.microsoft.tfs.client.common.ui.TeamExplorerEventArg;
import com.microsoft.tfs.client.common.ui.framework.helper.ContentProviderAdapter;
import com.microsoft.tfs.client.common.ui.framework.helper.SWTUtil;
import com.microsoft.tfs.client.common.ui.framework.layout.GridDataBuilder;
import com.microsoft.tfs.client.common.ui.helpers.AutomationIDHelper;
import com.microsoft.tfs.client.common.ui.teambuild.Messages;
import com.microsoft.tfs.client.common.ui.teambuild.TeamBuildImageHelper;
import com.microsoft.tfs.client.common.ui.teambuild.editors.BuildExplorer;
import com.microsoft.tfs.client.common.ui.teambuild.teamexplorer.events.BuildDefinitionEventArg;
import com.microsoft.tfs.client.common.ui.teambuild.teamexplorer.helpers.BuildHelpers;
import com.microsoft.tfs.client.common.ui.teamexplorer.TeamExplorerContext;
import com.microsoft.tfs.client.common.ui.teamexplorer.TeamExplorerEventListener;
import com.microsoft.tfs.client.common.ui.teamexplorer.TeamExplorerEvents;
import com.microsoft.tfs.client.common.ui.teamexplorer.helpers.PageHelpers;
import com.microsoft.tfs.client.common.ui.teamexplorer.sections.TeamExplorerBaseSection;
import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.core.clients.build.IBuildDefinition;
import com.microsoft.tfs.core.clients.build.IBuildServer;
import com.microsoft.tfs.core.clients.build.internal.TeamBuildCache;
import com.microsoft.tfs.core.clients.build.utils.BuildPath;
import com.microsoft.tfs.core.clients.commonstructure.ProjectInfo;
import com.microsoft.tfs.util.Check;

public class TeamExplorerBuildsDefinitionSection extends TeamExplorerBaseSection {
    public static final String BUILD_DEFINITIONS_TABLE_ID =
        "com.microsoft.tfs.client.common.ui.teambuild.teamexplorer.sections.BuildDefinitionsTable"; //$NON-NLS-1$

    public static final CodeMarker BUILD_DEFINITIONS_LOADED = new CodeMarker(
        "com.microsoft.tfs.client.common.ui.teambuild.teamexplorer.sections.TeamExplorerBuildsDefinitionSection#buildDefinitionsLoaded"); //$NON-NLS-1$

    public static final CodeMarker BUILD_DEFINITION_DBLCLICKED = new CodeMarker(
        "com.microsoft.tfs.client.common.ui.teambuild.teamexplorer.sections.TeamExplorerBuildsDefinitionSection#buildDefinitiondblClicked"); //$NON-NLS-1$

    private final String FILTER_WATERMARK =
        Messages.getString("TeamExplorerBuildsDefinitionSection.FilterBoxWatermarkText"); //$NON-NLS-1$

    private final TeamBuildImageHelper imageHelper = new TeamBuildImageHelper();

    private TeamExplorerContext context;
    private IBuildDefinition[] buildDefinitions;
    private IBuildDefinition[] filteredBuildDefinitions;

    private final BuildDefinitionAddedListener buildDefinitionAddedListener = new BuildDefinitionAddedListener();
    private final BuildDefinitionChangedListener buildDefinitionChangedListener = new BuildDefinitionChangedListener();
    private final BuildDefinitionDeletedListener buildDefinitionDeletedListener = new BuildDefinitionDeletedListener();

    private Text textFilter;
    private TableViewer tableViewer;

    @Override
    public boolean initializeInBackground(final TeamExplorerContext context) {
        return true;
    }

    @Override
    public void initialize(final IProgressMonitor monitor, final TeamExplorerContext context) {
        this.context = context;
        getBuildDefinitions(context, false);

        CodeMarkerDispatch.dispatch(BUILD_DEFINITIONS_LOADED);
    }

    @Override
    public Composite getSectionContent(
        final FormToolkit toolkit,
        final Composite parent,
        final int style,
        final TeamExplorerContext context) {
        final Composite composite = toolkit.createComposite(parent);

        // Text controls present in this composite, enable form-style borders,
        // must have at least 1 pixel margins
        toolkit.paintBordersFor(composite);
        SWTUtil.gridLayout(composite, 3, false, 1, 5);

        if (!context.isConnected()) {
            createDisconnectedContent(toolkit, composite);
            return composite;
        } else {
            final String linkText = Messages.getString("TeamExplorerBuildPage.NewBuildDefLinkText"); //$NON-NLS-1$
            final Hyperlink link = toolkit.createHyperlink(composite, linkText, SWT.WRAP);
            link.setUnderlined(false);
            link.setEnabled(context.isConnected());
            link.addHyperlinkListener(new HyperlinkAdapter() {
                @Override
                public void linkActivated(final HyperlinkEvent e) {
                    final boolean created = BuildHelpers.newBuildDefinition(composite.getShell(), context);
                    if (created) {
                        context.getEvents().notifyListener(TeamExplorerEvents.BUILD_DEFINITION_ADDED);
                    }
                }
            });

            GridDataBuilder.newInstance().applyTo(link);

            final Label separator = toolkit.createLabel(composite, "|", SWT.VERTICAL); //$NON-NLS-1$
            GridDataBuilder.newInstance().vFill().applyTo(separator);

            final String text = Messages.getString("TeamExplorerBuildPage.ActionsMenuText"); //$NON-NLS-1$
            final Menu menu = createActionMenu(composite.getShell(), context);

            // This could happen when server still disconnected
            if (composite != null && !composite.isDisposed()) {
                final ImageHyperlink actionsLink = PageHelpers.createDropHyperlink(toolkit, composite, text, menu);
                GridDataBuilder.newInstance().applyTo(actionsLink);
            }

            textFilter = toolkit.createText(composite, ""); //$NON-NLS-1$
            textFilter.setMessage(FILTER_WATERMARK);
            textFilter.addModifyListener(new FilterModifiedListener());
            GridDataBuilder.newInstance().hAlignFill().hSpan(3).hGrab().applyTo(textFilter);

            tableViewer = new TableViewer(composite, SWT.H_SCROLL | SWT.V_SCROLL | SWT.MULTI | SWT.FULL_SELECTION);
            tableViewer.getTable().setData(FormToolkit.KEY_DRAW_BORDER, Boolean.FALSE);
            tableViewer.setContentProvider(new BuildDefinitionsContentProvider());
            tableViewer.setLabelProvider(new BuildDefinitionsLabelProvider());
            tableViewer.addDoubleClickListener(new BuildDefinitionDoubleClickListener());
            tableViewer.setInput(filteredBuildDefinitions);
            AutomationIDHelper.setWidgetID(tableViewer, BUILD_DEFINITIONS_TABLE_ID);
            GridDataBuilder.newInstance().align(SWT.FILL, SWT.FILL).hSpan(3).grab(true, true).applyTo(
                tableViewer.getControl());

            registerContextMenu(context, tableViewer.getControl(), tableViewer);
        }

        context.getEvents().addListener(TeamExplorerEvents.BUILD_DEFINITION_ADDED, buildDefinitionAddedListener);
        context.getEvents().addListener(TeamExplorerEvents.BUILD_DEFINITION_CHANGED, buildDefinitionChangedListener);
        context.getEvents().addListener(TeamExplorerEvents.BUILD_DEFINITION_DELETED, buildDefinitionDeletedListener);

        composite.addDisposeListener(new DisposeListener() {
            @Override
            public void widgetDisposed(final DisposeEvent e) {
                imageHelper.dispose();

                context.getEvents().removeListener(
                    TeamExplorerEvents.BUILD_DEFINITION_ADDED,
                    buildDefinitionAddedListener);
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

    private Menu createActionMenu(final Shell shell, final TeamExplorerContext context) {
        final Menu menu = new Menu(shell, SWT.POP_UP);

        if (context == null || !context.isConnected()) {
            final MenuItem offlineItem = new MenuItem(menu, SWT.PUSH);
            offlineItem.setText(Messages.getString("TeamExplorerBuildPage.OfflineLabel")); //$NON-NLS-1$
            offlineItem.setEnabled(false);
            return menu;
        }

        final TFSServer server = context.getServer();
        final ProjectInfo projectInfo = context.getCurrentProjectInfo();

        final TFSTeamProjectCollection collection = server.getConnection();
        final String projectName = projectInfo.getName();

        final MenuItem viewBuildItem = new MenuItem(menu, SWT.PUSH);
        viewBuildItem.setText(Messages.getString("TeamExplorerBuildPage.ViewMyBuildsMenuText")); //$NON-NLS-1$
        viewBuildItem.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                final IBuildDefinition buildDefinition = context.getBuildServer().createBuildDefinition(projectName);
                buildDefinition.setName(BuildPath.RECURSION_OPERATOR);

                final BuildExplorer buildExplorer = BuildHelpers.openBuildExplorer(collection, buildDefinition);
                if (buildExplorer != null) {
                    buildExplorer.showOnlyMyBuildsView();
                }
            }
        });

        new MenuItem(menu, SWT.SEPARATOR);

        final MenuItem manageQueueItem = new MenuItem(menu, SWT.PUSH);
        manageQueueItem.setText(Messages.getString("TeamExplorerBuildPage.ManageQueueMenuText")); //$NON-NLS-1$
        manageQueueItem.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                final IBuildDefinition buildDefinition = context.getBuildServer().createBuildDefinition(projectName);
                buildDefinition.setName(BuildPath.RECURSION_OPERATOR);

                final BuildExplorer buildExplorer = BuildHelpers.openBuildExplorer(collection, buildDefinition);
                if (buildExplorer != null) {
                    buildExplorer.showManageQueueView();
                }
            }
        });

        final MenuItem manageQualitiesItem = new MenuItem(menu, SWT.PUSH);
        manageQualitiesItem.setText(Messages.getString("TeamExplorerBuildPage.ManageQualityMenuText")); //$NON-NLS-1$
        manageQualitiesItem.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                BuildHelpers.manageBuildQualities(shell, context);
            }
        });

        return menu;
    }

    @SuppressWarnings("restriction")
    private void getBuildDefinitions(final TeamExplorerContext context, final boolean forceRefresh) {
        if (context == null || context.getServer() == null || context.getServer().getConnection() == null) {
            return;
        }

        final IBuildServer buildServer = context.getServer().getConnection().getBuildServer();

        if (buildServer == null) {
            return;
        }

        final String projectName = context.getCurrentProjectInfo().getName();

        final TeamBuildCache buildCache;
        if (forceRefresh) {
            buildCache = TeamBuildCache.refreshInstance(buildServer, projectName);
        } else {
            buildCache = TeamBuildCache.getInstance(buildServer, projectName);
        }

        buildDefinitions = buildCache.getBuildDefinitions(true);

        final String filterText = textFilter == null ? null : textFilter.getText();
        filteredBuildDefinitions = filterBuildDefinitions(buildDefinitions, filterText);
    }

    private IBuildDefinition[] filterBuildDefinitions(
        final IBuildDefinition[] allDefinitions,
        final String filterText) {
        final List<IBuildDefinition> list = new ArrayList<IBuildDefinition>();
        final String lowerFilter = filterText == null ? null : filterText.toLowerCase();

        for (final IBuildDefinition definition : allDefinitions) {
            if (filterText == null || filterText.length() == 0) {
                list.add(definition);
            } else {
                if (definition.getName().toLowerCase().indexOf(lowerFilter) != -1) {
                    list.add(definition);
                }
            }
        }

        return list.toArray(new IBuildDefinition[list.size()]);
    }

    private class BuildDefinitionsContentProvider extends ContentProviderAdapter {
        @Override
        public Object[] getElements(final Object inputElement) {
            return (Object[]) inputElement;
        }
    }

    private class BuildDefinitionsLabelProvider extends LabelProvider implements ITableLabelProvider {
        @Override
        public Image getColumnImage(final Object element, final int columnIndex) {
            if (!(element instanceof IBuildDefinition) || columnIndex > 0) {
                return null;
            }

            final IBuildDefinition definition = (IBuildDefinition) element;
            return imageHelper.getBuildDefinitionImage(definition);
        }

        @Override
        public String getColumnText(final Object element, final int columnIndex) {
            if (!(element instanceof IBuildDefinition) || columnIndex > 0) {
                return null;
            }

            final IBuildDefinition definition = (IBuildDefinition) element;
            return definition.getName();
        }
    }

    private class BuildDefinitionDoubleClickListener implements IDoubleClickListener {
        @Override
        public void doubleClick(final DoubleClickEvent event) {
            final IStructuredSelection selection = (IStructuredSelection) event.getSelection();
            final Object element = selection.getFirstElement();

            if (element instanceof IBuildDefinition) {
                final IBuildDefinition buildDefinition = (IBuildDefinition) element;
                BuildHelpers.viewTodaysBuildsForDefinition(buildDefinition);
                CodeMarkerDispatch.dispatch(BUILD_DEFINITION_DBLCLICKED);
            }
        }
    }

    private class FilterModifiedListener implements ModifyListener {
        @Override
        public void modifyText(final ModifyEvent e) {
            filteredBuildDefinitions = filterBuildDefinitions(buildDefinitions, textFilter.getText());
            tableViewer.setInput(filteredBuildDefinitions);
            tableViewer.refresh(false);
        }
    }

    private class BuildDefinitionChangedListener implements TeamExplorerEventListener {
        @Override
        public void onEvent(final TeamExplorerEventArg arg) {
            getBuildDefinitions(context, true);
            tableViewer.setInput(filteredBuildDefinitions);
        }
    }

    private class BuildDefinitionAddedListener implements TeamExplorerEventListener {
        @Override
        public void onEvent(final TeamExplorerEventArg arg) {
            getBuildDefinitions(context, true);
            tableViewer.setInput(filteredBuildDefinitions);
        }
    }

    private class BuildDefinitionDeletedListener implements TeamExplorerEventListener {
        @Override
        public void onEvent(final TeamExplorerEventArg arg) {
            Check.isTrue(arg instanceof BuildDefinitionEventArg, "arg instanceof BuildDefinitionEventArg"); //$NON-NLS-1$

            final BuildDefinitionEventArg buildArg = (BuildDefinitionEventArg) arg;
            tableViewer.remove(buildArg.getBuildDefinition());
            getBuildDefinitions(context, true);
        }
    }
}
