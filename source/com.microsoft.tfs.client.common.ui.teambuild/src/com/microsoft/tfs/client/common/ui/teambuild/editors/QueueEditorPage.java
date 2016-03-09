// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teambuild.editors;

import java.text.MessageFormat;
import java.util.Arrays;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.GroupMarker;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.FontMetrics;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.EditorPart;

import com.microsoft.tfs.client.common.codemarker.CodeMarker;
import com.microsoft.tfs.client.common.codemarker.CodeMarkerDispatch;
import com.microsoft.tfs.client.common.ui.framework.helper.SWTUtil;
import com.microsoft.tfs.client.common.ui.framework.layout.GridDataBuilder;
import com.microsoft.tfs.client.common.ui.framework.sizing.ControlSize;
import com.microsoft.tfs.client.common.ui.framework.table.TableControl;
import com.microsoft.tfs.client.common.ui.tasks.ViewBuildReportTask;
import com.microsoft.tfs.client.common.ui.teambuild.Messages;
import com.microsoft.tfs.client.common.ui.teambuild.controls.QueuedBuildsTableControl;
import com.microsoft.tfs.client.common.ui.teambuild.enums.QueueStatusFilter;
import com.microsoft.tfs.core.clients.build.BuildConstants;
import com.microsoft.tfs.core.clients.build.IBuildController;
import com.microsoft.tfs.core.clients.build.IBuildDefinition;
import com.microsoft.tfs.core.clients.build.IBuildServer;
import com.microsoft.tfs.core.clients.build.IQueuedBuild;
import com.microsoft.tfs.core.clients.build.IQueuedBuildQueryResult;
import com.microsoft.tfs.core.clients.build.flags.QueryOptions;
import com.microsoft.tfs.core.clients.build.internal.TeamBuildCache;
import com.microsoft.tfs.core.clients.build.internal.soapextensions.BuildControllerComparer;
import com.microsoft.tfs.core.clients.build.internal.soapextensions.BuildControllerSpec;
import com.microsoft.tfs.core.clients.build.internal.soapextensions.BuildDefinitionSpec;
import com.microsoft.tfs.core.clients.build.internal.soapextensions.BuildQueueSpec;
import com.microsoft.tfs.core.clients.build.utils.BuildPath;
import com.microsoft.tfs.util.Check;

public class QueueEditorPage extends EditorPart implements ISelectionProvider {
    public static final CodeMarker CODEMARKER_TABLE_UPDATED =
        new CodeMarker("com.microsoft.tfs.client.common.ui.teambuild.editors.QueueEditorPage#updated"); //$NON-NLS-1$

    private Combo buildDefinitionFilterCombo;
    private Combo statusFilterCombo;
    private Combo controllerFilterCombo;
    private Button onlyMyBuildsCheck;
    private String selectedUser;
    private QueuedBuildsTableControl queuedBuildsTable;
    private IBuildServer buildServer;
    private static final int COMPLETED_AGE_WINDOW = 300;

    private IBuildDefinition selectedBuildDefinition;
    private IBuildDefinition[] allBuildDefinitions;

    private QueueStatusFilter selectedQueueStatus = QueueStatusFilter.ALL;
    private final QueueStatusFilter[] allQueueStatusFilters = new QueueStatusFilter[] {
        QueueStatusFilter.ALL,
        QueueStatusFilter.IN_PROGRESS,
        QueueStatusFilter.QUEUED,
        QueueStatusFilter.POSTPONED
    };

    // private String selectedBuildAgent = "*";
    // private IBuildAgent[] allBuildAgents;
    private String selectedBuildController = BuildConstants.STAR;
    private IBuildController[] allBuildControllers;

    private String teamProject;

    /**
     * @see org.eclipse.ui.part.EditorPart#doSave(org.eclipse.core.runtime.IProgressMonitor)
     */
    @Override
    public void doSave(final IProgressMonitor monitor) {
        // TODO Auto-generated method stub
    }

