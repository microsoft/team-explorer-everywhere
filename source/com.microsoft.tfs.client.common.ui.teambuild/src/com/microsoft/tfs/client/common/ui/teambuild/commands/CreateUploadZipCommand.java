// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teambuild.commands;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.Enumeration;
import java.util.concurrent.atomic.AtomicReference;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import com.microsoft.tfs.client.common.commands.TFSCommand;
import com.microsoft.tfs.client.common.ui.TFSCommonUIClientPlugin;
import com.microsoft.tfs.client.common.ui.teambuild.Messages;
import com.microsoft.tfs.client.common.ui.teambuild.VersionControlHelper;
import com.microsoft.tfs.core.clients.build.IBuildDefinition;
import com.microsoft.tfs.core.clients.versioncontrol.localworkspace.IgnoreFile;
import com.microsoft.tfs.core.clients.versioncontrol.path.LocalPath;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.LocaleUtil;
import com.microsoft.tfs.util.StringUtil;
import com.microsoft.tfs.util.temp.TempStorageService;

/**
 * Create an JDK/Ant archive based on existing folder or archive and upload it
 * to source control
 */
public class CreateUploadZipCommand extends TFSCommand {
    private static final Log log = LogFactory.getLog(CreateUploadZipCommand.class);
    private final String localPath;
    private final IBuildDefinition buildDefinition;
    private final String serverPath;
    private final String zipFileName;
    private final String buildToolName;
    private IgnoreFile ignoreFile;

    private String errorMsg;

    private static final String[] JDK_IGNORE_PATTERN = {
        "src.zip", //$NON-NLS-1$
        "\\sample", //$NON-NLS-1$
        "\\demo", //$NON-NLS-1$
        "\\db", //$NON-NLS-1$
        "!\\bin\\*", //$NON-NLS-1$
        "\\lib\\visualvm", //$NON-NLS-1$
        "*.html" //$NON-NLS-1$
    };

    private static final String[] ANT_IGNORE_PATTERN = {
        "\\docs", //$NON-NLS-1$
        "\\manual", //$NON-NLS-1$
        "!\\bin\\*" //$NON-NLS-1$
    };

    public CreateUploadZipCommand(
        final String localPath,
        final String zipFileName,
        final String serverPath,
        final IBuildDefinition buildDefinition,
        final String buildToolName) {
        this.localPath = localPath;
        this.zipFileName = zipFileName;
        this.serverPath = serverPath;
        this.buildDefinition = buildDefinition;
        this.buildToolName = buildToolName;
        this.setCancellable(true);
    }

    @Override
    public String getName() {
        final String messageFormat = Messages.getString("CreateUploadZipCommand.CommandMessageFormat"); //$NON-NLS-1$
        return MessageFormat.format(messageFormat, localPath);
    }

    private IBuildDefinition getBuildDefinition() {
        return buildDefinition;
    }

    @Override
    public String getErrorDescription() {
        return Messages.getString("CreateUploadZipCommand.CommandErrorMessage");//$NON-NLS-1$
    }

    @Override
    public String getLoggingDescription() {
        final String messageFormat = Messages.getString("CreateUploadZipCommand.CommandMessageFormat", LocaleUtil.ROOT); //$NON-NLS-1$
        return MessageFormat.format(messageFormat, localPath);
    }

    @Override
    protected IStatus doRun(final IProgressMonitor progressMonitor) throws Exception {
        Check.notNullOrEmpty(localPath, "localPath"); //$NON-NLS-1$
        Check.notNull(new File(localPath), "File exists"); //$NON-NLS-1$

        final String messageFormat = Messages.getString("CreateUploadZipCommand.ProgressMonitorTextFormat"); //$NON-NLS-1$
        final String message = MessageFormat.format(messageFormat, localPath);

        progressMonitor.beginTask(message, 5);

        final String tempFolder = TempStorageService.getInstance().createTempDirectory().getCanonicalPath();
        final String destFile = LocalPath.combine(tempFolder, zipFileName);

        progressMonitor.worked(1);

        progressMonitor.setTaskName(Messages.getString("CreateUploadZipCommand.PrepareForUploadText")); //$NON-NLS-1$

        try {
            if (localPath.endsWith(".zip")) //$NON-NLS-1$
            {
                copyZip(localPath, destFile);
            } else {
                zip(localPath, destFile);
            }
            progressMonitor.worked(1);

            if (progressMonitor.isCanceled()) {
                return Status.CANCEL_STATUS;
            }

            progressMonitor.setTaskName(Messages.getString("CreateUploadZipCommand.UploadArchiveText")); //$NON-NLS-1$

            VersionControlHelper.checkinTemporaryBuildConfigFolder(
                getBuildDefinition().getBuildServer().getConnection(),
                tempFolder,
                serverPath,
                true);

            progressMonitor.worked(2);

            progressMonitor.setTaskName(Messages.getString("CreateUploadZipCommand.CleanUpText")); //$NON-NLS-1$
            progressMonitor.worked(1);
            return Status.OK_STATUS;
        } catch (final Exception e) {
            log.error(e);
            if (StringUtil.isNullOrEmpty(errorMsg)) {
                errorMsg = e.getMessage();
            }
            return new Status(Status.ERROR, TFSCommonUIClientPlugin.PLUGIN_ID, errorMsg);
        } finally {
            progressMonitor.done();
            if (new File(destFile).exists()) {
                cleanTempFile(destFile);
            }
        }
    }

