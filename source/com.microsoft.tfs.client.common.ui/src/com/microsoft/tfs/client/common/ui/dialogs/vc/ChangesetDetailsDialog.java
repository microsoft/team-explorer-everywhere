// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.dialogs.vc;

import java.text.DateFormat;
import java.text.MessageFormat;

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
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.microsoft.tfs.client.common.commands.vc.UpdateChangesetCommand;
import com.microsoft.tfs.client.common.framework.command.ICommandExecutor;
import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.TFSCommonUIImages;
import com.microsoft.tfs.client.common.ui.controls.vc.changes.ChangeItemProvider;
import com.microsoft.tfs.client.common.ui.controls.vc.changes.ChangesetChangeItemProvider;
import com.microsoft.tfs.client.common.ui.controls.vc.checkin.CheckinControl;
import com.microsoft.tfs.client.common.ui.controls.vc.checkin.CheckinControlOptions;
import com.microsoft.tfs.client.common.ui.controls.vc.checkin.NotesCheckinControl.CheckinNoteEvent;
import com.microsoft.tfs.client.common.ui.controls.vc.checkin.NotesCheckinControl.CheckinNoteListener;
import com.microsoft.tfs.client.common.ui.controls.vc.checkin.SourceFilesCheckinControl;
import com.microsoft.tfs.client.common.ui.controls.vc.checkin.actions.CompareChangeWithLatestVersionAction;
import com.microsoft.tfs.client.common.ui.controls.vc.checkin.actions.CompareChangeWithPreviousVersionAction;
import com.microsoft.tfs.client.common.ui.controls.vc.checkin.actions.CompareChangeWithWorkspaceVersionAction;
import com.microsoft.tfs.client.common.ui.controls.vc.checkin.actions.ViewChangeAction;
import com.microsoft.tfs.client.common.ui.controls.vc.checkin.actions.ViewVersionType;
import com.microsoft.tfs.client.common.ui.framework.action.StandardActionConstants;
import com.microsoft.tfs.client.common.ui.framework.action.ToolbarPulldownAction;
import com.microsoft.tfs.client.common.ui.framework.command.UICommandExecutorFactory;
import com.microsoft.tfs.client.common.ui.framework.compare.CompareUIType;
import com.microsoft.tfs.client.common.ui.framework.layout.GridDataBuilder;
import com.microsoft.tfs.client.common.ui.framework.sizing.ControlSize;
import com.microsoft.tfs.client.common.util.DateHelper;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Changeset;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.CheckinNote;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.CheckinNoteFieldValue;

public class ChangesetDetailsDialog extends AbstractCheckinControlDialog {
    private final Changeset changeset;
    private final TFSRepository repository;
    private final DateFormat dateFormat = DateHelper.getDefaultDateTimeFormat();

    private CheckinControl checkinControl;

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

    private boolean changesetUpdated;

