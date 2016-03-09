// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.framework.resources.filter;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import com.microsoft.tfs.util.Check;

/**
 * <p>
 * A {@link FilteredResourceVisitor} is an abstract {@link IResourceVisitor}
 * that uses an {@link ResourceFilter} to filter the {@link IResource}s being
 * visited.
 * </p>
 *
 * <p>
 * Subclasses must implement the {@link #doVisit(IResource)} method, which is
 * called to visit each {@link IResource} that is not filtered by the
 * {@link ResourceFilter}.
 * </p>
 *
 * <p>
 * Optionally, a {@link FilteredResourceVisitor} can take an
 * {@link IProgressMonitor} instance that is polled to check for cancelation
 * while visiting {@link IResource}s. If cancelation is detected,
 * <code>false</code> is returned from {@link IResourceVisitor#visit(IResource)}
 * .
 * </p>
 *
 * @see IResourceVisitor
 * @see ResourceFilter
 * @see IProgressMonitor
 */
public abstract class FilteredResourceVisitor implements IResourceVisitor {
    private final ResourceFilter filter;
    private final int filterFlags;

    private final IProgressMonitor progressMonitor;

    /**
     * Constructs a new {@link FilteredResourceVisitor} that uses the specified
     * {@link ResourceFilter}. While visiting, the specified
     * {@link IProgressMonitor} is polled for cancellation as described in the
     * documentation for this class. No methods other than
     * {@link IProgressMonitor#isCanceled()} are called on the specified
     * {@link IProgressMonitor}. The specified filter flags are passed to the
     * {@link ResourceFilter#filter(IResource, int)} method.
     *
     * @param filter
     *        the {@link ResourceFilter} to use for this
     *        {@link FilteredResourceVisitor} (must not be <code>null</code>)
     * @param filterFlags
     *        the filter flags to pass
     * @param progressMonitor
     *        the {@link IProgressMonitor} to use for cancellation checking, or
     *        <code>null</code> to not check for cancellation
     */
    protected FilteredResourceVisitor(
        final ResourceFilter filter,
        final int filterFlags,
        final IProgressMonitor progressMonitor) {
        Check.notNull(filter, "filter"); //$NON-NLS-1$

        this.filter = filter;
        this.filterFlags = filterFlags;
        this.progressMonitor = progressMonitor;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.core.resources.IResourceVisitor#visit(org.eclipse.core.
     * resources .IResource)
     */
    @Override
    public final boolean visit(final IResource resource) throws CoreException {
        if (isCanceled()) {
            return false;
        }

        final ResourceFilterResult result = filter.filter(resource, filterFlags);

        if (!result.isAccept()) {
            /*
             * If the filter didn't accept the resource, we don't call the
             * subclass doVisit() method. If the filter rejected the children as
             * well, we can return false as an optimization and skip visiting
             * the children. Otherwise, we must visit the children and return
             * true.
             */
            return !result.isRejectChildren();
        }

        final boolean doVisitChildren = doVisit(resource);

        /*
         * We only visit children if the filter did not reject children and the
         * subclass wants to visit children.
         */
        return !result.isRejectChildren() && doVisitChildren;
    }

    /**
     * Subclasses must implement this method, which is called with each visited
     * {@link IResource} that is not filtered by this
     * {@link FilteredResourceVisitor}'s {@link ResourceFilter}. The return
     * value of this method becomes the return value of the
     * {@link #visit(IResource)} method for the specified {@link IResource}.
     *
     * @param resource
     *        an {@link IResource} to visit
     * @return <code>true</code> to visit the resource's members, or
     *         <code>false</code> to skip them
     * @throws CoreException
     *         if the visit fails for some reason
     */
    protected abstract boolean doVisit(IResource resource) throws CoreException;

    /**
     * @return <code>true</code> if the {@link IProgressMonitor} associated with
     *         this {@link FilteredResourceVisitor} (if any) has been canceled
     */
    protected final boolean isCanceled() {
        return progressMonitor != null && progressMonitor.isCanceled();
    }
}
