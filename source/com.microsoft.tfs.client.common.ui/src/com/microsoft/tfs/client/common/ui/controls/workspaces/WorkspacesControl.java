// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.controls.workspaces;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.osgi.service.prefs.Preferences;

import com.microsoft.tfs.client.common.codemarker.CodeMarker;
import com.microsoft.tfs.client.common.codemarker.CodeMarkerDispatch;
import com.microsoft.tfs.client.common.commands.CreateWorkspaceCommand;
import com.microsoft.tfs.client.common.commands.DeleteWorkspacesCommand;
import com.microsoft.tfs.client.common.commands.QueryLocalWorkspacesCommand;
import com.microsoft.tfs.client.common.commands.UpdateWorkspaceCommand;
import com.microsoft.tfs.client.common.framework.command.ICommandExecutor;
import com.microsoft.tfs.client.common.repository.RepositoryManager;
import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.TFSCommonUIClientPlugin;
import com.microsoft.tfs.client.common.ui.controls.generic.BaseControl;
import com.microsoft.tfs.client.common.ui.dialogs.workspaces.WorkspaceEditDialog;
import com.microsoft.tfs.client.common.ui.framework.command.UICommandExecutorFactory;
import com.microsoft.tfs.client.common.ui.framework.helper.ButtonHelper;
import com.microsoft.tfs.client.common.ui.framework.helper.MessageBoxHelpers;
import com.microsoft.tfs.client.common.ui.framework.layout.GridDataBuilder;
import com.microsoft.tfs.client.common.ui.framework.validation.ButtonValidatorBinding;
import com.microsoft.tfs.client.common.ui.helpers.AutomationIDHelper;
import com.microsoft.tfs.client.common.ui.tasks.vc.GetTask;
import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.core.clients.versioncontrol.GetOptions;
import com.microsoft.tfs.core.clients.versioncontrol.WorkspaceLocation;
import com.microsoft.tfs.core.clients.versioncontrol.WorkspaceOptions;
import com.microsoft.tfs.core.clients.versioncontrol.WorkspacePermissionProfile;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.GetRequest;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.WorkingFolder;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.WorkingFolderComparator;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.WorkingFolderComparatorType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.LatestVersionSpec;
import com.microsoft.tfs.jni.helpers.LocalHost;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.listeners.SingleListenerFacade;
import com.microsoft.tfs.util.valid.Validatable;
import com.microsoft.tfs.util.valid.Validator;
import com.microsoft.tfs.util.valid.ValidatorWrapper;

public class WorkspacesControl extends BaseControl implements Validatable {
    private static final String PREFS_NODE_LAST_WORKSPACE_NAME = "LastWorkspaceName"; //$NON-NLS-1$

    public static final String WORKSPACES_TABLE_ID = "WorkspacesControl.workspacesTable"; //$NON-NLS-1$
    public static final String ADD_BUTTON_ID = "WorkspacesControl.addButton"; //$NON-NLS-1$
    public static final String EDIT_BUTTON_ID = "WorkspacesControl.editButton"; //$NON-NLS-1$
    public static final String REMOVE_BUTTON_ID = "WorkspacesControl.removeButton"; //$NON-NLS-1$
    public static final String REFRESH_BUTTON_ID = "WorkspacesControl.refreshButton"; //$NON-NLS-1$

    public static final CodeMarker CODEMARKER_REFRESH_COMPLETE =
        new CodeMarker("com.microsoft.tfs.client.common.ui.controls.workspaces.WorkspacesControl#refreshComplete"); //$NON-NLS-1$

    private final String viewDataKey;
    private final ICommandExecutor commandExecutor;

    private final ValidatorWrapper validator;

    private final SingleListenerFacade editListeners = new SingleListenerFacade(WorkspaceEditListener.class);

    private final WorkspacesTable workspacesTable;
    private final Button addButton;
    private final Button refreshButton;
    private final Button removeButton;

    private TFSTeamProjectCollection connection;
    private boolean enableClearWorkspace;

