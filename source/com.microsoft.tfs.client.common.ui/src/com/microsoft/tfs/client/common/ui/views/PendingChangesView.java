// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.views;

import java.text.MessageFormat;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.IContributionManager;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ContributionItemFactory;
import org.eclipse.ui.part.IShowInSource;
import org.eclipse.ui.part.ShowInContext;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import com.microsoft.tfs.client.common.codemarker.CodeMarker;
import com.microsoft.tfs.client.common.codemarker.CodeMarkerDispatch;
import com.microsoft.tfs.client.common.repository.RepositoryManager;
import com.microsoft.tfs.client.common.repository.RepositoryManagerAdapter;
import com.microsoft.tfs.client.common.repository.RepositoryManagerEvent;
import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.TFSCommonUIClientPlugin;
import com.microsoft.tfs.client.common.ui.TFSCommonUIImages;
import com.microsoft.tfs.client.common.ui.controls.vc.WorkspaceToolbarPulldownAction;
import com.microsoft.tfs.client.common.ui.controls.vc.changes.ChangeItem;
import com.microsoft.tfs.client.common.ui.controls.vc.changes.ChangeItemType;
import com.microsoft.tfs.client.common.ui.controls.vc.changes.RepositoryChangeItemProvider;
import com.microsoft.tfs.client.common.ui.controls.vc.checkin.AbstractCheckinSubControl;
import com.microsoft.tfs.client.common.ui.controls.vc.checkin.CheckinControl;
import com.microsoft.tfs.client.common.ui.controls.vc.checkin.CheckinControl.ValidationResult;
import com.microsoft.tfs.client.common.ui.controls.vc.checkin.CheckinControlOptions;
import com.microsoft.tfs.client.common.ui.controls.vc.checkin.CheckinSubControlType;
import com.microsoft.tfs.client.common.ui.controls.vc.checkin.SourceFilesCheckinControl;
import com.microsoft.tfs.client.common.ui.controls.vc.checkin.actions.ComparePendingChangeWithLatestVersionAction;
import com.microsoft.tfs.client.common.ui.controls.vc.checkin.actions.ComparePendingChangeWithWorkspaceVersionAction;
import com.microsoft.tfs.client.common.ui.controls.vc.checkin.actions.RefreshRepositoryPendingChangesAction;
import com.microsoft.tfs.client.common.ui.controls.vc.checkin.actions.UndoPendingChangesAction;
import com.microsoft.tfs.client.common.ui.controls.vc.checkin.actions.UndoUnchangedPendingChangesAction;
import com.microsoft.tfs.client.common.ui.controls.vc.checkin.actions.ViewPendingChangeAction;
import com.microsoft.tfs.client.common.ui.controls.vc.checkin.actions.ViewVersionType;
import com.microsoft.tfs.client.common.ui.controls.vc.checkinpolicies.PolicyFailureData;
import com.microsoft.tfs.client.common.ui.framework.action.StandardActionConstants;
import com.microsoft.tfs.client.common.ui.framework.action.ToolbarPulldownAction;
import com.microsoft.tfs.client.common.ui.framework.compare.CompareUIType;
import com.microsoft.tfs.client.common.ui.framework.helper.UIHelpers;
import com.microsoft.tfs.client.common.ui.framework.validation.ActionValidatorBinding;
import com.microsoft.tfs.client.common.ui.helpers.ToggleMessageHelper;
import com.microsoft.tfs.client.common.ui.prefs.UIPreferenceConstants;
import com.microsoft.tfs.client.common.ui.tasks.vc.AbstractShelveTask;
import com.microsoft.tfs.client.common.ui.tasks.vc.CheckinTask;
import com.microsoft.tfs.client.common.ui.tasks.vc.ConflictResolutionTask;
import com.microsoft.tfs.client.common.ui.tasks.vc.ShelveWithPromptTask;
import com.microsoft.tfs.client.common.ui.tasks.vc.UnshelveTask;
import com.microsoft.tfs.core.ConnectivityFailureStatusChangeListener;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PendingChange;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PolicyOverrideInfo;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.WorkItemCheckinInfo;
import com.microsoft.tfs.core.pendingcheckin.PendingCheckin;

