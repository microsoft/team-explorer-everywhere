// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse.ui.actions.sync;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.widgets.Shell;

import com.microsoft.tfs.client.common.repository.RepositoryMap;
import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.common.ui.framework.image.ImageHelper;
import com.microsoft.tfs.client.eclipse.ui.TFSEclipseClientUIPlugin;
import com.microsoft.tfs.util.Check;

public abstract class SynchronizeAction extends Action {
    private final ImageHelper imageHelper = new ImageHelper(TFSEclipseClientUIPlugin.PLUGIN_ID);
    private final Shell shell;
    private IStructuredSelection selection;

    public SynchronizeAction(final Shell shell) {
        Check.notNull(shell, "shell"); //$NON-NLS-1$
        this.shell = shell;
    }

    public Shell getShell() {
        return shell;
    }

    protected ImageHelper getImageHelper() {
        return imageHelper;
    }

    /**
     * Extending classes should override this to perform enablement, but they
     * should always call this super method to ensure the resource are saved
     * before {@link #run()} is called.
     */
    public void addToContextMenu(final IMenuManager manager, final IResource[] selected) {
        setSelectedResources(selected);
    }

    public void setSelectedResources(final IResource[] selected) {
        selection = new StructuredSelection(selected);
    }

    protected IStructuredSelection getStructuredSelection() {
        return selection;
    }

    protected int getRepositoryCount(final RepositoryMap repositoryMap) {
        if (repositoryMap == null) {
            return 0;
        }

        final TFSRepository[] repositories = repositoryMap.getRepositories();

        if (repositories == null) {
            return 0;
        }

        return repositories.length;
    }
}
