// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.dialogs.vc;

import java.text.MessageFormat;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.microsoft.tfs.client.common.item.ServerItemPath;
import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.TFSCommonUIClientPlugin;
import com.microsoft.tfs.client.common.ui.controls.vc.VersionPickerControl;
import com.microsoft.tfs.client.common.ui.framework.dialog.BaseDialog;
import com.microsoft.tfs.client.common.ui.framework.helper.MessageBoxHelpers;
import com.microsoft.tfs.client.common.ui.framework.helper.SWTUtil;
import com.microsoft.tfs.client.common.ui.framework.image.ImageHelper;
import com.microsoft.tfs.client.common.ui.framework.layout.GridDataBuilder;
import com.microsoft.tfs.client.common.ui.framework.sizing.ControlSize;
import com.microsoft.tfs.client.common.ui.vc.serveritem.ServerItemType;
import com.microsoft.tfs.client.common.ui.vc.serveritem.VersionedItemSource;
import com.microsoft.tfs.core.clients.versioncontrol.GetOptions;
import com.microsoft.tfs.core.clients.versioncontrol.path.ServerPath;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.LatestVersionSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.VersionSpec;
import com.microsoft.tfs.util.Check;

/**
 * Dialog displayed when branching a branch.
 */
public class BranchBranchDialog extends BaseDialog {

    private final TFSRepository repository;
    private final String branchFromPath;
    private String branchToPath;
    private VersionSpec branchFromVersion;
    private String description;

    private Text sourceText;
    private Text targetText;
    private Text descriptionText;
    private VersionPickerControl versionDropdownWidget;

    private final ImageHelper imageHelper = new ImageHelper(TFSCommonUIClientPlugin.PLUGIN_ID);

    public BranchBranchDialog(
        final Shell parentShell,
        final TFSRepository repository,
        final String branchFromPath,
        String proposedBranchToPath) {
        super(parentShell);
        Check.notNull(repository, "repository"); //$NON-NLS-1$
        Check.notNull(branchFromPath, "branchFromPath"); //$NON-NLS-1$

        this.repository = repository;
        this.branchFromPath = branchFromPath;

        if (proposedBranchToPath == null) {
            proposedBranchToPath = ""; //$NON-NLS-1$
        }

        branchToPath = proposedBranchToPath;
        setOptionIncludeDefaultButtons(false);
        addButtonDescription(IDialogConstants.OK_ID, Messages.getString("BranchBranchDialog.BranchButtonText"), true); //$NON-NLS-1$
        addButtonDescription(IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);

    }

    @Override
    protected String provideDialogTitle() {
        final String messageFormat = Messages.getString("BranchBranchDialog.DialogTitleFormat"); //$NON-NLS-1$
        return MessageFormat.format(messageFormat, ServerPath.getFileName(branchFromPath));
    }

