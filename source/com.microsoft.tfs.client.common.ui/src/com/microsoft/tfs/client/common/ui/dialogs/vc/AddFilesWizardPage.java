// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.dialogs.vc;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import com.microsoft.tfs.client.common.framework.command.ICommandExecutor;
import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.controls.generic.SizeConstrainedComposite;
import com.microsoft.tfs.client.common.ui.controls.vc.AddFilesControl;
import com.microsoft.tfs.client.common.ui.controls.vc.FilterItemsExistOnServerCommand;
import com.microsoft.tfs.client.common.ui.framework.layout.GridDataBuilder;
import com.microsoft.tfs.client.common.ui.framework.wizard.ExtendedWizardPage;
import com.microsoft.tfs.client.common.ui.tasks.vc.SetWorkingFolderTask;
import com.microsoft.tfs.client.common.ui.tasks.vc.UncloakWorkingFolderTask;
import com.microsoft.tfs.client.common.ui.vc.serveritem.ServerItemType;
import com.microsoft.tfs.client.common.ui.vc.serveritem.VersionedItemSource;
import com.microsoft.tfs.core.clients.versioncontrol.path.LocalPath;
import com.microsoft.tfs.core.clients.versioncontrol.path.ServerPath;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.WorkingFolder;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;
import com.microsoft.tfs.jni.FileSystemAttributes;
import com.microsoft.tfs.jni.FileSystemUtils;

public class AddFilesWizardPage extends ExtendedWizardPage {

    public static final String PAGE_NAME = "AddFilesWizardPage"; //$NON-NLS-1$

    private String originalLocalPath;
    private String originalServerPath;
    private String localPath;
    private String serverPath;
    private AddFilesControl filesControl;

    private Label serverPathLabel;
    private Text serverPathText;
    private Button browseButton;

    private Label localPathLabel;
    private Text localPathText;
    private Button mapButton;
    private Button advanceButton;
    private boolean advanced;
    private boolean mapped;

    private final TFSRepository repository;
    private ICommandExecutor commandExecutor;

    public AddFilesWizardPage(
        final String title,
        final String localPath,
        final String serverPath,
        final TFSRepository repository) {
        super(PAGE_NAME, title, Messages.getString("AddFilesWizardPage.AddFilesDescriptionText")); //$NON-NLS-1$
        this.localPath = localPath;
        this.serverPath = serverPath;
        this.advanced = false;
        this.repository = repository;
        originalLocalPath = localPath;
        originalServerPath = serverPath;
    }

