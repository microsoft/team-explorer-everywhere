// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.framework.resources;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.IPath;

import com.microsoft.tfs.util.Check;

/**
 * <p>
 * {@link ResourceType} is a typesafe enum class that defines types of workspace
 * resources.
 * </p>
 *
 * <p>
 * File resources (represented by {@link #FILE}) correspond to
 * {@link IResource#FILE} and are modeled by {@link IFile} objects. Container
 * resources (represented by {@link #CONTAINER}) correspond to
 * {@link IResource#ROOT}, {@link IResource#PROJECT}, or
 * {@link IResource#FOLDER} and are modeled by {@link IWorkspaceRoot},
 * {@link IProject}, or {@link IFolder} (all {@link IContainer}) objects.
 * </p>
 *
 * <p>
 * The special resource type {@link #ANY} is used to indicate a "don't care" to
 * API methods that take a {@link ResourceType}. The caller is saying that the
 * type of the resource returned by the API is not important.
 * </p>
 */
public abstract class ResourceType {
    /**
     * Obtains the corresponding {@link ResourceType} for the specified Java
     * {@link File} object. If the {@link File} is a directory (
     * {@link File#isDirectory()} returns <code>true</code>), {@link #CONTAINER}
     * is returned. Otherwise, {@link #FILE} is returned.
     *
     * @param file
     *        the Java {@link File} to get a {@link ResourceType} for (must not
     *        be <code>null</code>)
     * @return a {@link ResourceType} as described above (never
     *         <code>null</code>)
     */
    public static ResourceType fromFile(final File file) {
        Check.notNull(file, "file"); //$NON-NLS-1$

        if (file.isDirectory()) {
            return CONTAINER;
        } else {
            return FILE;
        }
    }

    /**
     * A {@link ResourceType} that represents the file type of workspace
     * resources. This type of resources corresponds to the
     * {@link IResource#FILE} constant. The workspace models them as
     * {@link IFile} objects.
     */
    public static final ResourceType FILE = new FileResourceType();

    /**
     * A {@link ResourceType} that represents the container type of workspace
     * resources. This type of resources corresponds to the
     * {@link IResource#ROOT}, {@link IResource#PROJECT}, and
     * {@link IResource#FOLDER} constants. The workspace models them as
     * {@link IWorkspaceRoot}, {@link IProject}, and {@link IFolder} objects
     * that all share a common {@link IContainer} interface.
     */
    public static final ResourceType CONTAINER = new ContainerResourceType();

    /**
     * A special {@link ResourceType} that indicates to an API method that the
     * type of resource operated on or returned by that method is not important.
     */
    public static final ResourceType ANY = new AnyResourceType();

    private final String type;

    private ResourceType(final String type) {
        this.type = type;
    }

    /**
     * <p>
     * Obtains a resource of this {@link ResourceType} that corresponds to the
     * specified location.
     * </p>
     *
     * <p>
     * If <code>mustExist</code> is <code>true</code>, this method will only
     * return a resource that {@link IResource#exists()} returns
     * <code>true</code> from. If <code>mustExist</code> is <code>true</code>
     * and the corresponding resource does not exist, this method returns
     * <code>null</code>. If the specified location does not correspond to
     * anything in the workspace, this method returns <code>null</code>.
     * </p>
     *
     * @param location
     *        the location to get a resource for (must not be <code>null</code>)
     * @param root
     *        the workspace root (must not be <code>null</code>)
     * @param mustExist
     *        <code>true</code> to only return a resource that exists,
     *        <code>false</code> if returning a non-existing resource is OK
     * @return an {@link IResource} as describe above or <code>null</code> if no
     *         matching resource exists
     */
    abstract IResource getResourceForLocation(IPath location, IWorkspaceRoot root, boolean mustExist);

