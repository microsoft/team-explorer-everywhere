// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teambuild.dialogs;

import java.text.MessageFormat;
import java.util.Properties;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.microsoft.tfs.client.common.codemarker.CodeMarker;
import com.microsoft.tfs.client.common.codemarker.CodeMarkerDispatch;
import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.common.ui.TFSCommonUIClientPlugin;
import com.microsoft.tfs.client.common.ui.dialogs.vc.FindShelvesetDialog;
import com.microsoft.tfs.client.common.ui.framework.dialog.BaseDialog;
import com.microsoft.tfs.client.common.ui.framework.helper.SWTUtil;
import com.microsoft.tfs.client.common.ui.framework.layout.GridDataBuilder;
import com.microsoft.tfs.client.common.ui.framework.sizing.ControlSize;
import com.microsoft.tfs.client.common.ui.helpers.AutomationIDHelper;
import com.microsoft.tfs.client.common.ui.tasks.vc.AbstractShelveTask;
import com.microsoft.tfs.client.common.ui.tasks.vc.ShelveWithPromptTask;
import com.microsoft.tfs.client.common.ui.teambuild.Messages;
import com.microsoft.tfs.client.common.ui.teambuild.TeamBuildHelper;
import com.microsoft.tfs.client.common.ui.teambuild.teamexplorer.helpers.BuildHelpers;
import com.microsoft.tfs.core.clients.build.BuildSourceProviders;
import com.microsoft.tfs.core.clients.build.IBuildController;
import com.microsoft.tfs.core.clients.build.IBuildDefinition;
import com.microsoft.tfs.core.clients.build.IBuildRequest;
import com.microsoft.tfs.core.clients.build.IBuildServer;
import com.microsoft.tfs.core.clients.build.IProcessTemplate;
import com.microsoft.tfs.core.clients.build.IQueuedBuild;
import com.microsoft.tfs.core.clients.build.flags.BuildReason;
import com.microsoft.tfs.core.clients.build.flags.QueueOptions;
import com.microsoft.tfs.core.clients.build.internal.TeamBuildCache;
import com.microsoft.tfs.core.clients.build.internal.utils.XamlHelper;
import com.microsoft.tfs.core.clients.build.soapextensions.DefinitionTriggerType;
import com.microsoft.tfs.core.clients.build.soapextensions.QueuePriority;
import com.microsoft.tfs.core.clients.build.utils.BuildPath;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Shelveset;
import com.microsoft.tfs.core.clients.versioncontrol.specs.WorkspaceSpec;
import com.microsoft.tfs.core.util.UserNameUtil;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.StringUtil;

public class QueueBuildDialog extends BaseDialog {
    public static final CodeMarker CODEMARKER_DIALOG_OPEN =
        new CodeMarker("com.microsoft.tfs.client.common.ui.teambuild.dialogs.QueueBuildDialog#DialogOpen"); //$NON-NLS-1$
    public static final CodeMarker CODEMARKER_SHELVESET_SELECTED =
        new CodeMarker("com.microsoft.tfs.client.common.ui.teambuild.dialogs.QueueBuildDialog#ShelvesetSelected"); //$NON-NLS-1$

