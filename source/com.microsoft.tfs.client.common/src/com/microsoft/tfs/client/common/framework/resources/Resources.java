// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.framework.resources;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import com.microsoft.tfs.client.common.framework.resources.filter.ResourceFilter;
import com.microsoft.tfs.util.Check;

/**
 * {@link Resources} is a helper class containing static utility methods for
 * working with {@link IResource} objects.
 */
public class Resources {
    /**
     * <p>
     * Obtains the absolute locations ({@link IResource#getLocation()}) of an
     * array of resources. Each location is represented as a {@link String}
     * using the platform-dependent separator character (
     * {@link IPath#toOSString()}).
     * </p>
     *
     * <p>
     * For some resources, a location is not available. The
     * {@link LocationUnavailablePolicy} parameter determines the behavior in
     * this case. If the policy is {@link LocationUnavailablePolicy#THROW}, an
     * exception will be thrown if a resources does not have a location. If the
     * policy is {@link LocationUnavailablePolicy#IGNORE_RESOURCE}, a resource
     * that does not have a location will be ignored and will not have a
     * corresponding element in the returned array.
     * </p>
     *
     * @param resources
     *        the input array of {@link IResource}s - must not be
     *        <code>null</code>, and must not contain any <code>null</code>
     *        elements
     * @param locationUnavailablePolicy
     *        a {@link LocationUnavailablePolicy} specifying what to do if one
     *        of the input {@link IResource}s does not have a location (must not
     *        be <code>null</code>)
     * @return an array of {@link String} locations as described above
     */
    public static String[] getLocations(
        final IResource[] resources,
        final LocationUnavailablePolicy locationUnavailablePolicy) {
        Check.notNull(resources, "resources"); //$NON-NLS-1$
        Check.notNull(locationUnavailablePolicy, "locationUnavailablePolicy"); //$NON-NLS-1$

        final List locations = new ArrayList();

        for (int i = 0; i < resources.length; i++) {
            if (resources[i] == null) {
                throw new IllegalArgumentException("element " + i + " in the passed IResource array was null"); //$NON-NLS-1$ //$NON-NLS-2$
            }

            final String location = getLocation(resources[i], locationUnavailablePolicy);

            if (location != null) {
                locations.add(location);
            }
        }

        return (String[]) locations.toArray(new String[locations.size()]);
    }

    /**
     * <p>
     * Obtains the absolute location ({@link IResource#getLocation()}) of a
     * resource. The location is represented as a {@link String} using the
     * platform-dependent separator character ({@link IPath#toOSString()}).
     * </p>
     *
     * <p>
     * For some resources, a location is not available. The
     * {@link LocationUnavailablePolicy} parameter determines the behavior in
     * this case. If the policy is {@link LocationUnavailablePolicy#THROW}, an
     * exception will be thrown if the resource does not have a location. If the
     * policy is {@link LocationUnavailablePolicy#IGNORE_RESOURCE},
     * <code>null</code> will be returned from this method if the resource does
     * not have a location.
     * </p>
     *
     * @param resource
     *        the input {@link IResource} - must not be <code>null</code>
     * @param locationUnavailablePolicy
     *        a {@link LocationUnavailablePolicy} specifying what to do if the
     *        input {@link IResource} does not have a location (must not be
     *        <code>null</code>)
     * @return the {@link String} location as described above
     */
    public static String getLocation(
        final IResource resource,
        final LocationUnavailablePolicy locationUnavailablePolicy) {
        Check.notNull(resource, "resource"); //$NON-NLS-1$
        Check.notNull(locationUnavailablePolicy, "locationUnavailablePolicy"); //$NON-NLS-1$

        final IPath locationPath = resource.getLocation();

        if (locationPath == null) {
            if (LocationUnavailablePolicy.THROW == locationUnavailablePolicy) {
                final String messageFormat = "the resource [{0}] does not have a location"; //$NON-NLS-1$
                final String message = MessageFormat.format(messageFormat, resource);
                throw new RuntimeException(message);
            }
            return null;
        }

        return locationPath.toOSString();
    }

    /**
     * <p>
     * Given an absolute location on the local file system, attempts to find a
     * workspace file resource that corresponds to that location.
     * <b>Important:</b> this method will not find resources that are linked or
     * that are the children of linked resources.
     * </p>
     *
     * <p>
     * This method will only return an existing resource. If the specified
     * location corresponds to a location inside a project in the workspace, but
     * no file resource currently exists at that location, <code>null</code> is
     * returned.
     * </p>
     *
     * <p>
     * This method will only return file resources. If there is not a file
     * resource at the specified location, <code>null</code> is returned (even
     * if there is a container resource at the location).
     * </p>
     *
     * @param location
     *        an absolute location on the local file system (must not be
     *        <code>null</code>)
     * @return an {@link IFile} that corresponds to the specified file system
     *         location, or <code>null</code> if the location did not map to any
     *         resource or if <code>mustExist</code> is <code>true</code> and
     *         the location did not map to an existing resource
     */
    public static IFile getFileForLocation(final String location) {
        return (IFile) getResourceForLocation(location, ResourceType.FILE, true);
    }

