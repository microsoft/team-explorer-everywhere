// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.dialogs.vc;

import java.io.File;
import java.text.MessageFormat;

import org.eclipse.core.runtime.Path;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.framework.dialog.BaseDialog;
import com.microsoft.tfs.client.common.ui.framework.helper.ButtonHelper;
import com.microsoft.tfs.client.common.ui.framework.helper.SWTUtil;
import com.microsoft.tfs.client.common.ui.framework.sizing.ControlSize;
import com.microsoft.tfs.client.common.ui.helpers.AutomationIDHelper;
import com.microsoft.tfs.core.clients.versioncontrol.SupportedFeatures;
import com.microsoft.tfs.core.clients.versioncontrol.path.LocalPath;
import com.microsoft.tfs.core.clients.versioncontrol.path.ServerPath;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.RecursionType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;
import com.microsoft.tfs.core.clients.versioncontrol.specs.WorkspaceSpec;
import com.microsoft.tfs.util.Check;

public class SetWorkingFolderDialog extends BaseDialog {
    public static final String LOCALFOLDER_TEXT_ID = "SetWorkingFolderDialog.localFolderField"; //$NON-NLS-1$

    private String purpose = null;
    private String mappingStatus = null;

    private String localFolder;
    private String repositoryFolder;
    private final Workspace workspace;

    private Button browseLocalFolder;
    private Text statusText;
    private Text localFolderField;
    private Text repositoryFolderField;
    private boolean isRecursive = true;

    public SetWorkingFolderDialog(
        final Shell shell,
        final Workspace workspace,
        final String serverPath,
        final String purpose) {
        this(shell, workspace, serverPath, purpose, null, false);
    }

    public SetWorkingFolderDialog(
        final Shell shell,
        final Workspace workspace,
        final String serverPath,
        final String purpose,
        final String localPathHint,
        final boolean showStatus) {
        super(shell);

        Check.notNull(workspace, "workspace"); //$NON-NLS-1$
        Check.notNull(serverPath, "serverPath"); //$NON-NLS-1$

        this.workspace = workspace;
        this.repositoryFolder = serverPath;
        this.purpose = purpose;
        this.localFolder = (localPathHint == null) ? "" : localPathHint; //$NON-NLS-1$

        if (showStatus) {
            this.mappingStatus = ModifyFolderMappingDialog.getMappingStatusText(workspace, serverPath);
        }
        setOptionResizableDirections(SWT.HORIZONTAL);
    }

