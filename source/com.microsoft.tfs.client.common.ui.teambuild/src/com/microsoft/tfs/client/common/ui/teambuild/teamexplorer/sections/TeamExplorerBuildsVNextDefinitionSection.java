// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teambuild.teamexplorer.sections;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.IDialogConstants;
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
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Hyperlink;

import com.microsoft.alm.client.TeeClientHandler;
import com.microsoft.alm.teamfoundation.build.webapi.BuildDefinitionReference;
import com.microsoft.alm.teamfoundation.build.webapi.BuildDefinitionTemplate;
import com.microsoft.alm.teamfoundation.build.webapi.BuildHttpClient;
import com.microsoft.alm.teamfoundation.build.webapi.DefinitionReference;
import com.microsoft.tfs.client.common.ui.TeamExplorerEventArg;
import com.microsoft.tfs.client.common.ui.framework.helper.ContentProviderAdapter;
import com.microsoft.tfs.client.common.ui.framework.helper.SWTUtil;
import com.microsoft.tfs.client.common.ui.framework.layout.GridDataBuilder;
import com.microsoft.tfs.client.common.ui.tasks.NewBuildDefinitionVNextTask;
import com.microsoft.tfs.client.common.ui.tasks.OpenBuildDefinitionVNextTask;
import com.microsoft.tfs.client.common.ui.teambuild.Messages;
import com.microsoft.tfs.client.common.ui.teambuild.TeamBuildImageHelper;
import com.microsoft.tfs.client.common.ui.teambuild.dialogs.BuildDefinitionTemplateSelectionDialog;
import com.microsoft.tfs.client.common.ui.teambuild.teamexplorer.events.BuildDefinitionEventArg;
import com.microsoft.tfs.client.common.ui.teambuild.teamexplorer.helpers.BuildHelpers;
import com.microsoft.tfs.client.common.ui.teamexplorer.TeamExplorerContext;
import com.microsoft.tfs.client.common.ui.teamexplorer.TeamExplorerEventListener;
import com.microsoft.tfs.client.common.ui.teamexplorer.TeamExplorerEvents;
import com.microsoft.tfs.client.common.ui.teamexplorer.sections.TeamExplorerBaseSection;
import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.StringUtil;

public class TeamExplorerBuildsVNextDefinitionSection extends TeamExplorerBaseSection {
    private final String FILTER_WATERMARK =
        Messages.getString("TeamExplorerBuildsDefinitionSection.FilterBoxWatermarkText"); //$NON-NLS-1$

    private final TeamBuildImageHelper imageHelper = new TeamBuildImageHelper();

