// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse.resource;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.team.core.Team;

import com.microsoft.tfs.client.common.framework.resources.filter.CompositeResourceFilter.Builder;
import com.microsoft.tfs.client.common.framework.resources.filter.CompositeResourceFilter.CompositeResourceFilterType;
import com.microsoft.tfs.client.common.framework.resources.filter.ResourceFilter;
import com.microsoft.tfs.client.common.framework.resources.filter.ResourceFilters;
import com.microsoft.tfs.client.eclipse.resourcedata.ResourceDataManager;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ChangeType;

/**
 * <p>
 * Defines static {@link ResourceFilter} instances for use by the Eclipse
 * plug-in which make use of information provided by Eclipse's Team API and
 * other data sources unavailble to the lower-level {@link ResourceFilters}
 * class (which only depends on Eclipse's resource plug-in).
 * </p>
 * <p>
 * Some of this class's filters should be used almost everywhere (
 * {@link #STANDARD_FILTER}), but some are specific to action enablement and
 * have little use elsewhere.
 * </p>
 *
 * @threadsafety thread-safe
 */
public abstract class PluginResourceFilters {
    /**
     * Filters in this class do this to resources when their repository is
     * unavailable (offline).
     */
    private static final RepositoryUnavailablePolicy DEFAULT_RESOURCE_UNAVAILABLE_POLICY =
        RepositoryUnavailablePolicy.REJECT_RESOURCE;

    /**
     * Obtains a "first-pass" {@link ResourceFilter} appropriate for use in most
     * actions that process selections of resources. This filter can be used as
     * a first step by most actions to filter out resources that should never
     * participate in source control operations.
     *
     * @return an {@link ResourceFilter} as described above (never
     *         <code>null</code>)
     */
    private static ResourceFilter createStandardFilter() {
        final Builder builder = new Builder(CompositeResourceFilterType.ALL_MUST_ACCEPT);

        // Add the fast-running, most-likely-to-terminate filters first.

        builder.addFilter(LOCAL_WORKSPACE_BASELINE_FILTER);
        builder.addFilter(TEAM_IGNORED_RESOURCES_FILTER);
        builder.addFilter(ResourceFilters.TEAM_PRIVATE_RESOURCES_FILTER);
        builder.addFilter(ResourceFilters.LINKED_RESOURCES_FILTER);
        builder.addFilter(TPIGNORE_FILTER);
        builder.addFilter(TFS_IGNORE_FILTER);

        return builder.build();
    }

    /*
     * Generally useful filters.
     */

    /**
     * A resource filter which rejects items which match patterns in their
     * project's .tpginore file (if it exists).
     * <p>
     * <b>Note:</b> Only filters for server workspaces. Accepts all resources in
     * a local workspace. {@link #TFS_IGNORE_FILTER} should be used for TFS 2012
     * local workspaces.
     */
    public static final ResourceFilter TPIGNORE_FILTER =
        new TPIgnoreResourcesFilter(RepositoryUnavailablePolicy.ACCEPT_RESOURCE);

    /**
     * Inverse of {@link #TPIGNORE_FILTER}. Accepts items which match patterns
     * in their project's .tpignore file, rejects others.
     * <p>
     * <b>Note:</b> Only filters for server workspaces. Accepts all resources in
     * a local workspace. {@link #TFS_IGNORE_FILTER} should be used for TFS 2012
     * local workspaces.
     */
    public static final ResourceFilter TPIGNORE_FILTER_INVERSE =
        ResourceFilters.getInverse(PluginResourceFilters.TPIGNORE_FILTER);

    /**
     * A {@link ResourceFilter} which rejects Eclipse Team API ignored resources
     * (resources for which {@link Team#isIgnoredHint(IResource)} returns
     * <code>true</code>). Note that {@link Team#isIgnoredHint(IResource)} also
     * returns <code>true</code> for derived resources, so this filter also
     * covers derived resources.
     */
    public static final ResourceFilter TEAM_IGNORED_RESOURCES_FILTER = new TeamIgnoredResourcesFilter();

    /**
     * A {@link ResourceFilter} which rejects items which match any entry in the
     * exclusions list stored in the Team Foundation Server (which can be
     * customized locally), or in the .tfignore files found on disk.
     * <p>
     * <b>Note:</b> Only filters for TFS 2012 local workspaces. Accepts all
     * resources in a server workspace. Use {@link #TPIGNORE_FILTER} for server
     * workspaces.
     */
    public static final TFSIgnoreResourcesFilter TFS_IGNORE_FILTER =
        new TFSIgnoreResourcesFilter(RepositoryUnavailablePolicy.ACCEPT_RESOURCE);

    /**
     * A {@link ResourceFilter} which rejects TFS 2012 local workspace baseline
     * folders (.tf/$tf directories) and all children.
     * <p>
     * <b>Note:</b> Only filters for TFS 2012 local workspaces. Accepts all
     * resources in a server workspace.
     */
    public static final ResourceFilter LOCAL_WORKSPACE_BASELINE_FILTER =
        new LocalWorkspaceBaselineFilter(RepositoryUnavailablePolicy.ACCEPT_RESOURCE);