    /**
     * <p>
     * Given an absolute location on the local file system, attempts to find a
     * workspace file resource that corresponds to that location.
     * <b>Important:</b> this method will not find resources that are linked or
     * that are the children of linked resources.
     * </p>
     *
     * <p>
     * If the <code>mustExist</code> parameter is <code>false</code>, this
     * method could return a resource handle that corresponds to a non-existing
     * resource. If <code>mustExist</code> is <code>true</code>, a non-
     * <code>null</code> return value can be assumed to exist.
     * </p>
     *
     * <p>
     * This method will only return file resources. If there is not a file
     * resource at the specified location, <code>null</code> is returned (even
     * if there is a container resource at the location).
     * </p>
     *
     * @param location
     *        an absolute location on the local file system (must not be
     *        <code>null</code>) <code>null</code>)
     * @param mustExist
     *        if <code>true</code>, this method will only return an existing
     *        resource; if <code>false</code>, the returned resource may not
     *        actually exist in the workspace
     * @return an {@link IFile} that corresponds to the specified file system
     *         location, or <code>null</code> if the location did not map to any
     *         file resource or if <code>mustExist</code> is <code>true</code>
     *         and the location did not map to an existing resource
     */
    public static IFile getFileForLocation(final String location, final boolean mustExist) {
        return (IFile) getResourceForLocation(location, ResourceType.FILE, mustExist);
    }

    /**
     * <p>
     * Given an absolute location on the local file system, attempts to find a
     * workspace container resource that corresponds to that location.
     * <b>Important:</b> this method will not find resources that are linked or
     * that are the children of linked resources.
     * </p>
     *
     * <p>
     * This method will only return an existing resource. If the specified
     * location corresponds to a location inside a project in the workspace, but
     * no container resource currently exists at that location,
     * <code>null</code> is returned.
     * </p>
     *
     * <p>
     * This method will only return container resources. If there is not a
     * container resource at the specified location, <code>null</code> is
     * returned (even if there is a file resource at the location).
     * </p>
     *
     * @param location
     *        an absolute location on the local file system (must not be
     *        <code>null</code>)
     * @return an {@link IContainer} that corresponds to the specified file
     *         system location, or <code>null</code> if the location did not map
     *         to any resource or if <code>mustExist</code> is <code>true</code>
     *         and the location did not map to an existing resource
     */
    public static IContainer getContainerForLocation(final String location) {
        return (IContainer) getResourceForLocation(location, ResourceType.CONTAINER, true);
    }

    /**
     * <p>
     * Given an absolute location on the local file system, attempts to find a
     * workspace container resource that corresponds to that location.
     * <b>Important:</b> this method will not find resources that are linked or
     * that are the children of linked resources.
     * </p>
     *
     * <p>
     * If the <code>mustExist</code> parameter is <code>false</code>, this
     * method could return a resource handle that corresponds to a non-existing
     * resource. If <code>mustExist</code> is <code>true</code>, a non-
     * <code>null</code> return value can be assumed to exist.
     * </p>
     *
     * <p>
     * This method will only return container resources. If there is not a
     * container resource at the specified location, <code>null</code> is
     * returned (even if there is a file resource at the location).
     * </p>
     *
     * @param location
     *        an absolute location on the local file system (must not be
     *        <code>null</code>) <code>null</code>)
     * @param mustExist
     *        if <code>true</code>, this method will only return an existing
     *        resource; if <code>false</code>, the returned resource may not
     *        actually exist in the workspace
     * @return an {@link IContainer} that corresponds to the specified file
     *         system location, or <code>null</code> if the location did not map
     *         to any container resource or if <code>mustExist</code> is
     *         <code>true</code> and the location did not map to an existing
     *         resource
     */
    public static IContainer getContainerForLocation(final String location, final boolean mustExist) {
        return (IContainer) getResourceForLocation(location, ResourceType.CONTAINER, mustExist);
    }

