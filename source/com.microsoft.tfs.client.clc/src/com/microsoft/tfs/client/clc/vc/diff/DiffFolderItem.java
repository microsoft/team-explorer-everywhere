// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.clc.vc.diff;

import java.util.ArrayList;
import java.util.List;

import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ItemType;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.VersionSpec;
import com.microsoft.tfs.core.util.FileEncoding;
import com.microsoft.tfs.util.Check;

public class DiffFolderItem extends DiffItem {
    private final List directories = new ArrayList();
    private final List files = new ArrayList();

    public DiffFolderItem(
        final String serverPath,
        final String localPath,
        final String tempFile,
        final DiffItem rootItem,
        final boolean inRepository,
        final VersionSpec version) {
        super(
            serverPath,
            localPath,
            tempFile,
            FileEncoding.BINARY.getCodePage(),
            rootItem,
            ItemType.FOLDER,
            System.currentTimeMillis(),
            inRepository,
            version);
    }

    /**
     * Adds a directory diff item to this {@link DiffFolderItem}'s list of child
     * directories. If the directory is already a member, nothing happens.
     *
     * @param directoryItem
     *        the directory item to add (not null).
     */
    public void addDirectory(final DiffFolderItem directoryItem) {
        Check.notNull(directoryItem, "directoryItem"); //$NON-NLS-1$

        if (directories.contains(directoryItem) == false) {
            directories.add(directoryItem);
        }
    }

    /**
     * Adds a file diff item to this {@link DiffFolderItem}'s list of child
     * files. If the file is already a member, nothing happens.
     *
     * @param fileItem
     *        the file item to add (not null).
     */
    public void addFile(final DiffItem fileItem) {
        Check.notNull(fileItem, "fileItem"); //$NON-NLS-1$

        if (files.contains(fileItem) == false) {
            files.add(fileItem);
        }
    }

    /**
     * @return the list of directories in this folder item.
     */
    public List getDirectories() {
        return directories;
    }

    /**
     * @return the list of files in this folder item.
     */
    public List getFiles() {
        return files;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.microsoft.tfs.client.clc.vc.diff.DiffItem#isFolderItem()
     */
    @Override
    public boolean isFolderItem() {
        return true;
    }
}
