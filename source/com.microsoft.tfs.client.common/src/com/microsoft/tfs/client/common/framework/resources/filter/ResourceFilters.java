// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.framework.resources.filter;

import org.eclipse.core.resources.IResource;

import com.microsoft.tfs.client.common.framework.resources.compatibility.LinkedResources;
import com.microsoft.tfs.util.Check;

/**
 * <p>
 * {@link ResourceFilters}s is a utility class that exposes a number of static
 * {@link ResourceFilter}s. Each of the available {@link ResourceFilter}s is a
 * stateless, singleton instance obtained by referencing a public static field
 * of this class.
 * </p>
 *
 * <p>
 * The {@link ResourceFilter}s available through this class cover many of the
 * general implementations of {@link ResourceFilter} that only depend on the
 * resources API.
 * </p>
 *
 * <p>
 * Clients will often combine together multiple {@link ResourceFilter}s from
 * this class (as well as custom {@link ResourceFilter}s) by using the
 * {@link CompositeResourceFilter} class.
 * </p>
 *
 * @see ResourceFilter
 * @see CompositeResourceFilter
 */
public class ResourceFilters {
    /**
     * <p>
     * Returns a new {@link ResourceFilter} that is the inverse of the specified
     * filter. If the specified filter would reject a resource, the inverse
     * filter will accept that resource.
     * </p>
     *
     * <p>
     * Precisely, the inverse filter will return the inverse
     * {@link ResourceFilterResult} for any resource that the input filter
     * returns. The inverse result mapping is defined by the
     * {@link ResourceFilterResult#getInverse()} method.
     * </p>
     *
     * <p>
     * Warning: this simply inverts the results. If your filter is particularly
     * complex (for example, takes a RepositoryUnavailablePolicy), then this may
     * lead to incorrect results.
     * </p>
     *
     * @param input
     *        the {@link ResourceFilter} to get the inverse of (must not be
     *        <code>null</code>)
     * @return a new resource filter that is the inverse of the specified filter
     *         (must not be <code>null</code>)
     */
    public static ResourceFilter getInverse(final ResourceFilter input) {
        Check.notNull(input, "input"); //$NON-NLS-1$

        return new ResourceFilter() {
            @Override
            public ResourceFilterResult filter(final IResource resource, final int flags) {
                return input.filter(resource, flags).getInverse();
            }
        };
    }

    /**
     * An {@link ResourceFilter} that filters out derived resources (resources
     * that return <code>true</code> from {@link IResource#isDerived()}).
     */
    public static final ResourceFilter DERIVED_RESOURCES_FILTER = new DerivedResourcesFilter();

    /**
     * An {@link ResourceFilter} that filters out team private resources
     * (resources that return <code>true</code> from
     * {@link IResource#isTeamPrivateMember()}).
     */
    public static final ResourceFilter TEAM_PRIVATE_RESOURCES_FILTER = new TeamPrivateResourcesFilter();

    /**
     * <p>
     * An {@link ResourceFilter} that filters out linked resources.
     * </p>
     *
     * <p>
     * This filter respects the
     * {@link ResourceFilter#FILTER_FLAG_TREE_OPTIMIZATION} flag. If this flag
     * is passed, a more efficient check for linked resources is performed. The
     * more efficient check will only detect directly linked resources, not
     * resources that are the descendants of links.
     * </p>
     */
    public static final ResourceFilter LINKED_RESOURCES_FILTER = new LinkedResourcesFilter();

    /**
     * An {@link ResourceFilter} that filters out non-file resources (resources
     * that do not return {@link IResource#FILE} from
     * {@link IResource#getType()}).
     */
    public static final ResourceFilter NON_FILE_RESOURCES_FILTER = new ResourceTypeFilter(IResource.FILE);

    /**
     * An {@link ResourceFilter} that filters out non-folder resources
     * (resources that do not return {@link IResource#FOLDER} from
     * {@link IResource#getType()}).
     */
    public static final ResourceFilter NON_FOLDER_RESOURCES_FILTER = new ResourceTypeFilter(IResource.FOLDER);

    /**
     * An {@link ResourceFilter} that filters out non-project resources
     * (resources that do not return {@link IResource#PROJECT} from
     * {@link IResource#getType()}).
     */
    public static final ResourceFilter NON_PROJECT_RESOURCES_FILTER = new ResourceTypeFilter(IResource.PROJECT);

    /**
     * An {@link ResourceFilter} that filters out non-accessible resources
     * (resources that return <code>false</code> from
     * {@link IResource#isAccessible()}).
     */
    public static final ResourceFilter NON_ACCESSIBLE_RESOURCES_FILTER = new NonAccessibleResourcesFilter();

    private static class NonAccessibleResourcesFilter extends ResourceFilter {
        @Override
        public ResourceFilterResult filter(final IResource resource, final int flags) {
            return !resource.isAccessible() ? REJECT_AND_REJECT_CHILDREN : ACCEPT;
        }
    }

    private static class DerivedResourcesFilter extends ResourceFilter {
        @Override
        public ResourceFilterResult filter(final IResource resource, final int flags) {
            return resource.isDerived() ? REJECT_AND_REJECT_CHILDREN : ACCEPT;
        }
    }

    private static class TeamPrivateResourcesFilter extends ResourceFilter {
        @Override
        public ResourceFilterResult filter(final IResource resource, final int flags) {
            return resource.isTeamPrivateMember() ? REJECT_AND_REJECT_CHILDREN : ACCEPT;
        }
    }

    private static class LinkedResourcesFilter extends ResourceFilter {
        @Override
        public ResourceFilterResult filter(final IResource resource, final int flags) {
            if ((flags & FILTER_FLAG_TREE_OPTIMIZATION) != 0) {
                return resource.isLinked() ? REJECT_AND_REJECT_CHILDREN : ACCEPT;
            } else {
                return LinkedResources.isLinked(resource) ? REJECT_AND_REJECT_CHILDREN : ACCEPT;
            }
        }
    }
}
