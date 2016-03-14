// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teambuild.dialogs;

import java.text.MessageFormat;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import com.microsoft.tfs.client.common.ui.framework.command.UICommandExecutorFactory;
import com.microsoft.tfs.client.common.ui.framework.dialog.BaseDialog;
import com.microsoft.tfs.client.common.ui.framework.helper.SWTUtil;
import com.microsoft.tfs.client.common.ui.framework.image.ImageHelper;
import com.microsoft.tfs.client.common.ui.framework.layout.GridDataBuilder;
import com.microsoft.tfs.client.common.ui.teambuild.Messages;
import com.microsoft.tfs.client.common.ui.teambuild.TFSTeamBuildPlugin;
import com.microsoft.tfs.client.common.ui.teambuild.commands.CheckBuildFileExistsCommand;
import com.microsoft.tfs.client.common.ui.teambuild.commands.SaveBuildDefinitionCommand;
import com.microsoft.tfs.client.common.ui.teambuild.controls.ToolStripTabPage;
import com.microsoft.tfs.client.common.ui.teambuild.controls.ToolStripTabs;
import com.microsoft.tfs.client.common.ui.teambuild.controls.builddefinition.BuildDefaultsTabPage;
import com.microsoft.tfs.client.common.ui.teambuild.controls.builddefinition.BuildDefinitionTabPage;
import com.microsoft.tfs.client.common.ui.teambuild.controls.builddefinition.GeneralTabPage;
import com.microsoft.tfs.client.common.ui.teambuild.controls.builddefinition.ProjectFileTabPage;
import com.microsoft.tfs.client.common.ui.teambuild.controls.builddefinition.RetentionPolicyTabPage;
import com.microsoft.tfs.client.common.ui.teambuild.controls.builddefinition.TriggerTabPage;
import com.microsoft.tfs.core.clients.build.BuildSourceProviders;
import com.microsoft.tfs.core.clients.build.IBuildDefinition;
import com.microsoft.tfs.core.clients.build.IBuildDefinitionSourceProvider;
import com.microsoft.tfs.core.clients.build.IBuildServer;
import com.microsoft.tfs.core.clients.build.IProcessTemplate;
import com.microsoft.tfs.core.clients.build.ISchedule;
import com.microsoft.tfs.core.clients.build.exceptions.BuildException;
import com.microsoft.tfs.core.clients.build.soapextensions.ContinuousIntegrationType;
import com.microsoft.tfs.core.clients.build.soapextensions.DefinitionTriggerType;
import com.microsoft.tfs.core.clients.build.soapextensions.ProcessTemplateType;
import com.microsoft.tfs.core.clients.versioncontrol.VersionControlClient;
import com.microsoft.tfs.core.clients.versioncontrol.path.ServerPath;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.StringUtil;

/**
 * Show the edit build definition dialog.
 */
public abstract class BuildDefinitionDialog extends BaseDialog {
    public static final int TAB_COUNT = 6;

    public static final int TAB_GENERAL = 0;
    public static final int TAB_TRIGGER = 1;
    public static final int TAB_SOURCE_SETTING = 2;
    public static final int TAB_BUILD_DEFAULTS = 3;
    public static final int TAB_PROJECT_FILE = 4;
    public static final int TAB_RETENTION_POLICY = 5;

    private final IBuildServer buildServer;
    private final IBuildDefinition buildDefinition;
    protected final VersionControlClient versionControl;
    private final ToolStripTabPage[] tabs;
    private final IBuildDefinitionSourceProvider sourceProvider;
    private ToolStripTabs tabControl;

    private GeneralTabPage generalTabPage;
    private BuildDefinitionTabPage sourceSettingsTabPage;
    private ProjectFileTabPage projectFileTabPage;
    private RetentionPolicyTabPage retentionPolicyTabPage;
    private BuildDefaultsTabPage buildDefaultsTabPage;
    private TriggerTabPage triggerTabPage;
    private Label warningLabel;
    private Label warningMessage;

    private String lastDefinitionName;
    private String lastBuildFileLocation = ""; //$NON-NLS-1$

    private final ImageHelper imageHelper = new ImageHelper(TFSTeamBuildPlugin.PLUGIN_ID);

    protected BuildDefinitionDialog(final Shell parentShell, final IBuildDefinition buildDefinition) {
        super(parentShell);
        Check.notNull(buildDefinition, "buildDefinition"); //$NON-NLS-1$

        this.buildDefinition = buildDefinition;

        lastDefinitionName = buildDefinition.getName();
        buildServer = buildDefinition.getBuildServer();
        versionControl = buildServer.getConnection().getVersionControlClient();
        sourceProvider = buildDefinition.getDefaultSourceProvider();

        tabs = createTabs();
    }

