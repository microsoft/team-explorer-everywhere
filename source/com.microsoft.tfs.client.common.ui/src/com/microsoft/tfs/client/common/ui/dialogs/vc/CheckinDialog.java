// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.dialogs.vc;

import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.IContributionManager;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;

import com.microsoft.tfs.client.common.codemarker.CodeMarker;
import com.microsoft.tfs.client.common.codemarker.CodeMarkerDispatch;
import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.TFSCommonUIImages;
import com.microsoft.tfs.client.common.ui.controls.vc.changes.ChangeItemProvider;
import com.microsoft.tfs.client.common.ui.controls.vc.changes.RepositoryChangeItemProvider;
import com.microsoft.tfs.client.common.ui.controls.vc.checkin.CheckinControl;
import com.microsoft.tfs.client.common.ui.controls.vc.checkin.CheckinControl.ValidationResult;
import com.microsoft.tfs.client.common.ui.controls.vc.checkin.CheckinControlOptions;
import com.microsoft.tfs.client.common.ui.controls.vc.checkin.SourceFilesCheckinControl;
import com.microsoft.tfs.client.common.ui.controls.vc.checkin.actions.ComparePendingChangeWithLatestVersionAction;
import com.microsoft.tfs.client.common.ui.controls.vc.checkin.actions.ComparePendingChangeWithWorkspaceVersionAction;
import com.microsoft.tfs.client.common.ui.controls.vc.checkin.actions.RefreshRepositoryPendingChangesAction;
import com.microsoft.tfs.client.common.ui.controls.vc.checkin.actions.UndoPendingChangesAction;
import com.microsoft.tfs.client.common.ui.controls.vc.checkin.actions.ViewPendingChangeAction;
import com.microsoft.tfs.client.common.ui.controls.vc.checkin.actions.ViewVersionType;
import com.microsoft.tfs.client.common.ui.framework.action.StandardActionConstants;
import com.microsoft.tfs.client.common.ui.framework.action.ToolbarPulldownAction;
import com.microsoft.tfs.client.common.ui.framework.compare.CompareUIType;
import com.microsoft.tfs.client.common.ui.framework.layout.GridDataBuilder;
import com.microsoft.tfs.client.common.ui.framework.validation.ButtonValidatorBinding;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PendingChange;
import com.microsoft.tfs.core.pendingcheckin.PendingCheckin;

public class CheckinDialog extends AbstractCheckinControlDialog {
    public static final CodeMarker CODEMARKER_CHECKINVALIDATION_COMPLETE =
        new CodeMarker("com.microsoft.tfs.client.common.ui.dialogs.vc.CheckinDialog#CheckinValidationComplete"); //$NON-NLS-1$
    private final TFSRepository repository;
    private final PendingChange[] initialChanges;
    private final String initialComment;

    private MenuManager viewSubMenu;
    private ToolbarPulldownAction viewToolbarAction;
    private ViewPendingChangeAction viewAction;
    private ViewPendingChangeAction viewUnmodifiedAction;
    private ViewPendingChangeAction viewLatestAction;

    private MenuManager compareSubMenu;
    private ToolbarPulldownAction compareToolbarAction;
    private ComparePendingChangeWithWorkspaceVersionAction compareWithWorkspaceAction;
    private ComparePendingChangeWithLatestVersionAction compareWithLatestAction;

    private UndoPendingChangesAction undoAction;
    private RefreshRepositoryPendingChangesAction refreshAction;

    private CheckinControl checkinControl;
    private ValidationResult validationResult;
    private PendingCheckin pendingCheckinResult;

    public CheckinDialog(
        final Shell parentShell,
        final TFSRepository repository,
        final PendingChange[] initialChanges,
        final String initialComment) {
        super(parentShell);
        this.repository = repository;
        this.initialChanges = initialChanges;
        this.initialComment = initialComment;
    }

