// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.views;

import java.text.DateFormat;
import java.text.MessageFormat;

import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IContributionManager;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbenchActionConstants;

import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.TFSCommonUIImages;
import com.microsoft.tfs.client.common.ui.controls.vc.changes.ChangesetChangeItemProvider;
import com.microsoft.tfs.client.common.ui.controls.vc.checkin.AbstractCheckinSubControl;
import com.microsoft.tfs.client.common.ui.controls.vc.checkin.CheckinControl;
import com.microsoft.tfs.client.common.ui.controls.vc.checkin.CheckinControlOptions;
import com.microsoft.tfs.client.common.ui.controls.vc.checkin.CheckinSubControlType;
import com.microsoft.tfs.client.common.ui.controls.vc.checkin.SourceFilesCheckinControl;
import com.microsoft.tfs.client.common.ui.controls.vc.checkin.actions.CompareChangeWithLatestVersionAction;
import com.microsoft.tfs.client.common.ui.controls.vc.checkin.actions.CompareChangeWithPreviousVersionAction;
import com.microsoft.tfs.client.common.ui.controls.vc.checkin.actions.CompareChangeWithWorkspaceVersionAction;
import com.microsoft.tfs.client.common.ui.controls.vc.checkin.actions.ViewChangeAction;
import com.microsoft.tfs.client.common.ui.controls.vc.checkin.actions.ViewVersionType;
import com.microsoft.tfs.client.common.ui.framework.action.StandardActionConstants;
import com.microsoft.tfs.client.common.ui.framework.action.ToolbarPulldownAction;
import com.microsoft.tfs.client.common.ui.framework.compare.CompareUIType;
import com.microsoft.tfs.client.common.util.DateHelper;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Change;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Changeset;

public class ChangesetDetailsView extends AbstractCheckinControlView {
    public static final String ID = "com.microsoft.tfs.client.common.ui.views.changesetdetailsview"; //$NON-NLS-1$

    private static final String TOOLBAR_GROUP_COMPARE = "toolbar-group-compare"; //$NON-NLS-1$

    private final DateFormat dateFormat = DateHelper.getDefaultDateTimeFormat();

    private Changeset changeset;
    private MenuManager viewSubMenu;
    private ToolbarPulldownAction viewToolbarAction;
    private ViewChangeAction viewAction;
    private ViewChangeAction viewPreviousAction;
    private ViewChangeAction viewLatestAction;

    private MenuManager compareSubMenu;
    private ToolbarPulldownAction compareToolbarAction;
    private CompareChangeWithPreviousVersionAction compareWithPreviousAction;
    private CompareChangeWithWorkspaceVersionAction compareWithWorkspaceAction;
    private CompareChangeWithLatestVersionAction compareWithLatestAction;

    public void setChangeset(final Changeset changeset, final TFSRepository repository) {
        this.changeset = changeset;
        final String messageFormat = Messages.getString("ChangesetDetailsView.PartNameFormat"); //$NON-NLS-1$
        final String message = MessageFormat.format(messageFormat, Integer.toString(changeset.getChangesetID()));
        setPartName(message);

        viewAction.setRepository(repository);
        viewPreviousAction.setRepository(repository);
        viewLatestAction.setRepository(repository);
        compareWithPreviousAction.setRepository(repository);
        compareWithWorkspaceAction.setRepository(repository);
        compareWithLatestAction.setRepository(repository);

        setChangeItemProvider(new ChangesetChangeItemProvider(repository, changeset));

        updateStatusLine();
    }

    @Override
    protected void createActions() {
        final SourceFilesCheckinControl sourceFilesSubControl = getCheckinControl().getSourceFilesSubControl();
        final ISelectionProvider sourceFilesSelectionProvider = sourceFilesSubControl.getSelectionProvider();

        viewSubMenu = new MenuManager(Messages.getString("ChangesetDetailsView.ViewSubMenuText")); //$NON-NLS-1$

        viewToolbarAction = new ToolbarPulldownAction();
        viewToolbarAction.setImageDescriptor(TFSCommonUIImages.getImageDescriptor(TFSCommonUIImages.IMG_VIEW));

        viewAction = new ViewChangeAction(sourceFilesSelectionProvider, false, ViewVersionType.DEFAULT);
        viewSubMenu.add(viewAction);
        viewToolbarAction.addSubAction(viewAction);
        viewToolbarAction.setDefaultSubAction(viewAction);

        viewPreviousAction = new ViewChangeAction(sourceFilesSelectionProvider, false, ViewVersionType.PREVIOUS);
        viewSubMenu.add(viewPreviousAction);
        viewToolbarAction.addSubAction(viewPreviousAction);

        viewLatestAction = new ViewChangeAction(sourceFilesSelectionProvider, false, ViewVersionType.LATEST);
        viewSubMenu.add(viewLatestAction);
        viewToolbarAction.addSubAction(viewLatestAction);

        compareSubMenu = new MenuManager(Messages.getString("ChangesetDetailsView.CompareSubMenuText")); //$NON-NLS-1$

        compareToolbarAction = new ToolbarPulldownAction();
        compareToolbarAction.setImageDescriptor(TFSCommonUIImages.getImageDescriptor(TFSCommonUIImages.IMG_COMPARE));

        compareWithPreviousAction = new CompareChangeWithPreviousVersionAction(
            sourceFilesSelectionProvider,
            CompareUIType.EDITOR,
            getCheckinControl().getShell());
        compareSubMenu.add(compareWithPreviousAction);
        compareToolbarAction.addSubAction(compareWithPreviousAction);

        compareWithWorkspaceAction = new CompareChangeWithWorkspaceVersionAction(
            sourceFilesSelectionProvider,
            CompareUIType.EDITOR,
            getCheckinControl().getShell());
        compareSubMenu.add(compareWithWorkspaceAction);
        compareToolbarAction.addSubAction(compareWithWorkspaceAction);

        compareWithLatestAction = new CompareChangeWithLatestVersionAction(
            sourceFilesSelectionProvider,
            CompareUIType.EDITOR,
            getCheckinControl().getShell());
        compareSubMenu.add(compareWithLatestAction);
        compareToolbarAction.addSubAction(compareWithLatestAction);
        compareToolbarAction.setDefaultSubAction(compareWithLatestAction);
    }