    /**
     * The basic resource filter, which rejects: linked resources, team ignored
     * resources, team private resources, .tpignore resources, TFS 2012
     * exclusions, and baseline metadata folders.
     *
     * This filter accepts resources which are not in the repository, as well as
     * resources which are in the repository.
     */
    public static final ResourceFilter STANDARD_FILTER = PluginResourceFilters.createStandardFilter();

    /**
     * <p>
     * Extends {@link #STANDARD_FILTER} to reject resources which are not in the
     * TFS repository.
     * </p>
     * <p>
     * An {@link IFolder} or {@link IProject} is "in the repository" if it has a
     * non-cloak mapping to some server path. The item at the server path is not
     * validated in any way.
     * </p>
     * <p>
     * An {@link IFile} is "in the repository" if {@link ResourceDataManager}
     * has information about it. This means the server path is not null and the
     * changeset number is not zero in ResourceData.
     * </p>
     */
    public static final ResourceFilter IN_REPOSITORY_FILTER =
        new Builder(CompositeResourceFilterType.ALL_MUST_ACCEPT).addFilter(STANDARD_FILTER).addFilter(
            new InRepositoryFilter(DEFAULT_RESOURCE_UNAVAILABLE_POLICY)).build();

    /*
     * Filters probably useful only for action enablement and other unusual
     * cases.
     */

    /**
     * Accepts any resource that is in the repository *or* has pending BRANCH
     * changes.
     */
    public static final ResourceFilter IN_REPOSITORY_OR_HAS_BRANCH_PENDING_CHANGES_FILTER =
        new Builder(CompositeResourceFilterType.ONE_MUST_ACCEPT).addFilter(IN_REPOSITORY_FILTER).addFilter(
            new HasPendingChangeTypeFilter(new ChangeType[] {
                ChangeType.BRANCH
    }, DEFAULT_RESOURCE_UNAVAILABLE_POLICY, false)).build();

    /**
     * Accepts any resource which has any pending change on it.
     */
    public static final ResourceFilter HAS_PENDING_CHANGES_FILTER =
        new Builder(CompositeResourceFilterType.ALL_MUST_ACCEPT).addFilter(STANDARD_FILTER).addFilter(
            new HasPendingChangeFilter(DEFAULT_RESOURCE_UNAVAILABLE_POLICY)).build();

    /**
     * Accepts any resource which has a lock pending change directly on it.
     */
    public static final ResourceFilter HAS_LOCK_PENDING_CHANGES_FILTER =
        new Builder(CompositeResourceFilterType.ALL_MUST_ACCEPT).addFilter(STANDARD_FILTER).addFilter(
            new HasPendingChangeTypeFilter(new ChangeType[] {
                ChangeType.LOCK
    }, DEFAULT_RESOURCE_UNAVAILABLE_POLICY, false)).build();

    /**
     * Accepts any resource which does not have an add pending change directly
     * on it.
     */
    public static final ResourceFilter CAN_APPLY_LABEL_FILTER =
        new Builder(CompositeResourceFilterType.ALL_MUST_ACCEPT).addFilter(IN_REPOSITORY_FILTER).addFilter(
            new HasNoPendingChangeTypeFilter(new ChangeType[] {
                ChangeType.ADD
    }, DEFAULT_RESOURCE_UNAVAILABLE_POLICY, false)).build();

    /**
     * Accepts any resource which does not have an add or edit pending change
     * directly on that resource, and that resource exists in a local working
     * folder.
     */
    public static final ResourceFilter CAN_CHECKOUT_FILTER =
        new Builder(CompositeResourceFilterType.ALL_MUST_ACCEPT).addFilter(
            IN_REPOSITORY_OR_HAS_BRANCH_PENDING_CHANGES_FILTER).addFilter(
                new HasNoPendingChangeTypeFilter(new ChangeType[] {
                    ChangeType.ADD,
                    ChangeType.EDIT
    }, DEFAULT_RESOURCE_UNAVAILABLE_POLICY, false)).addFilter(new ResourceExistsFilter()).build();

    /**
     * Accepts any resource that is in the repository *or* has remote changes in
     * the synchronization data (ie, is in the remote repository and will become
     * a managed {@link IResource} when we do a get on the object.
     */
    public static final ResourceFilter SYNCHRONIZE_OR_IN_REPOSITORY_FILTER =
        new Builder(CompositeResourceFilterType.ONE_MUST_ACCEPT).addFilter(IN_REPOSITORY_FILTER).addFilter(
            new RemoteSyncInfoFilter()).build();

    public static final ResourceFilter HAS_PENDING_CHANGES_OR_IN_REPOSITORY_FILTER =
        new Builder(CompositeResourceFilterType.ONE_MUST_ACCEPT).addFilter(HAS_PENDING_CHANGES_FILTER).addFilter(
            IN_REPOSITORY_FILTER).build();

    /**
     * Accepts any resource that is in a TFS 2012 local workspace.
     */
    public static final ResourceFilter IN_LOCAL_WORKSPACE_FILTER =
        new InLocalWorkspaceFilter(RepositoryUnavailablePolicy.REJECT_RESOURCE);

    /**
     * Accepts any resource that is in a TFS 2005-2012 server workspace.
     */
    public static final ResourceFilter IN_SERVER_WORKSPACE_FILTER =
        ResourceFilters.getInverse(IN_LOCAL_WORKSPACE_FILTER);
}
