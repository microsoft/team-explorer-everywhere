// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teambuild.wizards;

import java.io.File;
import java.text.MessageFormat;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.osgi.framework.Bundle;

import com.microsoft.tfs.client.common.ui.framework.dialog.BaseDialog;
import com.microsoft.tfs.client.common.ui.framework.helper.MessageBoxHelpers;
import com.microsoft.tfs.client.common.ui.framework.helper.SWTUtil;
import com.microsoft.tfs.client.common.ui.framework.image.ImageHelper;
import com.microsoft.tfs.client.common.ui.framework.layout.GridDataBuilder;
import com.microsoft.tfs.client.common.ui.teambuild.Messages;
import com.microsoft.tfs.client.common.ui.teambuild.TFSTeamBuildPlugin;
import com.microsoft.tfs.core.clients.build.IBuildDefinition;
import com.microsoft.tfs.core.clients.versioncontrol.path.ServerPath;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.Platform;
import com.microsoft.tfs.util.StringUtil;

public class UploadArchiveDialog extends BaseDialog {
    private final String title;
    private final String buildToolName;
    private String serverPath;
    private Text serverPathText;

    private String localPath;
    private String localFolderPath;
    private String localZipPath;

    private String archiveName;
    private String folderZipName;
    private String fileZipName;

    private Composite folderSection;
    private Composite zipSection;
    private Button selectFolderRadio;
    private Button selectZipRadio;

    private boolean isFolderSelected;

    private Text folderPathText;
    private Text zipPathText;
    private Label iconLabel;
    private Label helpLabel;
    private final ImageHelper imageHelper = new ImageHelper(TFSTeamBuildPlugin.PLUGIN_ID);
    private final Image warningImage;
    private final Image infoImage;

    private final String folderHelper;
    private final String archiveHelper;
    private final String invalidServerPathText = Messages.getString("UploadArchiveDialog.InvalidServerPathText"); //$NON-NLS-1$
    private static final String JAVA = "java"; //$NON-NLS-1$
    private static final String ANT = "ant"; //$NON-NLS-1$
    private static final String MAVEN = "M2"; //$NON-NLS-1$
    private final IBuildDefinition buildDefinition;

    public UploadArchiveDialog(
        final Shell parentShell,
        final String title,
        final String buildToolName,
        final String serverPath,
        final IBuildDefinition buildDefinition) {
        super(parentShell);
        Check.notNull(title, "title"); //$NON-NLS-1$
        this.serverPath = serverPath;
        if (!serverPath.endsWith("/")) //$NON-NLS-1$
        {
            this.serverPath = serverPath + "/"; //$NON-NLS-1$
        }
        this.title = title;
        this.buildToolName = buildToolName;
        this.buildDefinition = buildDefinition;
        this.folderZipName = ""; //$NON-NLS-1$
        this.fileZipName = ""; //$NON-NLS-1$
        if (buildToolName.equalsIgnoreCase(JAVA)) {
            folderHelper =
                MessageFormat.format(Messages.getString("UploadArchiveDialog.SelectFolderHelperFormat"), "JDK"); //$NON-NLS-1$ //$NON-NLS-2$
            archiveHelper =
                MessageFormat.format(Messages.getString("UploadArchiveDialog.SelectArchiveHelperFormat"), "JDK"); //$NON-NLS-1$ //$NON-NLS-2$
        } else {
            folderHelper =
                MessageFormat.format(Messages.getString("UploadArchiveDialog.SelectFolderHelperFormat"), buildToolName); //$NON-NLS-1$
            archiveHelper = MessageFormat.format(
                Messages.getString("UploadArchiveDialog.SelectArchiveHelperFormat"), //$NON-NLS-1$
                buildToolName);
        }

        warningImage = imageHelper.getImage("icons/warning.gif"); //$NON-NLS-1$
        infoImage = imageHelper.getImage("icons/info.gif"); //$NON-NLS-1$
    }

    @Override
    protected String provideDialogTitle() {
        return title;
    }