    private TeamExplorerContext context;
    private BuildDefinitionReference[] buildDefinitions;
    private BuildDefinitionReference[] filteredBuildDefinitions;

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
        if (BuildHelpers.isBuildVNextSupported(context)) {
            getBuildDefinitions(context);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isVisible(final TeamExplorerContext context) {
        return BuildHelpers.isBuildVNextSupported(context);
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
        SWTUtil.gridLayout(composite, 1, true, 1, 5);

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
                    createNewDefinition();
                }
            });

            GridDataBuilder.newInstance().applyTo(link);

            textFilter = toolkit.createText(composite, ""); //$NON-NLS-1$
            textFilter.setMessage(FILTER_WATERMARK);
            textFilter.addModifyListener(new FilterModifiedListener());
            GridDataBuilder.newInstance().hAlignFill().hGrab().applyTo(textFilter);

            tableViewer = new TableViewer(composite, SWT.H_SCROLL | SWT.V_SCROLL | SWT.MULTI | SWT.FULL_SELECTION);
            tableViewer.getTable().setData(FormToolkit.KEY_DRAW_BORDER, Boolean.FALSE);
            tableViewer.setContentProvider(new BuildDefinitionsContentProvider());
            tableViewer.setLabelProvider(new BuildDefinitionsLabelProvider());
            tableViewer.addDoubleClickListener(new BuildDefinitionDoubleClickListener());
            tableViewer.setInput(filteredBuildDefinitions);
            GridDataBuilder.newInstance().align(SWT.FILL, SWT.FILL).grab(true, true).applyTo(tableViewer.getControl());

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

    private void createNewDefinition() {
        final TFSTeamProjectCollection connection = context.getServer().getConnection();
        final BuildHttpClient buildClient =
            new BuildHttpClient(new TeeClientHandler(connection.getHTTPClient()), connection.getBaseURI());

        final String projectName = context.getCurrentProjectInfo().getName();
        final List<BuildDefinitionTemplate> templates = buildClient.getTemplates(projectName);

        final Shell shell = context.getWorkbenchPart().getSite().getShell();
        final BuildDefinitionTemplateSelectionDialog dialog =
            new BuildDefinitionTemplateSelectionDialog(shell, templates);

        final BuildDefinitionTemplate template;
        switch (dialog.open()) {
            case IDialogConstants.FINISH_ID:
                template = null;
                break;
            case IDialogConstants.OK_ID:
                template = dialog.getSelectedTemplate();
                break;
            default:
                return;
        }

        new NewBuildDefinitionVNextTask(shell, connection, projectName, template).run();
    }

    private void getBuildDefinitions(final TeamExplorerContext context) {
        if (context == null || context.getServer() == null || context.getServer().getConnection() == null) {
            return;
        }

        final TFSTeamProjectCollection connection = context.getServer().getConnection();
        final BuildHttpClient buildClient =
            new BuildHttpClient(new TeeClientHandler(connection.getHTTPClient()), connection.getBaseURI());

        final UUID projectId = UUID.fromString(context.getCurrentProjectInfo().getGUID());
        final List<BuildDefinitionReference> rawDefinitions = buildClient.getDefinitions(projectId);

        final List<BuildDefinitionReference> list = new ArrayList<BuildDefinitionReference>();
        for (final DefinitionReference definition : rawDefinitions) {
            if (definition instanceof BuildDefinitionReference) {
                list.add((BuildDefinitionReference) definition);
            }
        }

        buildDefinitions = list.toArray(new BuildDefinitionReference[list.size()]);

        final String filterText = textFilter == null ? null : textFilter.getText();
        filteredBuildDefinitions = filterBuildDefinitions(buildDefinitions, filterText);
    }

    private BuildDefinitionReference[] filterBuildDefinitions(
        final BuildDefinitionReference[] allDefinitions,
        final String filterText) {
        final boolean noFilterProvided = StringUtil.isNullOrEmpty(filterText);

        final List<BuildDefinitionReference> list = new ArrayList<BuildDefinitionReference>();
        final String lowerFilter = noFilterProvided ? null : filterText.toLowerCase();

        for (final BuildDefinitionReference definition : allDefinitions) {
            if (noFilterProvided || definition.getName().toLowerCase().indexOf(lowerFilter) != -1) {
                list.add(definition);
            }
        }

        return list.toArray(new BuildDefinitionReference[list.size()]);
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
            if (!(element instanceof BuildDefinitionReference) || columnIndex > 0) {
                return null;
            }

            final BuildDefinitionReference definition = (BuildDefinitionReference) element;
            return imageHelper.getBuildDefinitionImage(definition);
        }

        @Override
        public String getColumnText(final Object element, final int columnIndex) {
            if (!(element instanceof BuildDefinitionReference) || columnIndex > 0) {
                return null;
            }

            final BuildDefinitionReference definition = (BuildDefinitionReference) element;
            return definition.getName();
        }
    }

    private class BuildDefinitionDoubleClickListener implements IDoubleClickListener {
        @Override
        public void doubleClick(final DoubleClickEvent event) {
            final IStructuredSelection selection = (IStructuredSelection) event.getSelection();
            final Object element = selection.getFirstElement();

            if (element instanceof BuildDefinitionReference) {
                final BuildDefinitionReference buildDefinition = (BuildDefinitionReference) element;
                new OpenBuildDefinitionVNextTask(
                    context.getWorkbenchPart().getSite().getShell(),
                    context.getServer().getConnection(),
                    buildDefinition).run();
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
            getBuildDefinitions(context);
            tableViewer.setInput(filteredBuildDefinitions);
        }
    }

    private class BuildDefinitionAddedListener implements TeamExplorerEventListener {
        @Override
        public void onEvent(final TeamExplorerEventArg arg) {
            getBuildDefinitions(context);
            tableViewer.setInput(filteredBuildDefinitions);
        }
    }

    private class BuildDefinitionDeletedListener implements TeamExplorerEventListener {
        @Override
        public void onEvent(final TeamExplorerEventArg arg) {
            Check.isTrue(arg instanceof BuildDefinitionEventArg, "arg instanceof BuildDefinitionEventArg"); //$NON-NLS-1$

            final BuildDefinitionEventArg buildArg = (BuildDefinitionEventArg) arg;
            tableViewer.remove(buildArg.getBuildDefinition());
            getBuildDefinitions(context);
        }
    }
}
