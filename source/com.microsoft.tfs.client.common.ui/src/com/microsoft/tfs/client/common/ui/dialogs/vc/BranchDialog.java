// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.dialogs.vc;

import java.text.MessageFormat;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.microsoft.tfs.client.common.commands.vc.SetWorkingFolderCommand;
import com.microsoft.tfs.client.common.item.ServerItemPath;
import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.controls.vc.VersionPickerControl;
import com.microsoft.tfs.client.common.ui.framework.command.UICommandExecutorFactory;
import com.microsoft.tfs.client.common.ui.framework.dialog.BaseDialog;
import com.microsoft.tfs.client.common.ui.helpers.AutomationIDHelper;
import com.microsoft.tfs.client.common.ui.vc.serveritem.ServerItemType;
import com.microsoft.tfs.client.common.ui.vc.serveritem.VersionedItemSource;
import com.microsoft.tfs.core.clients.versioncontrol.GetOptions;
import com.microsoft.tfs.core.clients.versioncontrol.WebServiceLevel;
import com.microsoft.tfs.core.clients.versioncontrol.exceptions.ServerPathFormatException;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.WorkingFolderType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.LatestVersionSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.VersionSpec;
import com.microsoft.tfs.util.Check;

/**
 * Dialog used when creating a new branch.
 */
public class BranchDialog extends BaseDialog {
    public static final String CONVERTBRANCH_BUTTON_ID = "BranchDialog.convertBranchButton"; //$NON-NLS-1$
    public static final String CREATELOCAL_BUTTON_ID = "BranchDialog.createLocalButton"; //$NON-NLS-1$
    public static final String TARGETPATH_TEXT_ID = "BranchDialog.targetText";//$NON-NLS-1$

    private final TFSRepository repository;
    private String branchFromPath;
    private String branchToPath;
    private final boolean itemIsFolder;
    private boolean createLocalCopy;
    private boolean convertToBranch;
    private VersionSpec branchFromVersion;

    private Text sourceText;
    private Text targetText;
    private VersionPickerControl versionDropdownWidget;

    private Button createLocalButton;
    private Button convertBranchButton;

    /**
     * Create a new Branch dialog with a provided from path and suggested to
     * path.
     */
    public BranchDialog(
        final Shell parentShell,
        final TFSRepository repository,
        final String branchFromPath,
        String proposedBranchToPath,
        final boolean itemIsFolder) {
        super(parentShell);

        Check.notNull(repository, "repository"); //$NON-NLS-1$
        Check.notNull(branchFromPath, "branchFromPath"); //$NON-NLS-1$

        this.repository = repository;
        this.branchFromPath = branchFromPath;
        this.itemIsFolder = itemIsFolder;

        if (proposedBranchToPath == null) {
            proposedBranchToPath = ""; //$NON-NLS-1$
        }

        branchToPath = proposedBranchToPath;
    }

    /*
     * (non-Javadoc)
     *
     * @seecom.microsoft.tfs.client.common.ui.shared.dialog.BaseDialog#
     * provideDialogTitle()
     */
    @Override
    protected String provideDialogTitle() {
        return Messages.getString("BranchDialog.DialogTitle"); //$NON-NLS-1$
    }

