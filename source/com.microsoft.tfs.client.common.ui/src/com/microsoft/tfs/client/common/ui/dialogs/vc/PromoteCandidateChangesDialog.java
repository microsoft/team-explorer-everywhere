// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.dialogs.vc;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import com.microsoft.tfs.client.common.commands.vc.ScanLocalWorkspaceCommand;
import com.microsoft.tfs.client.common.framework.command.Command;
import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.controls.vc.changes.CandidatesTable;
import com.microsoft.tfs.client.common.ui.controls.vc.changes.ChangeItem;
import com.microsoft.tfs.client.common.ui.dialogs.vc.candidates.CopyAction;
import com.microsoft.tfs.client.common.ui.dialogs.vc.candidates.DeleteFromDiskAction;
import com.microsoft.tfs.client.common.ui.dialogs.vc.candidates.IgnoreByExtensionAction;
import com.microsoft.tfs.client.common.ui.dialogs.vc.candidates.IgnoreByFileNameAction;
import com.microsoft.tfs.client.common.ui.dialogs.vc.candidates.IgnoreByFolderAction;
import com.microsoft.tfs.client.common.ui.dialogs.vc.candidates.IgnoreByLocalPathAction;
import com.microsoft.tfs.client.common.ui.dialogs.vc.candidates.PromoteAsRenameAction;
import com.microsoft.tfs.client.common.ui.dialogs.vc.candidates.RestoreAction;
import com.microsoft.tfs.client.common.ui.dialogs.vc.candidates.SelectAllAction;
import com.microsoft.tfs.client.common.ui.dialogs.vc.candidates.ViewLocalFolderAction;
import com.microsoft.tfs.client.common.ui.framework.command.UICommandExecutorFactory;
import com.microsoft.tfs.client.common.ui.framework.dialog.ExtendedButtonDialog;
import com.microsoft.tfs.client.common.ui.framework.layout.GridDataBuilder;
import com.microsoft.tfs.client.common.ui.framework.validation.ButtonValidatorBinding;
import com.microsoft.tfs.client.common.ui.teamexplorer.helpers.PendingChangesHelpers;
import com.microsoft.tfs.core.clients.versioncontrol.path.ServerPath;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PendingChange;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.RecursionType;
import com.microsoft.tfs.core.clients.versioncontrol.specs.ItemSpec;

public class PromoteCandidateChangesDialog extends ExtendedButtonDialog {
    private final TFSRepository repository;
    private ChangeItem[] candidates;
    private CandidatesTable table;

    private CopyAction copyAction;
    private SelectAllAction selectAllAction;
    private ViewLocalFolderAction viewLocalFolderAction;

    private IgnoreByLocalPathAction ignoreByLocalPathAction;
    private IgnoreByExtensionAction ignoreByExtensionAction;
    private IgnoreByFileNameAction ignoreByFileNameAction;
    private IgnoreByFolderAction ignoreByFolderAction;

    private DeleteFromDiskAction deleteFromDiskAction;
    private RestoreAction restoreAction;
    private PromoteAsRenameAction promoteAsRenameAction;

