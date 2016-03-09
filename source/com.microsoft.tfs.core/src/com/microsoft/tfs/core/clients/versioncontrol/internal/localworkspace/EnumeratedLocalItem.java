// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.internal.localworkspace;

import java.io.File;

import com.microsoft.tfs.jni.FileSystemAttributes;
import com.microsoft.tfs.jni.FileSystemUtils;

public class EnumeratedLocalItem {
    private final String name;
    private final String path;
    private final FileSystemAttributes attrs;

    private String serverItem;

    public EnumeratedLocalItem(final File file, final FileSystemAttributes attrs) {
        this.name = file.getName();
        this.path = file.getPath();
        this.attrs = attrs;
    }

    public EnumeratedLocalItem(final File file) {
        this(file, FileSystemUtils.getInstance().getAttributes(file));
    }

    public String getFileName() {
        return name;
    }

    public String getFullPath() {
        return path;
    }

    public long getFileSize() {
        if (attrs.isSymbolicLink()) {
            return 0;
        }
        return attrs.getSize();
    }

    public long getLastWriteTime() {
        // test for dangling symlinks
        if (!attrs.exists() || attrs.getModificationTime() == null) {
            return -1;
        }
        return attrs.getModificationTime().getWindowsFilesystemTime();
    }

    public boolean isDirectory() {
        if (attrs.isSymbolicLink()) {
            return false;
        }
        return attrs.isDirectory();
    }

    public boolean isSymbolicLink() {
        return attrs.isSymbolicLink();
    }

    public String getServerItem() {
        return serverItem;
    }

    public void setServerItem(final String value) {
        serverItem = value;
    }

    public boolean isExecutable() {
        if (isSymbolicLink()) {
            return false;
        }
        return attrs.isExecutable();
    }
}
