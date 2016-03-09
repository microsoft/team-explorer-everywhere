// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse.resource;

import java.text.MessageFormat;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.resources.IResource;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.synchronize.SyncInfo;

import com.microsoft.tfs.client.common.framework.resources.filter.ResourceFilter;
import com.microsoft.tfs.client.common.framework.resources.filter.ResourceFilterResult;
import com.microsoft.tfs.client.eclipse.sync.SynchronizeSubscriber;
import com.microsoft.tfs.client.eclipse.sync.syncinfo.SynchronizeInfo;

public class RemoteSyncInfoFilter extends ResourceFilter {
    private static final Log log = LogFactory.getLog(RemoteSyncInfoFilter.class);

    public RemoteSyncInfoFilter() {
    }

    @Override
    public ResourceFilterResult filter(final IResource resource, final int flags) {
        SyncInfo syncInfo;

        try {
            syncInfo = SynchronizeSubscriber.getInstance().getSyncInfo(resource);
        } catch (final TeamException e) {
            log.warn(MessageFormat.format("Could not determine synchronization info for {0}", resource), e); //$NON-NLS-1$
            return ResourceFilterResult.REJECT;
        }

        /*
         * Resource does not exist in the synchronization tree.
         */
        if (syncInfo != null && syncInfo instanceof SynchronizeInfo) {
            /* If there's a remote operation, allow this to proceed. */
            if (((SynchronizeInfo) syncInfo).getRemoteOperation() != null) {
                return ResourceFilterResult.ACCEPT;
            }
        }

        return ResourceFilterResult.REJECT;
    }
}
