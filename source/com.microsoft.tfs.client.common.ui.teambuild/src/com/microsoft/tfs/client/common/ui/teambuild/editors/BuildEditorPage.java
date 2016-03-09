// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teambuild.editors;

import java.text.MessageFormat;
import java.util.Calendar;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
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
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
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
import com.microsoft.tfs.client.common.ui.buildmanager.BuildManagerEvent;
import com.microsoft.tfs.client.common.ui.buildmanager.BuildManagerListener;
import com.microsoft.tfs.client.common.ui.buildmanager.BuildPropertyChangedEvent;
import com.microsoft.tfs.client.common.ui.framework.helper.SWTUtil;
import com.microsoft.tfs.client.common.ui.framework.layout.GridDataBuilder;
import com.microsoft.tfs.client.common.ui.framework.sizing.ControlSize;
import com.microsoft.tfs.client.common.ui.framework.table.TableControl;
import com.microsoft.tfs.client.common.ui.tasks.ViewBuildReportTask;
import com.microsoft.tfs.client.common.ui.teambuild.Messages;
import com.microsoft.tfs.client.common.ui.teambuild.controls.BuildsTableControl;
import com.microsoft.tfs.client.common.ui.teambuild.enums.DateFilter;
import com.microsoft.tfs.client.common.ui.teambuild.teamexplorer.helpers.BuildHelpers;
import com.microsoft.tfs.core.clients.build.IBuildDefinition;
import com.microsoft.tfs.core.clients.build.IBuildDetail;
import com.microsoft.tfs.core.clients.build.IBuildDetailSpec;
import com.microsoft.tfs.core.clients.build.IBuildQueryResult;
import com.microsoft.tfs.core.clients.build.IBuildServer;
import com.microsoft.tfs.core.clients.build.flags.BuildStatus;
import com.microsoft.tfs.core.clients.build.flags.InformationTypes;
import com.microsoft.tfs.core.clients.build.flags.QueryOptions;
import com.microsoft.tfs.core.clients.build.internal.TeamBuildCache;
import com.microsoft.tfs.core.clients.build.utils.BuildPath;

public class BuildEditorPage extends EditorPart implements ISelectionProvider {
    public static final CodeMarker CODEMARKER_TABLE_UPDATED =
        new CodeMarker("com.microsoft.tfs.client.common.ui.teambuild.editors.BuildEditorPage#updated"); //$NON-NLS-1$

    private Combo buildDefinitionFilterCombo;
    private Combo qualityFilterCombo;
    private Combo dateFilterCombo;
    private Button onlyMyBuildsCheck;
    private BuildsTableControl buildsTableControl;
    private IBuildServer buildServer;

    private IBuildDefinition selectedBuildDefinition;
    private IBuildDefinition[] allBuildDefinitions;
    private String selectedQualityFilter;
    private String selectedUser;
    private String[] allQualityFilters;
    private DateFilter selectedDateFilter;
    private final DateFilter[] allDateFilters = new DateFilter[] {
        DateFilter.TODAY,
        DateFilter.LAST_24_HOURS,
        DateFilter.LAST_48_HOURS,
        DateFilter.LAST_7_DAYS,
        DateFilter.LAST_14_DAYS,
        DateFilter.LAST_28_DAYS,
        DateFilter.ALL
    };

    private String teamProject;

    private final BuildManagerListener buildManagerListener = new MyBuildManagerListener();