/**
 * @deprecated Will be removed from the product soon, use
 *             {@link TeamExplorerView} isntead
 */
@Deprecated
public class PendingChangesView extends AbstractCheckinControlView implements IShowInSource {
    public static final String ID = "com.microsoft.tfs.client.common.ui.views.PendingChangesView"; //$NON-NLS-1$

    public static final CodeMarker BEFORE_CONFIRM_DIALOG =
        new CodeMarker("com.microsoft.tfs.client.common.ui.views.PendingChangesView#beforeConfirmDialog"); //$NON-NLS-1$

    private static final String TOOLBAR_GROUP_WORKSPACE = "toolbar-group-workspace"; //$NON-NLS-1$
    private static final String TOOLBAR_GROUP_CHECKIN = "toolbar-group-checkin"; //$NON-NLS-1$
    private static final String TOOLBAR_GROUP_SHELVE = "toolbar-group-shelve"; //$NON-NLS-1$
    private static final String TOOLBAR_GROUP_RESOLVE = "toolbar-group-resolve"; //$NON-NLS-1$
    private static final String TOOLBAR_GROUP_COMPARE = "toolbar-group-compare"; //$NON-NLS-1$
    private static final String TOOLBAR_GROUP_REFRESH = "toolbar-group-refresh"; //$NON-NLS-1$

    private TFSRepository repository;

    private IAction checkinAction;
    private IAction shelveAction;
    private IAction unshelveAction;
    private IAction resolveConflictsAction;

    private ActionContributionItem workspaceActionContribution;
    private ActionContributionItem checkinActionContribution;
    private ActionContributionItem shelveActionContribution;
    private ActionContributionItem unshelveActionContribution;
    private ActionContributionItem resolveConflictsActionContribution;

    private WorkspaceToolbarPulldownAction workspaceToolbarAction;

    private ToolbarPulldownAction viewToolbarAction;
    private ViewPendingChangeAction viewAction;
    private ViewPendingChangeAction viewUnmodifiedAction;
    private ViewPendingChangeAction viewLatestAction;

    private ToolbarPulldownAction compareToolbarAction;
    private ComparePendingChangeWithLatestVersionAction compareWithLatestAction;
    private ComparePendingChangeWithWorkspaceVersionAction compareWithWorkspaceAction;

    /*
     * These actions are hooked into context menus for the source files sub
     * control.
     */
    private UndoPendingChangesAction undoAction;

    private UndoUnchangedPendingChangesAction undoUnchangedAction;
    private RefreshRepositoryPendingChangesAction refreshPendingChangesAction;
    private IAction toggleButtonTextAction;

    private final PendingChangesConnectionListener connectionListener = new PendingChangesConnectionListener();

    /*
     * Allows us to defer repository settings until the view has been created.
     */
    private final Object drawStateLock = new Object();
    private boolean drawState = false;
    private TFSRepository drawRepository;

    @Override
    public void init(final IViewSite site) throws PartInitException {
        super.init(site);
        setInitialRepository();
    }

    @Override
    public void init(final IViewSite site, final IMemento memento) throws PartInitException {
        super.init(site, memento);
        setInitialRepository();
    }

    private void setInitialRepository() throws PartInitException {
        RepositoryManager repositoryManager;

        try {
            repositoryManager = TFSCommonUIClientPlugin.getDefault().getProductPlugin().getRepositoryManager();
            repositoryManager.addListener(connectionListener);

            setRepository(repositoryManager.getDefaultRepository());
        } catch (final Throwable t) {
            throw new PartInitException(Messages.getString("PendingChangesView.PartInitizationFailed"), t); //$NON-NLS-1$
        }
    }