    @Override
    protected void hookAddToDialogArea(final Composite dialogArea) {
        final GridLayout layout = new GridLayout(1, false);
        layout.horizontalSpacing = getHorizontalSpacing();
        layout.verticalSpacing = getVerticalSpacing();
        layout.marginWidth = getHorizontalMargin();
        layout.marginHeight = getVerticalMargin();
        dialogArea.setLayout(layout);

        final CheckinControlOptions options = new CheckinControlOptions();
        options.setForDialog(true);
        options.setSourceFilesCheckboxes(true);
        options.setSourceFilesCommentReadOnly(false);
        options.setPolicyEvaluationEnabled(true);

        checkinControl = new CheckinControl(dialogArea, SWT.NONE, options);
        GridDataBuilder.newInstance().grab().fill().applyTo(checkinControl);

        final ChangeItemProvider changeItemProvider = new RepositoryChangeItemProvider(repository);
        checkinControl.setChangeItemProvider(changeItemProvider);
        dialogArea.addDisposeListener(new DisposeListener() {
            @Override
            public void widgetDisposed(final DisposeEvent e) {
                changeItemProvider.dispose();
            }
        });

        /*
         * The control needs a repository for policy evaluation to function.
         * This must be set BEFORE the changes table has its check boxes set, so
         * that the check box events cause the policy evaluator to run correctly
         * when the dialog is first displayed.
         */
        checkinControl.setRepository(repository);

        final SourceFilesCheckinControl sourceFilesSubControl = checkinControl.getSourceFilesSubControl();

        sourceFilesSubControl.setComment(initialComment);

        sourceFilesSubControl.getChangesTable().setCheckedChangeItems(
            RepositoryChangeItemProvider.getChangeItemsFromPendingChanges(repository, initialChanges));

        setCheckinControl(checkinControl);

        createActions();
        contributeActions();

        /*
         * Reset the results.
         */
        pendingCheckinResult = null;
        validationResult = null;
    }

    @Override
    protected void hookAfterButtonsCreated() {
        final Button button = getButton(IDialogConstants.OK_ID);
        button.setText(Messages.getString("CheckinDialog.CheckinButtonText")); //$NON-NLS-1$
        setButtonLayoutData(button);

        final SourceFilesCheckinControl sourceFilesSubControl = getCheckinControl().getSourceFilesSubControl();
        new ButtonValidatorBinding(button).bind(sourceFilesSubControl.getChangesTable().getCheckboxValidator());
    }

    @Override
    protected void okPressed() {
        /*
         * Validate the pending change (checked work items, notes, policies,
         * etc.). The control's validation method may raise dialogs.
         */
        validationResult = getCheckinControl().validateForCheckin();

        /*
         * Disallow the close if the validation failed.
         */
        if (validationResult.getSucceeded() == false) {
            return;
        }

        /*
         * Store the PendingCheckin for retrieval after the control has been
         * disposed.
         */
        pendingCheckinResult = checkinControl.getPendingCheckin();

        if (validationResult.getConflicts() == null || validationResult.getConflicts().length == 0) {
            CodeMarkerDispatch.dispatch(CODEMARKER_CHECKINVALIDATION_COMPLETE);
        }

        super.okPressed();
    }

    /**
     * @return the result of the validation of the {@link CheckinControl} done
     *         before this dialog closed with {@link Status#OK_STATUS}. If the
     *         dialog closed with another status, this will be <code>null</code>
     */
    public ValidationResult getValidationResult() {
        return validationResult;
    }

    /**
     * @return the {@link PendingCheckin} that contains the information about
     *         the changes to be checked into the server. This is non-null if
     *         the dialog closed with {@link Status#OK_STATUS},
     *         <code>null</code> if it closed with a different status
     */
    public PendingCheckin getPendingCheckin() {
        return pendingCheckinResult;
    }

    @Override
    protected String getBaseTitle() {
        return Messages.getString("CheckinDialog.DialogBaseTitle"); //$NON-NLS-1$
    }

