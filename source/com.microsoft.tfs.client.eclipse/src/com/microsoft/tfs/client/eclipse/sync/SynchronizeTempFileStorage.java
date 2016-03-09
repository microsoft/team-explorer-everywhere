// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse.sync;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.team.core.TeamException;

import com.microsoft.tfs.client.eclipse.TFSEclipseClientPlugin;

public class SynchronizeTempFileStorage implements IStorage {
    private final File tempFile;

    public SynchronizeTempFileStorage(final String tempLocation) {
        this(new File(tempLocation));
    }

    public SynchronizeTempFileStorage(final File tempLocation) {
        tempFile = tempLocation;
    }

    @Override
    public InputStream getContents() throws CoreException {
        try {
            return new FileInputStream(tempFile);
        } catch (final Exception e) {
            throw new CoreException(
                new Status(Status.ERROR, TFSEclipseClientPlugin.PLUGIN_ID, TeamException.IO_FAILED, e.getMessage(), e));
        }
    }

    @Override
    public IPath getFullPath() {
        return new Path(tempFile.getAbsolutePath());
    }

    @Override
    public String getName() {
        return tempFile.getName();
    }

    @Override
    public boolean isReadOnly() {
        return (!tempFile.canWrite());
    }

    @Override
    public Object getAdapter(final Class adapter) {
        return null;
    }
}