    public String getServerPath() {
        return serverPath;
    }

    /**
     * Zip a file or folder to a zip file
     *
     *
     * @param srcFolder
     * @param destZipFile
     * @throws Exception
     */
    public void zip(final String srcFolder, final String destZipFile) throws Exception {
        checkFolder(localPath);
        final FileOutputStream fileWriter = new FileOutputStream(destZipFile);
        final ZipOutputStream zipout = new ZipOutputStream(fileWriter);

        try {
            loadIgnoreFile(srcFolder);

            /* preserve the folder when archive */
            final String parentPath =
                LocalPath.removeTrailingSeparators(new File(srcFolder).getParentFile().getAbsolutePath());
            addFileToZip(parentPath, new File(srcFolder).getAbsoluteFile(), zipout);
            zipout.flush();
        } catch (final Exception e) {
            errorMsg =
                MessageFormat.format(
                    Messages.getString("CreateUploadZipCommand.CreateArchiveErrorMessageFormat"), //$NON-NLS-1$
                    srcFolder);
            log.error("Exceptions when creating zip archive ", e); //$NON-NLS-1$
            throw e;
        } finally {
            zipout.close();
        }
    }

    /**
     * Load ignore file using .tfignore file from users; If user does not
     * specify .tfignore, using ours!
     *
     * @param srcFolder
     */
    private void loadIgnoreFile(final String srcFolder) {
        ignoreFile = IgnoreFile.load(srcFolder);
        if (ignoreFile == null) {
            loadDefaultExcludePattern(srcFolder);
        }
    }

    /**
     * Load default excclude patterns for Java/Ant
     *
     * @param srcFolder
     */
    private void loadDefaultExcludePattern(final String srcFolder) {
        if (buildToolName.equalsIgnoreCase("java")) //$NON-NLS-1$
        {
            ignoreFile = IgnoreFile.load(srcFolder, JDK_IGNORE_PATTERN);
        } else if (buildToolName.equalsIgnoreCase("ant")) //$NON-NLS-1$
        {
            ignoreFile = IgnoreFile.load(srcFolder, ANT_IGNORE_PATTERN);
        }
    }

    /**
     * Copy zip file and remove ignored files
     *
     * @param srcFile
     * @param destFile
     * @throws IOException
     */
    public void copyZip(final String srcFile, final String destFile) throws Exception {
        loadDefaultExcludePattern(getRootFolderInZip(srcFile));

        final ZipFile zipSrc = new ZipFile(srcFile);
        final ZipOutputStream out = new ZipOutputStream(new FileOutputStream(destFile));

        try {
            final Enumeration<? extends ZipEntry> entries = zipSrc.entries();
            while (entries.hasMoreElements()) {
                final ZipEntry entry = entries.nextElement();
                if (!isExcluded(LocalPath.combine(srcFile, entry.getName()), false, srcFile)) {
                    final ZipEntry newEntry = new ZipEntry(entry.getName());
                    out.putNextEntry(newEntry);

                    final BufferedInputStream in = new BufferedInputStream(zipSrc.getInputStream(entry));
                    int len;
                    final byte[] buf = new byte[65536];
                    while ((len = in.read(buf)) > 0) {
                        out.write(buf, 0, len);
                    }
                    out.closeEntry();
                    in.close();
                }
            }
            out.finish();
        } catch (final IOException e) {
            errorMsg = Messages.getString("CreateUploadZipCommand.CopyArchiveErrorMessageFormat"); //$NON-NLS-1$
            log.error("Exceptions when copying exising archive ", e); //$NON-NLS-1$
            throw e;
        } finally {
            out.close();
            zipSrc.close();
        }
    }

