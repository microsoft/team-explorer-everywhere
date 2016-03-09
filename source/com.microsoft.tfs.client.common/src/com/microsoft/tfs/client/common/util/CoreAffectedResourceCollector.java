// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.util;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.eclipse.core.resources.IResource;

import com.microsoft.tfs.client.common.framework.resources.ResourceType;
import com.microsoft.tfs.client.common.framework.resources.Resources;
import com.microsoft.tfs.client.common.vc.TypedItemSpec;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ItemType;

/**
 *
 * Subclasses may implement one of the following:
 *
 * resourcesChanged(Set): handles a Set of IResources affected by core events.
 * If you don't override this, it will simply call resourceChanged(IResource)
 * which you should then override.
 *
 * resourceChanged(IResource): handle a single IResource affected by core
 * resources. If you override resourcesChanged(Set), this is never called.
 *
 * @threadsafety unknown
 */
public class CoreAffectedResourceCollector extends CoreAffectedFileCollector {
    /**
     * Clients may override to perform resource handling in batch.
     *
     * If clients override, they should not override
     * {@link #resourceChanged(IResource)} as it will not be called.
     *
     * @param fileSet
     */
    protected void resourcesChanged(final Set<IResource> resourceSet) {
        for (final Iterator<IResource> i = resourceSet.iterator(); i.hasNext();) {
            resourceChanged(i.next());
        }
    }

    /**
     * Clients may override to perform per-resource handling.
     *
     * If clients override, they should not override
     * {@link #resourcesChanged(Set)} as this method will not be called.
     *
     * @param localItem
     */
    protected void resourceChanged(final IResource resource) {
    }

    @Override
    protected final void filesChanged(final Set<TypedItemSpec> itemSpecSet) {
        final Set<IResource> resourceSet = new HashSet<IResource>();

        for (final Iterator<TypedItemSpec> i = itemSpecSet.iterator(); i.hasNext();) {
            final TypedItemSpec itemSpec = i.next();
            IResource resource = null;

            final ResourceType resourceType =
                itemSpec.getType() == ItemType.FILE ? ResourceType.FILE : ResourceType.CONTAINER;

            try {
                resource = Resources.getResourceForLocation(itemSpec.getItem(), resourceType, false);
            } catch (final Exception ex) {
            }

            if (resource != null) {
                resourceSet.add(resource);
            }
        }

        resourcesChanged(resourceSet);
    }

    /*
     * Unused. Prevent subclasses from overriding.
     */
    protected final void fileChanged(final String file) {
    }
}
