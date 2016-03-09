// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.vcexplorer.versioncontrol;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IPersistableElement;

import com.microsoft.tfs.client.common.ui.vcexplorer.Messages;

/**
 * Editor input to create a new VersionControlEditor. The equals method is
 * overridden to only allow one instance of the source control editor to be
 * displayed.
 */
public class VersionControlEditorInput implements IEditorInput {
    private final IMemento memento;

    public IMemento getMemento() {
        return memento;
    }

    public VersionControlEditorInput() {
        this(null);
    }

    public VersionControlEditorInput(final IMemento memento) {
        super();
        this.memento = memento;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(final Object obj) {
        if (obj == this || obj instanceof VersionControlEditorInput) {
            return true;
        }
        return false;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return VersionControlEditorInput.class.hashCode();
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.ui.IEditorInput#exists()
     */
    @Override
    public boolean exists() {
        return true;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.ui.IEditorInput#getImageDescriptor()
     */
    @Override
    public ImageDescriptor getImageDescriptor() {
        return ImageDescriptor.getMissingImageDescriptor();
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.ui.IEditorInput#getName()
     */
    @Override
    public String getName() {
        return Messages.getString("VersionControlEditorInput.Name"); //$NON-NLS-1$
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
     * @see org.eclipse.ui.IEditorInput#getToolTipText()
     */
    @Override
    public String getToolTipText() {
        return Messages.getString("VersionControlEditorInput.ToolTipText"); //$NON-NLS-1$
    }

    @Override
    public Object getAdapter(final Class adapter) {
        return null;
    }
}
