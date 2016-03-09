// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.vcexplorer.versioncontrol;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IPersistableElement;

import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.common.ui.FindInSourceControlQuery;
import com.microsoft.tfs.client.common.ui.vcexplorer.Messages;
import com.microsoft.tfs.util.Check;

public class FindInSourceControlEditorInput implements IEditorInput {
    private final TFSRepository repository;
    private final FindInSourceControlQuery query;

    public FindInSourceControlEditorInput(final TFSRepository repository, final FindInSourceControlQuery query) {
        Check.notNull(repository, "repository"); //$NON-NLS-1$
        Check.notNull(query, "query"); //$NON-NLS-1$

        this.repository = repository;
        this.query = query;
    }

    public TFSRepository getRepository() {
        return repository;
    }

    public FindInSourceControlQuery getQuery() {
        return query;
    }

    @Override
    public Object getAdapter(final Class adapter) {
        return null;
    }

    @Override
    public boolean exists() {
        return true;
    }

    @Override
    public ImageDescriptor getImageDescriptor() {
        return null;
    }

    @Override
    public String getName() {
        return Messages.getString("FindInSourceControlEditorInput.EditorName"); //$NON-NLS-1$
    }

    @Override
    public String getToolTipText() {
        return Messages.getString("FindInSourceControlEditorInput.EditorName"); //$NON-NLS-1$
    }

    @Override
    public IPersistableElement getPersistable() {
        return null;
    }
}
