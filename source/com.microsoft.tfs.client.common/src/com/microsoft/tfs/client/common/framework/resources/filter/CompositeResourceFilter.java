// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.framework.resources.filter;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IResource;

import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.TypesafeEnum;

/**
 * <p>
 * A {@link CompositeResourceFilter} is used to compose together one or more
 * {@link ResourceFilter}s into a single {@link ResourceFilter}. Depending on
 * the given CompositeResourceFilterType, a {@link CompositeResourceFilter} will
 * either reject a resource if any of the composed filters reject the resource
 * (when using CompositeResourceFilterType#ACCEPTS_ALL), or will reject a
 * resource if all of the composed filters reject the resource (when using
 * CompositeResourceFilterType#ACCEPTS_ANY).
 * </p>
 *
 * <p>
 * The order that individual filters are added to a
 * {@link CompositeResourceFilter} is important for efficient use. When using
 * CompositeResourceFilterType#ACCEPTS_ALL, the first sub-filter to reject a
 * resource will stop processing, thus the filter should be front-loaded with
 * the least permissive filters. When using
 * {@link CompositeResourceFilter#ACCEPT_ANY}, the first sub-filter to accept a
 * resource will stop processing, thus the filter should be front-loaded with
 * the most permissive filters.
 * </p>
 *
 * <p>
 * {@link CompositeResourceFilter}s are immutable and threadsafe.
 * </p>
 *
 * <p>
 * Note that it normally makes no sense to get the inverse of a
 * {@link CompositeResourceFilter} of type
 * CompositeResourceFilterType#ACCEPTS_ALL (using the
 * {@link ResourceFilters#getInverse(ResourceFilter)} method). The inverse of a
 * CompositeResourceFilterType#ACCEPTS_ALL {@link CompositeResourceFilter}
 * accepts a resource if at least one of the composed filters rejects the
 * resource, which is not usually a useful operation.
 * </p>
 *
 * @see ResourceFilter
 */
public class CompositeResourceFilter extends ResourceFilter {
    /**
     * {@link Builder} is a convenience class to help construct instances of
     * {@link CompositeResourceFilter}. A {@link CompositeResourceFilter} is
     * immutable, and all sub-filters must be specified at construction time.
     * You can create a {@link Builder} and add sub-filters to it one at a time.
     * Once all needed sub-filters have been added, you can call
     * {@link #build()} to construct an immutable
     * {@link CompositeResourceFilter}.
     */
    public static class Builder {
        private final CompositeResourceFilterType type;
        private final List<ResourceFilter> filters = new ArrayList<ResourceFilter>();

        /**
         * @param type
         *        the CompositeResourceFilterType, either accept all (requiring
         *        all subfilters to accept the resource) or accept any
         *        (requiring any subfilter to accept the resource)
         */
        public Builder(final CompositeResourceFilterType type) {
            Check.notNull(type, "type"); //$NON-NLS-1$

            this.type = type;
        }

        /**
         * Adds a new sub-filter to this {@link Builder} instance.
         *
         * @param filter
         *        the sub-filter to add (must not be <code>null</code>)
         * @return this {@link Builder} instance for method chaining
         */
        public Builder addFilter(final ResourceFilter filter) {
            Check.notNull(filter, "filter"); //$NON-NLS-1$

            filters.add(filter);
            return this;
        }

        /**
         * @return a new {@link CompositeResourceFilter} built using the current
         *         sub-filters of this {@link Builder}
         */
        public CompositeResourceFilter build() {
            return new CompositeResourceFilter(type, filters.toArray(new ResourceFilter[filters.size()]));
        }
    }

    private final CompositeResourceFilterType type;
    private final ResourceFilter[] filters;