    @Override
    protected void hookAddToDialogArea(final Composite dialogArea) {
        final GridLayout layout = new GridLayout(3, false);
        layout.marginWidth = getHorizontalMargin();
        layout.marginHeight = getVerticalMargin();
        layout.horizontalSpacing = getHorizontalSpacing();
        layout.verticalSpacing = getVerticalSpacing();
        dialogArea.setLayout(layout);

        final Label sourceNameLabel =
            SWTUtil.createLabel(dialogArea, Messages.getString("BranchBranchDialog.SourceNameLabelText")); //$NON-NLS-1$
        GridDataBuilder.newInstance().hSpan(layout).hFill().hGrab().applyTo(sourceNameLabel);

        sourceText = new Text(dialogArea, SWT.READ_ONLY | SWT.BORDER);
        GridDataBuilder.newInstance().hSpan(layout).hFill().applyTo(sourceText);
        sourceText.setText(branchFromPath);

        final Label branchFromLabel =
            SWTUtil.createLabel(dialogArea, Messages.getString("BranchBranchDialog.BranchFromLabelText")); //$NON-NLS-1$
        GridDataBuilder.newInstance().hSpan(layout).hFill().hGrab().applyTo(branchFromLabel);

        versionDropdownWidget = new VersionPickerControl(dialogArea, SWT.NONE);
        GridDataBuilder.newInstance().hSpan(layout).hFill().hGrab().applyTo(versionDropdownWidget);
        versionDropdownWidget.setText(Messages.getString("BranchBranchDialog.VersionPickerLabelText")); //$NON-NLS-1$
        versionDropdownWidget.setRepository(repository);
        versionDropdownWidget.setVersionSpec(LatestVersionSpec.INSTANCE);

        final Label targetNameLabel =
            SWTUtil.createLabel(dialogArea, Messages.getString("BranchBranchDialog.TargetNameLabelText")); //$NON-NLS-1$
        GridDataBuilder.newInstance().hSpan(layout).hFill().hGrab().applyTo(targetNameLabel);
        targetText = new Text(dialogArea, SWT.BORDER);
        GridDataBuilder.newInstance().hSpan(2).hFill().hGrab().applyTo(targetText);
        targetText.setText(branchToPath);
        targetText.setSelection(ServerPath.getParent(targetText.getText()).length() + 1, targetText.getText().length());
        targetText.forceFocus();

        final Button browseButton = new Button(dialogArea, SWT.NONE);
        browseButton.setText(Messages.getString("BranchBranchDialog.BrowseButtonText")); //$NON-NLS-1$
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

        final Label descriptionLabel =
            SWTUtil.createLabel(dialogArea, Messages.getString("BranchBranchDialog.DescriptionLabelText")); //$NON-NLS-1$
        GridDataBuilder.newInstance().hSpan(layout).hGrab().hFill().applyTo(descriptionLabel);

        descriptionText = new Text(dialogArea, SWT.MULTI | SWT.WRAP | SWT.V_SCROLL | SWT.BORDER);
        GridDataBuilder.newInstance().hSpan(layout).grab().fill().applyTo(descriptionText);
        descriptionText.setText(Messages.getString("BranchBranchDialog.BranchedFromTexboxText") + branchFromPath); //$NON-NLS-1$
        ControlSize.setCharSizeHints(descriptionText, 80, 10);

        final Label infoImage = SWTUtil.createLabel(dialogArea, imageHelper.getImage("images/vc/info.gif")); //$NON-NLS-1$
        GridDataBuilder.newInstance().vAlignTop().applyTo(infoImage);

        final Label infoText =
            SWTUtil.createLabel(dialogArea, SWT.WRAP, Messages.getString("BranchBranchDialog.InfoLabelText")); //$NON-NLS-1$
        GridDataBuilder.newInstance().fill().align(SWT.FILL, SWT.BEGINNING).hGrab().wHint(
            IDialogConstants.MINIMUM_MESSAGE_AREA_WIDTH).hSpan(2).applyTo(infoText);

    }

    @Override
    protected void okPressed() {
        if (MessageBoxHelpers.dialogYesNoPrompt(
            getShell(),
            null,
            Messages.getString("BranchBranchDialog.ConfirmBranchDialogText"))) //$NON-NLS-1$
        {
            super.okPressed();
        }
    }

    private void browseClicked() {
        ServerItemPath branchTo;
        try {
            branchTo = new ServerItemPath(targetText.getText());
        } catch (final Exception e) {
            branchTo = new ServerItemPath(branchToPath);
        }

        final ServerItemTreeDialog treeDialog =
            new ServerItemTreeDialog(
                getShell(),
                Messages.getString("BranchBranchDialog.BranchDialogTitle"), //$NON-NLS-1$
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

    @Override
    protected void hookDialogAboutToClose() {
        branchToPath = targetText.getText();
        branchFromVersion = versionDropdownWidget.getVersionSpec();
        description = descriptionText.getText().trim();
        imageHelper.dispose();
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

    public GetOptions getGetOptions() {
        return null;
    }

    /**
     * @return the version the user wants to branch from
     */
    public VersionSpec getVersionSpec() {
        return branchFromVersion;
    }

    public String getDescription() {
        return description;
    }

}