    /*
     * (non-Javadoc)
     *
     * @seecom.microsoft.tfs.client.common.ui.shared.dialog.BaseDialog#
     * hookAddToDialogArea(org.eclipse .swt.widgets.Composite)
     */
    @Override
    protected void hookAddToDialogArea(final Composite dialogArea) {
        final GridLayout layout = new GridLayout(2, false);
        layout.marginWidth = getHorizontalMargin();
        layout.marginHeight = getVerticalMargin();
        layout.horizontalSpacing = getHorizontalSpacing();
        layout.verticalSpacing = getVerticalSpacing();
        dialogArea.setLayout(layout);

        final Label lblSource = new Label(dialogArea, SWT.NONE);
        lblSource.setLayoutData(new GridData(GridData.BEGINNING, GridData.CENTER, false, false, 2, 1));
        lblSource.setText(Messages.getString("BranchDialog.SourceLabelText")); //$NON-NLS-1$

        sourceText = new Text(dialogArea, SWT.READ_ONLY | SWT.BORDER);
        sourceText.setLayoutData(new GridData(GridData.FILL, GridData.CENTER, true, false, 2, 1));
        sourceText.setText(branchFromPath);

        final Label lblTarget = new Label(dialogArea, SWT.NONE);
        lblTarget.setLayoutData(new GridData(GridData.BEGINNING, GridData.CENTER, false, false, 2, 1));
        lblTarget.setText(Messages.getString("BranchDialog.TargetLabelText")); //$NON-NLS-1$

        targetText = new Text(dialogArea, SWT.BORDER);
        targetText.setLayoutData(new GridData(GridData.FILL, GridData.CENTER, true, false));
        targetText.setText(branchToPath);
        AutomationIDHelper.setWidgetID(targetText, TARGETPATH_TEXT_ID);
        targetText.selectAll();
        targetText.forceFocus();

        final Button browseButton = new Button(dialogArea, SWT.NONE);
        browseButton.setText(Messages.getString("BranchDialog.BrowseButtonText")); //$NON-NLS-1$
        browseButton.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetDefaultSelected(final SelectionEvent e) {
                browseClicked();
            }

            @Override
            public void widgetSelected(final SelectionEvent e) {
                browseClicked();
            }
        });

        final Group versionGroup = new Group(dialogArea, SWT.NONE);
        versionGroup.setLayoutData(new GridData(GridData.FILL, GridData.FILL, false, false, 2, 1));
        versionGroup.setText(Messages.getString("BranchDialog.versionGroupText")); //$NON-NLS-1$
        versionGroup.setLayout(new GridLayout(1, false));

        versionDropdownWidget = new VersionPickerControl(versionGroup, SWT.NONE);
        versionDropdownWidget.setText(Messages.getString("BranchDialog.VersionPickerLabelText")); //$NON-NLS-1$
        versionDropdownWidget.setLayoutData(new GridData(GridData.FILL, GridData.CENTER, true, false));
        versionDropdownWidget.setRepository(repository);
        versionDropdownWidget.setVersionSpec(LatestVersionSpec.INSTANCE);

        createLocalButton = new Button(dialogArea, SWT.CHECK);
        AutomationIDHelper.setWidgetID(createLocalButton, CREATELOCAL_BUTTON_ID);
        createLocalButton.setLayoutData(new GridData(GridData.BEGINNING, GridData.CENTER, false, false, 2, 1));
        createLocalButton.setText(Messages.getString("BranchDialog.CreateLocalButtonText")); //$NON-NLS-1$
        createLocalButton.setSelection(true);

        // Enable only for TFS 2010
        if (repository.getVersionControlClient().getServiceLevel().getValue() >= WebServiceLevel.TFS_2010.getValue()
            && itemIsFolder) {
            convertBranchButton = new Button(dialogArea, SWT.CHECK);
            AutomationIDHelper.setWidgetID(convertBranchButton, CONVERTBRANCH_BUTTON_ID);
            convertBranchButton.setLayoutData(new GridData(GridData.BEGINNING, GridData.CENTER, false, false, 2, 1));
            convertBranchButton.setText(Messages.getString("BranchDialog.ConvertBranchButtonText")); //$NON-NLS-1$
            convertBranchButton.setSelection(true);
        } else {
            convertToBranch = false;
        }
    }

    public void browseClicked() {
        ServerItemPath branchTo;
        try {
            branchTo = new ServerItemPath(targetText.getText());
        } catch (final Exception e) {
            branchTo = new ServerItemPath(branchToPath);
        }

        final ServerItemTreeDialog treeDialog =
            new ServerItemTreeDialog(
                getShell(),
                Messages.getString("BranchDialog.ServerItemDialogText"), //$NON-NLS-1$
                branchTo.getFullPath(),
                new VersionedItemSource(
                    repository.getVersionControlClient().getConnection(),
                    LatestVersionSpec.INSTANCE),
                ServerItemType.ALL_FOLDERS);

        if (treeDialog.open() == IDialogConstants.OK_ID) {
            targetText.setText(treeDialog.getSelectedServerPath());
            targetText.selectAll();
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.jface.dialogs.Dialog#okPressed()
     */
    @Override
    protected void okPressed() {
        branchFromPath = sourceText.getText();
        branchToPath = targetText.getText();
        createLocalCopy = createLocalButton.getSelection();
        if (convertBranchButton != null) {
            convertToBranch = convertBranchButton.getSelection();
        }
        branchFromVersion = versionDropdownWidget.getVersionSpec();

        boolean mapped = false;

        try {
            mapped = (repository.getWorkspace().getMappedLocalPath(branchToPath) != null);
        } catch (final ServerPathFormatException e) {
            MessageDialog.openError(
                getShell(),
                Messages.getString("BranchDialog.InvalidPathDialogTitle"), //$NON-NLS-1$
                Messages.getString("BranchDialog.InvalidPathDialogText")); //$NON-NLS-1$
            return;
        }

        if (createLocalCopy && !mapped) {
            if (mapTarget(branchToPath) == false) {
                return;
            }
        } else if (!mapped) {
            MessageDialog.openError(
                getShell(),
                Messages.getString("BranchDialog.NotMappedDialogTitle"), //$NON-NLS-1$
                Messages.getString("BranchDialog.NotMappedDialogText")); //$NON-NLS-1$
            return;
        }

        super.okPressed();
    }

    /**
     * Maps the target of the branch operation.
     *
     * @return true if the target could be successfully mapped.
     */
    private boolean mapTarget(final String target) {
        final String messageFormat = Messages.getString("BranchDialog.WorkingFolderDialogTextFormat"); //$NON-NLS-1$
        final String purpose = MessageFormat.format(messageFormat, target, target);

        final Workspace workspace = repository.getWorkspace();
        final SetWorkingFolderDialog dialog = new SetWorkingFolderDialog(getShell(), workspace, target, purpose);

        if (dialog.open() == IDialogConstants.OK_ID) {
            final SetWorkingFolderCommand setCommand = new SetWorkingFolderCommand(
                repository,
                target,
                dialog.getLocalFolder(),
                WorkingFolderType.MAP,
                dialog.getRecursionType(),
                false);

            if (!UICommandExecutorFactory.newUICommandExecutor(getShell()).execute(setCommand).isOK()) {
                return false;
            }

            return true;
        }

        return false;
    }

    /**
     * @return the branchFromPath
     */
    public String getBranchFromPath() {
        return branchFromPath;
    }

    /**
     * @return the branchToPath
     */
    public String getBranchToPath() {
        return branchToPath;
    }

    /**
     * @return the createLocalCopy
     */
    public GetOptions getGetOptions() {
        if (!createLocalCopy) {
            return GetOptions.PREVIEW;
        }

        return GetOptions.NONE;
    }

    /**
     * @return the version the user wants to branch from
     */
    public VersionSpec getVersionSpec() {
        return branchFromVersion;
    }

    public boolean convertToBranch() {
        return convertToBranch;
    }
}
