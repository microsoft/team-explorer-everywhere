// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.editors;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IPersistableElement;

import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.core.clients.versioncontrol.conflicts.ConflictDescription;
import com.microsoft.tfs.util.Check;

public class ConflictResolutionEditorInput implements IEditorInput {
    private final TFSRepository repository;
    private final ConflictDescription[] conflictDescriptions;

    public ConflictResolutionEditorInput(
        final TFSRepository repository,
        final ConflictDescription[] conflictDescriptions) {
        Check.notNull(repository, "repository"); //$NON-NLS-1$

        this.repository = repository;
        this.conflictDescriptions = conflictDescriptions;
    }

    @Override
    public Object getAdapter(final Class adapter) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean exists() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public ImageDescriptor getImageDescriptor() {
        return null;
    }

    @Override
    public String getName() {
        return Messages.getString("ConflictResolutionEditorInput.Name"); //$NON-NLS-1$
    }

    @Override
    public IPersistableElement getPersistable() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getToolTipText() {
        return Messages.getString("ConflictResolutionEditorInput.Tooltip"); //$NON-NLS-1$
    }

    public TFSRepository getRepository() {
        return repository;
    }

    public ConflictDescription[] getConflictDescriptions() {
        return conflictDescriptions;
    }

    /*
     * Any conflict resolution editor input is equal to any other. (To ensure
     * that the runtime only displays a single instance.)
     */
    @Override
    public boolean equals(final Object obj) {
        if (obj == this || obj instanceof ConflictResolutionEditorInput) {
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
        return ConflictResolutionEditorInput.class.hashCode();
    }
}
