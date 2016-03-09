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
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.PlatformUI;

import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.TFSCommonUIImages;
import com.microsoft.tfs.client.common.ui.controls.vc.changes.QueryShelvesetChangeItemProvider;
import com.microsoft.tfs.client.common.ui.controls.vc.checkin.AbstractCheckinSubControl;
import com.microsoft.tfs.client.common.ui.controls.vc.checkin.CheckinControl;
import com.microsoft.tfs.client.common.ui.controls.vc.checkin.CheckinControlOptions;
import com.microsoft.tfs.client.common.ui.controls.vc.checkin.CheckinSubControlType;
import com.microsoft.tfs.client.common.ui.controls.vc.checkin.SourceFilesCheckinControl;
import com.microsoft.tfs.client.common.ui.controls.vc.checkin.actions.CompareShelvedChangeWithLatestVersionAction;
import com.microsoft.tfs.client.common.ui.controls.vc.checkin.actions.CompareShelvedChangeWithUnmodifiedVersionAction;
import com.microsoft.tfs.client.common.ui.controls.vc.checkin.actions.ViewPendingChangeAction;
import com.microsoft.tfs.client.common.ui.controls.vc.checkin.actions.ViewVersionType;
import com.microsoft.tfs.client.common.ui.framework.action.StandardActionConstants;
import com.microsoft.tfs.client.common.ui.framework.action.ToolbarPulldownAction;
import com.microsoft.tfs.client.common.ui.framework.command.UICommandExecutorFactory;
import com.microsoft.tfs.client.common.ui.framework.compare.CompareUIType;
import com.microsoft.tfs.client.common.util.DateHelper;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PendingChange;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Shelveset;

public class ShelvesetDetailsView extends AbstractCheckinControlView {
    public static final String ID = "com.microsoft.tfs.client.common.ui.views.shelvesetdetailsview"; //$NON-NLS-1$

    private static final String TOOLBAR_GROUP_COMPARE = "toolbar-group-compare"; //$NON-NLS-1$

    private final DateFormat dateFormat = DateHelper.getDefaultDateTimeFormat();

    private MenuManager viewSubMenu;
    private ToolbarPulldownAction viewToolbarAction;
    private ViewPendingChangeAction viewAction;
    private ViewPendingChangeAction viewUnmodifiedAction;
    private ViewPendingChangeAction viewLatestAction;

    private MenuManager compareSubMenu;
    private ToolbarPulldownAction compareToolbarAction;
    private CompareShelvedChangeWithUnmodifiedVersionAction compareWithUnmodifiedAction;
    private CompareShelvedChangeWithLatestVersionAction compareWithLatestAction;

    private Shelveset shelveset;