    public ChangesetDetailsDialog(final Shell parentShell, final Changeset changeset, final TFSRepository repository) {
        super(parentShell);
        this.changeset = changeset;
        this.repository = repository;
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
        options.setSourceFilesCheckboxes(false);
        options.setSourceFilesCommentReadOnly(false);
        options.setWorkItemSearchEnabled(false);
        options.setWorkItemReadOnly(true);
        options.setCheckinNotesHistoric(true);
        options.setPolicyEvaluationEnabled(false);
        options.setChangesText(Messages.getString("ChangesetDetailsDialog.ChangesDialogTitle")); //$NON-NLS-1$

        checkinControl = new CheckinControl(dialogArea, SWT.NONE, options);
        checkinControl.setRepository(repository);
        GridDataBuilder.newInstance().grab().fill().applyTo(checkinControl);

        final ChangeItemProvider changeItemProvider = new ChangesetChangeItemProvider(repository, changeset);
        checkinControl.setChangeItemProvider(changeItemProvider);
        dialogArea.addDisposeListener(new DisposeListener() {
            @Override
            public void widgetDisposed(final DisposeEvent e) {
                changeItemProvider.dispose();
            }
        });

        checkinControl.getSourceFilesSubControl().setComment(changeset.getComment());
        checkinControl.getWorkItemSubControl().getWorkItemTable().setWorkItems(changeset.getWorkItems());
        checkinControl.getNotesSubControl().setCheckinNote(changeset.getCheckinNote());
        checkinControl.getPolicyWarningsSubControl().setHistoricPolicyOverrideInfo(changeset.getPolicyOverride());

        checkinControl.getNotesSubControl().addCheckinNoteChangedListener(new CheckinNoteListener() {
            @Override
            public void onCheckinNoteChanged(final CheckinNoteEvent e) {
                evaluateChangesetModifications();
            }
        });

        checkinControl.getSourceFilesSubControl().getCommentText().addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(final ModifyEvent e) {
                evaluateChangesetModifications();
            }
        });

        changesetUpdated = false;

        setCheckinControl(checkinControl);

        createActions();
        contributeActions();
    }

    private void evaluateChangesetModifications() {
        /*
         * Normalize empty comment as empty string (no nulls allowed) for
         * comparison. This is done because the historic changeset will contain
         * an empty string if none was supplied during checkin.
         */
        final String oldComment = changeset.getComment() != null ? changeset.getComment() : ""; //$NON-NLS-1$
        final String newComment = checkinControl.getSourceFilesSubControl().getCommentText().getText();
        final boolean commentsDifferent = !oldComment.equals(newComment);

        /*
         * See if the checkin notes have changed
         */
        final CheckinNote oldCheckinNote = changeset.getCheckinNote();
        final CheckinNote newCheckinNote = checkinControl.getNotesSubControl().getCheckinNote();

        final CheckinNoteFieldValue[] oldCheckinNoteValues =
            (oldCheckinNote != null && oldCheckinNote.getValues() != null) ? oldCheckinNote.getValues()
                : new CheckinNoteFieldValue[0];
        final CheckinNoteFieldValue[] newCheckinNoteValues =
            (newCheckinNote != null && newCheckinNote.getValues() != null) ? newCheckinNote.getValues()
                : new CheckinNoteFieldValue[0];
        boolean checkinNotesDifferent = false;

        if (oldCheckinNoteValues.length != newCheckinNoteValues.length) {
            checkinNotesDifferent = true;
        } else {
            for (int i = 0; i < oldCheckinNoteValues.length; i++) {
                boolean foundInNew = false;

                for (int j = 0; j < newCheckinNoteValues.length; j++) {
                    if (CheckinNote.canonicalizeName(oldCheckinNoteValues[i].getName()).equals(
                        CheckinNote.canonicalizeName(newCheckinNoteValues[j].getName()))) {
                        if (!oldCheckinNoteValues[i].getValue().equals(newCheckinNoteValues[j].getValue())) {
                            checkinNotesDifferent = true;
                        }

                        foundInNew = true;
                        break;
                    }
                }

                if (!foundInNew) {
                    checkinNotesDifferent = true;
                    break;
                }

                if (checkinNotesDifferent) {
                    break;
                }
            }
        }

        getButton(IDialogConstants.OK_ID).setEnabled(commentsDifferent || checkinNotesDifferent);
    }

    @Override
    protected void okPressed() {
        /*
         * Attempt to update the changeset on the server.
         */
        final Changeset newChangeset = new Changeset(
            changeset.getChanges(),
            checkinControl.getSourceFilesSubControl().getComment(),
            checkinControl.getNotesSubControl().getCheckinNote(),
            changeset.getPolicyOverride(),
            changeset.getCommitter(),
            changeset.getCommitterDisplayName(),
            changeset.getDate(),
            changeset.getChangesetID(),
            changeset.getOwner(),
            changeset.getOwnerDisplayName(),
            changeset.getPropertyValues());

        final UpdateChangesetCommand updateCommand =
            new UpdateChangesetCommand(repository.getVersionControlClient(), newChangeset);

        final ICommandExecutor executor = UICommandExecutorFactory.newUICommandExecutor(getShell());

        if (executor.execute(updateCommand) == Status.OK_STATUS) {
            changesetUpdated = true;
            super.okPressed();
        }
    }

    private void createActions() {
        final SourceFilesCheckinControl sourceFilesSubControl = getCheckinControl().getSourceFilesSubControl();
        final ISelectionProvider sourceFilesSelectionProvider = sourceFilesSubControl.getSelectionProvider();

        viewSubMenu = new MenuManager(Messages.getString("ChangesetDetailsDialog.ViewSubMenuText")); //$NON-NLS-1$

        viewToolbarAction = new ToolbarPulldownAction();
        viewToolbarAction.setImageDescriptor(TFSCommonUIImages.getImageDescriptor(TFSCommonUIImages.IMG_VIEW));

        viewAction = new ViewChangeAction(sourceFilesSelectionProvider, repository, true, ViewVersionType.DEFAULT);
        viewSubMenu.add(viewAction);
        viewToolbarAction.addSubAction(viewAction);
        viewToolbarAction.setDefaultSubAction(viewAction);

        viewPreviousAction =
            new ViewChangeAction(sourceFilesSelectionProvider, repository, true, ViewVersionType.PREVIOUS);
        viewSubMenu.add(viewPreviousAction);
        viewToolbarAction.addSubAction(viewPreviousAction);

        viewLatestAction = new ViewChangeAction(sourceFilesSelectionProvider, repository, true, ViewVersionType.LATEST);
        viewSubMenu.add(viewLatestAction);
        viewToolbarAction.addSubAction(viewLatestAction);

        compareSubMenu = new MenuManager(Messages.getString("ChangesetDetailsDialog.CompareSubMenuText")); //$NON-NLS-1$

        compareToolbarAction = new ToolbarPulldownAction();
        compareToolbarAction.setImageDescriptor(TFSCommonUIImages.getImageDescriptor(TFSCommonUIImages.IMG_COMPARE));

        compareWithPreviousAction = new CompareChangeWithPreviousVersionAction(
            sourceFilesSelectionProvider,
            repository,
            CompareUIType.DIALOG,
            getShell());
        compareSubMenu.add(compareWithPreviousAction);
        compareToolbarAction.addSubAction(compareWithPreviousAction);

        compareWithWorkspaceAction = new CompareChangeWithWorkspaceVersionAction(
            sourceFilesSelectionProvider,
            repository,
            CompareUIType.DIALOG,
            getShell());
        compareSubMenu.add(compareWithWorkspaceAction);
        compareToolbarAction.addSubAction(compareWithWorkspaceAction);

        compareWithLatestAction = new CompareChangeWithLatestVersionAction(
            sourceFilesSelectionProvider,
            repository,
            CompareUIType.DIALOG,
            getShell());
        compareSubMenu.add(compareWithLatestAction);
        compareToolbarAction.addSubAction(compareWithLatestAction);
        compareToolbarAction.setDefaultSubAction(compareWithLatestAction);
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
            }
        });

        sourceFilesSubControl.getChangesTable().addDoubleClickListener(new IDoubleClickListener() {
            @Override
            public void doubleClick(final DoubleClickEvent event) {
                if (viewAction.isEnabled()) {
                    viewAction.run();
                }
            }
        });

        final IContributionManager contributionManager = sourceFilesSubControl.getContributionManager();

        contributionManager.add(new Separator());
        contributionManager.add(compareToolbarAction);
        contributionManager.add(viewToolbarAction);

        contributionManager.update(false);
    }

    @Override
    protected void hookAfterButtonsCreated() {
        final Button button = getButton(IDialogConstants.OK_ID);
        button.setText(Messages.getString("ChangesetDetailsDialog.SaveButtonText")); //$NON-NLS-1$
        setButtonLayoutData(button);

        button.setEnabled(false);
    }

    @Override
    protected String getBaseTitle() {
        final String messageFormat = Messages.getString("ChangesetDetailsDialog.DialogBaseTitleFormat"); //$NON-NLS-1$
        return MessageFormat.format(messageFormat, Integer.toString(changeset.getChangesetID()));
    }

    @Override
    protected Control createButtonBar(final Composite parent) {
        final Composite composite = new Composite(parent, SWT.NONE);
        GridDataBuilder.newInstance().hGrab().hFill().applyTo(composite);

        final GridLayout layout = new GridLayout(4, false);
        layout.marginWidth = convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_MARGIN);
        layout.marginHeight = convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_MARGIN);
        layout.horizontalSpacing = convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_SPACING);
        layout.verticalSpacing = convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_SPACING);
        composite.setLayout(layout);

        Label label = new Label(composite, SWT.NONE);
        label.setText(Messages.getString("ChangesetDetailsDialog.ChangesetLabelText")); //$NON-NLS-1$

        Text text = new Text(composite, SWT.BORDER | SWT.READ_ONLY);
        text.setText(String.valueOf(changeset.getChangesetID()));
        GridDataBuilder.newInstance().hFill().applyTo(text);

        label = new Label(composite, SWT.NONE);
        label.setText(Messages.getString("ChangesetDetailsDialog.ByUserLabelText")); //$NON-NLS-1$

        text = new Text(composite, SWT.BORDER | SWT.READ_ONLY);
        text.setText(changeset.getOwnerDisplayName());
        GridDataBuilder.newInstance().hGrab().hFill().applyTo(text);

        label = new Label(composite, SWT.NONE);
        label.setText(Messages.getString("ChangesetDetailsDialog.CreatedOnLabelText")); //$NON-NLS-1$

        text = new Text(composite, SWT.BORDER | SWT.READ_ONLY);
        text.setText(dateFormat.format(changeset.getDate().getTime()));
        GridDataBuilder.newInstance().hFill().applyTo(text);
        ControlSize.setCharWidthHint(text, 30);

        final Control control = super.createButtonBar(composite);
        GridDataBuilder.newInstance().hSpan(2).hAlign(SWT.END).applyTo(control, true);

        final GridLayout subLayout = (GridLayout) ((Composite) control).getLayout();
        subLayout.marginHeight = 0;
        subLayout.marginWidth = 0;

        return composite;
    }

    /**
     * @return true if the changeset being viewed was updated while the dialog
     *         was displayed, false if the changeset was not updated
     */
    public boolean wasChangesetUpdated() {
        return changesetUpdated;
    }
}