    @Override
    protected void contributeActions() {
        final SourceFilesCheckinControl sourceFilesSubControl = getCheckinControl().getSourceFilesSubControl();

        sourceFilesSubControl.getContextMenu().addMenuListener(new IMenuListener() {
            @Override
            public void menuAboutToShow(final IMenuManager manager) {
                final String groupId = StandardActionConstants.HOSTING_CONTROL_CONTRIBUTIONS;

                manager.appendToGroup(groupId, viewSubMenu);
                manager.appendToGroup(groupId, new Separator());
                manager.appendToGroup(groupId, compareSubMenu);
            }
        });

        (sourceFilesSubControl).getChangesTable().addDoubleClickListener(new IDoubleClickListener() {
            @Override
            public void doubleClick(final DoubleClickEvent event) {
                if (viewAction.isEnabled()) {
                    viewAction.run();
                }
            }
        });
    }

    @Override
    protected void setupCheckinControlOptions(final CheckinControlOptions options) {
        options.setSourceFilesCheckboxes(false);
        options.setSourceFilesCommentReadOnly(true);
        options.setPolicyEvaluationEnabled(false);
    }

    @Override
    protected String getStatusLineMessage(final IStructuredSelection selection) {
        if (changeset == null) {
            return null;
        }

        final int id = changeset.getChangesetID();
        final String owner = changeset.getOwnerDisplayName();
        final String date = dateFormat.format(changeset.getDate().getTime());

        Change change = null;
        if (selection.size() >= 1) {
            final Object element = selection.getFirstElement();
            if (element instanceof Change) {
                change = (Change) element;
            }
        }

        if (change == null) {
            final String messageFormat = Messages.getString("ChangesetDetailsView.ChangesetStatusFormat"); //$NON-NLS-1$
            return MessageFormat.format(messageFormat, Integer.toString(id), owner, date);
        } else if (selection.size() == 1) {
            final String status = getStatusLineMessageForChange(change);
            final String messageFormat = Messages.getString("ChangesetDetailsView.SingleSelectChangesetStatusFormat"); //$NON-NLS-1$
            return MessageFormat.format(messageFormat, Integer.toString(id), owner, date, status);
        } else {
            final int size = selection.size();
            final String messageFormat = Messages.getString("ChangesetDetailsView.MultiSelectChangesetStatusFormat"); //$NON-NLS-1$
            return MessageFormat.format(messageFormat, Integer.toString(id), owner, date, size);
        }
    }

    private String getStatusLineMessageForChange(final Change change) {
        final String item = change.getItem().getServerItem();
        final String type = change.getChangeType().toUIString(true, change);

        if (change.getItem().getDeletionID() != 0) {
            final int id = change.getItem().getDeletionID();
            final String messageFormat = Messages.getString("ChangesetDetailsView.DeleteChangesetMessageFormat"); //$NON-NLS-1$
            return MessageFormat.format(messageFormat, item, Integer.toString(id), type);
        } else {
            final int id = change.getItem().getChangeSetID();
            final String messageFormat = Messages.getString("ChangesetDetailsView.ChangeMessageFormat"); //$NON-NLS-1$
            return MessageFormat.format(messageFormat, item, Integer.toString(id), type);
        }
    }

    @Override
    protected void onSubControlHidden(final AbstractCheckinSubControl subControl) {
        if (subControl.getSubControlType() == CheckinSubControlType.SOURCE_FILES) {
            final IContributionManager manager = getViewSite().getActionBars().getToolBarManager();

            manager.remove(new ActionContributionItem(compareToolbarAction));
            manager.remove(new ActionContributionItem(viewToolbarAction));

            getViewSite().getActionBars().updateActionBars();
        }
    }

    @Override
    protected void onSubControlShown(final AbstractCheckinSubControl subControl) {
        if (subControl.getSubControlType() == CheckinSubControlType.SOURCE_FILES) {
            final IContributionManager manager = getViewSite().getActionBars().getToolBarManager();

            manager.appendToGroup(TOOLBAR_GROUP_COMPARE, compareToolbarAction);
            manager.appendToGroup(TOOLBAR_GROUP_COMPARE, viewToolbarAction);

            getViewSite().getActionBars().updateActionBars();
        }
    }

    @Override
    protected void setupToolbar(final IToolBarManager toolbar) {
        toolbar.add(new Separator(CheckinControl.SUBCONTROL_CONTRIBUTION_GROUP_NAME));
        toolbar.add(new Separator(TOOLBAR_GROUP_COMPARE));
        toolbar.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
    }
}
