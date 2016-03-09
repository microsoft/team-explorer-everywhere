// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse.ui.offline;

import java.io.File;
import java.text.MessageFormat;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import com.microsoft.tfs.client.common.framework.resources.filter.ResourceFilter;
import com.microsoft.tfs.client.eclipse.resource.PluginResourceFilters;
import com.microsoft.tfs.core.clients.versioncontrol.offline.OfflineChangeType;
import com.microsoft.tfs.core.clients.versioncontrol.offline.OfflineSynchronizerFilter;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ItemType;

/**
 * An {@link OfflineSynchronizerFilter} for {@link IResource}s that uses the
 * standard plugin filters.
 *
 * @threadsafety unknown
 */
public class ResourceOfflineSynchronizerFilter extends OfflineSynchronizerFilter {
    private static final Log log = LogFactory.getLog(ResourceOfflineSynchronizerFilter.class);

    private final ResourceFilter filter = PluginResourceFilters.STANDARD_FILTER;

    public ResourceOfflineSynchronizerFilter() {
    }

    @Override
    public boolean shouldPend(final File file, final OfflineChangeType changeType, final ItemType serverItemType) {
        if (filter == null) {
            return true;
        }

        final IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
        final IPath path = new Path(file.getAbsolutePath());

        IResource resource = null;

        /*
         * If the change type is a delete, then the local item doesn't exist
         * (because it was deleted!), so test whether the corresponding server
         * item is a folder so we test the filter with the correct resource type
         * (it matters).
         */
        if ((changeType == OfflineChangeType.DELETE && serverItemType == ItemType.FOLDER) || file.isDirectory()) {
            resource = workspaceRoot.getContainerForLocation(path);
        } else {
            resource = workspaceRoot.getFileForLocation(path);
        }

        /*
         * We should really never get null back for this even if it doesn't
         * exist.
         */
        if (resource == null) {
            log.info(MessageFormat.format("Could not obtain resource for {0} (not analyzing offline state)", path)); //$NON-NLS-1$
            return false;
        }

        return filter.filter(resource).isAccept();
    }
}
