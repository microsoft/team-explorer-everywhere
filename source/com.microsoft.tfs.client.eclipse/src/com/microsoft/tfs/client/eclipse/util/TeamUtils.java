// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse.util;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.team.core.RepositoryProvider;

import com.microsoft.tfs.client.eclipse.resource.LocalWorkspaceBaselineFilter;
import com.microsoft.tfs.client.eclipse.resource.PluginResourceFilters;
import com.microsoft.tfs.util.Check;

/**
 * {@link TeamUtils} contains static utility methods useful for Team operation
 * in Eclipse.
 */
public final class TeamUtils {
    /*
     * TeamProvider.PROVIDER_PROP_KEY does not exist in eclipse 3.0.
     */
    public static final QualifiedName PROVIDER_PROP_KEY = new QualifiedName("org.eclipse.team.core", "repository"); //$NON-NLS-1$ //$NON-NLS-2$

    /* Helper class: should not be instantiated */
    private TeamUtils() {
    }

    /**
     * <p>
     * Obtains all of the <b>open</b> {@link IProject}s currently in the Eclipse
     * workspace that are configured to use the repository provider with the
     * specified ID.
     * </p>
     *
     * <p>
     * The results of this method should not be cached, as the set of projects
     * that matches the above criteria can change over the course of an Eclipse
     * session.
     * </p>
     *
     * @param providerId
     *        the repository provider ID to use in the tests (must not be
     *        <code>null</code>)
     * @return all current open projects in the Eclipse workspace configured to
     *         use a repository provider with the specified ID (never
     *         <code>null</code>)
     */
    public static IProject[] getProjectsConfiguredWith(final String providerId) {
        final IProject[] allProjects = ResourcesPlugin.getWorkspace().getRoot().getProjects();

        final List configuredProjects = new ArrayList();

        for (int i = 0; i < allProjects.length; i++) {
            if (isConfiguredWith(allProjects[i], providerId)) {
                configuredProjects.add(allProjects[i]);
            }
        }

        return (IProject[]) configuredProjects.toArray(new IProject[configuredProjects.size()]);
    }

    /**
     * <p>
     * Tests whether the specified resource's project is configured to use
     * <b>any</b> repository provider.
     * </p>
     *
     * <p>
     * To test whether the resource's project is configured to use a
     * <b>specific</b> repository provider, call
     * {@link #isConfiguredWith(IResource, String)} instead. However,
     * {@link #isConfigured(IResource)} is more efficient since it does not
     * require Eclipse to lazily instantiate a repository provider for the
     * resource's project.
     * </p>
     *
     * <p>
     * If the resource is contained in a closed project, this method will return
     * <code>false</code> even if the project is configured to use a repository
     * provider.
     * </p>
     *
     * <p>
     * If the specified resource is the root resource, this method returns
     * <code>false</code>.
     * </p>
     *
     * @param resource
     *        the resource to test (must not be <code>null</code>)
     * @return <code>true</code> if the resource's project is configured to use
     *         any repository provider, or <code>false</code> if the resource is
     *         in a closed project, is in a project not configured to use a
     *         repository provider, or is the root resource
     */
    public static boolean isConfigured(final IResource resource) {
        Check.notNull(resource, "resource"); //$NON-NLS-1$

        if (IResource.ROOT == resource.getType()) {
            return false;
        }

        return RepositoryProvider.isShared(resource.getProject());
    }

    /**
     * <p>
     * Tests whether a specified resource's project is configured to use a
     * repository provider with the specified ID.
     * </p>
     *
     * <p>
     * If the resource is contained within a closed project, this method will
     * return <code>false</code> even if the project is configured to use a
     * repository provider with the specified ID.
     * </p>
     *
     * <p>
     * If the specified resource is the root resource, this method will return
     * <code>false</code>.
     * </p>
     *
     * @param resource
     *        the {@link IResource} to test (must not be <code>null</code>)
     * @param providerId
     *        the repository provider ID to use in the tests (must not be
     *        <code>null</code>)
     * @return <code>true</code> if the resource's project is configured to use
     *         a repository provider with the specified ID, or
     *         <code>false</code> if the resource is in a closed project, a
     *         project that is not configured to use any repository provider, a
     *         project that is configured to use a repository provider with a
     *         different ID, or if the resource is the root resource
     */
    public static boolean isConfiguredWith(final IResource resource, final String providerId) {
        if (resource.isLinked(IResource.CHECK_ANCESTORS)) {
            return false;
        }

        return getRepositoryProvider(resource, providerId, false) != null;
    }

