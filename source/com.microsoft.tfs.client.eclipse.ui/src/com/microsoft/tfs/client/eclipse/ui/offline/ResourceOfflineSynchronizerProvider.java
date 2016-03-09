// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse.ui.offline;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;

import com.microsoft.tfs.core.clients.versioncontrol.offline.OfflineSynchronizerProvider;
import com.microsoft.tfs.util.Check;

/**
 * An {@link OfflineSynchronizerProvider} that uses {@link IResource}s.
 *
 * @threadsafety unknown
 */
public class ResourceOfflineSynchronizerProvider implements OfflineSynchronizerProvider {
    private final IResource[] resources;

    public ResourceOfflineSynchronizerProvider(final IResource[] resources) {
        Check.notNull(resources, "resources"); //$NON-NLS-1$
        this.resources = resources;
    }

    @Override
    public Object[] getResources() {
        return resources;
    }

    @Override
    public String getLocalPathForResource(final Object resource) {
        final IPath location = ((IResource) resource).getLocation();

        if (location == null) {
            return null;
        }

        return location.toOSString();
    }
}
