// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.framework.resources.filter;

import org.eclipse.core.resources.IResource;

import com.microsoft.tfs.client.common.framework.resources.Resources;

/**
 * <p>
 * An {@link ResourceFilter} is used to filter {@link IResource} objects.
 * Instances of this interface may be passed to code that needs to operate on a
 * subset of some set of resources. The {@link ResourceFilter} selects the
 * subset of resources to operate on. For instance the
 * {@link Resources#filter(IResource[], ResourceFilter)} method takes an
 * {@link ResourceFilter} to select a subset of resources from an input array of
 * {@link IResource}.
 * </p>
 *
 * <p>
 * {@link ResourceFilter}s return their result as an instance of
 * {@link ResourceFilterResult}. Using {@link ResourceFilterResult} allows a
 * filter to (optionally) provide information about the resource's children as
 * well as the resource itself. For example, a filter that rejects derived
 * resources can indicate that the resource's children should also be rejected
 * (since by definition the children are also derived).
 * </p>
 *
 * <p>
 * {@link ResourceFilter}s are often combined using
 * {@link CompositeResourceFilter}. This allows for very fine-grained resource
 * filters that can be composed together to create a coarser-grained filter.
 * </p>
 *
 * @see CompositeResourceFilter
 * @see Resources#filter(IResource[], ResourceFilter)
 */
public abstract class ResourceFilter {
    /**
     * A flag that can be passed to {@link #filter(IResource, int)}. This flag
     * indicates nothing.
     */
    public static final int FILTER_FLAG_NONE = 0;

    /**
     * <p>
     * A flag that can be passed to {@link #filter(IResource, int)} that allows
     * visitor patterns (or other filter usages dealing with hierarchical
     * traversal of filters) to exit evaluation early for an entire branch. This
     * is probably not useful for action enablement or simple filter usage.
     * </p>
     *
     * <p>
     * This flag should be used carefully. When passed, it enables "tree
     * optimizations" for certain {@link ResourceFilter} implementations that
     * support such optimizations. These optimizations allow for more efficient
     * filter computations, but they assume that a filter never sees a
     * descendant resource whose ancestor would have been rejected with
     * {@link ResourceFilterResult#REJECT_AND_REJECT_CHILDREN}.
     * </p>
     *
     * <p>
     * This flag should only be passed when the following is true:
     * <ul>
     * <li>The filter is being used as a resource tree is being traversed
     * downwards, always visiting parent resources before child resources</li>
     * <li>If the filter is part of a filter chain, none of the filters higher
     * in the chain return a <code>*ACCEPT_CHILDREN</code> result for a resource
     * that the filter would have returned
     * {@link ResourceFilterResult#REJECT_AND_REJECT_CHILDREN} for</li>
     * </ul>
     * </p>
     */
    public static final int FILTER_FLAG_TREE_OPTIMIZATION = 1;

    /**
     * A flag that can be passed into
     * {@link ResourceFilterResult#getInstance(int)}. This flag indicates
     * nothing.
     */
    public static final int RESULT_FLAG_NONE = 0;

    /**
     * A flag that can be passed into
     * {@link ResourceFilterResult#getInstance(int)}. This flag indicates that
     * the resource passed to a filter is accepted.
     */
    public static final int RESULT_FLAG_ACCEPT = 1;

    /**
     * A flag that can be passed into
     * {@link ResourceFilterResult#getInstance(int)}. This flag indicates that
     * the resource passed to a filter is rejected.
     */
    public static final int RESULT_FLAG_REJECT = 2;

    /**
     * A flag that can be passed into
     * {@link ResourceFilterResult#getInstance(int)}. This flag indicates that
     * the descendants of a resource passed to a filter are all accepted.
     */
    public static final int RESULT_FLAG_ACCEPT_CHILDREN = 4;

    /**
     * A flag that can be passed into
     * {@link ResourceFilterResult#getInstance(int)}. This flag indicates that
     * the descendants of a resource passed to a filter are all rejected.
     */
    public static final int RESULT_FLAG_REJECT_CHILDREN = 8;

    /**
     * <p>
     * A {@link ResourceFilterResult} which indicates that a resource is
     * <b>accepted</b> by a filter. This result does not imply either acceptance
     * or rejection of the resource's children by the filter.
     * </p>
     *
     * <p>
     * A synonym for {@link ResourceFilterResult#ACCEPT}. Using this synonym
     * allows implementations of {@link ResourceFilter} to use a syntactically
     * shorter form in expressions.
     * </p>
     */
    public static final ResourceFilterResult ACCEPT = ResourceFilterResult.ACCEPT;