    private ToolStripTabPage[] createTabs() {
        generalTabPage = new GeneralTabPage(buildDefinition);
        sourceSettingsTabPage = getSourceSettingsTabPage(buildDefinition);
        projectFileTabPage = getProjectFileTabPage(buildDefinition);
        retentionPolicyTabPage = new RetentionPolicyTabPage(buildDefinition);
        buildDefaultsTabPage = new BuildDefaultsTabPage(buildDefinition);
        triggerTabPage = new TriggerTabPage(buildDefinition);

        final ToolStripTabPage[] pages = new ToolStripTabPage[TAB_COUNT];

        pages[TAB_GENERAL] = generalTabPage;
        pages[TAB_TRIGGER] = triggerTabPage;
        pages[TAB_SOURCE_SETTING] = sourceSettingsTabPage;
        pages[TAB_BUILD_DEFAULTS] = buildDefaultsTabPage;
        pages[TAB_PROJECT_FILE] = projectFileTabPage;
        pages[TAB_RETENTION_POLICY] = retentionPolicyTabPage;

        return pages;
    }

    /**
     * Current git build only supports TF git only
     *
     * @return
     */
    private boolean isTfGit() {
        return (BuildSourceProviders.isTfGit(sourceProvider));
    }

    /**
     * @see com.microsoft.tfs.client.common.ui.framework.dialog.BaseDialog#hookAddToDialogArea(org.eclipse.swt.widgets.Composite)
     */
    @Override
    protected void hookAddToDialogArea(final Composite dialogArea) {
        setProcessTemplateIfNeeded();

        final FillLayout layout = new FillLayout();
        layout.marginWidth = getHorizontalMargin();
        layout.marginHeight = getVerticalMargin();
        layout.spacing = getSpacing();
        dialogArea.setLayout(layout);

        tabControl = new ToolStripTabs(dialogArea, tabs, SWT.NONE);

        tabControl.setSelectedPage(TAB_GENERAL);

        generalTabPage.getControl().getNameText().setFocus();
        generalTabPage.getControl().getNameText().addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(final FocusEvent e) {
                nameChanged(generalTabPage.getControl().getNameText().getText().trim());
                validate();
            }
        });

        final FocusAdapter validateFocusAdapter = new FocusAdapter() {
            @Override
            public void focusLost(final FocusEvent e) {
                validate();
            }
        };

        buildDefaultsTabPage.getControl().addFocusListener(validateFocusAdapter);
        buildDefaultsTabPage.getControl().addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                validate();
            }
        });

        projectFileTabPage.getControl().getConfigFolderText().addFocusListener(validateFocusAdapter);

        projectFileTabPage.getControl().getBrowseButton().addSelectionListener(
            getBrowseButtonSelectionListener(getShell()));
        projectFileTabPage.getControl().getCreateButton().addSelectionListener(
            getCreateButtonSelectionListener(getShell()));
    }

    private void nameChanged(final String newName) {
        if (!StringUtil.isNullOrEmpty(newName) && !newName.equalsIgnoreCase(lastDefinitionName)) {
            lastDefinitionName = newName;
            generalTabPage.getControl().getNameText().setText(newName);

            if (projectFileTabPage.getControl().getConfigFolderText().getText().trim().length() == 0) {
                projectFileTabPage.getControl().getConfigFolderText().setText(getDefaultBuildFileLocation(newName));
                checkForBuildFileExistence(false);
            }

            getShell().setText(getDialogTitle(lastDefinitionName));
        }
    }

    protected void checkForBuildFileExistence(final boolean forceCheckForBuildFile) {
        final boolean isFound = searchForBuildFile(forceCheckForBuildFile);
        projectFileTabPage.getControl().setProjectFileExists(isFound);
    }

    protected boolean searchForBuildFile(final boolean forceCheckForBuildFile) {
        if (!isMSBuildBasedBuild()) {
            projectFileTabPage.getControl().getConfigFolderText().setEnabled(false);
            projectFileTabPage.getControl().getBrowseButton().setEnabled(false);
            return true;
        }

        final String newLocation = projectFileTabPage.getControl().getConfigFolderText().getText();
        if (!forceCheckForBuildFile && ServerPath.equals(newLocation, lastBuildFileLocation)) {
            return projectFileTabPage.getControl().getProjectFileExists();
        }

        lastBuildFileLocation = newLocation;

        if (StringUtil.isNullOrEmpty(lastBuildFileLocation)
            || !projectFileTabPage.getControl().isServerPathValid()) {
            return false;
        }

        final CheckBuildFileExistsCommand command =
            new CheckBuildFileExistsCommand(versionControl, lastBuildFileLocation, isTfGit());

        UICommandExecutorFactory.newBusyIndicatorCommandExecutor(getShell()).execute(command);

        return command.getBuildFileExists();
    }

    protected void validate() {
        checkForBuildFileExistence(false);

        final boolean valid = tabControl.validate();

        getButton(IDialogConstants.OK_ID).setEnabled(valid);
        warningLabel.setVisible(!valid);
        warningMessage.setVisible(!valid);
    }

    protected boolean isMSBuildBasedBuild() {
        if (buildDefinition.getBuildServer().getBuildServerVersion().isLessThanV3()) {
            return true;
        }
        if (buildDefinition.getProcess() == null
            || buildDefinition.getProcess().getTemplateType().equals(ProcessTemplateType.UPGRADE)) {
            return true;
        }
        if (buildDefinition.getConfigurationFolderPath() != null) {
            return true;
        }
        return false;
    }

    /**
     * @see com.microsoft.tfs.client.common.ui.framework.dialog.BaseDialog#provideDialogTitle()
     */
    @Override
    protected String provideDialogTitle() {
        return getDialogTitle(buildDefinition.getName());
    }

    private String getDialogTitle(final String name) {
        final String messageFormat = Messages.getString("BuildDefinitionDialog.DialogTitleFormat"); //$NON-NLS-1$
        final String message = MessageFormat.format(
            messageFormat,
            (name != null ? name : Messages.getString("BuildDefinitionDialog.NameOfNewDefinition"))); //$NON-NLS-1$
        return message;
    }

    /**
     * @see org.eclipse.jface.dialogs.Dialog#createButtonBar(org.eclipse.swt.widgets.Composite)
     */
    @Override
    protected Control createButtonBar(final Composite parent) {
        final Composite bottom = new Composite(parent, SWT.NONE);
        GridDataBuilder.newInstance().hFill().hGrab().applyTo(bottom);

        SWTUtil.gridLayout(
            bottom,
            3,
            false,
            convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_MARGIN),
            convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_SPACING));

        warningLabel = SWTUtil.createLabel(bottom, imageHelper.getImage("icons/warning.gif")); //$NON-NLS-1$
        warningMessage = SWTUtil.createLabel(bottom, Messages.getString("BuildDefinitionDialog.WarningLabelText")); //$NON-NLS-1$
        GridDataBuilder.newInstance().hFill().hGrab().applyTo(warningMessage);

        final Composite composite = new Composite(bottom, SWT.NONE);
        // create a layout with spacing and margins appropriate for the font
        // size.
        final GridLayout layout = new GridLayout();
        layout.numColumns = 0; // this is incremented by createButton
        layout.makeColumnsEqualWidth = true;
        layout.marginWidth = 0;
        layout.marginHeight = 0;
        layout.horizontalSpacing = convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_SPACING);
        layout.verticalSpacing = convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_SPACING);
        composite.setLayout(layout);
        composite.setFont(parent.getFont());
        GridDataBuilder.newInstance().hFill().applyTo(composite);

        // Add the buttons to the button bar.
        createButtonsForButtonBar(composite);

        return composite;
    }

    @Override
    protected void hookAfterButtonsCreated() {
        validate();
    }

    /**
     * @see org.eclipse.jface.dialogs.Dialog#okPressed()
     */
    @Override
    protected void okPressed() {
        // Save the build definition.
        if (!updateAndVerifyBuildDefinition(false)) {
            return;
        }

        // Save Build Definition
        final SaveBuildDefinitionCommand command = new SaveBuildDefinitionCommand(buildDefinition);
        final IStatus status = UICommandExecutorFactory.newBusyIndicatorCommandExecutor(getShell()).execute(command);
        if (status.getSeverity() == IStatus.OK) {
            super.okPressed();
        }
    }

    protected boolean updateAndVerifyBuildDefinition(final boolean ignoreErrors) {
        buildDefinition.setName(generalTabPage.getControl().getNameText().getText().trim());
        buildDefinition.setDescription(generalTabPage.getControl().getDescText().getText().trim());
        buildDefinition.setEnabled(!generalTabPage.getControl().getDisableButton().getSelection());

        if (!updateSourceSettingAndProjectFiles()) {
            return false;
        }

        buildDefinition.setBuildController(buildDefaultsTabPage.getControl().getSelectedBuildController());
        buildDefinition.setDefaultDropLocation(buildDefaultsTabPage.getControl().getDropLocation());

        setProcessTemplateIfNeeded();

        // Build Schedule
        if (triggerTabPage.getControl().getNoCIButton().getSelection()) {
            // No Trigger
            buildDefinition.setContinuousIntegrationType(ContinuousIntegrationType.NONE);
            buildDefinition.setContinuousIntegrationQuietPeriod(0);
        } else if (triggerTabPage.getControl().getAccumulateButton().getSelection()) {
            // Accumulate
            buildDefinition.setContinuousIntegrationType(ContinuousIntegrationType.BATCH);
            int quietPeriod = 0;
            if (triggerTabPage.getControl().getMinimumWaitButton().getSelection()) {
                final String waitMinutesString =
                    triggerTabPage.getControl().getMinimumWaitMinutesText().getText().trim();
                try {
                    quietPeriod = -1;
                    quietPeriod = Integer.parseInt(waitMinutesString);
                } catch (final NumberFormatException e) {
                    // Ignore - will throw error in a bit.
                }

                if (!ignoreErrors && (waitMinutesString.length() == 0 || quietPeriod < 0)) {
                    tabControl.setSelectedPage(TAB_TRIGGER);
                    triggerTabPage.getControl().getMinimumWaitMinutesText().setFocus();
                    triggerTabPage.getControl().getMinimumWaitMinutesText().selectAll();

                    final String messageFormat = Messages.getString("BuildDefinitionDialog.ValueNotInRangeFormat"); //$NON-NLS-1$
                    final String message = MessageFormat.format(messageFormat, waitMinutesString, Integer.MAX_VALUE);

                    MessageDialog.openError(
                        getShell(),
                        Messages.getString("BuildDefinitionDialog.InvalidValueDialogTitle"), //$NON-NLS-1$
                        message);

                    return false;
                }
                buildDefinition.setContinuousIntegrationQuietPeriod(quietPeriod);
            }
        } else if (triggerTabPage.getControl().getEveryCheckInButton().getSelection()) {
            // On Every Check-in
            buildDefinition.setContinuousIntegrationType(ContinuousIntegrationType.INDIVIDUAL);
            buildDefinition.setContinuousIntegrationQuietPeriod(0);
        } else if (triggerTabPage.getControl().getGatedButton().getSelection()) {
            // Gated Check-in
            // If batchSize is greater than 1, then this is a batched gated
            // check-in.
            if (buildDefinition.getBatchSize() > 1) {
                buildDefinition.setTriggerType(DefinitionTriggerType.BATCHED_GATED_CHECKIN);
            } else {
                buildDefinition.setTriggerType(DefinitionTriggerType.GATED_CHECKIN);
            }
            buildDefinition.setContinuousIntegrationQuietPeriod(0);
        } else if (triggerTabPage.getControl().getScheduleButton().getSelection()) {
            // Scheduled Build

            if (triggerTabPage.getControl().getForcedSchedule().getSelection()) {
                buildDefinition.setContinuousIntegrationType(ContinuousIntegrationType.SCHEDULE_FORCED);
            } else {
                buildDefinition.setContinuousIntegrationType(ContinuousIntegrationType.SCHEDULE);
            }
            buildDefinition.setContinuousIntegrationQuietPeriod(0);

            final ISchedule schedule;
            if (buildDefinition.getSchedules().length == 0) {
                schedule = buildDefinition.addSchedule();
            } else {
                schedule = buildDefinition.getSchedules()[0];
            }

            schedule.setDaysToBuild(triggerTabPage.getControl().getScheduleDays());
            schedule.setStartTime(triggerTabPage.getControl().getScheduleTimeAsSecondsAfterMidnight());
        }

        return true;
    }

    private IProcessTemplate getUpgradeProcessTemplate(final IBuildDefinition definition) {
        // Find the upgrade template
        final IProcessTemplate[] templates =
            buildServer.queryProcessTemplates(definition.getTeamProject(), new ProcessTemplateType[] {
                ProcessTemplateType.UPGRADE
        });

        if (templates.length <= 0) {
            final String messageFormat =
                Messages.getString("BuildDefinitionDialog.UnableToUpgradeProcessTemplateFormat"); //$NON-NLS-1$
            final String message = MessageFormat.format(messageFormat, definition.getTeamProject());

            throw new BuildException(message);
        }

        return templates[0];
    }

    private void setProcessTemplateIfNeeded() {
        // Handle creation of process template if required.
        if (buildServer.getBuildServerVersion().isV3OrGreater() && buildDefinition.getProcess() == null) {
            buildDefinition.setProcess(getUpgradeProcessTemplate(buildDefinition));
        }
    }

    protected abstract BuildDefinitionTabPage getSourceSettingsTabPage(final IBuildDefinition buildDefinition);

    protected abstract ProjectFileTabPage getProjectFileTabPage(final IBuildDefinition buildDefinition);

    protected abstract SelectionListener getBrowseButtonSelectionListener(final Shell shell);

    protected abstract SelectionListener getCreateButtonSelectionListener(final Shell shell);

    protected abstract String getDefaultBuildFileLocation(final String newName);

    protected abstract boolean updateSourceSettingAndProjectFiles();

    public void commitChangesIfNeeded() {
        return;
    }
}