    private void createActions() {
        final SourceFilesCheckinControl sourceFilesSubControl = getCheckinControl().getSourceFilesSubControl();
        final ISelectionProvider sourceFilesSelectionProvider = sourceFilesSubControl.getSelectionProvider();

        viewSubMenu = new MenuManager(Messages.getString("CheckinDialog.ViewSubMenuText")); //$NON-NLS-1$

        viewToolbarAction = new ToolbarPulldownAction();
        viewToolbarAction.setImageDescriptor(
            PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(ISharedImages.IMG_OBJ_FOLDER));

        viewAction =
            new ViewPendingChangeAction(sourceFilesSelectionProvider, repository, true, ViewVersionType.DEFAULT);
        viewSubMenu.add(viewAction);
        viewToolbarAction.addSubAction(viewAction);
        viewToolbarAction.setDefaultSubAction(viewAction);

        viewUnmodifiedAction =
            new ViewPendingChangeAction(sourceFilesSelectionProvider, repository, true, ViewVersionType.UNMODIFIED);
        viewSubMenu.add(viewUnmodifiedAction);
        viewToolbarAction.addSubAction(viewUnmodifiedAction);

        viewLatestAction =
            new ViewPendingChangeAction(sourceFilesSelectionProvider, repository, true, ViewVersionType.LATEST);
        viewSubMenu.add(viewLatestAction);
        viewToolbarAction.addSubAction(viewLatestAction);

        compareSubMenu = new MenuManager(Messages.getString("CheckinDialog.CompareSubMenuText")); //$NON-NLS-1$

        compareToolbarAction = new ToolbarPulldownAction();
        compareToolbarAction.setImageDescriptor(TFSCommonUIImages.getImageDescriptor(TFSCommonUIImages.IMG_COMPARE));

        compareWithWorkspaceAction = new ComparePendingChangeWithWorkspaceVersionAction(
            sourceFilesSelectionProvider,
            repository,
            CompareUIType.DIALOG,
            getShell());
        compareSubMenu.add(compareWithWorkspaceAction);
        compareToolbarAction.addSubAction(compareWithWorkspaceAction);

        compareWithLatestAction = new ComparePendingChangeWithLatestVersionAction(
            sourceFilesSelectionProvider,
            repository,
            CompareUIType.DIALOG,
            getShell());
        compareSubMenu.add(compareWithLatestAction);
        compareToolbarAction.addSubAction(compareWithLatestAction);
        compareToolbarAction.setDefaultSubAction(compareWithLatestAction);

        undoAction = new UndoPendingChangesAction(sourceFilesSelectionProvider, repository, getShell());

        refreshAction = new RefreshRepositoryPendingChangesAction(repository);
    }

    private void contributeActions() {
        final SourceFilesCheckinControl sourceFilesSubControl = getCheckinControl().getSourceFilesSubControl();

        sourceFilesSubControl.getContextMenu().addMenuListener(new IMenuListener() {
            @Override
            public void menuAboutToShow(final IMenuManager manager) {
                final String groupId = StandardActionConstants.HOSTING_CONTROL_CONTRIBUTIONS;

                manager.appendToGroup(groupId, viewSubMenu);
                manager.appendToGroup(groupId, new Separator());
                manager.appendToGroup(groupId, compareSubMenu);
                manager.appendToGroup(groupId, undoAction);
                manager.appendToGroup(groupId, new Separator());
                manager.appendToGroup(groupId, refreshAction);
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

        final IContributionManager contributionManager = sourceFilesSubControl.getContributionManager();

        contributionManager.add(new Separator());
        contributionManager.add(compareToolbarAction);
        contributionManager.add(viewToolbarAction);
        contributionManager.add(undoAction);
        contributionManager.add(new Separator());
        contributionManager.add(refreshAction);

        contributionManager.update(false);
    }
}
