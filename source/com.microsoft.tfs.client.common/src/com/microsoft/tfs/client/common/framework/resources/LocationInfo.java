// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.framework.resources;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;

import com.microsoft.tfs.client.common.framework.resources.compatibility.LinkedResources;
import com.microsoft.tfs.util.Check;

public class LocationInfo {
    private final IPath location;
    private final IFile file;
    private final IContainer container;
    private final IFile[] files;
    private final IContainer[] containers;

    public LocationInfo(final IPath location) {
        Check.notNull(location, "location"); //$NON-NLS-1$
        this.location = location;

        final IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();

        file = root.getFileForLocation(location);
        container = root.getContainerForLocation(location);

        files = root.findFilesForLocation(location);
        containers = root.findContainersForLocation(location);
    }

    public IPath getLocation() {
        return location;
    }

    public boolean isWorkspaceRoot() {
        return container != null && container.getType() == IResource.ROOT;
    }

    public boolean isProject() {
        return container != null && container.getType() == IResource.PROJECT && container.exists();
    }

    public IProject getProject() {
        return (IProject) container;
    }

    public boolean isPotentialSubProjectResource() {
        return getSubProjectResource(false) != null;
    }

    public boolean isExistingSubProjectProjectResource() {
        return getSubProjectResource(true) != null;
    }

    public IResource getSubProjectResource() {
        return getSubProjectResource(true);
    }

    public IResource getSubProjectResource(final boolean mustExist) {
        if (file != null) {
            if (!mustExist || file.exists()) {
                return file;
            }
        }

        if (container != null && container.getType() != IResource.ROOT && container.getType() != IResource.PROJECT) {
            if (!mustExist || container.exists()) {
                return container;
            }
        }

        return null;
    }

    public boolean isPotentialLinkTarget() {
        return getLinkedResources(false).length > 0;
    }

    public boolean isExistingLinkTarget() {
        return getLinkedResources(true).length > 0;
    }

    public IResource[] getLinkedResources(final boolean mustExist) {
        final List linkedResources = new ArrayList();

        linkedResources.addAll(Arrays.asList(files));
        linkedResources.addAll(Arrays.asList(containers));

        for (final Iterator it = linkedResources.iterator(); it.hasNext();) {
            final IResource resource = (IResource) it.next();

            if (mustExist && !resource.exists()) {
                it.remove();
                continue;
            }

            if (!LinkedResources.isLinked(resource)) {
                it.remove();
                continue;
            }
        }

        return (IResource[]) linkedResources.toArray(new IResource[linkedResources.size()]);
    }
}
