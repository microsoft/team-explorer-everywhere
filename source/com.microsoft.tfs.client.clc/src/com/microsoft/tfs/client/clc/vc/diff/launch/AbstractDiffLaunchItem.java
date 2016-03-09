// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.clc.vc.diff.launch;

import java.io.File;
import java.io.IOException;

import com.microsoft.tfs.core.clients.versioncontrol.exceptions.VersionControlException;
import com.microsoft.tfs.core.clients.versioncontrol.path.LocalPath;
import com.microsoft.tfs.core.clients.versioncontrol.path.ServerPath;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ItemType;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.VersionSpec;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.FileHelpers;
import com.microsoft.tfs.util.temp.TempStorageService;

public abstract class AbstractDiffLaunchItem implements DiffLaunchItem {

    /**
     * Creates a full path for to use as a temp file from the given server path
     * with the version number mixed in (if given). Intermediate temp
     * directories are created, but the actual file is not.
     *
     * @param serverPath
     *        the server path of the file to make a temp file for (not null)
     * @param itemType
     *        the item's type (must not be <code>null</code>)
     * @param version
     *        the version to mix into the temp file name. May be null
     * @return the full path to the temp file derived from the server path's
     *         file name and the version. This file is not created on disk, but
     *         intermediate directories are.
     */
    protected String getVersionedTempFileFullPath(
        final String serverPath,
        final ItemType itemType,
        final VersionSpec version) {
        File tempDir;
        try {
            tempDir = TempStorageService.getInstance().createTempDirectory();
        } catch (final IOException ex) {
            throw new VersionControlException(ex);
        }

        final String name = getVersionedTempFileName(serverPath, itemType, version);

        return new File(tempDir, name).getAbsolutePath();
    }

    /**
     * Creates a file name to use as a temp file from the given server path with
     * the version number mixed in (if given). No files are created on disk.
     *
     * @param serverPath
     *        the server path of the file to make a temp file for (not null)
     * @param itemType
     *        the item's type (must not be <code>null</code>)
     * @param version
     *        the version to mix into the temp file name. May be null
     * @return the file name derived from the server path's file name and the
     *         version. This file is not created on disk anywhere.
     */
    private String getVersionedTempFileName(
        final String serverPath,
        final ItemType itemType,
        final VersionSpec version) {
        Check.notNull(serverPath, "serverPath"); //$NON-NLS-1$
        Check.notNull(itemType, "itemType"); //$NON-NLS-1$

        final String versionString =
            (version != null) ? FileHelpers.removeInvalidNTFSFileNameCharacters(version.toString()) : ""; //$NON-NLS-1$

        final String fileName = ServerPath.getFileName(serverPath);

        /*
         * Don't bother looking for extensions for folders.
         */
        if (itemType == ItemType.FOLDER) {
            return fileName + versionString;
        }

        // We can use LocalPath.getFileExtension() on the file part.
        final String extension = LocalPath.getFileExtension(fileName);

        if (extension.length() == 0) {
            return fileName;
        }

        final String fileWithNoExtension = fileName.substring(0, fileName.length() - extension.length());

        return fileWithNoExtension + versionString + extension;
    }

    /**
     * Creates an empty temp file on disk in a system temp directory with the
     * given file name.
     *
     * @param filePath
     *        the full path to the file to create (must not be <code>null</code>
     *        )
     * @throws IOException
     *         if an error occurred creating the file.
     */
    protected void createEmptyTempFile(final String filePath) throws IOException {
        Check.notNull(filePath, "fileName"); //$NON-NLS-1$

        new File(filePath).createNewFile();
    }
}
