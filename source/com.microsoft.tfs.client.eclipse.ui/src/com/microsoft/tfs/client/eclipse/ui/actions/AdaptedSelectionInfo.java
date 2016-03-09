// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse.ui.actions;

import org.eclipse.core.resources.IResource;

import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PendingChange;
import com.microsoft.tfs.util.Check;

/**
 * Contains the adapted selection information built by methods in
 * {@link EclipsePluginAction}. Fields in this class are never null; they may be
 * empty if they are not desired or described no resources/pending changes.
 *
 * @threadsafety thread-compatible
 */
public class AdaptedSelectionInfo {
    private final IResource[] resources;
    private final TFSRepository[] repositories;
    private final PendingChange[] pendingChanges;

    public AdaptedSelectionInfo(
        final IResource[] resources,
        final TFSRepository[] repositories,
        final PendingChange[] pendingChanges) {
        super();

        Check.notNull(resources, "resources"); //$NON-NLS-1$
        Check.notNull(repositories, "repositories"); //$NON-NLS-1$
        Check.notNull(pendingChanges, "pendingChanges"); //$NON-NLS-1$

        this.resources = resources;
        this.repositories = repositories;

        this.pendingChanges = pendingChanges;
    }

    /**
     * @return the {@link IResource}s that are selected (never <code>null</code>
     *         )
     */
    public IResource[] getResources() {
        return resources;
    }

    /**
     * @return the {@link TFSRepository}s that contain the {@link IResource} s
     *         returned by {@link #getResources()} (never <code>null</code>)
     */
    public TFSRepository[] getRepositories() {
        return repositories;
    }

    /**
     * @return the {@link PendingChange}s that match items in the current
     *         selection (never <code>null</code>)
     */
    public PendingChange[] getPendingChanges() {
        return pendingChanges;
    }
}