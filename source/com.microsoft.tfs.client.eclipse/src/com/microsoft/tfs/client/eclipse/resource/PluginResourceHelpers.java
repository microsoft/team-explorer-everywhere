// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse.resource;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;

import com.microsoft.tfs.client.common.framework.resources.LocationUnavailablePolicy;
import com.microsoft.tfs.client.common.framework.resources.Resources;
import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.common.repository.cache.pendingchange.PendingChangeCache;
import com.microsoft.tfs.client.common.vc.TypedItemSpec;
import com.microsoft.tfs.client.eclipse.TFSEclipseClientPlugin;
import com.microsoft.tfs.client.eclipse.repository.ResourceRepositoryMap;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ItemType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PendingChange;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.RecursionType;
import com.microsoft.tfs.util.Check;

/**
 * Utility methods for Eclipse {@link IResource}s. Methods exist to map
 * resources to their affected repositories and pending changes and to convert
 * {@link IResource}s into {@link TypedItemSpec}s.
 *
 * @threadsafety unknown
 */
public class PluginResourceHelpers {
    private final static Log log = LogFactory.getLog(PluginResourceHelpers.class);

    public static ResourceRepositoryMap mapResources(final IResource[] resources) {
        final Map<IProject, List<IResource>> projectsToResources = new HashMap<IProject, List<IResource>>();

        for (int i = 0; i < resources.length; i++) {
            final IProject project = resources[i].getProject();

            List<IResource> list = projectsToResources.get(project);
            if (list == null) {
                list = new ArrayList<IResource>();
                projectsToResources.put(project, list);
            }
            list.add(resources[i]);
        }

        final IProject[] projects =
            projectsToResources.keySet().toArray(new IProject[projectsToResources.keySet().size()]);

        final Map<TFSRepository, List<IProject>> repositoriesToProjects = new HashMap<TFSRepository, List<IProject>>();

        for (int i = 0; i < projects.length; i++) {
            final TFSRepository repository =
                TFSEclipseClientPlugin.getDefault().getProjectManager().getRepository(projects[i]);

            if (repository == null) {
                PluginResourceHelpers.log.warn(
                    MessageFormat.format("No TFS Repository for project {0}", projects[i].getName())); //$NON-NLS-1$
                continue;
            }

            List<IProject> list = repositoriesToProjects.get(repository);
            if (list == null) {
                list = new ArrayList<IProject>();
                repositoriesToProjects.put(repository, list);
            }
            list.add(projects[i]);
        }

        final TFSRepository[] repositories =
            repositoriesToProjects.keySet().toArray(new TFSRepository[repositoriesToProjects.keySet().size()]);

        final ResourceRepositoryMap resultMap = new ResourceRepositoryMap();

        for (int i = 0; i < repositories.length; i++) {
            final IProject[] projectsForRepository =
                repositoriesToProjects.get(repositories[i]).toArray(new IProject[] {});

            final List<IResource> resourcesForRepositoryList = new ArrayList<IResource>();
            for (int j = 0; j < projectsForRepository.length; j++) {
                resourcesForRepositoryList.addAll(projectsToResources.get(projectsForRepository[j]));
            }
            final IResource[] resourcesForRepository =
                resourcesForRepositoryList.toArray(new IResource[resourcesForRepositoryList.size()]);

            resultMap.addMappings(repositories[i], resourcesForRepository);
        }

        return resultMap;
    }

    public static int countPendingChangesForResource(final IResource resource, final TFSRepository repository) {
        return PluginResourceHelpers.countPendingChangesForResource(resource, repository, true);
    }

    public static int countPendingChangesForResource(
        final IResource resource,
        final TFSRepository repository,
        final boolean includeChildPendingChanges) {
        final String location = Resources.getLocation(resource, LocationUnavailablePolicy.THROW);

        final PendingChangeCache pendingChangesCache = repository.getPendingChangeCache();

        if (resource.getType() == IResource.FILE || !includeChildPendingChanges) {
            if (pendingChangesCache.getPendingChangeByLocalPath(location) != null) {
                return 1;
            }

            return 0;
        } else {
            return pendingChangesCache.getPendingChangesByLocalPathRecursive(location).length;
        }
    }

    public static PendingChange[] pendingChangesForResource(final IResource resource, final TFSRepository repository) {
        return PluginResourceHelpers.pendingChangesForResource(resource, repository, true);
    }

    public static PendingChange[] pendingChangesForResource(
        final IResource resource,
        final TFSRepository repository,
        final boolean includeChildPendingChanges) {
        final String location = Resources.getLocation(resource, LocationUnavailablePolicy.THROW);

        final PendingChangeCache pendingChangesCache = repository.getPendingChangeCache();

        if (resource.getType() == IResource.FILE || !includeChildPendingChanges) {
            final PendingChange change = pendingChangesCache.getPendingChangeByLocalPath(location);
            if (change != null) {
                return new PendingChange[] {
                    change
                };
            }
            return new PendingChange[] {};
        } else {
            final PendingChange[] changes = pendingChangesCache.getPendingChangesByLocalPathRecursive(location);
            return changes;
        }
    }

    public static TypedItemSpec[] typedItemSpecsForResources(
        final IResource[] resources,
        final boolean recursiveIfAppropriate,
        final LocationUnavailablePolicy locationUnavailablePolicy) {
        Check.notNull(resources, "resources"); //$NON-NLS-1$
        Check.notNull(locationUnavailablePolicy, "locationUnavailablePolicy"); //$NON-NLS-1$

        final List<TypedItemSpec> itemSpecs = new ArrayList<TypedItemSpec>();
        for (int i = 0; i < resources.length; i++) {
            final TypedItemSpec itemSpec = PluginResourceHelpers.typedItemSpecForResource(
                resources[i],
                recursiveIfAppropriate,
                locationUnavailablePolicy);
            if (itemSpec != null) {
                itemSpecs.add(itemSpec);
            }
        }

        return itemSpecs.toArray(new TypedItemSpec[itemSpecs.size()]);
    }

    public static TypedItemSpec typedItemSpecForResource(
        final IResource resource,
        final boolean recursiveIfAppropriate,
        final LocationUnavailablePolicy locationUnavailablePolicy) {
        Check.notNull(resource, "resource"); //$NON-NLS-1$
        Check.notNull(locationUnavailablePolicy, "locationUnavailablePolicy"); //$NON-NLS-1$

        final int type = resource.getType();

        if (type == IResource.ROOT) {
            throw new IllegalArgumentException("Can't create typed item spec for workspace root"); //$NON-NLS-1$
        }

        final String location = Resources.getLocation(resource, locationUnavailablePolicy);
        if (location == null) {
            return null;
        }

        RecursionType recursionType;
        if (recursiveIfAppropriate && type != IResource.FILE) {
            recursionType = RecursionType.FULL;
        } else {
            recursionType = RecursionType.NONE;
        }

        return new TypedItemSpec(location, recursionType, type == IResource.FILE ? ItemType.FILE : ItemType.FOLDER);
    }
}