    /**
     * Creates a new {@link CompositeResourceFilter} of the given type using the
     * sub-filters specified by the array of {@link ResourceFilter}s. Once
     * constructed, a {@link CompositeResourceFilter}'s sub-filters can't be
     * changed. Consider using the {@link Builder} class to build up a set of
     * sub-filters to create a {@link CompositeResourceFilter}.
     *
     * @param type
     *        the CompositeResourceFilterType, either accept all (requiring all
     *        subfilters to accept the resource) or accept any (requiring any
     *        subfilter to accept the resource)
     * @param filters
     *        the {@link ResourceFilter}s to use as the sub-filters of this
     *        {@link CompositeResourceFilter} (must not be <code>null</code>)
     */
    public CompositeResourceFilter(final CompositeResourceFilterType type, final ResourceFilter[] filters) {
        Check.notNull(type, "type"); //$NON-NLS-1$
        Check.notNull(filters, "filters"); //$NON-NLS-1$

        this.type = type;
        this.filters = filters.clone();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ResourceFilterResult filter(final IResource resource, final int flags) {
        if (type == CompositeResourceFilterType.ALL_MUST_ACCEPT) {
            return filterAllMustAccept(resource, flags);
        } else if (type == CompositeResourceFilterType.ONE_MUST_ACCEPT) {
            return filterOneMustAccept(resource, flags);
        }

        throw new IllegalArgumentException("The given composite filter type is unknown"); //$NON-NLS-1$
    }

    private ResourceFilterResult filterOneMustAccept(final IResource resource, final int flags) {
        boolean acceptChildren = false;

        for (int i = 0; i < filters.length; i++) {
            ResourceFilterResult result = filters[i].filter(resource, flags);

            if (result.isAccept()) {
                /*
                 * We need to ensure that accept but reject children is turned
                 * into only accept. Just because one filter would reject
                 * children does not mean that all would.
                 */
                if (result.isRejectChildren()) {
                    result = ResourceFilterResult.ACCEPT;
                }

                /*
                 * Another filter rejected this resource but will accept
                 * children. Upgrade this accept to an accept and accept
                 * children.
                 */
                return acceptChildren ? ResourceFilter.ACCEPT_AND_ACCEPT_CHILDREN : result;
            } else if (result.isAcceptChildren()) {
                acceptChildren = true;
            }
        }

        return acceptChildren ? ResourceFilterResult.REJECT_AND_ACCEPT_CHILDREN : ResourceFilterResult.REJECT;
    }

    private ResourceFilterResult filterAllMustAccept(final IResource resource, final int flags) {
        boolean acceptChildren = true;

        for (int i = 0; i < filters.length; i++) {
            ResourceFilterResult result = filters[i].filter(resource, flags);

            if (result.isReject()) {
                if (result.isAcceptChildren()) {
                    /*
                     * We need to ensure that REJECT_AND_ACCEPT_CHILDREN is
                     * turned into just REJECT. Just because one composed filter
                     * would accept all children does not imply that all
                     * composed filters would.
                     */
                    result = REJECT;
                }
                return result;
            } else {
                if (!result.isAcceptChildren()) {
                    /*
                     * If all of the composed filters accept the resource, we
                     * will return ACCEPT_AND_ACCEPT_CHILDREN, unless at least
                     * one composed filter did not accept children, in which
                     * case we will return just ACCEPT.
                     */
                    acceptChildren = false;
                }
            }
        }

        return acceptChildren ? ACCEPT_AND_ACCEPT_CHILDREN : ACCEPT;
    }

    public static class CompositeResourceFilterType extends TypesafeEnum {
        /**
         * Requires that all sub-filters accept a resource in order for the
         * {@link CompositeResourceFilter} to accept the resource.
         */
        public static final CompositeResourceFilterType ALL_MUST_ACCEPT = new CompositeResourceFilterType(0);

        /**
         * Requires that any sub-filters accept a resource in order for the
         * {@link CompositeResourceFilter} to accept the resource.
         */
        public static final CompositeResourceFilterType ONE_MUST_ACCEPT = new CompositeResourceFilterType(1);

        private CompositeResourceFilterType(final int value) {
            super(value);
        }
    }
}