    /**
     * <p>
     * Given an absolute location on the local file system, attempts to find a
     * workspace resource that corresponds to that location. <b>Important:</b>
     * this method will not find resources that are linked or that are the
     * children of linked resources.
     * </p>
     *
     * <p>
     * This method will only return an existing resource. If the specified
     * location corresponds to a location inside a project in the workspace, but
     * no resource currently exists at that location, <code>null</code> is
     * returned.
     * </p>
     *
     * <p>
     * This method should only be used when the desired type of resource is
     * unknown - for example, when processing user input. If the desired
     * resource type is known, call an overload that takes a
     * {@link ResourceType} instead.
     * </p>
     *
     * @param location
     *        an absolute location on the local file system (must not be
     *        <code>null</code>)
     * @return an {@link IResource} that corresponds to the specified file
     *         system location, or <code>null</code> if the location did not map
     *         to any existing resource
     */
    public static IResource getResourceForLocation(final String location) {
        return getResourceForLocation(location, ResourceType.ANY, true);
    }

    /**
     * <p>
     * Given an absolute location on the local file system, attempts to find a
     * workspace resource that corresponds to that location. <b>Important:</b>
     * this method will not find resources that are linked or that are the
     * children of linked resources.
     * </p>
     *
     * <p>
     * This method will only return an existing resource. If the specified
     * location corresponds to a location inside a project in the workspace, but
     * no resource currently exists at that location, <code>null</code> is
     * returned.
     * </p>
     *
     * <p>
     * The {@link ResourceType} parameter is used to determine what type of
     * resource (file or container) to look for. Whenever the desired resource
     * type is known, the appropriate {@link ResourceType} should be passed for
     * efficiency. If the desired resource type is not known,
     * {@link ResourceType#ANY} can be passed to indicate that any type of
     * returned resource is acceptable.
     * </p>
     *
     * @param location
     *        an absolute location on the local file system (must not be
     *        <code>null</code>)
     * @param resourceType
     *        determines what type of resource to look for (must not be
     *        <code>null</code>)
     * @return an {@link IResource} that corresponds to the specified file
     *         system location, or <code>null</code> if the location did not map
     *         to any existing resource
     */
    public static IResource getResourceForLocation(final String location, final ResourceType resourceType) {
        return getResourceForLocation(location, resourceType, true);
    }

    /**
     * <p>
     * Given an absolute location on the local file system, attempts to find a
     * workspace resource that corresponds to that location. <b>Important:</b>
     * this method will not find resources that are linked or that are the
     * children of linked resources.
     * </p>
     *
     * <p>
     * If the <code>mustExist</code> parameter is <code>false</code>, this
     * method could return a resource handle that corresponds to a non-existing
     * resource. If <code>mustExist</code> is <code>true</code>, a non-
     * <code>null</code> return value can be assumed to exist.
     * </p>
     *
     * <p>
     * The {@link ResourceType} parameter is used to determine what type of
     * resource (file or container) to look for. Whenever the desired resource
     * type is known, the appropriate {@link ResourceType} should be passed for
     * efficiency. If the desired resource type is not known,
     * {@link ResourceType#ANY} can be passed to indicate that any type of
     * returned resource is acceptable. However, if <code>mustExist</code> is
     * <code>false</code>, {@link ResourceType#ANY} is illegal to pass since
     * this is ambiguous.
     * </p>
     *
     * @param location
     *        an absolute location on the local file system (must not be
     *        <code>null</code>)
     * @param resourceType
     *        determines what type of resource to look for (must not be
     *        <code>null</code>)
     * @param mustExist
     *        if <code>true</code>, this method will only return an existing
     *        resource; if <code>false</code>, the returned resource may not
     *        actually exist in the workspace
     * @return an {@link IResource} that corresponds to the specified file
     *         system location, or <code>null</code> if the location did not map
     *         to any resource or if <code>mustExist</code> is <code>true</code>
     *         and the location did not map to an existing resource
     */
    public static IResource getResourceForLocation(
        final String location,
        final ResourceType resourceType,
        final boolean mustExist) {
        Check.notNull(location, "location"); //$NON-NLS-1$
        Check.notNull(resourceType, "resourceType"); //$NON-NLS-1$

        final IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();

        final IPath path = new Path(location);

        return resourceType.getResourceForLocation(path, root, mustExist);
    }