    public static final String BUILDDEF_COMBO_ID =
        "com.microsoft.tfs.client.common.ui.teambuild.dialogsQueueBuildDialog.buildDefCombo"; //$NON-NLS-1$
    public static final String BUDDY_BUILD_COMBO_ID =
        "com.microsoft.tfs.client.common.ui.teambuild.dialogsQueueBuildDialog.buddyBuildCombo"; //$NON-NLS-1$
    public static final String CONTROLLER_COMBO_ID =
        "com.microsoft.tfs.client.common.ui.teambuild.dialogsQueueBuildDialog.controllerCombo"; //$NON-NLS-1$
    public static final String PRIORITY_COMBO_ID =
        "com.microsoft.tfs.client.common.ui.teambuild.dialogsQueueBuildDialog.priorityCombo"; //$NON-NLS-1$
    public static final String SHELVESET_NAME_LABEL_ID =
        "com.microsoft.tfs.client.common.ui.teambuild.dialogsQueueBuildDialog.shelvesetNameLabel"; //$NON-NLS-1$
    public static final String SHELVESET_TEXT_ID =
        "com.microsoft.tfs.client.common.ui.teambuild.dialogsQueueBuildDialog.shelvesetText"; //$NON-NLS-1$
    public static final String FIND_SHELVESET_BUTTON_ID =
        "com.microsoft.tfs.client.common.ui.teambuild.dialogsQueueBuildDialog.findShelvesetButton"; //$NON-NLS-1$
    public static final String CREATE_SHELVESET_BUTTON_ID =
        "com.microsoft.tfs.client.common.ui.teambuild.dialogsQueueBuildDialog.createShelvesetButton"; //$NON-NLS-1$
    public static final String CHECKIN_AFTER_SUCCESS_BUTTON_ID =
        "com.microsoft.tfs.client.common.ui.teambuild.dialogsQueueBuildDialog.checkinAfterSuccessButton"; //$NON-NLS-1$
    public static final String DROP_LOCATION_TEXT_ID =
        "com.microsoft.tfs.client.common.ui.teambuild.dialogsQueueBuildDialog.dropText"; //$NON-NLS-1$
    public static final String DESCRIPTION_TEXT_ID =
        "com.microsoft.tfs.client.common.ui.teambuild.dialogsQueueBuildDialog.descriptionText"; //$NON-NLS-1$
    public static final String QUEUE_POSITION_TEXT_ID =
        "com.microsoft.tfs.client.common.ui.teambuild.dialogsQueueBuildDialog.queuePositionText"; //$NON-NLS-1$
    public static final String COMMAND_ARGS_TEXT_ID =
        "com.microsoft.tfs.client.common.ui.teambuild.dialogsQueueBuildDialog.commandArgsText"; //$NON-NLS-1$

    private IBuildDefinition selectedBuildDefinition;
    private final IBuildDefinition[] buildDefinitions;
    private final IBuildController[] buildControllers;

    private IBuildController selectedBuildController;
    private IBuildRequest selectedBuildRequest;
    private final IBuildServer buildServer;

    private final String teamProject;
    private final String defaultQueuePriorityText;

    private final Image image;
    private Combo buildDefinitionCombo;
    private Text descriptionText;
    private Combo buddyBuildCombo;
    private Combo buildControllerCombo;
    private Text dropText;
    private Combo priority;
    private Text position;
    private Text commandLineArgs;

    private Composite shelvesetComposite;
    private Label shelvesetLabel;
    private Text shelvesetNameText;
    private Button findShelvesetButton;
    private Button createShelvesetButton;
    private Button checkinAfterSuccessButton;

    private boolean isFileContainerDropLocation;
    private boolean isNoDropLocation;