    /**
     * @see org.eclipse.ui.part.EditorPart#doSaveAs()
     */
    @Override
    public void doSaveAs() {
        // TODO Auto-generated method stub
    }

    /**
     * @see org.eclipse.ui.part.EditorPart#init(org.eclipse.ui.IEditorSite,
     *      org.eclipse.ui.IEditorInput)
     */
    @Override
    public void init(final IEditorSite site, final IEditorInput input) throws PartInitException {
        if (!(input instanceof BuildExplorerEditorInput)) {
            throw new PartInitException("Invalid Input: Must be BuildExplorerEditorInput"); //$NON-NLS-1$
        }

        selectedBuildDefinition = ((BuildExplorerEditorInput) input).getBuildDefinition();
        buildServer = selectedBuildDefinition.getBuildServer();
        teamProject = selectedBuildDefinition.getTeamProject();
        setSite(site);
        setInput(input);
    }

    /**
     * @see org.eclipse.ui.part.EditorPart#isDirty()
     */
    @Override
    public boolean isDirty() {
        return false;
    }

    /**
     * @see org.eclipse.ui.part.EditorPart#isSaveAsAllowed()
     */
    @Override
    public boolean isSaveAsAllowed() {
        return false;
    }

    /**
     * @see org.eclipse.ui.part.WorkbenchPart#createPartControl(org.eclipse.swt.widgets.Composite)
     */
    @Override
    public void createPartControl(final Composite parent) {
        createControls(parent);
    }

    /**
     * @see org.eclipse.ui.part.WorkbenchPart#setFocus()
     */
    @Override
    public void setFocus() {
        queuedBuildsTable.setFocus();
    }

    public void setEnabled(final boolean enabled) {
        final Control[] pageControls = new Control[] {
            buildDefinitionFilterCombo,
            statusFilterCombo,
            controllerFilterCombo,
            onlyMyBuildsCheck,
            queuedBuildsTable
        };

        for (int i = 0; i < pageControls.length; i++) {
            if (pageControls[i] != null && !pageControls[i].isDisposed()) {
                pageControls[i].setEnabled(enabled);
            }
        }
    }