    public BuildEditorPage() {
    }

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
        selectedDateFilter = DateFilter.TODAY;

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
        buildsTableControl.setFocus();
    }

    public void setEnabled(final boolean enabled) {
        final Control[] pageControls = new Control[] {
            buildDefinitionFilterCombo,
            qualityFilterCombo,
            dateFilterCombo,
            onlyMyBuildsCheck,
            buildsTableControl
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

        SWTUtil.createLabel(header, Messages.getString("BuildEditorPage.BuildDefinitionLabelText")); //$NON-NLS-1$
        SWTUtil.createLabel(header, Messages.getString("BuildEditorPage.QualityFilterLabelText")); //$NON-NLS-1$
        SWTUtil.createLabel(header, Messages.getString("BuildEditorPage.DateFilterLabelText")); //$NON-NLS-1$

        buildDefinitionFilterCombo = new Combo(header, SWT.READ_ONLY);
        GridDataBuilder.newInstance().fill().hGrab().applyTo(buildDefinitionFilterCombo);

        qualityFilterCombo = new Combo(header, SWT.READ_ONLY);
        GridDataBuilder.newInstance().fill().applyTo(qualityFilterCombo);
        ControlSize.setCharWidthHint(qualityFilterCombo, 30);

        dateFilterCombo = new Combo(header, SWT.READ_ONLY);
        GridDataBuilder.newInstance().fill().applyTo(dateFilterCombo);
        ControlSize.setCharWidthHint(dateFilterCombo, 25);

        onlyMyBuildsCheck = new Button(header, SWT.CHECK);
        onlyMyBuildsCheck.setText(Messages.getString("BuildEditorPage.OnlyMyBuilds")); //$NON-NLS-1$
        onlyMyBuildsCheck.setEnabled(buildServer.getBuildServerVersion().isV3OrGreater());
        GridDataBuilder.newInstance().fill().hSpan(3).applyTo(onlyMyBuildsCheck);

        final Label separator = new Label(composite, SWT.SEPARATOR | SWT.HORIZONTAL);
        GridDataBuilder.newInstance().hGrab().hFill().applyTo(separator);

        buildsTableControl = new BuildsTableControl(
            composite,
            SWT.MULTI | SWT.FULL_SELECTION | TableControl.NO_BORDER,
            buildServer,
            teamProject);
        GridDataBuilder.newInstance().grab().fill().applyTo(buildsTableControl);

        buildsTableControl.addDoubleClickListener(new IDoubleClickListener() {
            @Override
            public void doubleClick(final DoubleClickEvent event) {
                onDoubleClick(event);
            }
        });
        buildsTableControl.getContextMenu().addMenuListener(new IMenuListener() {
            @Override
            public void menuAboutToShow(final IMenuManager manager) {
                fillMenu(manager);
            }
        });

        // Add listener to be informed when build details are changed and our
        // copy might become stale. Remove the listener on dispose.
        BuildHelpers.getBuildManager().addBuildManagerListener(buildManagerListener);

        buildsTableControl.addDisposeListener(new DisposeListener() {
            @Override
            public void widgetDisposed(final DisposeEvent e) {
                BuildHelpers.getBuildManager().removeBuildManagerListener(buildManagerListener);
            }
        });

        populateCombos(false);

        buildDefinitionFilterCombo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                filterChanged();
            }
        });
        dateFilterCombo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                filterChanged();
            }
        });
        qualityFilterCombo.addSelectionListener(new SelectionAdapter() {
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

        getSite().setSelectionProvider(this);

        if (buildServer.getBuildServerVersion().isV1()) {
            dateFilterCombo.setText(Messages.getString("BuildEditorPage.AnyTimeChoice")); //$NON-NLS-1$
            dateFilterCombo.setEnabled(false);
            qualityFilterCombo.setEnabled(false);
        }
    }

    protected void fillMenu(final IMenuManager menuMgr) {
        menuMgr.add(new Separator("group0")); //$NON-NLS-1$
        menuMgr.add(new Separator("group1")); //$NON-NLS-1$
        menuMgr.add(new Separator("group2")); //$NON-NLS-1$
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
        final IBuildDetail buildDetail = getSelectedBuild();
        if (buildDetail != null) {
            new ViewBuildReportTask(
                getSite().getShell(),
                buildServer,
                buildDetail.getURI(),
                buildDetail.getBuildNumber()).run();
        }
    }

    public void updateResults() {
        if (selectedBuildDefinition == null || selectedDateFilter == null) {
            return;
        }

        String message;
        if (selectedBuildDefinition.getName() != null && !selectedBuildDefinition.getName().equals("*")) //$NON-NLS-1$
        {
            final String messageFormat = Messages.getString("BuildEditorPage.QuerySpecificBuildsTextFormat"); //$NON-NLS-1$
            message = MessageFormat.format(messageFormat, selectedBuildDefinition.getName());
        } else {
            message = Messages.getString("BuildEditorPage.QueryAllBuildsText"); //$NON-NLS-1$
        }

        final Job populateJob = new Job(message) {
            @Override
            protected IStatus run(final IProgressMonitor monitor) {
                query(selectedBuildDefinition, selectedQualityFilter, selectedDateFilter, selectedUser, false);
                CodeMarkerDispatch.dispatch(BuildEditorPage.CODEMARKER_TABLE_UPDATED);
                return Status.OK_STATUS;
            }
        };
        populateJob.setUser(false);
        populateJob.setPriority(Job.SHORT);
        populateJob.schedule();
    }

    protected void filterChanged() {
        selectedBuildDefinition = allBuildDefinitions[buildDefinitionFilterCombo.getSelectionIndex()];
        selectedDateFilter = allDateFilters[dateFilterCombo.getSelectionIndex()];
        selectedQualityFilter = allQualityFilters[qualityFilterCombo.getSelectionIndex()];

        if (onlyMyBuildsCheck.getSelection() == true) {
            selectedUser = buildServer.getConnection().getAuthorizedTFSUser().toString();
        } else {
            selectedUser = null;
        }

        updateResults();
    }

    public void query(
        final IBuildDefinition buildDefinition,
        final String qualityFilter,
        final DateFilter dateFilter,
        final String requestedForUser,
        final boolean updateFilterControls) {
        String definitionName = null;
        if (selectedBuildDefinition != null) {
            definitionName = buildDefinition.getName();
        }
        if (definitionName == null || definitionName.length() == 0) {
            definitionName = "*"; //$NON-NLS-1$
        }
        final IBuildDetailSpec buildDetailSpec = buildServer.createBuildDetailSpec(teamProject, definitionName);

        buildDetailSpec.setStatus(BuildStatus.combine(new BuildStatus[] {
            BuildStatus.STOPPED,
            BuildStatus.FAILED,
            BuildStatus.PARTIALLY_SUCCEEDED,
            BuildStatus.SUCCEEDED
        }));
        buildDetailSpec.setQueryOptions(
            QueryOptions.AGENTS.combine(QueryOptions.DEFINITIONS).combine(QueryOptions.BATCHED_REQUESTS));
        buildDetailSpec.setQuality(qualityFilter);
        buildDetailSpec.setRequestedFor(requestedForUser);

        if (!dateFilter.equals(DateFilter.ALL)) {
            buildDetailSpec.setMinFinishTime(getStartRange(dateFilter));
        }

        /*
         * We do not need most of the detailed build information, so set the
         * information types to null. (see
         * http://blogs.msdn.com/buckh/archive/2007
         * /10/09/using-vsts-2008-memory-allocation-profiling.aspx for more
         * information) suffice to say that it reduces the amount downloaded by
         * an order or magnitude at least.
         *
         * We do need the check in outcome types, so we can enable/disable the
         * gated build context menu items.
         */
        buildDetailSpec.setInformationTypes(new String[] {
            InformationTypes.CHECK_IN_OUTCOME
        });

        final IBuildQueryResult result = buildServer.queryBuilds(buildDetailSpec);

        if (result.getFailures().length > 0) {
            Display.getDefault().asyncExec(new Runnable() {
                @Override
                public void run() {
                    MessageDialog.openError(
                        getSite().getShell(),
                        Messages.getString("BuildEditorPage.BuildQueryErrorDialogTitle"), //$NON-NLS-1$
                        result.getFailures()[0].getMessage());
                }
            });
        }

        Display.getDefault().asyncExec(new Runnable() {
            @Override
            public void run() {
                if (!buildsTableControl.isDisposed() && !buildsTableControl.getViewer().isCellEditorActive()) {
                    buildsTableControl.setBuilds(result.getBuilds());
                    if (buildsTableControl.getSelectionCount() == 0) {
                        buildsTableControl.selectFirst();
                    }
                }
            }
        });
    }

    private Calendar getStartRange(final DateFilter dateFilter) {
        final Calendar today = Calendar.getInstance();

        if (!dateFilter.equals(DateFilter.LAST_24_HOURS) && !dateFilter.equals(DateFilter.LAST_48_HOURS)) {
            // Clear the time portion.
            today.clear(Calendar.MILLISECOND);
            today.clear(Calendar.SECOND);
            today.clear(Calendar.MINUTE);
            today.set(Calendar.HOUR_OF_DAY, 0);
        }

        today.add(Calendar.DAY_OF_YEAR, -dateFilter.getDaysAgo());

        return today;
    }

    private void populateCombos(final boolean forceRefresh) {
        populateBuildDefinitionCombo(forceRefresh);
        populateQualityFilterCombo(forceRefresh);
        populateDateFilterCombo(forceRefresh);
    }

    private void populateDateFilterCombo(final boolean forceRefresh) {
        int selectedIndex = 0;
        dateFilterCombo.removeAll();
        for (int i = 0; i < allDateFilters.length; i++) {
            dateFilterCombo.add(
                allDateFilters[i].equals(DateFilter.ALL) ? Messages.getString("BuildEditorPage.AnyTimeChoice") //$NON-NLS-1$
                    : allDateFilters[i].toString());
            if (allDateFilters[i].equals(selectedDateFilter)) {
                selectedIndex = i;
            }
        }
        dateFilterCombo.select(selectedIndex);

    }

    private void populateQualityFilterCombo(final boolean forceRefresh) {
        qualityFilterCombo.removeAll();
        qualityFilterCombo.add(Messages.getString("BuildEditorPage.AnyBuildQualityChoice")); //$NON-NLS-1$
        final String[] qualities = TeamBuildCache.getInstance(buildServer, teamProject).getBuildQualities(forceRefresh);
        allQualityFilters = new String[qualities.length + 1];
        allQualityFilters[0] = null;
        int selectedIndex = 0;
        for (int i = 0; i < qualities.length; i++) {
            qualityFilterCombo.add(qualities[i]);
            allQualityFilters[i + 1] = qualities[i];
            if (qualities[i].equals(selectedQualityFilter)) {
                selectedIndex = i + 1;
            }
        }
        qualityFilterCombo.select(selectedIndex);
    }

    private void populateBuildDefinitionCombo(final boolean forceRefresh) {
        final IBuildDefinition[] buildDefinitions =
            TeamBuildCache.getInstance(buildServer, teamProject).getBuildDefinitions(forceRefresh);
        allBuildDefinitions = new IBuildDefinition[buildDefinitions.length + 1];

        buildDefinitionFilterCombo.removeAll();

        buildDefinitionFilterCombo.add(Messages.getString("BuildEditorPage.AnyBuildDefinitionChoice")); //$NON-NLS-1$
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

    /**
     * @return the buildsTableControl
     */
    public BuildsTableControl getBuildsTableControl() {
        return buildsTableControl;
    }

    @Override
    public void addSelectionChangedListener(final ISelectionChangedListener listener) {
        buildsTableControl.addSelectionChangedListener(listener);
    }

    @Override
    public ISelection getSelection() {
        return buildsTableControl.getSelection();
    }

    @Override
    public void removeSelectionChangedListener(final ISelectionChangedListener listener) {
        buildsTableControl.removeSelectionChangedListener(listener);
    }

    @Override
    public void setSelection(final ISelection selection) {
        buildsTableControl.setSelection(selection);
    }

    public MenuManager getContextMenu() {
        return buildsTableControl.getContextMenu();
    }

    public void setSelectedBuilds(final IBuildDetail[] builds) {
        buildsTableControl.setSelectedBuilds(builds);
    }

    public void setSelectedBuild(final IBuildDetail build) {
        buildsTableControl.setSelectedBuild(build);
    }

    public IBuildDetail[] getSelectedBuilds() {
        return buildsTableControl.getSelectedBuilds();
    }

    public IBuildDetail getSelectedBuild() {
        return buildsTableControl.getSelectedBuild();
    }

    public void removeBuilds(final IBuildDetail[] builds) {
        buildsTableControl.removeBuilds(builds);
    }

    /**
     * @return the buildDefinitionFilterCombo
     */
    public Combo getBuildDefinitionFilterCombo() {
        return buildDefinitionFilterCombo;
    }

    /**
     * @return the qualityFilterCombo
     */
    public Combo getQualityFilterCombo() {
        return qualityFilterCombo;
    }

    /**
     * @return the dateFilterCombo
     */
    public Combo getDateFilterCombo() {
        return dateFilterCombo;
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
            buildsTableControl.updateContext(buildServer, teamProject);
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

        // query();
    }

    public void setTodaysBuildsForDefinitionFilters(final IBuildDefinition buildDefinition) {
        setSelectedBuildDefinition(buildDefinition);
        qualityFilterCombo.select(0);
        onlyMyBuildsCheck.setSelection(false);
        setDateFilter(DateFilter.TODAY);

        filterChanged();
    }

    public void setOnlyMyBuildsFilters() {
        buildDefinitionFilterCombo.select(0);
        qualityFilterCombo.select(0);
        onlyMyBuildsCheck.setSelection(true);
        setDateFilter(DateFilter.LAST_7_DAYS);

        filterChanged();
    }

    public void setDateFilter(final DateFilter dateFilter) {
        for (int i = 0; i < dateFilterCombo.getItemCount(); i++) {
            final String item = dateFilterCombo.getItem(i);
            if (item.equals(dateFilter.toString())) {
                dateFilterCombo.select(i);
                break;
            }
        }
    }

    public void reloadBuildQualities() {
        if (buildsTableControl != null && !buildsTableControl.isDisposed()) {
            populateQualityFilterCombo(false);
            buildsTableControl.loadBuildQualities();
            filterChanged();
        }
    }

    public void reloadBuildDefinitions() {
        populateBuildDefinitionCombo(false);
        filterChanged();
    }

    private boolean showingAnyBuild(final IBuildDetail[] buildDetails) {
        for (final IBuildDetail buildDetail : buildDetails) {
            for (final IBuildDetail existingBuild : buildsTableControl.getBuilds()) {
                if (existingBuild.getBuildNumber().equals(buildDetail.getBuildNumber())) {
                    return true;
                }
            }
        }

        return false;
    }

    private class MyBuildManagerListener implements BuildManagerListener {
        @Override
        public void onBuildPropertyChanged(final BuildPropertyChangedEvent event) {
            updateResults();
        }

        @Override
        public void onBuildDetailsChanged(final BuildManagerEvent event) {
            update(event);
        }

        @Override
        public void onBuildQueued(final BuildManagerEvent event) {
        }

        @Override
        public void onBuildStopped(final BuildManagerEvent event) {
            update(event);
        }

        @Override
        public void onBuildDeleted(final BuildManagerEvent event) {
            update(event);
        }

        @Override
        public void onBuildsDeleted(final BuildManagerEvent event) {
            update(event);
        }

        @Override
        public void onBuildPostponedOrResumed(final BuildManagerEvent event) {
            update(event);
        }

        @Override
        public void onBuildPrioritiesChanged(final BuildManagerEvent event) {
            update(event);
        }

        private void update(final BuildManagerEvent event) {
            if (event.getSource() instanceof BuildExplorer) {
                buildsTableControl.refresh();
                return;
            }

            if (event.getBuildDetails() == null || showingAnyBuild(event.getBuildDetails())) {
                updateResults();
            }
        }
    }
}