    /**
     * <p>
     * Tests whether a specified resource's project is configured to use a a
     * repository provider that does not have the specified ID.
     * </p>
     *
     * <p>
     * If the resource is contained within a closed project, this method will
     * return <code>false</code> even if the project is configured to use a
     * different repository provider.
     * </p>
     *
     * <p>
     * If the specified resource is the root resource, this method will return
     * <code>false</code>.
     * </p>
     *
     * @param resource
     *        the {@link IResource} to test (must not be <code>null</code>)
     * @param providerId
     *        the repository provider ID to use in the tests (must not be
     *        <code>null</code>)
     * @return <code>true</code> if the resource's project is configured with a
     *         repository provider and that provider does not have the specified
     *         ID
     */
    public static boolean isConfiguredWithOtherProvider(final IResource resource, final String providerId) {
        return isConfigured(resource) && (getRepositoryProvider(resource, providerId, false) == null);
    }

    /**
     * <p>
     * Gets the repository provider for the specified resource's project.
     * </p>
     *
     * <p>
     * The repository provider must have the specified ID. If it does not, or if
     * the resource's project is not configured to use a repository provider, an
     * {@link UnexpectedProjectConfigurationException} is thrown.
     * </p>
     *
     * @throws UnexpectedProjectConfigurationException
     *         if the resource's project is closed, configured to use a
     *         repository provider with a different ID, or not configured to use
     *         a repository provider
     *
     * @throws IllegalArgumentException
     *         if the specified resource is the root resource
     *
     * @param resource
     *        the resource to get the repository provider for (must not be
     *        <code>null</code> or the root resource)
     * @param providerId
     *        the repository provider's ID (must not be <code>null</code>)
     * @return the resource's project's {@link RepositoryProvider} (never
     *         <code>null</code>)
     */
    public static RepositoryProvider getRepositoryProvider(final IResource resource, final String providerId) {
        return getRepositoryProvider(resource, providerId, true);
    }

    /**
     * <p>
     * Gets the repository provider for the specified resource's project.
     * </p>
     *
     * <p>
     * The repository provider must have the specified ID. If it does not, or if
     * the resource's project is not configured to use a repository provider,
     * the <code>mustExist</code> parameter determines the result: if
     * <code>mustExist</code> is <code>true</code>, an
     * {@link UnexpectedProjectConfigurationException} is thrown, otherwise
     * <code>null</code> is returned.
     * </p>
     *
     * <p>
     * If the resource's project is closed, this method will either thrown an
     * exception (if <code>mustExist</code> is <code>true</code>) or return
     * <code>null</code>.
     * </p>
     *
     * <p>
     * If the specified resource is the root resource, this method will either
     * thrown an {@link IllegalArgumentException} (if <code>mustExist</code> is
     * <code>true</code>) or return <code>null</code>.
     * </p>
     *
     * @throws UnexpectedProjectConfigurationException
     *         if <code>mustExist</code> is <code>true</code> and the resource's
     *         project is closed, configured to use a repository provider with a
     *         different ID, or not configured to use a repository provider
     *
     * @throws IllegalArgumentException
     *         if called with the root resource and <code>mustExist</code> is
     *         <code>true</code>
     *
     * @param resource
     *        the resource to get the repository provider for (must not be
     *        <code>null</code> and must not be the root resource is
     *        <code>mustExist</code> is <code>true</code>)
     * @param providerId
     *        the repository provider's ID (must not be <code>null</code>)
     * @param mustExist
     *        <code>true</code> if the resource's project must be configured
     *        with a repository provider having the specified ID
     * @return the resource's project's {@link RepositoryProvider}; never
     *         <code>null</code> if <code>mustExist</code> is <code>true</code>,
     *         or <code>null</code> if <code>mustExist</code> is
     *         <code>false</code> and no matching repository provider exists or
     *         if the resource is the root resource
     */
    public static RepositoryProvider getRepositoryProvider(
        final IResource resource,
        final String providerId,
        final boolean mustExist) {
        Check.notNull(resource, "resource"); //$NON-NLS-1$
        Check.notNull(providerId, "providerId"); //$NON-NLS-1$

        if (IResource.ROOT == resource.getType()) {
            if (mustExist) {
                throw new IllegalArgumentException(
                    "it is illegal to call getRepositoryProvider() with the root resource when mustExist is true"); //$NON-NLS-1$
            }

            return null;
        }

        final RepositoryProvider provider = RepositoryProvider.getProvider(resource.getProject(), providerId);

        if (mustExist && provider == null) {
            throw new UnexpectedProjectConfigurationException(resource, providerId);
        }

        return provider;
    }

