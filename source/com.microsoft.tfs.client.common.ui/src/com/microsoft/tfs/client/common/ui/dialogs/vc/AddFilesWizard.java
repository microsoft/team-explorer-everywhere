// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.dialogs.vc;

import java.io.File;
import java.text.MessageFormat;
import java.util.ArrayList;

import org.eclipse.jface.wizard.IWizardPage;

import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.TFSCommonUIClientPlugin;
import com.microsoft.tfs.client.common.ui.framework.image.ImageHelper;
import com.microsoft.tfs.client.common.ui.framework.wizard.ExtendedWizard;
import com.microsoft.tfs.core.clients.versioncontrol.path.LocalPath;
import com.microsoft.tfs.core.clients.versioncontrol.path.ServerPath;
import com.microsoft.tfs.jni.helpers.FileCopyHelper;

public class AddFilesWizard extends ExtendedWizard {

    public static final String SELECTED_FILES = "AddFilesWizard.SelectedFiles"; //$NON-NLS-1$
    public static final String SERVER_PATH = "AddFilesWizard.ServerPath"; //$NON-NLS-1$

    // mapped local path
    public static final String LOCAL_PATH = "AddFilesWizard.LocalPath"; //$NON-NLS-1$

    // the unmapped local path from which items are added
    public static final String UPLOAD_PATH = "AddFilesWizard.UploadPath"; //$NON-NLS-1$

    private AddFilesWizardPage addFilesPage;
    private SelectFilesWizardPage selectFilesPage;
    private final String startingLocalPath;
    private final String startingServerPath;

    private final TFSRepository repository;
    private final static ImageHelper imageHelper = new ImageHelper(TFSCommonUIClientPlugin.PLUGIN_ID);

    public AddFilesWizard(final String startingLocalPath, final String startingServerPath) {
        super(
            Messages.getString("AddFilesWizard.WizardTitle"), //$NON-NLS-1$
            imageHelper.getImageDescriptor("images/wizard/pageheader.png")); //$NON-NLS-1$

        this.startingLocalPath = startingLocalPath;
        this.startingServerPath = startingServerPath;
        this.repository = getRepository();
    }

    @Override
    public void addPages() {
        addFilesPage = new AddFilesWizardPage(
            Messages.getString("AddFilesWizard.AddFilesWizardPageTitle"), //$NON-NLS-1$
            startingLocalPath,
            startingServerPath,
            repository);
        selectFilesPage = new SelectFilesWizardPage(Messages.getString("AddFilesWizard.SelectFilesWizardPageTitle")); //$NON-NLS-1$
        addPage(addFilesPage);
        addPage(selectFilesPage);
    }

    @Override
    public IWizardPage getNextPage(final IWizardPage page) {
        if (!hasPageData(SELECTED_FILES)) {
            return getPage(AddFilesWizardPage.PAGE_NAME);
        } else {
            return getPage(SelectFilesWizardPage.PAGE_NAME);
        }
    }

    public String[] getLocalPaths() {
        String[] paths = new String[0];

        if (hasPageData(SELECTED_FILES)) {
            final File[] files = (File[]) getPageData(SELECTED_FILES);
            paths = getPathsFromFiles(files);
        }

        return paths;
    }

    @Override
    public void dispose() {
        imageHelper.dispose();
        super.dispose();
    }

    @Override
    public boolean enableNext(final IWizardPage page) {
        return page instanceof AddFilesWizardPage && page.isPageComplete();
    }

    @Override
    protected boolean enableFinish(final IWizardPage currentPage) {
        return currentPage.isPageComplete();
    }

    @Override
    protected boolean doPerformFinish() {
        if (hasPageData(LOCAL_PATH)
            && hasPageData(SERVER_PATH)
            && hasPageData(SELECTED_FILES)
            && hasPageData(UPLOAD_PATH)) {
            final String mappedLocalPath = (String) getPageData(LOCAL_PATH);
            final String mappedServerPath = (String) getPageData(SERVER_PATH);
            final File[] files = (File[]) getPageData(SELECTED_FILES);
            final String uploadPath = (String) getPageData(UPLOAD_PATH);
            copyFilesIfNeeded(mappedLocalPath, mappedServerPath, files, uploadPath);
            return true;
        } else {
            // keep wizard open if data not complete
            return false;
        }
    }

    /**
     * Copy local files into mapped local folder if necessary.
     *
     * @param localPath
     * @param files
     */
    private void copyFilesIfNeeded(
        final String localPath,
        final String serverPath,
        final File[] files,
        final String uploadPath) {
        if (LocalPath.equals(localPath, uploadPath)) {
            return;
        }

        for (int i = 0; i < files.length; i++) {
            final File file = files[i];
            final String fromPath = file.getPath();
            try {
                // copy folders and files to working folder based on
                // existing hierarchy
                final String relativePath = fromPath.substring(uploadPath.length());
                final String serverDestPath = ServerPath.combine(serverPath, relativePath);
                final String toFilePath = repository.getWorkspace().getMappedLocalPath(serverDestPath);

                if (toFilePath == null) {
                    files[i] = null;
                    TFSCommonUIClientPlugin.getDefault().getConsole().printErrorMessage(
                        MessageFormat.format(Messages.getString("AddFilesWizard.NotMappedTextFormat"), fromPath)); //$NON-NLS-1$
                } else {
                    final File toFile = new File(toFilePath);

                    if (!toFile.getParentFile().exists()) {
                        toFile.getParentFile().mkdirs();
                    }

                    if (!toFile.exists()) {
                        FileCopyHelper.copy(fromPath, toFile.getPath());
                    } else if (!LocalPath.equals(fromPath, toFilePath)) {
                        TFSCommonUIClientPlugin.getDefault().getConsole().printErrorMessage(
                            MessageFormat.format(
                                Messages.getString("AddFilesWizard.ItemAlreadyInWorkingFolderFormat"), //$NON-NLS-1$
                                toFile.getName()));
                    }

                    files[i] = toFile;
                }
            } catch (final Exception e) {
                TFSCommonUIClientPlugin.getDefault().getConsole().printErrorMessage(
                    MessageFormat.format(Messages.getString("AddFilesWizard.CopyErrorTextFormat"), fromPath)); //$NON-NLS-1$
                TFSCommonUIClientPlugin.getDefault().getConsole().printErrorMessage(e.getMessage());
                files[i] = null;
            }
        }

        final ArrayList<File> fileList = new ArrayList<File>();
        for (final File f : files) {
            if (f != null) {
                fileList.add(f);
            }
        }
        this.setPageData(SELECTED_FILES, fileList.toArray(new File[fileList.size()]));
    }

    private TFSRepository getRepository() {
        return TFSCommonUIClientPlugin.getDefault().getProductPlugin().getRepositoryManager().getDefaultRepository();
    }

    /**
     * @return local paths of the files selected by the user to be added
     */
    public String[] getPathsFromFiles(final File[] files) {
        final String[] filepaths = new String[files.length];

        for (int i = 0; i < filepaths.length; i++) {
            filepaths[i] = LocalPath.canonicalize(files[i].getPath());
        }

        return filepaths;
    }

    @Override
    public boolean isHelpAvailable() {
        return false;
    }
}