    /**
     * <p>
     * Obtains all resource of this {@link ResourceType} that corresponds to the
     * specified location.
     * </p>
     *
     * <p>
     * If <code>mustExist</code> is <code>true</code>, this method will only
     * return a resource that {@link IResource#exists()} returns
     * <code>true</code> from. If <code>mustExist</code> is <code>true</code>
     * and the corresponding resource does not exist, this method returns
     * <code>null</code>. If the specified location does not correspond to
     * anything in the workspace, this method returns <code>null</code>.
     * </p>
     *
     * @param location
     *        the location to get a resource for (must not be <code>null</code>)
     * @param root
     *        the workspace root (must not be <code>null</code>)
     * @param mustExist
     *        <code>true</code> to only return resources that exists,
     *        <code>false</code> if returning a non-existing resource is OK
     * @return all {@link IResource}s as describe above or an empty array if no
     *         matching resource exists
     */
    abstract IResource[] getAllResourcesForLocation(IPath path, IWorkspaceRoot root, boolean mustExist);

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return type;
    }

    private static class FileResourceType extends ResourceType {
        public FileResourceType() {
            super("file"); //$NON-NLS-1$
        }

        /*
         * (non-Javadoc)
         *
         * @see
         * com.microsoft.tfs.client.common.framework.resources.ResourceType#
         * getResourceForLocation(org.eclipse.core.runtime.IPath,
         * org.eclipse.core.resources.IWorkspaceRoot, boolean)
         */
        @Override
        public IResource getResourceForLocation(
            final IPath location,
            final IWorkspaceRoot root,
            final boolean mustExist) {
            IFile file = root.getFileForLocation(location);
            if (mustExist && file != null && !file.exists()) {
                file = null;
            }
            return file;
        }

        /*
         * (non-Javadoc)
         *
         * @see
         * com.microsoft.tfs.client.common.framework.resources.ResourceType#
         * getAllResourcesForLocation(org.eclipse.core.runtime.IPath,
         * org.eclipse.core.resources.IWorkspaceRoot, boolean)
         */
        @Override
        public IResource[] getAllResourcesForLocation(
            final IPath location,
            final IWorkspaceRoot root,
            final boolean mustExist) {
            IFile[] files = root.findFilesForLocation(location);

            if (mustExist && files != null) {
                final List<IFile> existingFiles = new ArrayList<IFile>();

                for (int i = 0; i < files.length; i++) {
                    if (files[i].exists()) {
                        existingFiles.add(files[i]);
                    }
                }

                files = existingFiles.toArray(new IFile[existingFiles.size()]);
            }

            return files;
        }
    }

    private static class ContainerResourceType extends ResourceType {
        public ContainerResourceType() {
            super("container"); //$NON-NLS-1$
        }

        /*
         * (non-Javadoc)
         *
         * @see com.microsoft.tfs.client.common.resources.ResourceType#
         * getResourceForLocation(org.eclipse.core.runtime.IPath,
         * org.eclipse.core.resources.IWorkspaceRoot, boolean)
         */
        @Override
        public IResource getResourceForLocation(
            final IPath location,
            final IWorkspaceRoot root,
            final boolean mustExist) {
            IContainer container = root.getContainerForLocation(location);
            if (mustExist && container != null && !container.exists()) {
                container = null;
            }
            return container;
        }

        /*
         * (non-Javadoc)
         *
         * @see
         * com.microsoft.tfs.client.common.framework.resources.ResourceType#
         * getAllResourcesForLocation(org.eclipse.core.runtime.IPath,
         * org.eclipse.core.resources.IWorkspaceRoot, boolean)
         */
        @Override
        public IResource[] getAllResourcesForLocation(
            final IPath location,
            final IWorkspaceRoot root,
            final boolean mustExist) {
            IContainer[] containers = root.findContainersForLocation(location);

            if (mustExist && containers != null) {
                final List<IContainer> existingContainers = new ArrayList<IContainer>();

                for (int i = 0; i < containers.length; i++) {
                    if (containers[i].exists()) {
                        existingContainers.add(containers[i]);
                    }
                }

                containers = existingContainers.toArray(new IContainer[existingContainers.size()]);
            }

            return containers;
        }
    }

    private static class AnyResourceType extends ResourceType {
        public AnyResourceType() {
            super("any"); //$NON-NLS-1$
        }

        /*
         * (non-Javadoc)
         *
         * @see com.microsoft.tfs.client.common.ui.resources.ResourceType#
         * getResourceForLocation(org.eclipse.core.runtime.IPath,
         * org.eclipse.core.resources.IWorkspaceRoot, boolean)
         */
        @Override
        IResource getResourceForLocation(final IPath location, final IWorkspaceRoot root, final boolean mustExist) {
            if (!mustExist) {
                throw new IllegalArgumentException("mustExist==false is not compatible with ResourceType.ANY"); //$NON-NLS-1$
            }

            final IResource resource = FILE.getResourceForLocation(location, root, true);
            if (resource != null) {
                return resource;
            }
            return CONTAINER.getResourceForLocation(location, root, true);
        }

        /*
         * (non-Javadoc)
         *
         * @see
         * com.microsoft.tfs.client.common.framework.resources.ResourceType#
         * getAllResourcesForLocation(org.eclipse.core.runtime.IPath,
         * org.eclipse.core.resources.IWorkspaceRoot, boolean)
         */
        @Override
        public IResource[] getAllResourcesForLocation(
            final IPath location,
            final IWorkspaceRoot root,
            final boolean mustExist) {
            if (!mustExist) {
                throw new IllegalArgumentException("mustExist==false is not compatible with ResourceType.ANY"); //$NON-NLS-1$
            }

            final List<IResource> resources = new ArrayList<IResource>();

            resources.addAll(Arrays.asList(FILE.getAllResourcesForLocation(location, root, true)));
            resources.addAll(Arrays.asList(CONTAINER.getAllResourcesForLocation(location, root, true)));

            return resources.toArray(new IResource[resources.size()]);
        }
    }
}