    /**
     * Get the root directory in zip archive to build a fake directory path and
     * use ignore file to exclude zip entries. e.g. Given a zip C:\java.zip if
     * top level folder in Java.zip is Java5, it will return C:\Java.zip\Java5\
     *
     * This method is only used to build the right ignore pattern to exclude zip
     * entries
     *
     * @param zipPath
     * @return
     * @throws IOException
     */
    private String getRootFolderInZip(final String zipPath) throws Exception {
        final ZipFile zipSrc = new ZipFile(zipPath);
        final Enumeration<? extends ZipEntry> entries = zipSrc.entries();

        int minBinDepth = Integer.MAX_VALUE;
        String binEntryName = null;
        final int zipDepth = LocalPath.getFolderDepth(zipPath);

        while (entries.hasMoreElements()) {
            final ZipEntry entry = entries.nextElement();
            final String parentEntryName = LocalPath.getParent(LocalPath.combine(zipPath, entry.getName()));
            final String name = LocalPath.getFileName(parentEntryName);

            if (name.equalsIgnoreCase("bin")) //$NON-NLS-1$
            {
                final int depth = LocalPath.getFolderDepth(parentEntryName);
                if (minBinDepth > depth) {
                    minBinDepth = depth;
                    binEntryName = parentEntryName;
                }
                if (minBinDepth == zipDepth + 1) {
                    break;
                }
            }
        }

        if (binEntryName != null) {
            return LocalPath.getParent(binEntryName);
        } else {
            errorMsg =
                MessageFormat.format(
                    Messages.getString("CreateUploadZipCommand.InvalidArchiveErrorMessageFormat"), //$NON-NLS-1$
                    buildToolName);
            log.error("Invalid archive " + zipPath); //$NON-NLS-1$
            throw new Exception("The archive does not contain valid " + buildToolName); //$NON-NLS-1$
        }
    }

    private void addFileToZip(final String basePath, final File file, final ZipOutputStream zipout) throws IOException {
        if (file == null || !file.exists()) {
            return;
        }

        final String filePath = file.getPath();
        if (file.isDirectory()) {
            if (!isExcluded(filePath, true, basePath)) {
                final File[] files = file.listFiles();
                for (final File f : files) {
                    addFileToZip(basePath, f, zipout);
                }
            }
        } else {
            if (!isExcluded(filePath, false, basePath)) {
                final byte[] buf = new byte[65536];
                int len;
                FileInputStream in = null;
                try {
                    in = new FileInputStream(file);
                    zipout.putNextEntry(new ZipEntry(filePath.substring(basePath.length() + 1)));
                    while ((len = in.read(buf)) > 0) {
                        zipout.write(buf, 0, len);
                    }
                } finally {
                    if (in != null) {
                        in.close();
                    }
                }
            }
        }
    }

    private boolean isExcluded(final String localItem, final boolean isFolder, final String startLocalItem) {
        if (ignoreFile != null) {
            if (ignoreFile.getFullPath().equalsIgnoreCase(localItem)) {
                return true;
            }
            final AtomicReference<String> innerAppliedExclusion = new AtomicReference<String>();
            final Boolean isExcluded =
                ignoreFile.isExcluded(localItem, isFolder, startLocalItem, innerAppliedExclusion);
            if (isExcluded != null) {
                return isExcluded.booleanValue();
            }
        }
        return false;
    }

    private void checkFolder(final String folderPath) throws Exception {
        final File file = new File(folderPath);
        final FilenameFilter binFolderFilter = new FilenameFilter() {
            @Override
            public boolean accept(final File dir, final String name) {
                return name.equalsIgnoreCase("bin"); //$NON-NLS-1$
            }
        };
        if (!file.exists()
            || file.list() == null
            || file.list().length == 0
            || file.list(binFolderFilter).length == 0) {
            errorMsg =
                MessageFormat.format(
                    Messages.getString("CreateUploadZipCommand.InvalidFolderErrorMessageFormat"), //$NON-NLS-1$
                    buildToolName);
            log.error("Invalid folder " + folderPath); //$NON-NLS-1$
            throw new Exception("The folder does not contain valid " + buildToolName); //$NON-NLS-1$
        }
    }

    private void cleanTempFile(final String file) {
        try {
            // delete the temp zip file
            new File(file).delete();

            // delete the temp folder
            new File(file).getParentFile().delete();
        } catch (final Exception e) {
            log.error(e);
        }
    }
}