    @Override
    public void createPartControl(final Composite parent) {
        super.createPartControl(parent);

        synchronized (drawStateLock) {
            final boolean needsPaint = (drawState == false);

            drawState = true;

            if (needsPaint) {
                setRepository(drawRepository);
            }
        }
    }

    @Override
    protected void setupToolbar(final IToolBarManager toolbar) {
        toolbar.add(new Separator(TOOLBAR_GROUP_WORKSPACE));
        toolbar.add(new Separator(TOOLBAR_GROUP_CHECKIN));
        toolbar.add(new Separator(TOOLBAR_GROUP_SHELVE));
        toolbar.add(new Separator(TOOLBAR_GROUP_RESOLVE));
        toolbar.add(new Separator(CheckinControl.SUBCONTROL_CONTRIBUTION_GROUP_NAME));
        toolbar.add(new Separator(TOOLBAR_GROUP_COMPARE));
        toolbar.add(new Separator(TOOLBAR_GROUP_REFRESH));
        toolbar.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
    }

    @Override
    public ShowInContext getShowInContext() {
        final IStructuredSelection selection =
            (IStructuredSelection) getViewSite().getSelectionProvider().getSelection();

        final Object[] elements = selection.toArray();

        for (int i = 0; i < elements.length; i++) {
            if (elements[i] instanceof ChangeItem) {
                final ChangeItem changeItem = (ChangeItem) elements[i];
                if (changeItem.getType() == ChangeItemType.PENDING) {
                    final PendingChange pendingChange = changeItem.getPendingChange();
                    final IResource resource = getResourceForPendingChange(pendingChange);

                    if (resource != null) {
                        elements[i] = resource;
                    }
                }
            }
        }

        return new ShowInContext(null, new StructuredSelection(elements));
    }

    /* TODO: extension point goes here */
    private IResource getResourceForPendingChange(final PendingChange pendingChange) {
        /* TFSEclipseResources.resourceForPendingChange(pendingChange) */
        return null;
    }