    public QueueBuildDialog(final Shell parentShell, final IBuildDefinition buildDefinition) {
        super(parentShell);

        selectedBuildDefinition = buildDefinition;
        buildServer = selectedBuildDefinition.getBuildServer();
        teamProject = selectedBuildDefinition.getTeamProject();

        final TeamBuildCache cache = TeamBuildCache.getInstance(buildServer, teamProject);

        buildDefinitions = cache.getBuildDefinitions(false);
        buildControllers = cache.getBuildControllers(false);

        defaultQueuePriorityText = buildServer.getDisplayText(QueuePriority.NORMAL);
        image = getSWTImage(SWT.ICON_INFORMATION);
        setOptionIncludeDefaultButtons(false);
        addButtonDescription(IDialogConstants.OK_ID, Messages.getString("QueueBuildDialog.QueueButtonText"), true); //$NON-NLS-1$
        addButtonDescription(IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
    }

    public QueueBuildDialog(
        final Shell parentShell,
        final IBuildDefinition selectedBuildDefinition,
        final IBuildServer buildServer,
        final String teamProjectName,
        final IBuildDefinition[] buildDefinitions,
        final IBuildController[] buildControllers) {
        super(parentShell);
        this.selectedBuildDefinition = selectedBuildDefinition;
        this.buildServer = buildServer;
        teamProject = teamProjectName;
        this.buildDefinitions = buildDefinitions;
        this.buildControllers = buildControllers;

        defaultQueuePriorityText = this.buildServer.getDisplayText(QueuePriority.NORMAL);
        image = getSWTImage(SWT.ICON_INFORMATION);
        setOptionIncludeDefaultButtons(false);
        addButtonDescription(IDialogConstants.OK_ID, Messages.getString("QueueBuildDialog.QueueButtonText"), true); //$NON-NLS-1$
        addButtonDescription(IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
    }

    @Override
    protected void hookAddToDialogArea(final Composite dialogArea) {
        GridLayout layout = SWTUtil.gridLayout(dialogArea, 3);
        layout.marginWidth = getHorizontalMargin();
        layout.marginHeight = getVerticalMargin();
        layout.horizontalSpacing = getHorizontalSpacing();
        layout.verticalSpacing = getVerticalSpacing();

        if (image != null) {
            final Label imageLabel = new Label(dialogArea, SWT.NULL);
            image.setBackground(imageLabel.getBackground());
            imageLabel.setImage(image);

            GridDataBuilder.newInstance().fill().align(SWT.CENTER, SWT.BEGINNING).applyTo(imageLabel);
        }

        final Label messageLabel = new Label(dialogArea, SWT.WRAP);
        messageLabel.setText(Messages.getString("QueueBuildDialog.MessageLabelText")); //$NON-NLS-1$

        GridDataBuilder.newInstance().fill().align(SWT.FILL, SWT.BEGINNING).hGrab().wHint(
            IDialogConstants.MINIMUM_MESSAGE_AREA_WIDTH).hSpan(2).applyTo(messageLabel);

        SWTUtil.createLabel(dialogArea, ""); //$NON-NLS-1$
        SWTUtil.createLabel(dialogArea, ""); //$NON-NLS-1$
        SWTUtil.createLabel(dialogArea, ""); //$NON-NLS-1$

        final Label buildDefLabel =
            SWTUtil.createLabel(dialogArea, Messages.getString("QueueBuildDialog.BuildDefinitionLabelText")); //$NON-NLS-1$
        GridDataBuilder.newInstance().fill().hGrab().hSpan(3).applyTo(buildDefLabel);

        buildDefinitionCombo = new Combo(dialogArea, SWT.READ_ONLY);
        GridDataBuilder.newInstance().fill().hGrab().hSpan(3).applyTo(buildDefinitionCombo);
        AutomationIDHelper.setWidgetID(buildDefinitionCombo, BUILDDEF_COMBO_ID);
        buildDefinitionCombo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                final Combo combo = (Combo) e.widget;
                final int ix = combo.getSelectionIndex();
                newBuildDefinitionSelected(ix);
                enableControls();
            }
        });

        descriptionText = new Text(dialogArea, SWT.BORDER | SWT.MULTI | SWT.READ_ONLY | SWT.V_SCROLL | SWT.WRAP);
        GridDataBuilder.newInstance().fill().grab().hSpan(3).applyTo(descriptionText);
        ControlSize.setCharHeightHint(descriptionText, 2);
        AutomationIDHelper.setWidgetID(descriptionText, DESCRIPTION_TEXT_ID);

        final String buddyLabelText = Messages.getString("QueueBuildDialog.BuddyBuildLabelText"); //$NON-NLS-1$
        final Label buddyBuildLabel = SWTUtil.createLabel(dialogArea, buddyLabelText);
        GridDataBuilder.newInstance().fill().hGrab().hSpan(3).applyTo(buddyBuildLabel);

        buddyBuildCombo = new Combo(dialogArea, SWT.READ_ONLY);
        GridDataBuilder.newInstance().fill().hGrab().hSpan(3).applyTo(buddyBuildCombo);
        AutomationIDHelper.setWidgetID(buddyBuildCombo, BUDDY_BUILD_COMBO_ID);