    /*
     * A client may set workspaces which cannot be removed or modified, except
     * for workfolds. (Ie, the plugin does not allow you to delete the workspace
     * you're using out from under it.)
     */
    private Workspace[] immutableWorkspaces = null;

    public WorkspacesControl(final Composite parent, final int style) {
        this(parent, style, null, null);
    }

    public WorkspacesControl(final Composite parent, final int style, final String viewDataKey) {
        this(parent, style, viewDataKey, null);
    }

    public WorkspacesControl(final Composite parent, final int style, final ICommandExecutor commandExecutor) {
        this(parent, style, null, commandExecutor);
    }

    public WorkspacesControl(
        final Composite parent,
        final int style,
        final String viewDataKey,
        ICommandExecutor commandExecutor) {
        super(parent, style);

        this.viewDataKey = viewDataKey;

        if (commandExecutor == null) {
            commandExecutor = UICommandExecutorFactory.newUICommandExecutor(getShell());
        }

        this.commandExecutor = commandExecutor;

        final GridLayout layout = new GridLayout(4, false);
        layout.marginWidth = 0;
        layout.marginHeight = 0;
        layout.horizontalSpacing = getHorizontalSpacing();
        layout.verticalSpacing = getVerticalSpacing();
        setLayout(layout);

        workspacesTable = new WorkspacesTable(this, SWT.MULTI | SWT.FULL_SELECTION, viewDataKey);
        AutomationIDHelper.setWidgetID(workspacesTable.getTable(), WORKSPACES_TABLE_ID);
        GridDataBuilder.newInstance().hSpan(layout).fill().grab().hHint(130).applyTo(workspacesTable);
        workspacesTable.getTable().addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(final KeyEvent e) {
                WorkspacesControl.this.onWorkspacesTableKeyPressed(e);
            }
        });

        workspacesTable.addSelectionChangedListener(new ISelectionChangedListener() {
            @Override
            public void selectionChanged(final SelectionChangedEvent event) {
                final Workspace[] workspaces = workspacesTable.getSelectedWorkspaces();
                boolean enable = workspaces.length > 0;

                for (final Workspace workspace : workspaces) {
                    if (!workspace.hasAdministerPermission()) {
                        enable = false;
                        break;
                    }
                }

                removeButton.setEnabled(enable);
            }
        });

        addButton = new Button(this, SWT.NONE);
        AutomationIDHelper.setWidgetID(addButton, ADD_BUTTON_ID);
        addButton.setText(Messages.getString("WorkspacesControl.AddButtonText")); //$NON-NLS-1$
        addButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                addClicked();
            }
        });
        addButton.setEnabled(false);

        final Button editButton = new Button(this, SWT.NONE);
        AutomationIDHelper.setWidgetID(editButton, EDIT_BUTTON_ID);
        editButton.setText(Messages.getString("WorkspacesControl.EditButtonText")); //$NON-NLS-1$
        editButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                editClicked();
            }
        });

        removeButton = new Button(this, SWT.NONE);
        AutomationIDHelper.setWidgetID(removeButton, REMOVE_BUTTON_ID);
        removeButton.setText(Messages.getString("WorkspacesControl.RemoveButtonText")); //$NON-NLS-1$
        removeButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                removeClicked();
            }
        });

        refreshButton = new Button(this, SWT.NONE);
        AutomationIDHelper.setWidgetID(refreshButton, REFRESH_BUTTON_ID);
        refreshButton.setText(Messages.getString("WorkspacesControl.RefreshButtonText")); //$NON-NLS-1$
        refreshButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                refreshClicked();
            }
        });
        refreshButton.setEnabled(false);

        ButtonHelper.setButtonsToButtonBarSize(new Button[] {
            addButton,
            editButton,
            removeButton,
            refreshButton
        });

        new ButtonValidatorBinding(editButton).bind(workspacesTable.getSingleSelectionValidator());
        new ButtonValidatorBinding(removeButton).bind(workspacesTable.getSelectionValidator());

        validator = new ValidatorWrapper(
            workspacesTable.getSingleSelectionValidator(),
            Messages.getString("WorkspacesControl.MustSelectOneWorkspace")); //$NON-NLS-1$

        createContextMenu();

        addDisposeListener(new DisposeListener() {
            @Override
            public void widgetDisposed(final DisposeEvent e) {
                WorkspacesControl.this.onDisposed(e);
            }
        });
    }

    public void setImmutableWorkspaces(final Workspace[] immutableWorkspaces) {
        this.immutableWorkspaces = immutableWorkspaces;
    }

    public void addEditListener(final WorkspaceEditListener listener) {
        editListeners.addListener(listener);
    }

    public void removeEditListener(final WorkspaceEditListener listener) {
        editListeners.removeListener(listener);
    }

    @Override
    public Validator getValidator() {
        return validator;
    }

    public WorkspacesTable getWorkspacesTable() {
        return workspacesTable;
    }

    public void refresh(
        final TFSTeamProjectCollection connection,
        final boolean createWorkspaceIfNone,
        final boolean autoSelect) {
        this.connection = connection;

        addButton.setEnabled(connection != null);
        refreshButton.setEnabled(connection != null);

        final Workspace[] previouslySelectedWorkspaces = workspacesTable.getSelectedWorkspaces();

        try {
            validator.suspendValidation();

            workspacesTable.setWorkspaces(null);

            if (connection != null) {
                final QueryLocalWorkspacesCommand queryCommand = new QueryLocalWorkspacesCommand(connection);
                if (commandExecutor.execute(queryCommand).getSeverity() != IStatus.OK) {
                    return;
                }
                Workspace[] workspaces = queryCommand.getWorkspaces();

                if (createWorkspaceIfNone && (workspaces == null || workspaces.length == 0)) {
                    /*
                     * No workspaces - create a default one.
                     */
                    final CreateWorkspaceCommand createCommand = new CreateWorkspaceCommand(
                        connection,
                        null,
                        LocalHost.getShortName(),
                        null,
                        null,
                        null,
                        WorkspacePermissionProfile.getPrivateProfile());
                    if (commandExecutor.execute(createCommand).getSeverity() != IStatus.OK) {
                        return;
                    }
                    workspaces = new Workspace[] {
                        createCommand.getWorkspace()
                    };
                }

                workspacesTable.setWorkspaces(workspaces);
                workspacesTable.setSelectedWorkspaces(previouslySelectedWorkspaces);

                if (autoSelect && workspacesTable.getSelectionCount() == 0) {
                    // Use the default workspace as the initial selection if one
                    // exists. Otherwise retrieve the last referenced workspace
                    // name and set it as the default selection.
                    final RepositoryManager manager =
                        TFSCommonUIClientPlugin.getDefault().getProductPlugin().getRepositoryManager();
                    final TFSRepository defaultRepository = manager.getDefaultRepository();

                    if (defaultRepository != null) {
                        workspacesTable.setSelectedWorkspace(defaultRepository.getWorkspace());
                    } else {
                        final String lastWorkspaceName =
                            getPreferencesNode(viewDataKey, connection).get(PREFS_NODE_LAST_WORKSPACE_NAME, null);
                        if (lastWorkspaceName != null) {
                            workspacesTable.setSelectedWorkspace(lastWorkspaceName);
                        }
                    }

                    if (workspacesTable.getSelectionCount() == 0) {
                        workspacesTable.selectFirst();
                    }
                }

                workspacesTable.setFocus();
            }
        } finally {
            validator.resumeValidation();
            CodeMarkerDispatch.dispatch(CODEMARKER_REFRESH_COMPLETE);
        }
    }

    public void editSelectedWorkspace() {
        editClicked();
    }

    private void onWorkspacesTableKeyPressed(final KeyEvent e) {
        if (e.keyCode == SWT.DEL && e.stateMask == SWT.NONE) {
            removeClicked();
        }
    }

    private void onDisposed(final DisposeEvent e) {
        if (connection == null) {
            return;
        }

        final String name = workspacesTable.getSelectedWorkspaceName();
        if (name == null) {
            return;
        }

        final Preferences prefsNode = getPreferencesNode(viewDataKey, connection);

        prefsNode.put(PREFS_NODE_LAST_WORKSPACE_NAME, name);
    }

    private void createContextMenu() {
        final IAction clearWorkspaceAction = new Action() {
            @Override
            public void run() {
                if (MessageBoxHelpers.dialogConfirmPrompt(
                    getShell(),
                    Messages.getString("WorkspacesControl.ConfirmClearDialogTitle"), //$NON-NLS-1$
                    Messages.getString("WorkspacesControl.ConfirmClearDialogBody"))) //$NON-NLS-1$
                {
                    clearWorkspace();
                }
            }
        };
        clearWorkspaceAction.setText(Messages.getString("WorkspacesControl.ClearWorkspaceActionText")); //$NON-NLS-1$

        workspacesTable.getContextMenu().addMenuListener(new IMenuListener() {
            @Override
            public void menuAboutToShow(final IMenuManager manager) {
                final int selectionCount = workspacesTable.getSelectionCount();

                if (enableClearWorkspace && selectionCount == 1) {
                    manager.add(clearWorkspaceAction);
                }
            }
        });

        workspacesTable.getTable().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseDown(final MouseEvent e) {
                enableClearWorkspace = ((e.stateMask & SWT.SHIFT) > 0);
            }
        });
    }

    private void refreshClicked() {
        refresh(connection, false, false);
    }

    private boolean isWorkspaceImmutable(final Workspace workspace) {
        Check.notNull(workspace, "workspace"); //$NON-NLS-1$

        if (immutableWorkspaces == null) {
            return false;
        }

        for (int i = 0; i < immutableWorkspaces.length; i++) {
            if (workspace.equals(immutableWorkspaces[i])) {
                return true;
            }
        }

        return false;
    }

    private void removeClicked() {
        /* Check to see if we're being asked to remove an immutable workspace */
        final Workspace[] workspacesToRemove = workspacesTable.getSelectedWorkspaces();

        for (int i = 0; i < workspacesToRemove.length; i++) {
            if (isWorkspaceImmutable(workspacesToRemove[i])) {
                final String titleFormat = Messages.getString("WorkspacesControl.CannotDeleteDialogTitleFormat"); //$NON-NLS-1$
                final String title = MessageFormat.format(titleFormat, workspacesToRemove[i].getName());
                final String messageFormat = Messages.getString("WorkspacesControl.CannotDeleteDialogTextFormat"); //$NON-NLS-1$
                final String message = MessageFormat.format(messageFormat, workspacesToRemove[i].getName());
                MessageDialog.openError(getShell(), title, message);
                return;
            }
        }

        String title;
        String message;
        if (workspacesTable.getSelectedWorkspaces().length == 1) {
            title = Messages.getString("WorkspacesControl.SingleDeleteConfirmDialogTitle"); //$NON-NLS-1$
            message = Messages.getString("WorkspacesControl.SingleDeleteConfirmDialogText"); //$NON-NLS-1$
        } else {
            title = Messages.getString("WorkspacesControl.MultiDeleteConfirmDialogTitle"); //$NON-NLS-1$
            message = Messages.getString("WorkspacesControl.MultiDeleteConfirmDialogText"); //$NON-NLS-1$
        }

        if (!MessageBoxHelpers.dialogConfirmPrompt(getShell(), title, message)) {
            return;
        }

        removeSelectedWorkspaces();
    }

    private void editClicked() {
        final Workspace selectedWorkspace = workspacesTable.getSelectedWorkspace();

        /* See if this workspace is immutable */
        final boolean immutable = isWorkspaceImmutable(selectedWorkspace);

        final WorkspaceData dataToEdit = new WorkspaceData(selectedWorkspace);
        final WorkspaceData oldData = new WorkspaceData(selectedWorkspace);

        boolean keepGoing = true;
        while (keepGoing) {
            final WorkspaceEditDialog dialog = new WorkspaceEditDialog(getShell(), true, dataToEdit, connection);
            dialog.setImmutable(immutable);

            if (IDialogConstants.OK_ID == dialog.open()) {
                final UpdateWorkspaceCommand command = new UpdateWorkspaceCommand(
                    selectedWorkspace,
                    dataToEdit.getWorkspaceDetails().getName(),
                    dataToEdit.getWorkspaceDetails().getComment(),
                    dataToEdit.getWorkingFolderDataCollection().createWorkingFolders(),
                    dataToEdit.getWorkspaceDetails().getWorkspaceOptions(),
                    dataToEdit.getWorkspaceDetails().getWorkspaceLocation(),
                    dataToEdit.getWorkspaceDetails().getPermissionProfile());

                if (commandExecutor.execute(command).isOK()) {
                    final WorkspaceEditEvent event = new WorkspaceEditEvent(this, oldData, selectedWorkspace);
                    final WorkspaceEditListener listener = (WorkspaceEditListener) editListeners.getListener();
                    listener.onWorkspaceEdited(event);

                    workspacesTable.refresh();
                    keepGoing = false;

                    /*
                     * Check to see if any folders changed.
                     */
                    final WorkingFolder[] oldFolders = oldData.getWorkingFolderDataCollection().createWorkingFolders();
                    final WorkingFolder[] newFolders =
                        dataToEdit.getWorkingFolderDataCollection().createWorkingFolders();

                    Arrays.sort(oldFolders, new WorkingFolderComparator(WorkingFolderComparatorType.SERVER_PATH));
                    Arrays.sort(newFolders, new WorkingFolderComparator(WorkingFolderComparatorType.SERVER_PATH));
                    final boolean foldersChanged = Arrays.equals(oldFolders, newFolders) == false;

                    final WorkspaceOptions oldOptions = oldData.getWorkspaceDetails().getWorkspaceOptions();
                    final WorkspaceOptions newOptions = dataToEdit.getWorkspaceDetails().getWorkspaceOptions();

                    final boolean fileTimeChanged = !oldOptions.contains(WorkspaceOptions.SET_FILE_TO_CHECKIN)
                        && newOptions.contains(WorkspaceOptions.SET_FILE_TO_CHECKIN)
                        && newFolders.length > 0;

                    final GetOptions getOptions = fileTimeChanged ? GetOptions.GET_ALL : GetOptions.NONE;

                    if (foldersChanged || fileTimeChanged) {
                        /*
                         * Prompt to get latest.
                         *
                         * Prefer the file time message if folders also changed
                         * since the force get will handle everything.
                         */
                        final boolean getNow = MessageDialog.openQuestion(
                            getShell(),
                            Messages.getString("WorkspacesControl.WorkspaceModifiedDialogTitle"), //$NON-NLS-1$
                            fileTimeChanged ? Messages.getString("WorkspacesControl.SetFileTimeToCheckinGetPrompt") //$NON-NLS-1$
                                : Messages.getString("WorkspacesControl.WorkspaceChangedMessage")); //$NON-NLS-1$

                        if (getNow) {
                            final RepositoryManager manager =
                                TFSCommonUIClientPlugin.getDefault().getProductPlugin().getRepositoryManager();
                            TFSRepository repository = manager.getRepository(selectedWorkspace);
                            if (repository == null) {
                                repository = new TFSRepository(selectedWorkspace);
                            }

                            /*
                             * GetTask will not prompt for
                             * "all files up to date" like GetLatestTask does,
                             * and it accepts GetOptions. A GetRequest with a
                             * null item spec means "get the whole workspace."
                             */
                            final GetTask task = new GetTask(getShell(), repository, new GetRequest[] {
                                new GetRequest(null, LatestVersionSpec.INSTANCE)
                            }, getOptions);
                            task.run();
                        }
                    }
                }
            } else {
                keepGoing = false;
            }
        }

        workspacesTable.setFocus();
    }

    private void addClicked() {
        final Workspace[] workspaces = workspacesTable.getWorkspaces();
        /*
         * Compute a default name for the new workspace.
         */
        final String defaultWorkspaceName = Workspace.computeNewWorkspaceName(LocalHost.getShortName(), workspaces);
        final WorkspaceData workspaceData = new WorkspaceData(connection, defaultWorkspaceName);

        boolean keepGoing = true;
        while (keepGoing) {
            final WorkspaceEditDialog dialog = new WorkspaceEditDialog(getShell(), false, workspaceData, connection);

            if (IDialogConstants.OK_ID == dialog.open()) {
                final CreateWorkspaceCommand command = new CreateWorkspaceCommand(
                    connection,
                    workspaceData.getWorkingFolderDataCollection().createWorkingFolders(),
                    workspaceData.getWorkspaceDetails().getName(),
                    workspaceData.getWorkspaceDetails().getComment(),
                    workspaceData.getWorkspaceDetails().getWorkspaceLocation(),
                    workspaceData.getWorkspaceDetails().getWorkspaceOptions(),
                    workspaceData.getWorkspaceDetails().getPermissionProfile());

                if (commandExecutor.execute(command).isOK()) {
                    final Workspace[] newWorkspaces = new Workspace[workspaces.length + 1];
                    System.arraycopy(workspaces, 0, newWorkspaces, 0, workspaces.length);
                    final Workspace newWorkspace = command.getWorkspace();
                    newWorkspaces[workspaces.length] = newWorkspace;

                    workspacesTable.setWorkspaces(newWorkspaces);
                    workspacesTable.setSelectedWorkspace(newWorkspace);
                    workspacesTable.setFocus();
                    keepGoing = false;
                }
            } else {
                keepGoing = false;
            }
        }
    }

    private boolean removeSelectedWorkspaces() {
        final Workspace[] workspacesToRemove = workspacesTable.getSelectedWorkspaces();

        final DeleteWorkspacesCommand command = new DeleteWorkspacesCommand(workspacesToRemove);

        final IStatus status = commandExecutor.execute(command);

        final List<Workspace> newWorkspaceList = new ArrayList<Workspace>();
        newWorkspaceList.addAll(Arrays.asList(workspacesTable.getWorkspaces()));
        newWorkspaceList.removeAll(Arrays.asList(command.getDeletedWorkspaces()));
        final Workspace[] newWorkspaces = newWorkspaceList.toArray(new Workspace[newWorkspaceList.size()]);

        workspacesTable.setWorkspaces(newWorkspaces);

        return status.isOK();
    }

    private void clearWorkspace() {
        final Workspace selectedWorkspace = workspacesTable.getSelectedWorkspace();
        final String name = selectedWorkspace.getName();
        final WorkspaceLocation location = selectedWorkspace.getLocation();
        final WorkspaceOptions options = selectedWorkspace.getOptions();
        final WorkspacePermissionProfile permissionProfile = selectedWorkspace.getPermissionsProfile();

        try {
            validator.suspendValidation();

            if (removeSelectedWorkspaces()) {
                final CreateWorkspaceCommand command =
                    new CreateWorkspaceCommand(connection, null, name, "", location, options, permissionProfile); //$NON-NLS-1$

                if (commandExecutor.execute(command).isOK()) {
                    final Workspace[] currentWorkspaces = workspacesTable.getWorkspaces();
                    final Workspace[] newWorkspaces = new Workspace[currentWorkspaces.length + 1];
                    System.arraycopy(currentWorkspaces, 0, newWorkspaces, 0, currentWorkspaces.length);
                    final Workspace newWorkspace = command.getWorkspace();
                    newWorkspaces[currentWorkspaces.length] = newWorkspace;

                    workspacesTable.setWorkspaces(newWorkspaces);
                    workspacesTable.setFocus();

                    workspacesTable.setSelectedWorkspace(newWorkspace);
                    workspacesTable.setFocus();
                }
            }
        } finally {
            validator.resumeValidation();
        }
    }

    private static Preferences getPreferencesNode(final String viewDataKey, final TFSTeamProjectCollection connection) {
        final IEclipsePreferences prefs = new InstanceScope().getNode(TFSCommonUIClientPlugin.PLUGIN_ID);
        Preferences node = prefs.node("WorkspacesControl"); //$NON-NLS-1$
        if (viewDataKey != null) {
            node = node.node(viewDataKey);
        }
        node = node.node(connection.getInstanceID().toString());
        node = node.node(connection.getAuthorizedTFSUser().toString());
        return node;
    }
}
