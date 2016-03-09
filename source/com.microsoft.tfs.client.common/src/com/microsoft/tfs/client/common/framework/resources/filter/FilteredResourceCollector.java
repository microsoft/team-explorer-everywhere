// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.framework.resources.filter;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

/**
 * <p>
 * A {@link FilteredResourceCollector} is a concrete subclass of
 * {@link FilteredResourceVisitor}. All resources that are visited by this
 * visitor and are accepted by this visitor's {@link ResourceFilter} are
 * collected. After visiting has finished, the collected resources can be
 * retrieved by calling {@link #getResources()}.
 * </p>
 *
 * <p>
 * Typical usage:
 *
 * <pre>
 * IResource                 resourceToVisit = ...
 * IResourceFilter           filter = ...
 * FilteredResourceCollector collector = new FilteredResourceCollector(filter);
 * resourceToVisit.accept(collector);
 * IResource[] collectedResources = collector.getResources();
 * </pre>
 *
 * </p>
 *
 * @see FilteredResourceVisitor
 * @see ResourceFilter
 */
public class FilteredResourceCollector extends FilteredResourceVisitor {
    /**
     * This {@link List} of {@link IResource}s that this
     * {@link FilteredResourceCollector} adds to during visit.
     */
    private final List<IResource> resources = new ArrayList<IResource>();

    /**
     * Constructs a new {@link FilteredResourceCollector} with the specified
     * {@link ResourceFilter} that does not poll for cancellation.
     *
     * @param filter
     *        the {@link ResourceFilter} to use for this visitor (must not be
     *        <code>null</code>)
     */
    public FilteredResourceCollector(final ResourceFilter filter) {
        super(filter, ResourceFilter.FILTER_FLAG_NONE, null);
    }

    /**
     * Constructs a new {@link FilteredResourceCollector} with the specified
     * {@link ResourceFilter} that does not poll for cancellation. The specified
     * filter flags are passed to the
     * {@link ResourceFilter#filter(IResource, int)} method.
     *
     * @param filter
     *        the {@link ResourceFilter} to use for this visitor (must not be
     *        <code>null</code>)
     * @param filterFlags
     *        the flags to use during filtering
     */
    public FilteredResourceCollector(final ResourceFilter filter, final int filterFlags) {
        super(filter, filterFlags, null);
    }

    /**
     * Constructs a new {@link FilteredResourceCollector} with the specified
     * {@link ResourceFilter} which uses the specified {@link IProgressMonitor}
     * to poll for cancellation. No methods other than
     * {@link IProgressMonitor#isCanceled()} are called on the specified
     * {@link IProgressMonitor}.
     *
     * @param filter
     *        the {@link ResourceFilter} to use for this visitor (must not be
     *        <code>null</code>)
     * @param progressMonitor
     *        an {@link IProgressMonitor} to poll for cancellation of this
     *        visitor, or <code>null</code> if no cancellation polling should be
     *        done
     */
    public FilteredResourceCollector(final ResourceFilter filter, final IProgressMonitor progressMonitor) {
        super(filter, ResourceFilter.FILTER_FLAG_NONE, progressMonitor);
    }

    /**
     * Constructs a new {@link FilteredResourceCollector} with the specified
     * {@link ResourceFilter} which uses the specified {@link IProgressMonitor}
     * to poll for cancellation. No methods other than
     * {@link IProgressMonitor#isCanceled()} are called on the specified
     * {@link IProgressMonitor}. The specified filter flags are passed to the
     * {@link ResourceFilter#filter(IResource, int)} method.
     *
     * @param filter
     *        the {@link ResourceFilter} to use for this visitor (must not be
     *        <code>null</code>)
     * @param filterFlags
     *        the flags to use during filtering
     * @param progressMonitor
     *        an {@link IProgressMonitor} to poll for cancellation of this
     *        visitor, or <code>null</code> if no cancellation polling should be
     *        done
     */
    public FilteredResourceCollector(
        final ResourceFilter filter,
        final int filterFlags,
        final IProgressMonitor progressMonitor) {
        super(filter, filterFlags, progressMonitor);
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.microsoft.tfs.client.common.resources.filter.FilteredResourceVisitor
     * #doVisit(org.eclipse.core.resources.IResource)
     */
    @Override
    protected boolean doVisit(final IResource resource) throws CoreException {
        resources.add(resource);

        /*
         * FILE resources do not have members to visit.
         */
        return IResource.FILE != resource.getType();
    }

    /**
     * @return the {@link IResource}s collected by this
     *         {@link FilteredResourceCollector}
     */
    public IResource[] getResources() {
        return resources.toArray(new IResource[resources.size()]);
    }
}