    /**
     * <p>
     * Gets the repository provider for the specified resource's project. This
     * method will obtain <b>any</b> repository provider that the project may be
     * configured to use. If a <b>specific</b> repository provider is expected,
     * use {@link #getRepositoryProvider(IResource, String, boolean)} instead.
     * </p>
     *
     * @throws UnexpectedProjectConfigurationException
     *         if the resource's project is closed or not configured to use a
     *         repository provider
     *
     * @throws IllegalArgumentException
     *         if the specified resource is the root resource
     *
     * @param resource
     *        the resource to get the repository provider for (must not be
     *        <code>null</code> or the root resource)
     * @return the resource's project's {@link RepositoryProvider} (never
     *         <code>null</code>)
     */
    public static RepositoryProvider getRepositoryProvider(final IResource resource) {
        return getRepositoryProvider(resource, true);
    }

    /**
     * <p>
     * Gets the repository provider for the specified resource's project. This
     * method will obtain <b>any</b> repository provider that the project may be
     * configured to use. If a <b>specific</b> repository provider is expected,
     * use {@link #getRepositoryProvider(IResource, String, boolean)} instead.
     * </p>
     *
     * <p>
     * If the resource's project is not configured to use a repository provider,
     * the <code>mustExist</code> parameter determines the result: if
     * <code>true</code>, an {@link UnexpectedProjectConfigurationException} is
     * thrown, otherwise <code>null</code> is returned.
     * </p>
     *
     * <p>
     * If the resource's project is closed, this method will either thrown an
     * exception (if <code>mustExist</code> is <code>true</code>) or return
     * <code>null</code>.
     * </p>
     *
     * <p>
     * If the specified resource is the root resource, this method will either
     * thrown an {@link IllegalArgumentException} (if <code>mustExist</code> is
     * <code>true</code>) or return <code>null</code>.
     * </p>
     *
     * @throws UnexpectedProjectConfigurationException
     *         if <code>mustExist</code> is <code>true</code> and the resource's
     *         project is closed or not configured to use a repository provider
     *
     * @throws IllegalArgumentException
     *         if called with the root resource and <code>mustExist</code> is
     *         <code>true</code>
     *
     * @param resource
     *        the resource to get the repository provider for (must not be
     *        <code>null</code> and must not be the root resource is
     *        <code>mustExist</code> is <code>true</code>)
     * @param mustExist
     *        <code>true</code> if the resource's project must be configured
     *        with a repository provider
     * @return the resource's project's {@link RepositoryProvider}; never
     *         <code>null</code> if <code>mustExist</code> is <code>true</code>,
     *         or <code>null</code> if <code>mustExist</code> is
     *         <code>false</code> and no repository provider exists or if the
     *         resource is the root resource
     */
    public static RepositoryProvider getRepositoryProvider(final IResource resource, final boolean mustExist) {
        Check.notNull(resource, "resource"); //$NON-NLS-1$

        if (IResource.ROOT == resource.getType()) {
            if (mustExist) {
                throw new IllegalArgumentException(
                    "it is illegal to call getRepositoryProvider() with the root resource when mustExist is true"); //$NON-NLS-1$
            }

            return null;
        }

        final RepositoryProvider repositoryProvider = RepositoryProvider.getProvider(resource.getProject());

        if (mustExist && repositoryProvider == null) {
            throw new UnexpectedProjectConfigurationException(resource, null);
        }

        return repositoryProvider;
    }