    @Override
    protected void createActions() {
        final SourceFilesCheckinControl sourceFilesSubControl = getCheckinControl().getSourceFilesSubControl();
        final ISelectionProvider sourceFilesSelectionProvider = sourceFilesSubControl.getSelectionProvider();

        workspaceToolbarAction = new WorkspaceToolbarPulldownAction(getSite().getShell());
        workspaceToolbarAction.setImageDescriptor(TFSCommonUIImages.getImageDescriptor(TFSCommonUIImages.IMG_OPTIONS));

        compareToolbarAction = new ToolbarPulldownAction();
        compareToolbarAction.setImageDescriptor(TFSCommonUIImages.getImageDescriptor(TFSCommonUIImages.IMG_COMPARE));

        compareWithWorkspaceAction = new ComparePendingChangeWithWorkspaceVersionAction(
            sourceFilesSelectionProvider,
            CompareUIType.EDITOR,
            getCheckinControl().getShell());
        compareToolbarAction.addSubAction(compareWithWorkspaceAction);

        compareWithLatestAction = new ComparePendingChangeWithLatestVersionAction(
            sourceFilesSelectionProvider,
            CompareUIType.EDITOR,
            getCheckinControl().getShell());
        compareToolbarAction.addSubAction(compareWithLatestAction);
        compareToolbarAction.setDefaultSubAction(compareWithLatestAction);

        viewToolbarAction = new ToolbarPulldownAction();
        viewToolbarAction.setImageDescriptor(
            PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(ISharedImages.IMG_OBJ_FOLDER));

        viewAction = new ViewPendingChangeAction(sourceFilesSelectionProvider, false, ViewVersionType.DEFAULT);
        viewToolbarAction.addSubAction(viewAction);
        viewToolbarAction.setDefaultSubAction(viewAction);

        viewUnmodifiedAction =
            new ViewPendingChangeAction(sourceFilesSelectionProvider, false, ViewVersionType.UNMODIFIED);
        viewToolbarAction.addSubAction(viewUnmodifiedAction);

        viewLatestAction = new ViewPendingChangeAction(sourceFilesSelectionProvider, false, ViewVersionType.LATEST);
        viewToolbarAction.addSubAction(viewLatestAction);

        undoAction = new UndoPendingChangesAction(sourceFilesSelectionProvider, getViewSite().getShell());

        undoUnchangedAction =
            new UndoUnchangedPendingChangesAction(sourceFilesSubControl.getChangesTable(), getViewSite().getShell());

        toggleButtonTextAction = new Action() {
            @Override
            public void run() {
                toggleHideTextOnButtonsOption();
                setToolbarButtonTextMode();
                getCheckinControl().showSourceFilesSubControl();
            }
        };

        /*
         * Actions for source files sub control.
         */
        refreshPendingChangesAction = new RefreshRepositoryPendingChangesAction();

        /*
         * create the checkin action
         */
        checkinAction = new Action() {
            @Override
            public void run() {
                checkin();
            }
        };
        checkinAction.setImageDescriptor(
            AbstractUIPlugin.imageDescriptorFromPlugin(TFSCommonUIClientPlugin.PLUGIN_ID, "images/vc/checkin.gif")); //$NON-NLS-1$
        checkinAction.setToolTipText(Messages.getString("PendingChangesView.CheckinActionTooltip")); //$NON-NLS-1$
        checkinAction.setText(Messages.getString("PendingChangesView.CheckinActionText")); //$NON-NLS-1$

        /*
         * create the shelve action
         */
        shelveAction = new Action() {
            @Override
            public void run() {
                shelve();
            }
        };
        shelveAction.setImageDescriptor(
            AbstractUIPlugin.imageDescriptorFromPlugin(TFSCommonUIClientPlugin.PLUGIN_ID, "images/vc/shelve.gif")); //$NON-NLS-1$
        shelveAction.setToolTipText(Messages.getString("PendingChangesView.ShelveActionTooltip")); //$NON-NLS-1$
        shelveAction.setText(Messages.getString("PendingChangesView.ShelveActionText")); //$NON-NLS-1$

        /*
         * create the unshelve action
         */
        unshelveAction = new Action() {
            @Override
            public void run() {
                unshelve();
            }
        };
        unshelveAction.setImageDescriptor(
            AbstractUIPlugin.imageDescriptorFromPlugin(TFSCommonUIClientPlugin.PLUGIN_ID, "images/vc/unshelve.gif")); //$NON-NLS-1$
        unshelveAction.setToolTipText(Messages.getString("PendingChangesView.UnshelveActionTooltip")); //$NON-NLS-1$
        unshelveAction.setText(Messages.getString("PendingChangesView.UnshelveActionText")); //$NON-NLS-1$
        unshelveAction.setEnabled(false);

        /* create the resolve action */
        resolveConflictsAction = new Action() {
            @Override
            public void run() {
                resolveConflicts();
            }
        };
        resolveConflictsAction.setImageDescriptor(
            AbstractUIPlugin.imageDescriptorFromPlugin(TFSCommonUIClientPlugin.PLUGIN_ID, "images/vc/conflict.gif")); //$NON-NLS-1$
        resolveConflictsAction.setToolTipText(Messages.getString("PendingChangesView.ResolveConflictsActionTooltip")); //$NON-NLS-1$
        resolveConflictsAction.setText(Messages.getString("PendingChangesView.ResolveConflictsActionText")); //$NON-NLS-1$
        resolveConflictsAction.setEnabled(false);

        /*
         * setup validation for checkin and shelve actions
         */
        new ActionValidatorBinding(checkinAction).bind(sourceFilesSubControl.getChangesTable().getCheckboxValidator());
        new ActionValidatorBinding(shelveAction).bind(sourceFilesSubControl.getChangesTable().getCheckboxValidator());
    }