    @Override
    protected Control createDialogArea(final Composite parent) {
        final Composite container = (Composite) super.createDialogArea(parent);

        final GridLayout layout = new GridLayout(3, false);
        layout.marginWidth = getHorizontalMargin();
        layout.marginHeight = getVerticalMargin();
        layout.horizontalSpacing = getHorizontalSpacing();
        layout.verticalSpacing = getVerticalSpacing();
        container.setLayout(layout);

        if (purpose != null) {
            final Label purposeLabel = new Label(container, SWT.NONE);
            purposeLabel.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, true, false, 3, 1));
            purposeLabel.setText(purpose);
        }

        if (mappingStatus != null) {
            final Label statusLabel = new Label(container, SWT.NONE);
            statusLabel.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, true, false, 3, 1));
            statusLabel.setText(Messages.getString("SetWorkingFolderDialog.CurrentStatusHeaderLabel")); //$NON-NLS-1$

            statusText = new Text(container, SWT.BORDER);
            statusText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
            statusText.setText(mappingStatus);
            statusText.setEnabled(false);
        }

        final Label label = new Label(container, SWT.NONE);
        label.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, true, false, 3, 1));
        label.setText(provideServerFolderLabelText());

        repositoryFolderField = new Text(container, SWT.BORDER);
        repositoryFolderField.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
        repositoryFolderField.setText(repositoryFolder);
        repositoryFolderField.setEditable(false);

        // fill the last cell on this grid row with a spacer.
        SWTUtil.createGridLayoutSpacer(container);

        final Label label_1 = new Label(container, SWT.NONE);
        label_1.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, true, false, 3, 1));
        label_1.setText(Messages.getString("SetWorkingFolderDialog.LocalFolderLabelText")); //$NON-NLS-1$

        localFolderField = new Text(container, SWT.BORDER);
        AutomationIDHelper.setWidgetID(localFolderField, LOCALFOLDER_TEXT_ID);
        localFolderField.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
        localFolderField.setText(localFolder);
        localFolderField.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(final ModifyEvent e) {
                validate();
            }
        });

        browseLocalFolder = new Button(container, SWT.NONE);
        browseLocalFolder.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1));
        browseLocalFolder.setText(Messages.getString("SetWorkingFolderDialog.BrowseLocalButtonText")); //$NON-NLS-1$
        browseLocalFolder.addSelectionListener(new SelectionListener() {
            @Override
            public void widgetDefaultSelected(final SelectionEvent e) {
                browseLocalFolderClicked();
            }

            @Override
            public void widgetSelected(final SelectionEvent e) {
                browseLocalFolderClicked();
            }
        });

        if (workspace.getClient().getServerSupportedFeatures().contains(SupportedFeatures.ONE_LEVEL_MAPPING)) {
            final Button checkRecursive = new Button(container, SWT.CHECK);
            checkRecursive.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1));
            checkRecursive.setText(Messages.getString("SetWorkingFolderDialog.RecursiveCheckboxText")); //$NON-NLS-1$
            checkRecursive.setSelection(isRecursive);
            checkRecursive.addSelectionListener(new SelectionListener() {
                @Override
                public void widgetSelected(final SelectionEvent e) {
                    isRecursive = checkRecursive.getSelection();
                }

                @Override
                public void widgetDefaultSelected(final SelectionEvent e) {
                    isRecursive = checkRecursive.getSelection();
                }
            });
        }

        ControlSize.setCharWidthHint(repositoryFolderField, 80);

        localFolderField.setFocus();

        ButtonHelper.setButtonsToButtonBarSize(new Button[] {
            browseLocalFolder
        });

        return container;
    }

    /*
     * (non-Javadoc)
     *
     * @seecom.microsoft.tfs.client.common.ui.shared.dialog.BaseDialog#
     * hookAfterButtonsCreated()
     */
    @Override
    protected void hookAfterButtonsCreated() {
        validate();
    }

    private void browseLocalFolderClicked() {
        final DirectoryDialog dirDialog = new DirectoryDialog(getShell());

        dirDialog.setMessage(Messages.getString("SetWorkingFolderDialog.SelectLocalDialogText")); //$NON-NLS-1$

        final String selectedDir = dirDialog.open();

        if (selectedDir != null && selectedDir.length() > 0) {
            /*
             * Append the last segment of the server path to the local path.
             * There are some exceptions: if the repository path is invalid, or
             * is root, or the local path already ends with the exact same last
             * segment, we don't append (just use the browsed path).
             */
            final String browsedPath = new Path(selectedDir).toOSString();

            /*
             * Canonicalize strips any final slash, getFileName just gets the
             * last path part (doesn't know if it's a file or folder).
             */
            final String lastRepositoryPathSegment = ServerPath.getFileName(ServerPath.canonicalize(repositoryFolder));

            if (ServerPath.isServerPath(repositoryFolder) == false
                || ServerPath.equals(repositoryFolder, ServerPath.ROOT)
                || LocalPath.equals(LocalPath.getFileName(browsedPath), lastRepositoryPathSegment)) {
                localFolder = browsedPath;
            } else if (ServerPath.isServerPath(repositoryFolder)) {
                // Valid path which needs the last segment appended.
                localFolder = new File(browsedPath, lastRepositoryPathSegment).getAbsolutePath();
            }

            localFolderField.setText(localFolder);
        }
        validate();
    }

    public String getLocalFolder() {
        return localFolder;
    }

    public String getRepositoryFolder() {
        return repositoryFolder;
    }

    public RecursionType getRecursionType() {
        return (isRecursive) ? RecursionType.FULL : RecursionType.ONE_LEVEL;
    }

    public void setPurpose(final String purpose) {
        this.purpose = purpose;
    }

    public void setMappingStatus(final String status) {
        this.mappingStatus = status;
    }

    private void validate() {
        final Button okButton = getButton(IDialogConstants.OK_ID);
        if (okButton != null) {
            okButton.setEnabled(isValid());
        }
    }

    private boolean isValid() {
        if (repositoryFolderField.getText().trim().length() < 1) {
            // No Server path specified
            return false;
        }

        if (localFolderField.getText().trim().length() < 1) {
            // No local path for an "Active" working folder mapping.
            return false;
        }

        localFolder = localFolderField.getText().trim();
        repositoryFolder = repositoryFolderField.getText().trim();

        return repositoryFolder.startsWith("$/"); //$NON-NLS-1$
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.jface.dialogs.Dialog#okPressed()
     */
    @Override
    protected void okPressed() {
        localFolder = localFolderField.getText().trim();
        repositoryFolder = repositoryFolderField.getText().trim();

        // Verify we don't already have a mapping for this local path.
        if (localFolderAlreadyMapped()) {
            final WorkspaceSpec spec = new WorkspaceSpec(workspace.getName(), workspace.getOwnerDisplayName());

            final String title = Messages.getString("SetWorkingFolderDialog.ErrorDialogTitle"); //$NON-NLS-1$
            final String messageFormat = Messages.getString("SetWorkingFolderDialog.ErrorDialogTextFormat"); //$NON-NLS-1$
            final String message = MessageFormat.format(messageFormat, spec.toString(), localFolder);
            MessageDialog.openWarning(getShell(), title, message);

            // Don't exit the dialog.
            return;
        }

        super.okPressed();
    }

    @Override
    protected String provideDialogTitle() {
        return Messages.getString("SetWorkingFolderDialog.WorkingFolderDialogTitle"); //$NON-NLS-1$
    }

    protected String provideServerFolderLabelText() {
        return Messages.getString("SetWorkingFolderDialog.RepositoryFolderLabelText"); //$NON-NLS-1$
    }

    protected boolean localFolderAlreadyMapped() {
        return workspace.getExactMappingForLocalPath(localFolder) != null;
    }
}