    /**
     * Scans the resource, fully recursive, looking for any {@link IFolder}
     * that's a TFS 2012 local workspace baseline folder (named $tf or .tf) and
     * marks it as a "team private" resource.
     *
     * @param resource
     *        the resource (usually an {@link IProject} but could be an
     *        {@link IFolder}) to scan for baseline folders and mark private
     *        (must not be <code>null</code>)
     * @return the baseline folders that were found and marked team private
     *         (never <code>null</code>)
     * @throws CoreException
     *         if an exception happened setting a folder as a team private
     *         resource
     * @see LocalWorkspaceBaselineFilter
     */
    public static List<IFolder> markBaselineFoldersTeamPrivate(final IResource resource) throws CoreException {
        Check.notNull(resource, "resource"); //$NON-NLS-1$

        final List<IFolder> baselines = new ArrayList<IFolder>();

        resource.accept(new IResourceVisitor() {
            @Override
            public boolean visit(final IResource resource) throws CoreException {
                if (PluginResourceFilters.LOCAL_WORKSPACE_BASELINE_FILTER.filter(resource).isReject()) {
                    baselines.add((IFolder) resource);
                    resource.setTeamPrivateMember(true);

                    /*
                     * You might see resources marked "team private" by this
                     * method remain visible the package explorer until you
                     * manually refresh the view. This is because marking them
                     * doesn't fire events that cause views to redraw.
                     *
                     * Forcing a local refresh on the resource doesn't seem to
                     * work either (even if run as a deferred job). I dont know
                     * of a work-around as of Eclipse 3.7.
                     */

                    return false;
                }

                return true;
            }
        }, IResource.DEPTH_INFINITE, true);

        return baselines;
    }

    /**
     * {@link UnexpectedProjectConfigurationException} is a
     * {@link RuntimeException} that can be thrown when an {@link IProject}'s
     * Team configuration is in an unexpected state.
     */
    public static class UnexpectedProjectConfigurationException extends RuntimeException {
        private final IResource resource;
        private final String message;

        /**
         * Creates a new {@link UnexpectedProjectConfigurationException} for the
         * specified project's resource. If a
         * {@link UnexpectedProjectConfigurationException} is being thrown
         * because the resource's project was expected to be configured with a
         * specific repository provider, the expected repository provider's ID
         * should be specified.
         *
         * @param resource
         *        the resource whose project's Team configuration was unexpected
         *        (must not be <code>null</code>)
         * @param expectedProviderId
         *        the expected repository provider's ID, or <code>null</code> if
         *        no specific repository provider was expected
         */
        public UnexpectedProjectConfigurationException(final IResource resource, final String expectedProviderId) {
            Check.notNull(resource, "resource"); //$NON-NLS-1$

            this.resource = resource;

            final RepositoryProvider provider = TeamUtils.getRepositoryProvider(resource, false);

            /*
             * This string remains unlocalized because it is complex, and we do
             * not expect customers to be able to take action to correct this
             * error.
             */

            final StringBuffer buffer = new StringBuffer();
            buffer.append("Unexpected configuration for project ["); //$NON-NLS-1$
            buffer.append(resource.getProject().getName());
            buffer.append("]: "); //$NON-NLS-1$

            if (expectedProviderId != null) {
                buffer.append("expected provider was ["); //$NON-NLS-1$
                buffer.append(expectedProviderId);
                buffer.append("] "); //$NON-NLS-1$

                if (provider != null) {
                    buffer.append("but actual provider was ["); //$NON-NLS-1$
                    buffer.append(provider.getID());
                    buffer.append("]"); //$NON-NLS-1$
                } else {
                    buffer.append("but project does not have a provider"); //$NON-NLS-1$
                }
            } else {
                if (provider != null) {
                    buffer.append("provider is [" + provider.getID() + "]"); //$NON-NLS-1$ //$NON-NLS-2$
                } else {
                    buffer.append("project has no provider"); //$NON-NLS-1$
                }
            }

            message = buffer.toString();
        }

        /*
         * (non-Javadoc)
         *
         * @see java.lang.Throwable#getMessage()
         */
        @Override
        public String getMessage() {
            return message;
        }

        /**
         * @return the {@link IResource} whose project triggered this
         *         {@link UnexpectedProjectConfigurationException}
         */
        public IResource getResource() {
            return resource;
        }
    }
}