    @Override
    protected void doCreateControl(final Composite parent, final IDialogSettings dialogSettings) {
        final SizeConstrainedComposite container = new SizeConstrainedComposite(parent, SWT.NONE);
        container.setDefaultSize(SWT.DEFAULT, 400);
        setControl(container);

        final GridLayout layout = new GridLayout(2, false);
        layout.horizontalSpacing = getHorizontalSpacing();
        layout.verticalSpacing = getVerticalSpacing();
        layout.marginWidth = getHorizontalMargin();
        layout.marginHeight = getVerticalMargin();
        container.setLayout(layout);

        commandExecutor = getCommandExecutor();
        filesControl = new AddFilesControl(container, SWT.NONE, commandExecutor);
        if (new File(localPath).exists()) {
            filesControl.setFilterPath(localPath);
        } else {
            filesControl.setFilterPath(LocalPath.getPathRoot(localPath));
        }
        filesControl.addPathChangeListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                localPathChanged();
            }
        });

        filesControl.addDoubleClickListener(new IDoubleClickListener() {
            @Override
            public void doubleClick(final DoubleClickEvent event) {
                localPathChanged();
            }
        });

        GridDataBuilder.newInstance().hSpan(2).hHint(200).grab().fill().applyTo(filesControl);
        filesControl.addTableSelectionListener(new ISelectionChangedListener() {
            @Override
            public void selectionChanged(final SelectionChangedEvent event) {
                toggleNextFinishState();
            }
        });

        serverPathLabel = new Label(container, SWT.WRAP);
        serverPathLabel.setText(Messages.getString("AddFilesWizardPage.ServerPathLabelText")); //$NON-NLS-1$
        GridDataBuilder.newInstance().hSpan(2).applyTo(serverPathLabel);

        serverPathText = new Text(container, SWT.BORDER);
        GridDataBuilder.newInstance().hGrab().hFill().vAlignCenter().applyTo(serverPathText);
        serverPathText.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(final FocusEvent e) {
            }

            @Override
            public void focusLost(final FocusEvent e) {
                validateServerPath();
            }
        });

        browseButton = new Button(container, SWT.PUSH);
        browseButton.setText(Messages.getString("AddFilesWizardPage.BrowseLabelText")); //$NON-NLS-1$
        GridDataBuilder.newInstance().vAlignCenter().hAlignCenter().applyTo(browseButton);
        browseButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                browseOnServer();
                toggleNextFinishState();
            }
        });

        localPathLabel = new Label(container, SWT.WRAP);
        localPathLabel.setText(Messages.getString("AddFilesWizardPage.LocalPathLabelText")); //$NON-NLS-1$
        GridDataBuilder.newInstance().hSpan(2).applyTo(localPathLabel);

        localPathText = new Text(container, SWT.BORDER);
        GridDataBuilder.newInstance().hGrab().hFill().vAlignCenter().applyTo(localPathText);
        localPathText.setEditable(false);

        mapButton = new Button(container, SWT.PUSH);
        mapButton.setText(Messages.getString("AddFilesWizardPage.MapButtonText")); //$NON-NLS-1$
        GridDataBuilder.newInstance().vAlignCenter().hAlignCenter().applyTo(mapButton);
        mapButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                mapPressed();
                toggleNextFinishState();
            }
        });

        advanceButton = new Button(container, SWT.PUSH);
        GridDataBuilder.newInstance().applyTo(advanceButton);
        advanceButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                setAdvanced(!advanced);
            }
        });

        setServerPath(serverPath);
        setAdvanced(advanced);
        toggleNextFinishState();
    }

    protected void localPathChanged() {
        final String newPath = filesControl.getFilterPath();
        // if user enters another folder which is also mapped, update the
        // server path accordingly
        final String path = repository.getWorkspace().getMappedServerPath(newPath);
        if (path != null) {
            setServerPath(path);
        } else {
            localPath = originalLocalPath;
            // set to original server path if out of mapping
            setServerPath(originalServerPath);
        }
    }

    private void validateServerPath() {
        final String path = serverPathText.getText().trim();
        if (!ServerPath.isServerPath(path)) {
            localPathText.setText(Messages.getString("AddFilesWizardPage.InvalidServerPathText")); //$NON-NLS-1$
            mapButton.setEnabled(false);
            advanceButton.setEnabled(false);
            mapped = false;
        } else {
            if (repository.getWorkspace().serverPathExists(path)) {
                setServerPath(path);
            } else {
                localPathText.setText(Messages.getString("AddFilesWizardPage.ServerPathNotExistText")); //$NON-NLS-1$
                mapButton.setEnabled(false);
                advanceButton.setEnabled(false);
                mapped = false;
            }
        }
        toggleNextFinishState();
    }

    private void toggleNextFinishState() {
        final boolean moreThanOneFileSelected = filesControl.fileSelected();
        setPageComplete(mapped && moreThanOneFileSelected);
    }

    private File[] getFilesSelected() {
        final String[] selectedFiles = filesControl.getSelectedFileNames();
        final String selectedDirectory = filesControl.getFilterPath();

        File[] filteredFiles;

        final Workspace workspace = repository.getWorkspace();
        final List<File> files = new ArrayList<File>();
        for (int i = 0; i < selectedFiles.length; i++) {
            final File file = new File(selectedDirectory, selectedFiles[i]);

            final FileSystemAttributes attr = FileSystemUtils.getInstance().getAttributes(file);
            if (attr.exists()) {
                if (file.isDirectory() && !attr.isSymbolicLink()) {
                    final FilterItemsExistOnServerCommand command =
                        new FilterItemsExistOnServerCommand(file.getAbsolutePath(), true);
                    commandExecutor.execute(command);
                    final List subFiles = Arrays.asList(command.getLocalItems());
                    files.addAll(subFiles);
                } else {
                    // check for one level mapping (file not mapped, file's
                    // parent mapped)
                    final String parentLocalPath = file.getParentFile().getAbsolutePath();
                    final boolean mapped = workspace.isLocalPathMapped(file.getAbsolutePath());
                    if (!mapped && parentLocalPath != null && workspace.isLocalPathMapped(parentLocalPath)) {
                        continue;
                    } else {
                        files.add(file);
                    }
                }
            }
        }
        filteredFiles = files.toArray(new File[files.size()]);

        return filteredFiles;
    }

    @Override
    public void refresh() {
        setPageComplete(filesControl.fileSelected());
    }

    @Override
    protected boolean onPageFinished() {
        if (!mapped) {
            return false;
        }

        final File[] files = getFilesSelected();

        if (files == null || files.length == 0) {
            MessageDialog.openInformation(
                getShell(),
                Messages.getString("AddFilesWizardPage.NoItemsToAddTitle"), //$NON-NLS-1$
                Messages.getString("AddFilesWizardPage.NoItemsToAddText")); //$NON-NLS-1$
            return false;
        }

        if (serverPath != null) {
            getExtendedWizard().setPageData(AddFilesWizard.SERVER_PATH, serverPath);
        }

        if (localPath != null) {
            getExtendedWizard().setPageData(AddFilesWizard.LOCAL_PATH, LocalPath.addTrailingSeparator(localPath));
        }

        if (files != null && files.length > 0) {
            getExtendedWizard().setPageData(AddFilesWizard.SELECTED_FILES, files);
        }

        final String uploadPath = filesControl.getFilterPath();
        if (uploadPath != null) {
            getExtendedWizard().setPageData(AddFilesWizard.UPLOAD_PATH, LocalPath.addTrailingSeparator(uploadPath));
        }

        return true;
    }

    private void mapPressed() {
        final WorkingFolder workingFolder = repository.getWorkspace().getExactMappingForServerPath(serverPath);

        // If the folder is cloaked, open uncloak dialog.
        if (workingFolder != null && workingFolder.isCloaked()) {
            final IStatus status = new UncloakWorkingFolderTask(getShell(), repository, serverPath).run();
            if (status.isOK()) {
                setServerPath(serverPath);
                filesControl.setFilterPath(localPath);
                originalServerPath = serverPath;
                originalLocalPath = localPath;
            }
        }
        // Otherwise, open map dialog.
        else {
            final IStatus status = new SetWorkingFolderTask(getShell(), repository, serverPath, true).run();
            if (status.isOK()) {
                setServerPath(serverPath);
                filesControl.setFilterPath(localPath);
                originalServerPath = serverPath;
                originalLocalPath = localPath;
            }
        }
    }

    private void browseOnServer() {
        String destinationPath = serverPathText.getText();

        if (!destinationPath.startsWith(ServerPath.ROOT)) {
            destinationPath = ServerPath.ROOT;
        }

        final ServerItemTreeDialog treeDialog = new ServerItemTreeDialog(
            getShell(),
            Messages.getString("AddFilesWizardPage.BrowseOnServerTitleText"), //$NON-NLS-1$
            destinationPath,
            new VersionedItemSource(repository.getVersionControlClient().getConnection()),
            ServerItemType.ALL_FOLDERS);

        if (treeDialog.open() != IDialogConstants.OK_ID) {
            return;
        }

        destinationPath = treeDialog.getSelectedServerPath();
        setServerPath(destinationPath);
        setAdvanced(advanced);
    }

    public void setServerPath(final String path) {
        if (path == null) {
            if (localPath != null) {
                serverPath = repository.getWorkspace().getMappedServerPath(localPath);
            } else {
                setMapped(false);
                return;
            }
        } else {
            serverPath = path;
        }

        if (serverPath == null) {
            setMapped(false);
            return;
        }

        serverPathText.setText(serverPath);

        mapped = repository.getWorkspace().isServerPathMapped(serverPath);
        setMapped(mapped);
        if (mapped) {
            localPath = repository.getWorkspace().getMappedLocalPath(serverPath);
            localPathText.setText(localPath);
        }
    }

    public void setAdvanced(final boolean advanced) {
        this.advanced = advanced;
        if (advanced) {
            advanceButton.setText(Messages.getString("AddFilesWizardPage.AdvancedCollapseText")); //$NON-NLS-1$
            serverPathText.setEditable(true);
            browseButton.setVisible(true);
            localPathLabel.setVisible(true);
            localPathText.setVisible(true);
            mapButton.setVisible(true);
            mapButton.setEnabled(!mapped);

            ((GridData) serverPathText.getLayoutData()).horizontalSpan = 1;
            ((GridData) browseButton.getLayoutData()).exclude = false;
            ((GridData) localPathLabel.getLayoutData()).exclude = false;
            ((GridData) localPathText.getLayoutData()).exclude = false;
            ((GridData) mapButton.getLayoutData()).exclude = false;
            advanceButton.setEnabled(mapped);
        } else {
            advanceButton.setText(Messages.getString("AddFilesWizardPage.AdvancedExpandText")); //$NON-NLS-1$
            serverPathText.setEditable(false);
            browseButton.setVisible(false);
            localPathLabel.setVisible(false);
            localPathText.setVisible(false);
            mapButton.setVisible(false);
            advanceButton.setEnabled(true);

            ((GridData) browseButton.getLayoutData()).exclude = true;
            ((GridData) serverPathText.getLayoutData()).horizontalSpan = 2;
            ((GridData) localPathLabel.getLayoutData()).exclude = true;
            ((GridData) localPathText.getLayoutData()).exclude = true;
            ((GridData) mapButton.getLayoutData()).exclude = true;
        }
        mapButton.getParent().layout();
        this.getControl().redraw();
    }

    public void setMapped(final boolean isMapped) {
        mapped = isMapped;
        if (mapped) {
            advanceButton.setEnabled(true);
            mapButton.setEnabled(false);
        } else {
            advanceButton.setEnabled(false);
            mapButton.setEnabled(true);
            advanced = true;
            localPathText.setText(Messages.getString("AddFilesWizardPage.NotMappedText")); //$NON-NLS-1$
            serverPathText.setEditable(true);
        }
    }
}