    @Override
    protected void contributeActions() {
        final SourceFilesCheckinControl sourceFilesSubControl = getCheckinControl().getSourceFilesSubControl();

        sourceFilesSubControl.getContextMenu().addMenuListener(new IMenuListener() {
            @Override
            public void menuAboutToShow(final IMenuManager manager) {
                final String groupId = StandardActionConstants.HOSTING_CONTROL_CONTRIBUTIONS;

                final MenuManager compareSubMenu =
                    new MenuManager(Messages.getString("PendingChangesView.CompareMenuText")); //$NON-NLS-1$
                compareSubMenu.add(compareWithWorkspaceAction);
                compareSubMenu.add(compareWithLatestAction);

                final MenuManager viewSubMenu = new MenuManager(Messages.getString("PendingChangesView.ViewMenuText")); //$NON-NLS-1$
                viewSubMenu.add(viewAction);
                viewSubMenu.add(viewUnmodifiedAction);
                viewSubMenu.add(viewLatestAction);

                final IContributionItem showInContributions =
                    ContributionItemFactory.VIEWS_SHOW_IN.create(getViewSite().getWorkbenchWindow());

                final MenuManager showInSubMenu =
                    new MenuManager(Messages.getString("PendingChangesView.ShowInMenuText")); //$NON-NLS-1$
                showInSubMenu.add(showInContributions);

                manager.appendToGroup(groupId, viewSubMenu);
                manager.appendToGroup(groupId, new Separator());
                manager.appendToGroup(groupId, compareSubMenu);
                manager.appendToGroup(groupId, undoAction);
                manager.appendToGroup(groupId, new Separator());

                if (showInSubMenu.getMenu() != null && showInSubMenu.getMenu().getItems().length > 0) {
                    manager.appendToGroup(groupId, showInSubMenu);
                    manager.appendToGroup(groupId, new Separator());
                }

                manager.appendToGroup(groupId, refreshPendingChangesAction);
            }
        });

        sourceFilesSubControl.getChangesTable().addDoubleClickListener(new IDoubleClickListener() {
            @Override
            public void doubleClick(final DoubleClickEvent event) {
                if (viewAction.isEnabled()) {
                    viewAction.run();
                } else if (viewUnmodifiedAction.isEnabled()) {
                    viewUnmodifiedAction.run();
                }
            }
        });

        final IContributionManager contributionManager = getViewSite().getActionBars().getToolBarManager();

        workspaceActionContribution = new ActionContributionItem(workspaceToolbarAction);
        contributionManager.appendToGroup(TOOLBAR_GROUP_WORKSPACE, workspaceActionContribution);

        checkinActionContribution = new ActionContributionItem(checkinAction);
        contributionManager.appendToGroup(TOOLBAR_GROUP_CHECKIN, checkinActionContribution);

        shelveActionContribution = new ActionContributionItem(shelveAction);
        contributionManager.appendToGroup(TOOLBAR_GROUP_SHELVE, shelveActionContribution);

        unshelveActionContribution = new ActionContributionItem(unshelveAction);
        contributionManager.appendToGroup(TOOLBAR_GROUP_SHELVE, unshelveActionContribution);

        resolveConflictsActionContribution = new ActionContributionItem(resolveConflictsAction);

        setToolbarButtonTextMode();

        final IMenuManager localViewMenu = getViewSite().getActionBars().getMenuManager();

        localViewMenu.add(undoUnchangedAction);
        localViewMenu.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
        localViewMenu.add(toggleButtonTextAction);
        localViewMenu.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));

        getViewSite().getActionBars().updateActionBars();
    }

    private boolean getHideTextOnButtonsOption() {
        final IPreferenceStore preferences = TFSCommonUIClientPlugin.getDefault().getPreferenceStore();
        return preferences.getBoolean(UIPreferenceConstants.HIDE_TEXT_IN_PENDING_CHANGE_VIEW_BUTTONS);
    }

    private void toggleHideTextOnButtonsOption() {
        final IPreferenceStore preferences = TFSCommonUIClientPlugin.getDefault().getPreferenceStore();
        final boolean currentValue =
            preferences.getBoolean(UIPreferenceConstants.HIDE_TEXT_IN_PENDING_CHANGE_VIEW_BUTTONS);
        preferences.setValue(UIPreferenceConstants.HIDE_TEXT_IN_PENDING_CHANGE_VIEW_BUTTONS, !currentValue);
    }

    private void setToolbarButtonTextMode() {
        final boolean hideText = getHideTextOnButtonsOption();

        final String toggleButtonText = hideText ? Messages.getString("PendingChangesView.ShowToolbarText") //$NON-NLS-1$
            : Messages.getString("PendingChangesView.HideToolbarText"); //$NON-NLS-1$

        toggleButtonTextAction.setText(toggleButtonText);

        final int contributionMode = hideText ? 0 : ActionContributionItem.MODE_FORCE_TEXT;

        workspaceActionContribution.setMode(contributionMode);
        checkinActionContribution.setMode(contributionMode);
        shelveActionContribution.setMode(contributionMode);
        unshelveActionContribution.setMode(contributionMode);
        resolveConflictsActionContribution.setMode(contributionMode);

        getViewSite().getActionBars().getToolBarManager().update(false);
    }

    @Override
    protected String getStatusLineMessage(final IStructuredSelection selection) {
        if (selection.size() >= 1) {
            final Object element = selection.getFirstElement();

            if (element instanceof ChangeItem) {
                if (selection.size() == 1) {
                    return getStatusLineMessageForPendingChange(((ChangeItem) element).getPendingChange());
                }

                final String messageFormat = Messages.getString("PendingChangesView.SelectedPendingChangesFormat"); //$NON-NLS-1$
                return MessageFormat.format(messageFormat, selection.size());
            } else if (element instanceof WorkItemCheckinInfo) {
                if (selection.size() == 1) {
                    return getStatusLineMessageForWorkItemCheckinInfo((WorkItemCheckinInfo) element);
                }

                final String messageFormat = Messages.getString("PendingChangesView.SelectedWorkItemsFormat"); //$NON-NLS-1$
                return MessageFormat.format(messageFormat, selection.size());
            } else if (element instanceof PolicyFailureData) {
                if (selection.size() == 1) {
                    return getStatusLineMessageForPolicyFailureData((PolicyFailureData) element);
                }

                final String messageFormat = Messages.getString("PendingChangesView.SelectedPolicyFailuresFormat"); //$NON-NLS-1$
                return MessageFormat.format(messageFormat, selection.size());
            }

            // TODO: work items, etc

            return null;
        }
        return null;
    }

    private String getStatusLineMessageForPendingChange(final PendingChange pendingChange) {
        final int version = pendingChange.getVersion();
        final String type = pendingChange.getChangeType().toUIString(true, pendingChange);
        final String messageFormat = Messages.getString("PendingChangesView.PendingChangeStatusFormat"); //$NON-NLS-1$

        if (pendingChange.getLocalItem() != null) {
            final String item = pendingChange.getLocalItem();
            return MessageFormat.format(messageFormat, item, Integer.toString(version), type);
        } else {
            final String item = pendingChange.getServerItem();
            return MessageFormat.format(messageFormat, item, Integer.toString(version), type);
        }
    }

    private String getStatusLineMessageForPolicyFailureData(final PolicyFailureData failureData) {
        /*
         * Can we add anything to PolicyFailureData that's worth showing here?
         * Currently that type only includes the failure message, and there's
         * not much more to it.
         */
        return null;
    }

    private String getStatusLineMessageForWorkItemCheckinInfo(final WorkItemCheckinInfo info) {
        if (info.getAction() != null) {
            final String messageFormat = Messages.getString("PendingChangesView.CheckinPendingChangesStatusFormat"); //$NON-NLS-1$
            return MessageFormat.format(
                messageFormat,
                info.getActionString(),
                Integer.toString(info.getWorkItem().getFields().getID()));
        } else {
            return String.valueOf(info.getWorkItem().getFields().getID());
        }
    }

    private void checkin() {
        /*
         * Get the PendingCheckin that the control has been keeping up-to-date.
         * This has most of the information required for check-in.
         */
        final PendingCheckin pendingCheckin = getCheckinControl().getPendingCheckin();

        final int changeCount = pendingCheckin.getPendingChanges().getCheckedPendingChanges().length;
        if (changeCount == 0) {
            MessageDialog.openWarning(
                getSite().getShell(),
                Messages.getString("PendingChangesView.NoChangesDialogTitle"), //$NON-NLS-1$
                Messages.getString("PendingChangesView.ChangeChangesDialogText")); //$NON-NLS-1$
            return;
        }

        /*
         * Validate the pending change (checked work items, notes, policies,
         * etc.). The control's validation method may raise dialogs to fix
         * problems (policy override comment, save dirty editors, etc.).
         */
        final ValidationResult validationResult = getCheckinControl().validateForCheckin();
        if (validationResult.getSucceeded() == false) {
            return;
        }

        /*
         * Confirm the checkin.
         */

        String message;
        if (changeCount > 1) {
            final String messageFormat = Messages.getString("PendingChangesView.MultiChangesConfirmTextFormat"); //$NON-NLS-1$
            message = MessageFormat.format(messageFormat, changeCount);
        } else {
            message = Messages.getString("PendingChangesView.SingleChangeConfirmTextFormat"); //$NON-NLS-1$
        }

        CodeMarkerDispatch.dispatch(BEFORE_CONFIRM_DIALOG);
        if (!ToggleMessageHelper.openYesNoQuestion(
            getSite().getShell(),
            Messages.getString("PendingChangesView.ConfirmCheckinDialogTitle"), //$NON-NLS-1$
            message,
            Messages.getString("PendingChangesView.CheckinWithoutPromptCheckboxText"), //$NON-NLS-1$
            false,
            UIPreferenceConstants.PROMPT_BEFORE_CHECKIN)) {
            return;
        }

        /*
         * Build the override information from the validation result.
         */
        PolicyOverrideInfo policyOverrideInfo = null;
        if (validationResult.getPolicyOverrideReason() != null
            && validationResult.getPolicyOverrideReason().length() > 0) {
            policyOverrideInfo = new PolicyOverrideInfo(
                validationResult.getPolicyOverrideReason(),
                validationResult.getPolicyFailures());
        }

        final CheckinTask checkinTask =
            new CheckinTask(getSite().getShell(), repository, pendingCheckin, policyOverrideInfo);

        checkinTask.run();

        if (checkinTask.getPendingChangesCleared()) {
            getCheckinControl().afterCheckin();
        }
    }

    private void shelve() {
        final PendingCheckin pendingCheckin = getCheckinControl().getPendingCheckin();

        final AbstractShelveTask shelveTask = new ShelveWithPromptTask(
            getViewSite().getShell(),
            repository,
            pendingCheckin.getPendingChanges().getCheckedPendingChanges(),
            pendingCheckin.getPendingChanges().getComment(),
            pendingCheckin.getWorkItems().getCheckedWorkItems(),
            pendingCheckin.getCheckinNotes().getCheckinNotes());

        final IStatus shelveStatus = shelveTask.run();

        if (shelveStatus.isOK()) {
            getCheckinControl().afterShelve();
        }
    }

    private void unshelve() {
        final UnshelveTask unshelveTask = new UnshelveTask(getViewSite().getShell(), repository);
        unshelveTask.run();
    }

    private void resolveConflicts() {
        final ConflictResolutionTask resolveTask =
            new ConflictResolutionTask(getViewSite().getShell(), repository, null);
        resolveTask.run();
    }

    @Override
    protected void onSubControlShown(final AbstractCheckinSubControl subControl) {
        if (subControl.getSubControlType() == CheckinSubControlType.SOURCE_FILES) {
            final IContributionManager manager = getViewSite().getActionBars().getToolBarManager();

            manager.appendToGroup(TOOLBAR_GROUP_RESOLVE, resolveConflictsActionContribution);
            manager.appendToGroup(TOOLBAR_GROUP_COMPARE, compareToolbarAction);
            manager.appendToGroup(TOOLBAR_GROUP_COMPARE, viewToolbarAction);
            manager.appendToGroup(TOOLBAR_GROUP_COMPARE, undoAction);
            manager.appendToGroup(TOOLBAR_GROUP_REFRESH, refreshPendingChangesAction);

            getViewSite().getActionBars().updateActionBars();
        }
    }

    @Override
    protected void onSubControlHidden(final AbstractCheckinSubControl subControl) {
        if (subControl.getSubControlType() == CheckinSubControlType.SOURCE_FILES) {
            final IContributionManager manager = getViewSite().getActionBars().getToolBarManager();

            manager.remove(resolveConflictsActionContribution);
            manager.remove(new ActionContributionItem(compareToolbarAction));
            manager.remove(new ActionContributionItem(viewToolbarAction));
            manager.remove(new ActionContributionItem(undoAction));
            manager.remove(new ActionContributionItem(refreshPendingChangesAction));

            getViewSite().getActionBars().updateActionBars();
        }
    }

    public void setRepository(final TFSRepository repository) {
        synchronized (drawStateLock) {
            if (drawState == false) {
                drawRepository = repository;
                return;
            }
        }

        /* Remove the connection error hook from the existing connection. */
        if (this.repository != null) {
            this.repository.getConnection().removeConnectivityFailureStatusChangeListener(connectionListener);
        }

        this.repository = repository;

        final boolean enabled = (repository != null);

        getCheckinControl().setRepository(repository);
        compareWithLatestAction.setRepository(repository);
        compareWithWorkspaceAction.setRepository(repository);
        viewAction.setRepository(repository);
        viewUnmodifiedAction.setRepository(repository);
        viewLatestAction.setRepository(repository);
        undoAction.setRepository(repository);
        undoUnchangedAction.setRepository(repository);
        refreshPendingChangesAction.setRepository(repository);
        refreshPendingChangesAction.setEnabled(enabled);
        unshelveAction.setEnabled(enabled);
        resolveConflictsAction.setEnabled(enabled);

        if (repository != null) {
            setChangeItemProvider(new RepositoryChangeItemProvider(repository));
            workspaceToolbarAction.setCurrentWorkspace(repository.getWorkspace());

            repository.getConnection().addConnectivityFailureStatusChangeListener(connectionListener);
        } else {
            setChangeItemProvider(null);
            workspaceToolbarAction.setCurrentWorkspace(null);
        }
    }

    public void refreshConnection() {
        getCheckinControl().refreshConnection();
    }

    @Override
    protected void setupCheckinControlOptions(final CheckinControlOptions options) {
        options.setSourceFilesCheckboxes(true);
        options.setSourceFilesCommentReadOnly(false);
        options.setPolicyEvaluationEnabled(true);
    }

    @Override
    public void dispose() {
        super.dispose();

        if (connectionListener != null) {
            TFSCommonUIClientPlugin.getDefault().getProductPlugin().getRepositoryManager().removeListener(
                connectionListener);

            if (repository != null) {
                repository.getConnection().removeConnectivityFailureStatusChangeListener(connectionListener);
            }
        }

        if (workspaceToolbarAction != null) {
            workspaceToolbarAction.dispose();
        }
    }

    private class PendingChangesConnectionListener extends RepositoryManagerAdapter
        implements ConnectivityFailureStatusChangeListener {
        @Override
        public void onDefaultRepositoryChanged(final RepositoryManagerEvent event) {
            UIHelpers.runOnUIThread(true, new Runnable() {
                @Override
                public void run() {
                    setRepository(event.getRepositoryManager().getDefaultRepository());
                }
            });
        }

        @Override
        public void onConnectivityFailureStatusChange() {
            UIHelpers.runOnUIThread(true, new Runnable() {
                @Override
                public void run() {
                    refreshConnection();
                }
            });
        }
    }
}