    public PromoteCandidateChangesDialog(
        final Shell parentShell,
        final TFSRepository repository,
        final ChangeItem[] candidates) {
        super(parentShell);
        this.repository = repository;
        this.candidates = candidates;

        // Disable standard OK/Cancel buttons.
        setOptionIncludeDefaultButtons(false);

        // Add back Promote/Close in their place. Order is important here, we
        // want Cancel on the right so add it last.
        addButtonDescription(
            IDialogConstants.OK_ID,
            Messages.getString("PromoteCandidateChangesDialog.PromoteButtonText"), //$NON-NLS-1$
            true);

        addButtonDescription(IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
    }

    // For action classes
    public CandidatesTable getTable() {
        return table;
    }

    // For action classes
    public TFSRepository getRepository() {
        return repository;
    }

    @Override
    protected String provideDialogTitle() {
        return Messages.getString("PromoteCandidateChangesDialog.PromoteCandidatesDialogTitle"); //$NON-NLS-1$
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void hookAddToDialogArea(final Composite dialogArea) {
        final GridLayout dialogLayout = new GridLayout();
        dialogLayout.marginWidth = getHorizontalMargin();
        dialogLayout.marginTop = getVerticalMargin();
        dialogLayout.marginBottom = 0;
        dialogLayout.horizontalSpacing = getHorizontalSpacing();
        dialogLayout.verticalSpacing = getVerticalSpacing();
        dialogArea.setLayout(dialogLayout);

        final Label label = new Label(dialogArea, SWT.WRAP);
        label.setText(Messages.getString("PromoteCandidateChangesDialog.PromoteCandidatesLabelText")); //$NON-NLS-1$
        GridDataBuilder.newInstance().wHint(10).hAlignFill().applyTo(label);

        table = new CandidatesTable(dialogArea, SWT.FULL_SELECTION | SWT.MULTI | SWT.CHECK, candidates);

        GridDataBuilder.newInstance().minHeight(200).minWidth(400).wHint(600).align(SWT.FILL, SWT.FILL).grab(
            true,
            true).applyTo(table);

        table.getContextMenu().addMenuListener(new IMenuListener() {
            @Override
            public void menuAboutToShow(final IMenuManager manager) {
                fillMenu(manager);
            }
        });

        copyAction = new CopyAction(this);
        selectAllAction = new SelectAllAction(this);
        viewLocalFolderAction = new ViewLocalFolderAction(this);

        ignoreByLocalPathAction = new IgnoreByLocalPathAction(this);
        ignoreByExtensionAction = new IgnoreByExtensionAction(this);
        ignoreByFileNameAction = new IgnoreByFileNameAction(this);
        ignoreByFolderAction = new IgnoreByFolderAction(this);

        deleteFromDiskAction = new DeleteFromDiskAction(this);
        restoreAction = new RestoreAction(this);
        promoteAsRenameAction = new PromoteAsRenameAction(this);

        // Pack the first time so the label will layout with the narrow width.
        // Let the table layout determine the width of the overall layout, not
        // the label. Once pack finishes, then we can set the width hint on the
        // label to be equal to the value computed for the label, and re-pack it
        // so the label will re-layout the text and wrap it correctly.
        dialogArea.pack();
        final GridData labelData = (GridData) label.getLayoutData();
        labelData.widthHint = label.getBounds().width;
        dialogArea.pack();
    }

    @Override
    protected void hookDialogIsOpen() {
        /*
         * We have to do this after hookAddToDialogArea completes to ensure the
         * base class has created the buttons, because we declined to use
         * default buttons in the constructor.
         *
         * Checkbox and elements validators are required, because the table
         * doesn't fire an uncheck event in the case where an element is removed
         * from the table.
         */
        final Button button = getButton(IDialogConstants.OK_ID);
        new ButtonValidatorBinding(button).bind(table.getCheckboxValidator());
        new ButtonValidatorBinding(button).bind(table.getElementsValidator());
    }

    public ChangeItem[] getCheckedCandidates() {
        return table.getCheckedChangeItems();
    }

    private void fillMenu(final IMenuManager manager) {
        final IStructuredSelection selection = (IStructuredSelection) table.getSelection();

        manager.removeAll();

        manager.add(copyAction);
        manager.add(selectAllAction);
        manager.add(viewLocalFolderAction);

        if (ignoreByLocalPathAction.isVisible(selection)
            || ignoreByExtensionAction.isVisible(selection)
            || ignoreByFileNameAction.isVisible(selection)
            || ignoreByFolderAction.isVisible(selection)) {
            manager.add(new Separator());
            manager.add(ignoreByLocalPathAction);
            manager.add(ignoreByExtensionAction);
            manager.add(ignoreByFileNameAction);
            manager.add(ignoreByFolderAction);
        }

        if (deleteFromDiskAction.isVisible(selection)) {
            manager.add(new Separator());
            manager.add(deleteFromDiskAction);
        }

        if (restoreAction.isVisible(selection)) {
            manager.add(new Separator());
            manager.add(restoreAction);
        }

        if (promoteAsRenameAction.isVisible(selection)) {
            manager.add(new Separator());
            manager.add(promoteAsRenameAction);
        }
    }

    /**
     * Scans the specified paths then refreshes the pending changes table with
     * the candidates in the current workspace.
     *
     * @param changesToScan
     *        the changes to scan (or <code>null</code> to scan all)
     */
    public void refreshCandidateTable(ChangeItem[] changesToScan) {
        if (changesToScan == null) {
            changesToScan = candidates;
        }

        final List<String> paths = new ArrayList<String>();
        for (final ChangeItem change : changesToScan) {
            final String path = change.getPendingChange().getLocalItem();
            if (path != null) {
                paths.add(path);
            }
        }

        if (paths.size() > 0) {
            final Command command = new ScanLocalWorkspaceCommand(repository, paths);
            UICommandExecutorFactory.newUICommandExecutor(getShell()).execute(command);
        }

        // Update table from workspace regardless of scan result

        final ItemSpec[] itemSpecs = new ItemSpec[] {
            new ItemSpec(ServerPath.ROOT, RecursionType.FULL)
        };

        final AtomicReference<PendingChange[]> outCandidateChanges = new AtomicReference<PendingChange[]>();
        repository.getWorkspace().getPendingChangesWithCandidates(itemSpecs, false, outCandidateChanges);

        candidates = PendingChangesHelpers.pendingChangesToChangeItems(repository, outCandidateChanges.get());
        table.setChangeItems(candidates);
    }
}