    /**
     * <p>
     * Given an absolute location on the local file system, attempts to find all
     * workspace resource that corresponds to that location. <b>Important:</b>
     * this method will not find resources that are linked or that are the
     * children of linked resources.
     * </p>
     *
     * <p>
     * If the <code>mustExist</code> parameter is <code>false</code>, this
     * method could return a resource handle that corresponds to a non-existing
     * resource. If <code>mustExist</code> is <code>true</code>, a non-
     * <code>null</code> return value can be assumed to exist.
     * </p>
     *
     * <p>
     * The {@link ResourceType} parameter is used to determine what type of
     * resource (file or container) to look for. Whenever the desired resource
     * type is known, the appropriate {@link ResourceType} should be passed for
     * efficiency. If the desired resource type is not known,
     * {@link ResourceType#ANY} can be passed to indicate that any type of
     * returned resource is acceptable. However, if <code>mustExist</code> is
     * <code>false</code>, {@link ResourceType#ANY} is illegal to pass since
     * this is ambiguous.
     * </p>
     *
     * @param location
     *        an absolute location on the local file system (must not be
     *        <code>null</code>)
     * @param resourceType
     *        determines what type of resource to look for (must not be
     *        <code>null</code>)
     * @param mustExist
     *        if <code>true</code>, this method will only return an existing
     *        resource; if <code>false</code>, the returned resource may not
     *        actually exist in the workspace
     * @return all {@link IResource}s that corresponds to the specified file
     *         system location, or <code>null</code> if the location did not map
     *         to any resource or if <code>mustExist</code> is <code>true</code>
     *         and the location did not map to any existing resources
     */
    public static IResource[] getAllResourcesForLocation(
        final String location,
        final ResourceType resourceType,
        final boolean mustExist) {
        Check.notNull(location, "location"); //$NON-NLS-1$
        Check.notNull(resourceType, "resourceType"); //$NON-NLS-1$

        final IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();

        final IPath path = new Path(location);

        return resourceType.getAllResourcesForLocation(path, root, mustExist);
    }

    public static LocationInfo getLocationInfo(final String location) {
        Check.notNull(location, "location"); //$NON-NLS-1$

        final IPath path = new Path(location);

        return new LocationInfo(path);
    }

    /**
     * Selects a subset of the input array of {@link IResource}s using an
     * {@link ResourceFilter}.
     *
     * @param resources
     *        the input array of {@link IResource}s - must not be
     *        <code>null</code>, and must not contain any <code>null</code>
     *        elements
     * @param filter
     *        an {@link ResourceFilter} used to select the returned resources
     *        (must not be <code>null</code>)
     * @return an array of {@link IResource}s which is never <code>null</code>
     *         and is a subset of the resources passed to this method
     */
    public static IResource[] filter(final IResource[] resources, final ResourceFilter filter) {
        Check.notNull(resources, "resources"); //$NON-NLS-1$
        Check.notNull(filter, "filter"); //$NON-NLS-1$

        final List results = new ArrayList();

        for (int i = 0; i < resources.length; i++) {
            if (resources[i] == null) {
                throw new IllegalArgumentException("element " + i + " in the passed IResource array was null"); //$NON-NLS-1$ //$NON-NLS-2$
            }

            if (filter.filter(resources[i]).isAccept()) {
                results.add(resources[i]);
            }
        }

        return (IResource[]) results.toArray(new IResource[results.size()]);
    }

    /**
     * Equivalent to calling {@link IContainer#members()} on the specified
     * container, and then running all members through a {@link ResourceFilter}.
     * Any members that are accepted by the filter will be returned from this
     * method.
     *
     * @param container
     *        the container to call {@link IContainer#members(int)} on (must not
     *        be <code>null</code>)
     * @param resourceFilter
     *        the {@link ResourceFilter} to use to filter the members (must not
     *        be <code>null</code>)
     * @return the filtered members of the container (never <code>null</code>)
     * @throws CoreException
     */
    public static IResource[] getFilteredMembers(final IContainer container, final ResourceFilter resourceFilter)
        throws CoreException {
        return getFilteredMembers(container, IResource.NONE, resourceFilter);
    }

    /**
     * Equivalent to calling {@link IContainer#members(int)} on the specified
     * container, and then running all members through a {@link ResourceFilter}.
     * Any members that are accepted by the filter will be returned from this
     * method.
     *
     * @param container
     *        the container to call {@link IContainer#members(int)} on (must not
     *        be <code>null</code>)
     * @param memberFlags
     *        the flags to pass to {@link IContainer#members(int)}
     * @param resourceFilter
     *        the {@link ResourceFilter} to use to filter the members (must not
     *        be <code>null</code>)
     * @return the filtered members of the container (never <code>null</code>)
     * @throws CoreException
     */
    public static IResource[] getFilteredMembers(
        final IContainer container,
        final int memberFlags,
        final ResourceFilter resourceFilter) throws CoreException {
        Check.notNull(container, "container"); //$NON-NLS-1$
        Check.notNull(resourceFilter, "resourceFilter"); //$NON-NLS-1$

        final IResource[] allMembers = container.members(memberFlags);

        final List filteredMembers = new ArrayList();

        for (int i = 0; i < allMembers.length; i++) {
            if (resourceFilter.filter(allMembers[i]).isAccept()) {
                filteredMembers.add(allMembers[i]);
            }
        }

        return (IResource[]) filteredMembers.toArray(new IResource[filteredMembers.size()]);
    }
}