    @Override
    protected Control createDialogArea(final Composite parent) {
        this.getShell().setMinimumSize(500, 300);
        final Composite composite = (Composite) super.createDialogArea(parent);
        final GridLayout gridLayout = new GridLayout(2, false);
        gridLayout.marginWidth = getHorizontalMargin();
        gridLayout.marginHeight = getVerticalMargin();
        gridLayout.horizontalSpacing = getHorizontalSpacing();
        gridLayout.verticalSpacing = getVerticalSpacing();
        composite.setLayout(gridLayout);

        final Label label = new Label(composite, SWT.NONE);
        label.setText(Messages.getString("UploadArchiveDialog.UploadToServerPathLabel")); //$NON-NLS-1$
        GridDataBuilder.newInstance().hSpan(2).vAlign(SWT.CENTER).applyTo(label);

        serverPathText = new Text(composite, SWT.BORDER);
        serverPathText.setText(serverPath);
        GridDataBuilder.newInstance().hFill().hGrab().hSpan(2).vAlign(SWT.CENTER).applyTo(serverPathText);

        selectFolderRadio = new Button(composite, SWT.RADIO);

        final String tmpLabel = buildToolName.equalsIgnoreCase(JAVA) ? "JDK" : buildToolName; //$NON-NLS-1$
        selectFolderRadio.setText(
            MessageFormat.format(Messages.getString("UploadArchiveDialog.RadioFolderLabelFormat"), tmpLabel)); //$NON-NLS-1$
        GridDataBuilder.newInstance().vAlign(SWT.CENTER).applyTo(selectFolderRadio);
        folderSection = selectFolderComposite(composite);

        selectZipRadio = new Button(composite, SWT.RADIO);
        selectZipRadio.setText(
            MessageFormat.format(Messages.getString("UploadArchiveDialog.RadioArchiveLabelFormat"), tmpLabel)); //$NON-NLS-1$
        GridDataBuilder.newInstance().vAlign(SWT.CENTER).applyTo(selectZipRadio);
        zipSection = selectZipComposite(composite);
        attachSelectionListener(selectFolderRadio);
        attachSelectionListener(selectZipRadio);

        GridDataBuilder.newInstance().hSpan(2).hFill().hGrab().applyTo(createInfoComposite(composite));

        serverPathText.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(final ModifyEvent e) {
                final String inputPath = serverPathText.getText().trim();
                if (StringUtil.isNullOrEmpty(inputPath) || !ServerPath.isServerPath(inputPath)) {
                    getButton(IDialogConstants.OK_ID).setEnabled(false);
                    iconLabel.setImage(warningImage);
                    helpLabel.setText(invalidServerPathText);
                    return;
                } else {
                    getButton(IDialogConstants.OK_ID).setEnabled(true);
                    setHelperLabel();
                }

                if (inputPath.endsWith(".zip")) //$NON-NLS-1$
                {
                    final String serverDir =
                        inputPath.substring(0, inputPath.lastIndexOf(ServerPath.PREFERRED_SEPARATOR_CHARACTER) + 1);
                    final String inputZipName = ServerPath.getFileName(inputPath);
                    if (!StringUtil.isNullOrEmpty(serverDir) && !StringUtil.isNullOrEmpty(inputZipName)) {
                        archiveName = inputZipName;
                        serverPath = serverDir;
                    }
                } else {
                    serverPath = inputPath;
                    if (!serverPath.endsWith("/")) //$NON-NLS-1$
                    {
                        serverPath = serverPath + ServerPath.PREFERRED_SEPARATOR_CHARACTER;
                    }
                }
            }
        });

        composite.addDisposeListener(new DisposeListener() {
            @Override
            public void widgetDisposed(final DisposeEvent e) {
                imageHelper.dispose();
                warningImage.dispose();
                infoImage.dispose();
            }
        });
        return composite;
    }

    @Override
    protected void okPressed() {
        if (StringUtil.isNullOrEmpty(serverPath)) {
            iconLabel.setImage(warningImage);
            helpLabel.setText(invalidServerPathText);
            return;
        }

        if (!new File(localPath).exists()) {
            MessageBoxHelpers.errorMessageBox(
                getShell(),
                Messages.getString("UploadArchiveDialog.WarningMessage"), //$NON-NLS-1$
                MessageFormat.format(Messages.getString("UploadArchiveDialog.FileNotExistMessageFormat"), localPath)); //$NON-NLS-1$
            return;
        }

        boolean confirmUpload = false;

        if (isFolderSelected) {
            confirmUpload = MessageBoxHelpers.dialogYesNoPrompt(
                getShell(),
                Messages.getString("UploadArchiveDialog.ConfirmMessage"), //$NON-NLS-1$
                MessageFormat.format(
                    Messages.getString("UploadArchiveDialog.ConfirmCreateUploadArchiveFormat"), //$NON-NLS-1$
                    serverPath + archiveName,
                    localPath));
        } else {
            confirmUpload = MessageBoxHelpers.dialogYesNoPrompt(
                getShell(),
                Messages.getString("UploadArchiveDialog.ConfirmMessage"), //$NON-NLS-1$
                MessageFormat.format(
                    Messages.getString("UploadArchiveDialog.ConfirmUploadArchiveFormat"), //$NON-NLS-1$
                    localPath,
                    serverPath + archiveName));
        }

        if (!confirmUpload) {
            return;
        }

        super.okPressed();
    }

    @Override
    protected void hookAfterButtonsCreated() {
        if (StringUtil.isNullOrEmpty(archiveName)
            || StringUtil.isNullOrEmpty(localPath)
            || StringUtil.isNullOrEmpty(serverPath)) {
            getButton(IDialogConstants.OK_ID).setEnabled(false);
        }
    }

    private Composite selectFolderComposite(final Composite parent) {
        final Composite composite = SWTUtil.createComposite(parent);
        SWTUtil.gridLayout(composite, 2, false);
        GridDataBuilder.newInstance().hFill().hGrab().applyTo(composite);
        folderPathText = new Text(composite, SWT.BORDER);
        GridDataBuilder.newInstance().hFill().hGrab().vAlign(SWT.CENTER).applyTo(folderPathText);

        /*
         * Smart default first choice: get path from environment variables
         */
        final String envVar = buildToolName.equalsIgnoreCase("Maven") ? MAVEN : buildToolName; //$NON-NLS-1$
        localFolderPath = getEnvVariablePath(envVar);

        /*
         * Smart default second choice: get Ant path from eclipse directory
         */
        if (StringUtil.isNullOrEmpty(localFolderPath) && buildToolName.equalsIgnoreCase(ANT)) {
            localFolderPath = getEclipseAntPath();
        }

        if (!StringUtil.isNullOrEmpty(localFolderPath) && generateZipName(true)) {
            folderPathText.setText(localFolderPath);
        }

        folderPathText.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(final ModifyEvent e) {
                final String path = folderPathText.getText().trim();
                localFolderPath = path;
                generateZipName(false);
                processInput();
            }
        });

        final Button browseFolderButton = new Button(composite, SWT.PUSH);
        browseFolderButton.setText(Messages.getString("UploadArchiveDialog.BrowserFolderButtonLabel")); //$NON-NLS-1$
        GridDataBuilder.newInstance().vAlign(SWT.CENTER).applyTo(browseFolderButton);
        browseFolderButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                final DirectoryDialog dlg = new DirectoryDialog(getShell(), SWT.OPEN);

                // Set the initial filter path according to what is typed in
                // or set default path based on environment variables
                if (StringUtil.isNullOrEmpty(folderPathText.getText())) {
                    dlg.setFilterPath(localFolderPath);
                } else {
                    dlg.setFilterPath(folderPathText.getText().trim());
                }

                dlg.setText(
                    MessageFormat.format(Messages.getString("UploadArchiveDialog.UploadTitleFormat"), buildToolName)); //$NON-NLS-1$
                dlg.setMessage(
                    MessageFormat.format(Messages.getString("UploadArchiveDialog.UploadTextFormat"), buildToolName)); //$NON-NLS-1$

                final String path = dlg.open();
                if (!StringUtil.isNullOrEmpty(path)) {
                    localFolderPath = path;
                    if (generateZipName(false)) {
                        folderPathText.setText(localPath);
                    }
                    processInput();
                }
            }
        });
        return composite;
    }

    private boolean generateZipName(final boolean fromEnvVariable) {
        Check.notNull(localFolderPath, "localFolderPath"); //$NON-NLS-1$

        try {
            final String name = new File(localFolderPath).getCanonicalFile().getName();
            if (StringUtil.isNullOrEmpty(name)) {
                if (!fromEnvVariable) {
                    MessageBoxHelpers.warningMessageBox(
                        getShell(),
                        Messages.getString("UploadArchiveDialog.WarningMessage"), //$NON-NLS-1$
                        Messages.getString("UploadArchiveDialog.FolderNotAccessedText")); //$NON-NLS-1$
                }
                return false;
            }
            if (Platform.isCurrentPlatform(Platform.WINDOWS) && buildToolName.equalsIgnoreCase(JAVA)) {
                folderZipName = name + "-win.zip"; //$NON-NLS-1$
            } else {
                folderZipName = name + ".zip"; //$NON-NLS-1$
            }
            localPath = localFolderPath;
            archiveName = folderZipName;
            serverPathText.setText(serverPath + archiveName);
            return true;
        } catch (final Exception exception) {
            MessageBoxHelpers.warningMessageBox(
                getShell(),
                Messages.getString("UploadArchiveDialog.WarningMessage"), //$NON-NLS-1$
                Messages.getString("UploadArchiveDialog.FolderNotAccessedText")); //$NON-NLS-1$
            return false;
        }
    }

    private Composite selectZipComposite(final Composite parent) {
        final Composite composite = SWTUtil.createComposite(parent);
        SWTUtil.gridLayout(composite, 2, false);
        GridDataBuilder.newInstance().hFill().hGrab().applyTo(composite);
        zipPathText = new Text(composite, SWT.BORDER);
        GridDataBuilder.newInstance().hFill().hGrab().vAlign(SWT.CENTER).applyTo(zipPathText);
        zipPathText.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(final ModifyEvent e) {
                final String path = zipPathText.getText();
                if (path.endsWith(".zip") && path.length() > 4) //$NON-NLS-1$
                {
                    localZipPath = path;
                    try {
                        archiveName = new File(localZipPath).getCanonicalFile().getName();
                        if (!StringUtil.isNullOrEmpty(archiveName)) {
                            localPath = localZipPath;
                            fileZipName = archiveName;
                            serverPathText.setText(serverPath + archiveName);
                        }
                    } catch (final Exception exception) {
                    }
                }
            }
        });

        final Button browseZipButton = new Button(composite, SWT.PUSH);
        browseZipButton.setText(Messages.getString("UploadArchiveDialog.BrowserFolderButtonLabel")); //$NON-NLS-1$
        GridDataBuilder.newInstance().vAlign(SWT.CENTER).applyTo(browseZipButton);
        browseZipButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                final FileDialog dlg = new FileDialog(getShell(), SWT.OPEN);
                // Set the initial filter path according
                // to anything they've selected or typed in
                dlg.setFilterPath(zipPathText.getText());
                dlg.setText(
                    MessageFormat.format(Messages.getString("UploadArchiveDialog.UploadTitleFormat"), buildToolName)); //$NON-NLS-1$
                dlg.setFilterExtensions(new String[] {
                    "*.zip" //$NON-NLS-1$
                });
                final String path = dlg.open();
                if (!StringUtil.isNullOrEmpty(path)) {
                    localZipPath = path;
                    try {
                        archiveName = new File(localZipPath).getCanonicalFile().getName();
                        if (archiveName != null) {
                            localPath = localZipPath;
                            fileZipName = archiveName;
                            zipPathText.setText(localZipPath);
                            serverPathText.setText(serverPath + archiveName);
                        }
                    } catch (final Exception exception) {
                        MessageBoxHelpers.warningMessageBox(
                            getShell(),
                            Messages.getString("UploadArchiveDialog.WarningMessage"), //$NON-NLS-1$
                            Messages.getString("UploadArchiveDialog.FileNotAccessedText")); //$NON-NLS-1$
                    }
                }
                processInput();
            }
        });
        return composite;
    }

    private Composite createInfoComposite(final Composite parent) {
        final Composite composite = SWTUtil.createComposite(parent);
        SWTUtil.gridLayout(composite, 2, false);
        GridDataBuilder.newInstance().hFill().hGrab().applyTo(composite);

        iconLabel = SWTUtil.createLabel(composite, infoImage);
        GridDataBuilder.newInstance().vAlign(SWT.TOP).applyTo(iconLabel);

        helpLabel = new Label(composite, SWT.WRAP);
        helpLabel.setText(folderHelper);
        GridDataBuilder.newInstance().hFill().hGrab().wHint(IDialogConstants.MINIMUM_MESSAGE_AREA_WIDTH).applyTo(
            helpLabel);
        return composite;
    }

    private void attachSelectionListener(final Button button) {
        button.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                validate();
            }
        });
    }

    private void setHelperLabel() {
        iconLabel.setImage(infoImage);
        if (isFolderSelected) {
            helpLabel.setText(folderHelper);
        } else {
            helpLabel.setText(archiveHelper);
        }
    }

    protected void validate() {
        if (selectFolderRadio.getSelection()) {
            isFolderSelected = true;
            localPath = localFolderPath;
            archiveName = folderZipName;
            helpLabel.setText(folderHelper);
        } else if (selectZipRadio.getSelection()) {
            isFolderSelected = false;
            localPath = localZipPath;
            archiveName = fileZipName;
            helpLabel.setText(archiveHelper);
        }

        serverPathText.setText(serverPath + archiveName);
        setSectionEnabled(folderSection, selectFolderRadio.getSelection());
        setSectionEnabled(zipSection, selectZipRadio.getSelection());
        processInput();
    }

    protected void setSectionEnabled(final Composite section, final boolean isEnabled) {
        final Control[] children = section.getChildren();

        for (final Control control : children) {
            control.setEnabled(isEnabled);
        }
    }

    private void processInput() {
        final Button okButton = getButton(IDialogConstants.OK_ID);

        /*
         * sanity check
         */
        if (okButton == null) {
            return;
        }
        if (StringUtil.isNullOrEmpty(archiveName) || archiveName.trim().length() == 0) {
            okButton.setEnabled(false);
        } else if (selectFolderRadio.getSelection()) {
            final String folderText = folderPathText.getText() != null ? folderPathText.getText().trim() : ""; //$NON-NLS-1$
            okButton.setEnabled((folderText.length() > 0));
        } else if (selectZipRadio.getSelection()) {
            final String zipText = zipPathText.getText() != null ? zipPathText.getText().trim() : ""; //$NON-NLS-1$
            okButton.setEnabled(zipText.length() > 0);
        }
    }

    public String getEnvVariablePath(final String envVar) {
        String path = System.getenv(envVar + "_HOME"); //$NON-NLS-1$

        if (!StringUtil.isNullOrEmpty(path)) {
            return path;
        }

        path = System.getenv(envVar + "HOME"); //$NON-NLS-1$

        if (!StringUtil.isNullOrEmpty(path)) {
            return path;
        }

        path = System.getenv(envVar);
        return path;
    }

    public String getEclipseAntPath() {
        String path = null;
        final Bundle bundle = org.eclipse.core.runtime.Platform.getBundle("org.apache.ant"); //$NON-NLS-1$
        if (bundle == null) {
            return null;
        }

        final String location = bundle.getLocation();

        /*
         * Under Windows, the location string looks like:
         * reference:file:/C:/home/user/mydir, Under linux, it's
         * reference:file:/home/user/mydir, Leave the first forward slash in so
         * that it parses properly as a file under both Windows and Linux
         */
        if (location.startsWith("reference:file:")) //$NON-NLS-1$
        {
            path = new File(location.substring(15)).getAbsolutePath();
        }

        return path;
    }

    public IBuildDefinition getBuildDefinition() {
        return buildDefinition;
    }

    public String getLocalPath() {
        return localPath;
    }

    public String getServerPath() {
        return ServerPath.canonicalize(serverPath);
    }

    public String getArchiveName() {
        return archiveName;
    }
}
