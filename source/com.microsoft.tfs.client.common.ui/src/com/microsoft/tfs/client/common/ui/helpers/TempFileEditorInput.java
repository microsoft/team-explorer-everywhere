// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.helpers;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IPathEditorInput;
import org.eclipse.ui.IPersistableElement;
import org.eclipse.ui.IStorageEditorInput;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.model.IWorkbenchAdapter;

import com.microsoft.tfs.client.common.ui.TFSCommonUIClientPlugin;

/**
 * A file editor imput that can be pointed at any file in the file system. This
 * is designed for accessing the temporary files. Files are marked as not
 * persistable, to make them editable you would have to do file, save as.
 */
public class TempFileEditorInput implements IWorkbenchAdapter, IAdaptable, IStorageEditorInput, IPathEditorInput {

    private final File tempFile;
    private final Storage storage;

    public TempFileEditorInput(final String filePath) {
        tempFile = new File(filePath);
        storage = new Storage(tempFile);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.ui.IEditorInput#exists()
     */
    @Override
    public boolean exists() {
        return tempFile.exists();
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.core.runtime.IAdaptable#getAdapter(java.lang.Class)
     */
    @Override
    public Object getAdapter(final Class adapter) {
        if (adapter == IWorkbenchAdapter.class) {
            return this;
        }
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.ui.IEditorInput#getImageDescriptor()
     */
    @Override
    public ImageDescriptor getImageDescriptor() {
        return PlatformUI.getWorkbench().getEditorRegistry().getImageDescriptor(getName());
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.ui.IEditorInput#getName()
     */
    @Override
    public String getName() {
        return tempFile.getName();
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.ui.IEditorInput#getPersistable()
     */
    @Override
    public IPersistableElement getPersistable() {
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.ui.IStorageEditorInput#getStorage()
     */
    @Override
    public IStorage getStorage() throws CoreException {
        return storage;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.ui.IEditorInput#getToolTipText()
     */
    @Override
    public String getToolTipText() {
        return getName();
    }

    @Override
    public Object[] getChildren(final Object o) {
        return null;
    }

    @Override
    public ImageDescriptor getImageDescriptor(final Object object) {
        return null;
    }

    @Override
    public String getLabel(final Object o) {
        return null;
    }

    @Override
    public Object getParent(final Object o) {
        return null;
    }

    protected class Storage implements IStorage {
        private final File file;

        public Storage(final File file) {
            this.file = file;
        }

        @Override
        public InputStream getContents() throws CoreException {
            try {
                return new FileInputStream(file);
            } catch (final FileNotFoundException e) {
                throw new CoreException(
                    new Status(Status.ERROR, TFSCommonUIClientPlugin.PLUGIN_ID, Status.OK, e.getLocalizedMessage(), e));
            }
        }

        @Override
        public IPath getFullPath() {
            return new Path(file.getAbsolutePath());
        }

        @Override
        public String getName() {
            return file.getName();
        }

        @Override
        public boolean isReadOnly() {
            return true;
        }

        @Override
        public Object getAdapter(final Class adapter) {
            return Platform.getAdapterManager().getAdapter(this, adapter);
        }

    }

    @Override
    public IPath getPath() {
        return new Path(tempFile.getAbsolutePath());
    }
}
