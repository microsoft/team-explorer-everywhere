// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse.sync;

import org.eclipse.core.resources.IResource;
import org.eclipse.team.core.variants.IResourceVariant;
import org.eclipse.team.core.variants.IResourceVariantComparator;

import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.eclipse.sync.syncinfo.LatestResourceVariant;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PendingChange;

public class SynchronizeComparator implements IResourceVariantComparator {
    /*
     * Compare local (possibly changed) version to remote. (non-Javadoc)
     *
     * @see
     * org.eclipse.team.core.variants.IResourceVariantComparator#compare(org
     * .eclipse.core.resources.IResource,
     * org.eclipse.team.core.variants.IResourceVariant)
     */
    @Override
    public boolean compare(final IResource local, final IResourceVariant remoteVariant) {
        if (!(remoteVariant instanceof LatestResourceVariant)) {
            return false;
        }

        final LatestResourceVariant latest = (LatestResourceVariant) remoteVariant;
        final TFSRepository repository = latest.getRepository();

        final PendingChange pendingChange =
            repository.getPendingChangeCache().getPendingChangeByLocalPath(local.getLocation().toOSString());

        // pending changes mean we're not the same as the remote
        if (pendingChange == null) {
            return false;
        }

        // otherwise a get operation means we're not the same
        if (latest.getOperation() != null) {
            return false;
        }

        return true;
    }

    /*
     * Compare base (ie, unchanged, server workspace version) to remote version.
     * Simply comparing changeset numbers is enough.
     *
     * (non-Javadoc)
     *
     * @see
     * org.eclipse.team.core.variants.IResourceVariantComparator#compare(org
     * .eclipse.team.core.variants.IResourceVariant,
     * org.eclipse.team.core.variants.IResourceVariant)
     */
    @Override
    public boolean compare(final IResourceVariant baseVariant, final IResourceVariant remoteVariant) {
        if (!(remoteVariant instanceof LatestResourceVariant)) {
            return false;
        }

        final LatestResourceVariant latest = (LatestResourceVariant) remoteVariant;

        // no get operation means the base is the latest
        if (latest.getOperation() == null) {
            return true;
        }

        return false;
    }

    /*
     * We always have support for three-way merging. (Giggity.)
     *
     * (non-Javadoc)
     *
     * @see
     * org.eclipse.team.core.variants.IResourceVariantComparator#isThreeWay()
     */
    @Override
    public boolean isThreeWay() {
        return true;
    }
}