        buddyBuildCombo.add(Messages.getString("QueueBuildDialog.LatestSourcesComboChoice")); //$NON-NLS-1$
        buddyBuildCombo.add(Messages.getString("QueueBuildDialog.LatestSourcesWithShelveComboChoice")); //$NON-NLS-1$
        buddyBuildCombo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                enableControls();
            }
        });

        if (!buildServer.getBuildServerVersion().isV3OrGreater()
            || !BuildSourceProviders.isTfVersionControl(selectedBuildDefinition.getDefaultSourceProvider())) {
            buddyBuildCombo.select(0);
            buddyBuildCombo.setEnabled(false);
        } else {
            shelvesetComposite = new Composite(dialogArea, SWT.NONE);
            GridDataBuilder.newInstance().fill().hGrab().hSpan(3).applyTo(shelvesetComposite);
            layout = SWTUtil.gridLayout(shelvesetComposite, 3);
            layout.marginHeight = 0;
            layout.marginWidth = 0;
            layout.horizontalSpacing = getHorizontalSpacing();
            layout.verticalSpacing = getVerticalSpacing();

            final String shelvesetLabelText = Messages.getString("QueueBuildDialog.ShelvesetLabelText"); //$NON-NLS-1$
            shelvesetLabel = SWTUtil.createLabel(shelvesetComposite, shelvesetLabelText);
            GridDataBuilder.newInstance().fill().hGrab().hSpan(3).applyTo(shelvesetLabel);
            AutomationIDHelper.setWidgetID(shelvesetLabel, SHELVESET_NAME_LABEL_ID);

            shelvesetNameText = new Text(shelvesetComposite, SWT.BORDER);
            GridDataBuilder.newInstance().hFill().hGrab().applyTo(shelvesetNameText);
            AutomationIDHelper.setWidgetID(shelvesetNameText, SHELVESET_TEXT_ID);

            findShelvesetButton = new Button(shelvesetComposite, SWT.PUSH);
            findShelvesetButton.setText(Messages.getString("QueueBuildDialog.FindShelveButtonText")); //$NON-NLS-1$
            GridDataBuilder.newInstance().applyTo(findShelvesetButton);
            findShelvesetButton.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(final SelectionEvent e) {
                    findShelveset();
                }
            });
            AutomationIDHelper.setWidgetID(findShelvesetButton, FIND_SHELVESET_BUTTON_ID);

            createShelvesetButton = new Button(shelvesetComposite, SWT.PUSH);
            createShelvesetButton.setText(Messages.getString("QueueBuildDialog.CreateShelveButtonText")); //$NON-NLS-1$
            GridDataBuilder.newInstance().applyTo(createShelvesetButton);
            createShelvesetButton.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(final SelectionEvent e) {
                    createShelveset();
                }
            });
            AutomationIDHelper.setWidgetID(createShelvesetButton, CREATE_SHELVESET_BUTTON_ID);

            checkinAfterSuccessButton = new Button(shelvesetComposite, SWT.CHECK);
            checkinAfterSuccessButton.setText(Messages.getString("QueueBuildDialog.CheckinAfterSuccessButtonText")); //$NON-NLS-1$
            GridDataBuilder.newInstance().fill().hGrab().hSpan(3).applyTo(checkinAfterSuccessButton);
            checkinAfterSuccessButton.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(final SelectionEvent e) {
                    enableControls();
                }
            });
            AutomationIDHelper.setWidgetID(checkinAfterSuccessButton, CHECKIN_AFTER_SUCCESS_BUTTON_ID);
        }

        final String controllerLabel = buildServer.getBuildServerVersion().isLessThanV3()
            ? Messages.getString("QueueBuildDialog.BuildAgentLabelText") //$NON-NLS-1$
            : Messages.getString("QueueBuildDialog.BuildControllerLabelText"); //$NON-NLS-1$

        final Label buildAgentLabel = SWTUtil.createLabel(dialogArea, controllerLabel);
        GridDataBuilder.newInstance().fill().hGrab().hSpan(3).applyTo(buildAgentLabel);

        buildControllerCombo = new Combo(dialogArea, SWT.READ_ONLY);
        GridDataBuilder.newInstance().fill().hGrab().hSpan(3).applyTo(buildControllerCombo);
        AutomationIDHelper.setWidgetID(buildControllerCombo, CONTROLLER_COMBO_ID);
        buildControllerCombo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                final int selection = buildControllerCombo.getSelectionIndex();

                if (selection >= 0 && selection < buildControllers.length) {
                    selectedBuildController = buildControllers[selection];
                } else {
                    selectedBuildController = null;
                }

                calculatePosition();
                enableControls();
            }
        });

        final Label dropFolderLabel =
            SWTUtil.createLabel(dialogArea, Messages.getString("QueueBuildDialog.DropFolderLabelText")); //$NON-NLS-1$
        GridDataBuilder.newInstance().fill().hGrab().hSpan(3).applyTo(dropFolderLabel);

        dropText = new Text(dialogArea, SWT.BORDER);
        GridDataBuilder.newInstance().fill().hGrab().hSpan(3).applyTo(dropText);
        AutomationIDHelper.setWidgetID(dropText, DROP_LOCATION_TEXT_ID);

        final Label queueLabel =
            SWTUtil.createLabel(dialogArea, Messages.getString("QueueBuildDialog.PriorityLabelText")); //$NON-NLS-1$
        GridDataBuilder.newInstance().fill().hGrab().hSpan(2).applyTo(queueLabel);

        SWTUtil.createLabel(dialogArea, Messages.getString("QueueBuildDialog.PositionLabelText")); //$NON-NLS-1$

        priority = new Combo(dialogArea, SWT.READ_ONLY);
        GridDataBuilder.newInstance().fill().hGrab().hSpan(2).applyTo(priority);
        AutomationIDHelper.setWidgetID(priority, PRIORITY_COMBO_ID);
        priority.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                calculatePosition();
            }
        });

        position = new Text(dialogArea, SWT.BORDER | SWT.READ_ONLY);
        GridDataBuilder.newInstance().fill().applyTo(position);
        ControlSize.setCharWidthHint(position, 20);
        AutomationIDHelper.setWidgetID(position, QUEUE_POSITION_TEXT_ID);

        final Label commandArgsLabel =
            SWTUtil.createLabel(dialogArea, Messages.getString("QueueBuildDialog.CommandArgsLabelText")); //$NON-NLS-1$
        GridDataBuilder.newInstance().fill().hGrab().hSpan(3).applyTo(commandArgsLabel);

        commandLineArgs = new Text(dialogArea, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.WRAP);
        AutomationIDHelper.setWidgetID(commandLineArgs, COMMAND_ARGS_TEXT_ID);
        commandLineArgs.addTraverseListener(new TraverseListener() {
            @Override
            public void keyTraversed(final TraverseEvent e) {
                if (e.detail == SWT.TRAVERSE_TAB_NEXT || e.detail == SWT.TRAVERSE_TAB_PREVIOUS) {
                    e.doit = true;
                }
            }
        });
        GridDataBuilder.newInstance().fill().grab().hSpan(3).applyTo(commandLineArgs);
        ControlSize.setCharHeightHint(commandLineArgs, 2);

        if (buildServer.getBuildServerVersion().isV1()) {
            buildControllerCombo.setEnabled(false);
            dropText.setEnabled(false);
            priority.setEnabled(false);
            position.setEnabled(false);
            commandLineArgs.setEnabled(false);
        }

        // populate drop downs.
        loadBuildControllers();
        loadQueuePriorities();
        final int selectedIndex = loadBuildDefinitions();

        // Populate with default values.
        newBuildDefinitionSelected(selectedIndex);
        enableControls();
    }

    protected void calculatePosition() {
        if (!buildServer.getBuildServerVersion().isV1()) {
            position.setText(Messages.getString("QueueBuildDialog.Recalculating")); //$NON-NLS-1$
            final IBuildRequest request = getBuildRequest();

            final String messageFormat = Messages.getString("QueueBuildDialog.CalculatingQueuePositionLabelTextFormat"); //$NON-NLS-1$
            final String message = MessageFormat.format(messageFormat, getSelectedBuildDefinition().getName());
            final Job populateJob = new Job(message) {
                @Override
                protected IStatus run(final IProgressMonitor monitor) {
                    try {
                        final IQueuedBuild queuedBuild = buildServer.queueBuild(request, QueueOptions.PREVIEW);
                        Display.getDefault().asyncExec(new Runnable() {
                            @Override
                            public void run() {
                                if (!position.isDisposed()) {
                                    position.setText(Integer.toString(queuedBuild.getQueuePosition()));
                                }
                            }
                        });
                    } catch (final Exception e) {
                        /*
                         * Suppress the internal error message box
                         */
                    }
                    return Status.OK_STATUS;
                }
            };
            populateJob.setUser(false);
            populateJob.setPriority(Job.SHORT);
            populateJob.schedule(100);
        } else {
            position.setText(""); //$NON-NLS-1$
        }
    }

    @Override
    public void okPressed() {
        if (isBuddyBuild()) {
            if (shelvesetNameText != null && shelvesetNameText.getText().length() == 0) {
                MessageDialog.openError(
                    getShell(),
                    Messages.getString("QueueBuildDialog.NoShelvesetErrorTitle"), //$NON-NLS-1$
                    Messages.getString("QueueBuildDialog.NoShelvesetErrorText")); //$NON-NLS-1$

                shelvesetNameText.setFocus();
                return;
            }
        }

        super.okPressed();
    }

    @Override
    public void hookDialogIsOpen() {
        CodeMarkerDispatch.dispatch(CODEMARKER_DIALOG_OPEN);
    }

    @Override
    public void hookDialogAboutToClose() {
        selectedBuildRequest = getBuildRequest();
    }

    public IBuildRequest getSelectedBuildRequest() {
        return selectedBuildRequest;
    }

    private void enableControls() {
        setBuddyBuildEnablement();

        final IBuildDefinition definition = getSelectedBuildDefinition();
        final IProcessTemplate process = definition == null ? null : definition.getProcess();

        // Disable the "Checkin after successful build" check box if we aren't
        // doing a private build or the process doesn't allow it. Note, the
        // check box is only allocated for 2010 and above and will be null for
        // earlier versions.
        if (checkinAfterSuccessButton != null) {
            if (process != null && isBuddyBuild()) {
                final boolean enabled = process.getSupportedReasons().contains(getBuildReason());
                checkinAfterSuccessButton.setEnabled(enabled);
            } else {
                checkinAfterSuccessButton.setEnabled(false);
            }
        }

        if (buildServer.getBuildServerVersion().isV3OrGreater()
            && (buildControllers == null || buildControllers.length == 0)) {
            buildControllerCombo.setEnabled(false);
        } else {
            final boolean enabled = checkinAfterSuccessButton == null
                || !checkinAfterSuccessButton.getEnabled()
                || !checkinAfterSuccessButton.getSelection();

            buildControllerCombo.setEnabled(enabled);
        }
    }

    private IBuildRequest getBuildRequest() {
        final IBuildDefinition selectedBuildDefinition = getSelectedBuildDefinition();
        final IBuildRequest request = selectedBuildDefinition.createBuildRequest();

        request.setBuildController(getSelectedBuildController());

        if (buildServer.getBuildServerVersion().isV2()) {
            request.setProcessParameters(getSelectedCommandLine().trim());
        } else if (getSelectedCommandLine() != null && getSelectedCommandLine().trim().length() > 0) {
            final Properties properties = new Properties();
            properties.setProperty("MSBuildArguments", getSelectedCommandLine()); //$NON-NLS-1$
            request.setProcessParameters(XamlHelper.save(properties));
        }

        request.setDropLocation(getSelectedDropLocation());
        request.setPriority(getSelectedQueuePriority());
        request.setReason(getBuildReason());

        if (isBuddyBuild()) {
            // The shelveset spec must include the user. Add the current user if
            // none was specified, or complete the user specification if it is
            // not full.
            String shelvesetName = getSelectedShelvesetName();
            if (shelvesetName != null && shelvesetName.length() > 0) {
                final String authorizedUserName =
                    getCurrentRepository().getVersionControlClient().getConnection().getAuthorizedIdentity().getUniqueName();

                // Parse the specified specification into a name and user parts.
                // Specify the current authorized TFS user as the fallback in
                // the case no user is specified.
                final WorkspaceSpec spec = WorkspaceSpec.parse(shelvesetName, authorizedUserName);

                // Reset the sheleveset name with the full specification.
                shelvesetName = spec.toString();
            }
            request.setShelvesetName(shelvesetName);
        }

        return request;
    }

    public boolean isBuddyBuild() {
        return buddyBuildCombo.getSelectionIndex() == 1;
    }

    public BuildReason getBuildReason() {
        if (isBuddyBuild()) {
            if (checkinAfterSuccessButton.getSelection()) {
                return BuildReason.CHECK_IN_SHELVESET;
            } else {
                return BuildReason.VALIDATE_SHELVESET;
            }
        } else {
            return BuildReason.MANUAL;
        }
    }

    public String getSelectedShelvesetName() {
        if (shelvesetNameText != null) {
            return shelvesetNameText.getText();
        } else {
            return ""; //$NON-NLS-1$
        }
    }

    public QueuePriority getSelectedQueuePriority() {
        return QueuePriority.fromDisplayText(priority.getText());
    }

    public String getSelectedDropLocation() {
        if (isNoDropLocation) {
            return StringUtil.EMPTY;
        } else if (isFileContainerDropLocation) {
            return BuildHelpers.DROP_TO_FILE_CONTAINER_LOCATION;
        } else {
            return dropText.getText();
        }
    }

    public IBuildDefinition getSelectedBuildDefinition() {
        return selectedBuildDefinition;
    }

    public IBuildController getSelectedBuildController() {
        return selectedBuildController;
    }

    public String getSelectedCommandLine() {
        return commandLineArgs.getText();
    }

    protected void newBuildDefinitionSelected(final int index) {
        selectedBuildDefinition = buildDefinitions[index];

        getShell().setText(provideDialogTitle());

        // change display values
        descriptionText.setText(selectedBuildDefinition.getDescription() == null ? "" //$NON-NLS-1$
            : selectedBuildDefinition.getDescription());

        // Note that drop location could be null if unable to parse the TFS2005
        // build file.
        final String dropLocation = selectedBuildDefinition.getDefaultDropLocation();
        if (StringUtil.isNullOrEmpty(dropLocation)) {
            dropText.setText(Messages.getString("QueueBuildDialog.NoDropOptionText")); //$NON-NLS-1$
            dropText.setEnabled(false);
            isNoDropLocation = true;
            isFileContainerDropLocation = false;
        } else if (dropLocation.equals(BuildHelpers.DROP_TO_FILE_CONTAINER_LOCATION)) {
            dropText.setText(Messages.getString("QueueBuildDialog.FileContainerDropOptionText")); //$NON-NLS-1$
            dropText.setEnabled(false);
            isNoDropLocation = false;
            isFileContainerDropLocation = true;
        } else {
            dropText.setText(dropLocation);
            dropText.setEnabled(true);
            isNoDropLocation = false;
            isFileContainerDropLocation = false;
        }

        if (shelvesetNameText != null) {
            shelvesetNameText.setText(""); //$NON-NLS-1$
        }

        // Set priority
        selectDefaultPriority();

        // Select default build agent
        selectDefaultBuildController();

        // populate the buddy build controls
        if (selectedBuildDefinition.getTriggerType().contains(DefinitionTriggerType.GATED_CHECKIN)) {
            buddyBuildCombo.select(1);
        } else {
            buddyBuildCombo.select(0);
        }

        // Set position
        if (selectedBuildDefinition.isEnabled()) {
            calculatePosition();
        }
    }

    /**
     * @see com.microsoft.tfs.client.common.ui.framework.dialog.BaseDialog#provideDialogTitle()
     */
    @Override
    protected String provideDialogTitle() {
        String buildType = selectedBuildDefinition.getName();
        if (buildType == null || buildType.length() == 0 || buildType.equals(BuildPath.RECURSION_OPERATOR)) {
            buildType = selectedBuildDefinition.getTeamProject();
        }

        final String messageFormat = Messages.getString("QueueBuildDialog.DialogTitleFormat"); //$NON-NLS-1$
        final String message = MessageFormat.format(messageFormat, buildType);
        return message;
    }

    private void findShelveset() {
        final FindShelvesetDialog dialog = new FindShelvesetDialog(getShell(), getCurrentRepository());

        if (dialog.open() == IDialogConstants.OK_ID) {
            final Shelveset selectedShelveset = dialog.getSelectedShelveset();
            Check.notNull(selectedShelveset, "selectedShelveset"); //$NON-NLS-1$

            final String currentUserName =
                getCurrentRepository().getVersionControlClient().getConnection().getAuthorizedIdentity().getUniqueName();
            final String shelvesetUserName = selectedShelveset.getOwnerName();

            // Append the user name to the shelveset name if the selected user
            // and current user are not the same.
            if (UserNameUtil.equals(currentUserName, shelvesetUserName)) {
                shelvesetNameText.setText(selectedShelveset.getName());
            } else {
                final WorkspaceSpec spec =
                    new WorkspaceSpec(selectedShelveset.getName(), selectedShelveset.getOwnerDisplayName());

                shelvesetNameText.setText(spec.toString());
            }

            CodeMarkerDispatch.dispatch(CODEMARKER_SHELVESET_SELECTED);
        }
    }

    private void createShelveset() {
        final AbstractShelveTask task = new ShelveWithPromptTask(getShell(), getCurrentRepository());
        final IStatus status = task.run();

        if (status.getSeverity() == IStatus.OK) {
            final String shelvesetName = task.getSelectedShelvesetName();
            if (shelvesetName != null && shelvesetName.length() > 0) {
                shelvesetNameText.setText(shelvesetName);
            }

            CodeMarkerDispatch.dispatch(CODEMARKER_SHELVESET_SELECTED);
        }
    }

    private int loadBuildDefinitions() {
        int selectedIndex = 0;
        for (int i = 0; i < buildDefinitions.length; i++) {
            buildDefinitionCombo.add(buildDefinitions[i].getName());
            if (selectedBuildDefinition.getFullPath().equals(buildDefinitions[i].getFullPath())) {
                selectedIndex = i;
            }
        }
        buildDefinitionCombo.select(selectedIndex);
        return selectedIndex;
    }

    private void loadBuildControllers() {
        if (buildControllers != null) {
            for (int i = 0; i < buildControllers.length; i++) {
                buildControllerCombo.add(TeamBuildHelper.getControllerDisplayString(buildServer, buildControllers[i]));
            }
        }
    }

    private void selectDefaultBuildController() {
        final IBuildDefinition buildDefinition = getSelectedBuildDefinition();
        int selectIndex = 0;
        for (int i = 0; i < buildControllers.length; i++) {
            if (buildControllers[i].equals(buildDefinition.getBuildController())) {
                selectIndex = i;
            }
        }
        selectedBuildController = buildControllers[selectIndex];
        buildControllerCombo.select(selectIndex);
    }

    private void selectDefaultPriority() {
        final String[] priorities = priority.getItems();
        for (int i = 0; i < priorities.length; i++) {
            if (priorities[i].equals(defaultQueuePriorityText)) {
                priority.select(i);
            }
        }
    }

    private int loadQueuePriorities() {
        final String[] priorities = buildServer.getDisplayTextValues(QueuePriority.class);
        int selectedIndex = 0;
        for (int i = 0; i < priorities.length; i++) {
            priority.add(priorities[i]);
            if (priorities[i].equals(defaultQueuePriorityText)) {
                selectedIndex = i;
            }
        }
        priority.select(selectedIndex);
        return selectedIndex;
    }

    private void setBuddyBuildEnablement() {
        if (shelvesetComposite != null) {
            final boolean enabled = isBuddyBuild();

            shelvesetLabel.setEnabled(enabled);
            shelvesetNameText.setEnabled(enabled);
            findShelvesetButton.setEnabled(enabled);
            createShelvesetButton.setEnabled(enabled);
            checkinAfterSuccessButton.setEnabled(enabled);
        }
    }

    /**
     * Get an <code>Image</code> from the provide SWT image constant.
     *
     * @param imageID
     *        the SWT image constant
     * @return image the image
     */
    private Image getSWTImage(final int imageID) {
        Shell shell = getShell();
        final Display display;
        if (shell == null) {
            shell = getParentShell();
        }
        if (shell == null) {
            display = Display.getCurrent();
        } else {
            display = shell.getDisplay();
        }

        final Image[] image = new Image[1];
        display.syncExec(new Runnable() {
            @Override
            public void run() {
                image[0] = display.getSystemImage(imageID);
            }
        });

        return image[0];
    }

    public int getBuildDefinitionCount() {
        if (buildDefinitions == null) {
            return 0;
        }
        return buildDefinitions.length;
    }

    private TFSRepository getCurrentRepository() {
        return TFSCommonUIClientPlugin.getDefault().getProductPlugin().getRepositoryManager().getDefaultRepository();
    }
}