    public void setShelveset(final Shelveset shelveset, final TFSRepository repository) {
        this.shelveset = shelveset;
        final String messageFormat = Messages.getString("ShelvesetDetailsView.PartNameFormat"); //$NON-NLS-1$
        final String message = MessageFormat.format(messageFormat, shelveset.getName());
        setPartName(message);

        viewAction.setRepository(repository);
        viewUnmodifiedAction.setRepository(repository);
        viewLatestAction.setRepository(repository);

        compareWithUnmodifiedAction.setRepository(repository);
        compareWithUnmodifiedAction.setShelvesetInfo(shelveset.getName(), shelveset.getOwnerName());

        compareWithLatestAction.setRepository(repository);
        compareWithLatestAction.setShelvesetInfo(shelveset.getName(), shelveset.getOwnerName());

        setChangeItemProvider(
            new QueryShelvesetChangeItemProvider(
                repository,
                UICommandExecutorFactory.newUICommandExecutor(getViewSite().getShell()),
                shelveset));

        updateStatusLine();
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
                } else if (viewUnmodifiedAction.isEnabled()) {
                    viewUnmodifiedAction.run();
                }
            }
        });
    }

    @Override
    protected void createActions() {
        final SourceFilesCheckinControl sourceFilesSubControl = getCheckinControl().getSourceFilesSubControl();
        final ISelectionProvider sourceFilesSelectionProvider = sourceFilesSubControl.getSelectionProvider();

        viewSubMenu = new MenuManager(Messages.getString("ShelvesetDetailsView.ViewMenuText")); //$NON-NLS-1$

        viewToolbarAction = new ToolbarPulldownAction();
        viewToolbarAction.setImageDescriptor(
            PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(ISharedImages.IMG_OBJ_FOLDER));

        viewAction = new ViewPendingChangeAction(sourceFilesSelectionProvider, false, ViewVersionType.SHELVED);
        viewSubMenu.add(viewAction);
        viewToolbarAction.addSubAction(viewAction);
        viewToolbarAction.setDefaultSubAction(viewAction);

        viewUnmodifiedAction =
            new ViewPendingChangeAction(sourceFilesSelectionProvider, false, ViewVersionType.UNMODIFIED);
        viewSubMenu.add(viewUnmodifiedAction);
        viewToolbarAction.addSubAction(viewUnmodifiedAction);

        viewLatestAction = new ViewPendingChangeAction(sourceFilesSelectionProvider, false, ViewVersionType.LATEST);
        viewSubMenu.add(viewLatestAction);
        viewToolbarAction.addSubAction(viewLatestAction);

        compareSubMenu = new MenuManager(Messages.getString("ShelvesetDetailsView.CompareMenuText")); //$NON-NLS-1$

        compareToolbarAction = new ToolbarPulldownAction();
        compareToolbarAction.setImageDescriptor(TFSCommonUIImages.getImageDescriptor(TFSCommonUIImages.IMG_COMPARE));

        compareWithUnmodifiedAction = new CompareShelvedChangeWithUnmodifiedVersionAction(
            sourceFilesSelectionProvider,
            CompareUIType.EDITOR,
            shelveset.getName(),
            shelveset.getOwnerName(),
            getCheckinControl().getShell());
        compareSubMenu.add(compareWithUnmodifiedAction);
        compareToolbarAction.addSubAction(compareWithUnmodifiedAction);

        compareWithLatestAction = new CompareShelvedChangeWithLatestVersionAction(
            sourceFilesSelectionProvider,
            CompareUIType.EDITOR,
            shelveset.getName(),
            shelveset.getOwnerName(),
            getCheckinControl().getShell());
        compareSubMenu.add(compareWithLatestAction);
        compareToolbarAction.addSubAction(compareWithLatestAction);
        compareToolbarAction.setDefaultSubAction(compareWithLatestAction);
    }

    @Override
    protected void setupCheckinControlOptions(final CheckinControlOptions options) {
        options.setSourceFilesCheckboxes(true);
        options.setSourceFilesCommentReadOnly(true);
        options.setPolicyEvaluationEnabled(false);
    }

    @Override
    protected String getStatusLineMessage(final IStructuredSelection selection) {
        if (shelveset == null) {
            return null;
        }

        final String shelveName = shelveset.getName();
        final String date = dateFormat.format(shelveset.getCreationDate().getTime());
        final String userName = shelveset.getOwnerDisplayName();

        PendingChange change = null;
        if (selection.size() >= 1) {
            final Object element = selection.getFirstElement();
            if (element instanceof PendingChange) {
                change = (PendingChange) element;
            }
        }

        if (change == null) {
            final String messageFormat = Messages.getString("ShelvesetDetailsView.ShelveStatusFormat"); //$NON-NLS-1$
            return MessageFormat.format(messageFormat, shelveName, userName, date);
        } else if (selection.size() == 1) {
            final String status = getStatusLineMessageForShelvedChange(change);
            final String messageFormat = Messages.getString("ShelvesetDetailsView.SingleSelectShelveStatusFormat"); //$NON-NLS-1$
            return MessageFormat.format(messageFormat, shelveName, userName, date, status);
        } else {
            final int size = selection.size();
            final String messageFormat = Messages.getString("ShelvesetDetailsView.MultiSelectShelveStatusFormat"); //$NON-NLS-1$
            return MessageFormat.format(messageFormat, shelveName, userName, date, size);
        }
    }

    private String getStatusLineMessageForShelvedChange(final PendingChange pendingChange) {
        final String messageFormat = Messages.getString("ShelvesetDetailsView.StatusForShelveChangeFormat"); //$NON-NLS-1$
        return MessageFormat.format(
            messageFormat,
            pendingChange.getServerItem(),
            Integer.toString(pendingChange.getVersion()),
            pendingChange.getChangeType().toUIString(true, pendingChange));
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
