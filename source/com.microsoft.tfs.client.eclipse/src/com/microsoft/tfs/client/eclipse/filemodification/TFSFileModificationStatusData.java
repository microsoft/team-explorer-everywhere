// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse.filemodification;

import org.eclipse.core.resources.IFile;

import com.microsoft.tfs.util.Check;

public class TFSFileModificationStatusData {
    private final IFile file;

    private final long modificationStamp;
    private final long timeStamp;

    private final long startTime;

    public TFSFileModificationStatusData(final IFile file) {
        Check.notNull(file, "file"); //$NON-NLS-1$

        this.file = file;
        modificationStamp = file.getModificationStamp();
        timeStamp = file.getLocalTimeStamp();

        startTime = System.currentTimeMillis();
    }

    public IFile getFile() {
        return file;
    }

    public long getModificationStamp() {
        return modificationStamp;
    }

    public long getTimeStamp() {
        return timeStamp;
    }

    public long getStartTime() {
        return startTime;
    }
}