    private void createControls(final Composite composite) {
        final GridLayout layout = new GridLayout(1, false);
        layout.horizontalSpacing = 0;
        layout.verticalSpacing = 0;
        layout.marginWidth = 0;
        layout.marginHeight = 0;
        composite.setLayout(layout);

        final Composite header = new Composite(composite, SWT.NONE);
        GridDataBuilder.newInstance().hFill().hGrab().applyTo(header);

        /* Compute metrics in pixels */
        final GC gc = new GC(header);
        final FontMetrics fontMetrics = gc.getFontMetrics();
        gc.dispose();

        final GridLayout headerLayout = new GridLayout(3, false);
        headerLayout.horizontalSpacing =
            Dialog.convertHorizontalDLUsToPixels(fontMetrics, IDialogConstants.HORIZONTAL_SPACING);
        headerLayout.verticalSpacing =
            Dialog.convertVerticalDLUsToPixels(fontMetrics, IDialogConstants.VERTICAL_SPACING);
        headerLayout.marginWidth =
            Dialog.convertHorizontalDLUsToPixels(fontMetrics, IDialogConstants.HORIZONTAL_MARGIN);
        headerLayout.marginHeight = Dialog.convertVerticalDLUsToPixels(fontMetrics, IDialogConstants.VERTICAL_MARGIN);
        header.setLayout(headerLayout);

        SWTUtil.createLabel(header, Messages.getString("QueueEditorPage.BuildDefinitionLabelText")); //$NON-NLS-1$
        SWTUtil.createLabel(header, Messages.getString("QueueEditorPage.StatusFilterLabelText")); //$NON-NLS-1$
        SWTUtil.createLabel(
            header,
            buildServer.getBuildServerVersion().isV3OrGreater()
                ? Messages.getString("QueueEditorPage.ControlFilterLabelText") //$NON-NLS-1$
                : Messages.getString("QueueEditorPage.AgentFilterLabelText")); //$NON-NLS-1$

        buildDefinitionFilterCombo = new Combo(header, SWT.READ_ONLY);
        GridDataBuilder.newInstance().fill().hGrab().applyTo(buildDefinitionFilterCombo);

        statusFilterCombo = new Combo(header, SWT.READ_ONLY);
        GridDataBuilder.newInstance().fill().applyTo(statusFilterCombo);
        ControlSize.setCharWidthHint(statusFilterCombo, 25);

        controllerFilterCombo = new Combo(header, SWT.READ_ONLY);
        GridDataBuilder.newInstance().fill().applyTo(controllerFilterCombo);
        ControlSize.setCharWidthHint(controllerFilterCombo, 30);

        onlyMyBuildsCheck = new Button(header, SWT.CHECK);
        onlyMyBuildsCheck.setText(Messages.getString("BuildEditorPage.OnlyMyBuilds")); //$NON-NLS-1$
        onlyMyBuildsCheck.setEnabled(buildServer.getBuildServerVersion().isV3OrGreater());
        GridDataBuilder.newInstance().fill().hSpan(3).applyTo(onlyMyBuildsCheck);

        final Label separator = new Label(composite, SWT.SEPARATOR | SWT.HORIZONTAL);
        GridDataBuilder.newInstance().hGrab().hFill().applyTo(separator);

        queuedBuildsTable = new QueuedBuildsTableControl(
            composite,
            SWT.MULTI | SWT.FULL_SELECTION | TableControl.NO_BORDER,
            buildServer);
        GridDataBuilder.newInstance().hSpan(layout).grab().fill().applyTo(queuedBuildsTable);
        queuedBuildsTable.addDoubleClickListener(new IDoubleClickListener() {
            @Override
            public void doubleClick(final DoubleClickEvent event) {
                onDoubleClick(event);
            }
        });
        queuedBuildsTable.getContextMenu().addMenuListener(new IMenuListener() {
            @Override
            public void menuAboutToShow(final IMenuManager manager) {
                fillMenu(manager);
            }
        });

        populateCombos(false);

        getSite().setSelectionProvider(this);

        buildDefinitionFilterCombo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                filterChanged();
            }
        });
        statusFilterCombo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                filterChanged();
            }
        });
        controllerFilterCombo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                filterChanged();
            }
        });
        onlyMyBuildsCheck.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                filterChanged();
            }
        });
    }

    private void filterChanged() {
        selectedBuildDefinition = allBuildDefinitions[buildDefinitionFilterCombo.getSelectionIndex()];
        if (allBuildControllers[controllerFilterCombo.getSelectionIndex()] == null) {
            selectedBuildController = BuildConstants.STAR;
        } else {
            selectedBuildController = allBuildControllers[controllerFilterCombo.getSelectionIndex()].getName();
        }
        selectedQueueStatus = allQueueStatusFilters[statusFilterCombo.getSelectionIndex()];

        if (onlyMyBuildsCheck.getSelection() == true) {
            selectedUser = buildServer.getConnection().getAuthorizedTFSUser().toString();
        } else {
            selectedUser = null;
        }

        updateResults();
    }

    public void updateResults() {
        if (selectedBuildDefinition == null) {
            return;
        }

        String message;
        if (selectedBuildDefinition.getName() != null && !selectedBuildDefinition.getName().equals("*")) //$NON-NLS-1$
        {
            final String messageFormat = Messages.getString("QueueEditorPage.QuerySpecificBuildQueueTextFormat"); //$NON-NLS-1$
            message = MessageFormat.format(messageFormat, selectedBuildDefinition.getName());
        } else {
            message = Messages.getString("QueueEditorPage.QueryAllBuildQueueText"); //$NON-NLS-1$
        }

        final Job populateJob = new Job(message) {
            @Override
            protected IStatus run(final IProgressMonitor monitor) {
                query(selectedBuildDefinition, selectedQueueStatus, selectedBuildController, selectedUser, false);
                CodeMarkerDispatch.dispatch(QueueEditorPage.CODEMARKER_TABLE_UPDATED);
                return Status.OK_STATUS;
            }
        };
        populateJob.setUser(false);
        populateJob.setPriority(Job.SHORT);
        populateJob.schedule();
    }

    protected void fillMenu(final IMenuManager menuMgr) {
        menuMgr.add(new Separator("group0")); //$NON-NLS-1$
        menuMgr.add(new Separator("group1")); //$NON-NLS-1$
        menuMgr.add(new Separator("group2")); //$NON-NLS-1$
        menuMgr.add(new GroupMarker("group2.top")); //$NON-NLS-1$
        menuMgr.add(new GroupMarker("group2.middle")); //$NON-NLS-1$
        menuMgr.add(new GroupMarker("group2.bottom")); //$NON-NLS-1$
        menuMgr.add(new Separator("group3")); //$NON-NLS-1$
        menuMgr.add(new Separator("group4")); //$NON-NLS-1$
        menuMgr.add(new Separator("group5")); //$NON-NLS-1$
        menuMgr.add(new Separator("group6")); //$NON-NLS-1$
        menuMgr.add(new Separator("group7")); //$NON-NLS-1$
        menuMgr.add(new Separator("group8")); //$NON-NLS-1$
        menuMgr.add(new Separator("group9")); //$NON-NLS-1$
        menuMgr.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
    }

    protected void onDoubleClick(final DoubleClickEvent event) {
        final IQueuedBuild qb = getSelectedQueuedBuild();
        if (qb != null && qb.getBuild() != null && qb.getBuild().getURI() != null) {
            new ViewBuildReportTask(
                getSite().getShell(),
                buildServer,
                qb.getBuild().getURI(),
                qb.getBuild().getBuildNumber()).run();
        }
    }

    private void populateCombos(final boolean forceRefresh) {
        populateBuildDefinitionCombo(forceRefresh);
        populateQueueStatusCombo(forceRefresh);
        populateBuildControllerCombo(forceRefresh);
    }

    private void populateBuildControllerCombo(final boolean forceRefresh) {
        controllerFilterCombo.removeAll();

        final IBuildController[] buildControllers =
            TeamBuildCache.getInstance(buildServer, teamProject).getBuildControllers(forceRefresh);
        allBuildControllers = new IBuildController[buildControllers.length + 1];

        allBuildControllers[0] = null;
        // allBuildAgents[0].setName(BuildPath.RECURSION_OPERATOR);

        if (buildServer.getBuildServerVersion().isV3OrGreater()) {
            controllerFilterCombo.add(Messages.getString("QueueEditorPage.AnyBuildControllerChoice")); //$NON-NLS-1$

        } else {
            controllerFilterCombo.add(Messages.getString("QueueEditorPage.AnyBuildAgentChoice")); //$NON-NLS-1$
        }

        int selectedIndex = 0;
        Arrays.sort(buildControllers, new BuildControllerComparer(buildServer));

        for (int i = 0; i < buildControllers.length; i++) {
            allBuildControllers[i + 1] = buildControllers[i];
            controllerFilterCombo.add(buildControllers[i].getName());
            if (buildControllers[i].getName().equals(selectedBuildController)) {
                selectedIndex = i + 1;
            }
        }
        controllerFilterCombo.select(selectedIndex);
    }

    private void populateQueueStatusCombo(final boolean forceRefresh) {
        int selectedIndex = 0;
        statusFilterCombo.removeAll();

        for (int i = 0; i < allQueueStatusFilters.length; i++) {
            statusFilterCombo.add(
                allQueueStatusFilters[i].equals(QueueStatusFilter.ALL)
                    ? Messages.getString("QueueEditorPage.AnyStatusChoice") //$NON-NLS-1$
                    : allQueueStatusFilters[i].getDisplayText());
            if (selectedQueueStatus.equals(allQueueStatusFilters[i])) {
                selectedIndex = i;
            }
        }
        statusFilterCombo.select(selectedIndex);
    }

    private void populateBuildDefinitionCombo(final boolean forceRefresh) {
        final IBuildDefinition[] buildDefinitions =
            TeamBuildCache.getInstance(buildServer, teamProject).getBuildDefinitions(forceRefresh);
        allBuildDefinitions = new IBuildDefinition[buildDefinitions.length + 1];

        buildDefinitionFilterCombo.removeAll();
        buildDefinitionFilterCombo.add(Messages.getString("QueueEditorPage.AnyBuildDefinitionChoice")); //$NON-NLS-1$
        allBuildDefinitions[0] = buildServer.createBuildDefinition(teamProject);
        allBuildDefinitions[0].setName(BuildPath.RECURSION_OPERATOR);

        int selectedBuildDefinitionIndex = 0;
        for (int i = 0; i < buildDefinitions.length; i++) {
            buildDefinitionFilterCombo.add(buildDefinitions[i].getName());
            allBuildDefinitions[i + 1] = buildDefinitions[i];
            if (buildDefinitions[i].equals(selectedBuildDefinition)) {
                selectedBuildDefinitionIndex = i + 1;
            }
        }
        buildDefinitionFilterCombo.select(selectedBuildDefinitionIndex);
    }

    @SuppressWarnings("restriction")
    public void query(
        final IBuildDefinition definition,
        final QueueStatusFilter statusFilter,
        final String controllerFilter,
        final String requestedForUser,
        final boolean updateFilterControls) {
        final BuildQueueSpec queueSpec =
            (BuildQueueSpec) buildServer.createBuildQueueSpec(teamProject, definition.getName());

        queueSpec.setCompletedAge(COMPLETED_AGE_WINDOW);
        queueSpec.setRequestedFor(requestedForUser);
        queueSpec.setQueryOptions(QueryOptions.ALL);
        queueSpec.setStatus(statusFilter.getQueueStatus());

        final BuildDefinitionSpec definitionSpec =
            (BuildDefinitionSpec) buildServer.createBuildDefinitionSpec(definition);
        final BuildControllerSpec controllerSpec = (BuildControllerSpec) buildServer.createBuildControllerSpec();
        controllerSpec.setName(controllerFilter);

        queueSpec.getWebServiceObject().setControllerSpec(controllerSpec.getWebServiceObject());
        queueSpec.getWebServiceObject().setDefinitionSpec(definitionSpec.getWebServiceObject());

        final IQueuedBuildQueryResult result = buildServer.queryQueuedBuilds(queueSpec);

        if (result.getFailures().length > 0) {
            Display.getDefault().asyncExec(new Runnable() {
                @Override
                public void run() {
                    MessageDialog.openError(
                        getSite().getShell(),
                        Messages.getString("QueueEditorPage.BuildQueryErrorDialogTitle"), //$NON-NLS-1$
                        result.getFailures()[0].getMessage());
                }
            });
        }

        Display.getDefault().asyncExec(new Runnable() {
            @Override
            public void run() {
                if (!queuedBuildsTable.isDisposed() && !queuedBuildsTable.getViewer().isCellEditorActive()) {
                    queuedBuildsTable.setQueuedBuilds(result.getQueuedBuilds());

                    if (queuedBuildsTable.getSelectionCount() == 0) {
                        queuedBuildsTable.selectFirst();
                    }

                    queuedBuildsTable.afterQueryRefresh();
                }
            }
        });

    }

    @Override
    public void addSelectionChangedListener(final ISelectionChangedListener listener) {
        queuedBuildsTable.addSelectionChangedListener(listener);
    }

    @Override
    public ISelection getSelection() {
        return queuedBuildsTable.getSelection();
    }

    @Override
    public void removeSelectionChangedListener(final ISelectionChangedListener listener) {
        queuedBuildsTable.removeSelectionChangedListener(listener);
    }

    @Override
    public void setSelection(final ISelection selection) {
        queuedBuildsTable.setSelection(selection);
    }

    public void setSelectedQueuedBuilds(final IQueuedBuild[] queuedBuilds) {
        queuedBuildsTable.setSelectedQueuedBuilds(queuedBuilds);
    }

    public void setSelectedQueuedBuild(final IQueuedBuild queuedBuild) {
        queuedBuildsTable.setSelectedQueuedBuild(queuedBuild);
    }

    public IQueuedBuild[] getSelectedQueuedBuilds() {
        return queuedBuildsTable.getSelectedQueuedBuilds();
    }

    public IQueuedBuild getSelectedQueuedBuild() {
        return queuedBuildsTable.getSelectedQueuedBuild();
    }

    public MenuManager getContextMenu() {
        return queuedBuildsTable.getContextMenu();
    }

    /**
     * @return the buildDefinitionFilterCombo
     */
    public Combo getBuildDefinitionFilterCombo() {
        return buildDefinitionFilterCombo;
    }

    /**
     * @return the statusFilterCombo
     */
    public Combo getStatusFilterCombo() {
        return statusFilterCombo;
    }

    /**
     * @return the agentFilterCombo
     */
    public Combo getAgentFilterCombo() {
        return controllerFilterCombo;
    }

    /**
     * @return the selectedBuildDefinition
     */
    public IBuildDefinition getSelectedBuildDefinition() {
        return selectedBuildDefinition;
    }

    public void setSelectedBuildDefinition(
        final IBuildDefinition selectedBuildDefinition,
        final boolean updateControls) {
        if (updateControls) {
            this.selectedBuildDefinition = selectedBuildDefinition;
            buildServer = this.selectedBuildDefinition.getBuildServer();
            teamProject = this.selectedBuildDefinition.getTeamProject();
            queuedBuildsTable.setBuildServer(buildServer);
            populateCombos(false);
        }
        setSelectedBuildDefinition(selectedBuildDefinition);
    }

    /**
     * @param selectedBuildDefinition
     *        the selectedBuildDefinition to set
     */
    public void setSelectedBuildDefinition(final IBuildDefinition selectedBuildDefinition) {
        this.selectedBuildDefinition = selectedBuildDefinition;
        // look for build definition in list of build definitions.
        int selectedIndex = -1;
        for (int i = 0; i < allBuildDefinitions.length; i++) {
            if (allBuildDefinitions[i].equals(this.selectedBuildDefinition)) {
                selectedIndex = i;
            }
        }
        if (selectedIndex >= 0) {
            buildDefinitionFilterCombo.select(selectedIndex);
        }
    }

    public void setManageQueueFilters() {
        buildDefinitionFilterCombo.select(0);
        statusFilterCombo.select(0);
        controllerFilterCombo.select(0);
        onlyMyBuildsCheck.setSelection(false);

        filterChanged();
    }

    public void setControllerQueueViewFilters(final String controllerURI) {
        Check.notNull(controllerURI, "controllerURI"); //$NON-NLS-1$

        buildDefinitionFilterCombo.select(0);
        statusFilterCombo.select(0);
        onlyMyBuildsCheck.setSelection(false);
        selectControllerFilter(controllerURI);

        filterChanged();
    }

    private void selectControllerFilter(final String controllerURI) {
        boolean controllerSelected = false;
        for (int i = 0; i < allBuildControllers.length; i++) {
            if (allBuildControllers[i] != null) {
                if (allBuildControllers[i].getURI().equals(controllerURI)) {
                    controllerFilterCombo.select(i);
                    controllerSelected = true;
                    break;
                }
            }
        }

        if (!controllerSelected && controllerFilterCombo.getItems().length > 0) {
            controllerFilterCombo.select(0);
        }
    }

    /**
     * @return the queuedBuildsTable
     */
    public QueuedBuildsTableControl getQueuedBuildsTable() {
        return queuedBuildsTable;
    }

    public void reloadBuildAgents() {
        populateBuildControllerCombo(false);
        filterChanged();
    }

    public void reloadBuildDefinitions() {
        populateBuildDefinitionCombo(false);
        filterChanged();
    }

}