    /**
     * <p>
     * A {@link ResourceFilterResult} which indicates that a resource is
     * <b>rejected</b> by a filter. This result does not imply either acceptance
     * or rejection of the resource's children by the filter.
     * </p>
     *
     * <p>
     * A synonym for {@link ResourceFilterResult#REJECT}. Using this synonym
     * allows implementations of {@link ResourceFilter} to use a syntactically
     * shorter form in expressions.
     * </p>
     */
    public static final ResourceFilterResult REJECT = ResourceFilterResult.REJECT;

    /**
     * <p>
     * A {@link ResourceFilterResult} which indicates that a resource is
     * <b>accepted</b> by a filter. This result also indicates that all of the
     * resource's children will be <b>accepted</b> by the same filter.
     * </p>
     *
     * <p>
     * A synonym for {@link ResourceFilterResult#ACCEPT_AND_ACCEPT_CHILDREN}.
     * Using this synonym allows implementations of {@link ResourceFilter} to
     * use a syntactically shorter form in expressions.
     * </p>
     */
    public static final ResourceFilterResult ACCEPT_AND_ACCEPT_CHILDREN =
        ResourceFilterResult.ACCEPT_AND_ACCEPT_CHILDREN;

    /**
     * <p>
     * A {@link ResourceFilterResult} which indicates that a resource is
     * <b>accepted</b> by a filter. This result also indicates that all of the
     * resource's children will be <b>rejected</b> by the same filter.
     * </p>
     *
     * <p>
     * A synonym for {@link ResourceFilterResult#ACCEPT_AND_REJECT_CHILDREN}.
     * Using this synonym allows implementations of {@link ResourceFilter} to
     * use a syntactically shorter form in expressions.
     * </p>
     */
    public static final ResourceFilterResult ACCEPT_AND_REJECT_CHILDREN =
        ResourceFilterResult.ACCEPT_AND_REJECT_CHILDREN;

    /**
     * <p>
     * A {@link ResourceFilterResult} which indicates that a resource is
     * <b>rejected</b> by a filter. This result also indicates that all of the
     * resource's children will be <b>accepted</b> by the same filter.
     * </p>
     *
     * <p>
     * A synonym for {@link ResourceFilterResult#REJECT_AND_ACCEPT_CHILDREN}.
     * Using this synonym allows implementations of {@link ResourceFilter} to
     * use a syntactically shorter form in expressions.
     * </p>
     */
    public static final ResourceFilterResult REJECT_AND_ACCEPT_CHILDREN =
        ResourceFilterResult.REJECT_AND_ACCEPT_CHILDREN;

    /**
     * <p>
     * A {@link ResourceFilterResult} which indicates that a resource is
     * <b>rejected</b> by a filter. This result also indicates that all of the
     * resource's children will be <b>rejected</b> by the same filter.
     * </p>
     *
     * <p>
     * A synonym for {@link ResourceFilterResult#REJECT_AND_REJECT_CHILDREN}.
     * Using this synonym allows implementations of {@link ResourceFilter} to
     * use a syntactically shorter form in expressions.
     * </p>
     */
    public static final ResourceFilterResult REJECT_AND_REJECT_CHILDREN =
        ResourceFilterResult.REJECT_AND_REJECT_CHILDREN;

    /**
     * Tests whether or not the specified {@link IResource} should be included
     * in the set of resources being selected by this filter. This method
     * returns a {@link ResourceFilterResult} that indicates acceptance or
     * rejection of the resource by this filter. By accepting a resource, the
     * filter is indicating that the resource should be included in the subset
     * of resources that is being built. The result object may optionally
     * indicate acceptance or rejection of all of the resource's descendants by
     * this filter - this can be used as an optimization by some clients.
     *
     * @param resource
     *        the {@link IResource} under consideration (must not be
     *        <code>null</code>)
     * @return a {@link ResourceFilterResult} (never <code>null</code>)
     */
    public ResourceFilterResult filter(final IResource resource) {
        return filter(resource, FILTER_FLAG_NONE);
    }

    /**
     * <p>
     * Tests whether or not the specified {@link IResource} should be included
     * in the set of resources being selected by this filter. This method
     * returns a {@link ResourceFilterResult} that indicates acceptance or
     * rejection of the resource by this filter. By accepting a resource, the
     * filter is indicating that the resource should be included in the subset
     * of resources that is being built. The result object may optionally
     * indicate acceptance or rejection of all of the resource's descendants by
     * this filter - this can be used as an optimization by some clients.
     * </p>
     *
     * <p>
     * The flags parameter supports the {@link #FILTER_FLAG_TREE_OPTIMIZATION}
     * flag - see the documentation of that flag for more details.
     * </p>
     *
     * @param resource
     *        the {@link IResource} under consideration (must not be
     *        <code>null</code>)
     * @param flags
     *        filter flags, or {@link #FILTER_FLAG_NONE} for default behavior
     * @return a {@link ResourceFilterResult} (never <code>null</code>)
     */
    public abstract ResourceFilterResult filter(IResource resource, int flags);
}
