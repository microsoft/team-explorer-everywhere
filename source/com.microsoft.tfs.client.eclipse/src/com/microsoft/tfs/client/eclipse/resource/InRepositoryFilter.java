// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse.resource;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;

import com.microsoft.tfs.client.common.framework.resources.filter.ResourceFilter;
import com.microsoft.tfs.client.common.framework.resources.filter.ResourceFilterResult;
import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.eclipse.TFSEclipseClientPlugin;
import com.microsoft.tfs.client.eclipse.resourcedata.ResourceDataManager;
import com.microsoft.tfs.core.exceptions.InputValidationException;
import com.microsoft.tfs.util.Check;

/**
 * Accepts {@link IResource}s which are files or folders which are in the TFS
 * repository (folders must have a non-cloak mapping, files must exist in the
 * item cache), rejects all others. This filter is non-recursive.
 *
 * @threadsafety thread-compatible
 */
public class InRepositoryFilter extends ResourceFilter {
    private final RepositoryUnavailablePolicy repositoryUnavailablePolicy;

    public InRepositoryFilter(final RepositoryUnavailablePolicy repositoryUnavailablePolicy) {
        Check.notNull(repositoryUnavailablePolicy, "repositoryUnavailablePolicy"); //$NON-NLS-1$

        this.repositoryUnavailablePolicy = repositoryUnavailablePolicy;
    }

    @Override
    public ResourceFilterResult filter(final IResource resource, final int flags) {
        final IProject project = resource.getProject();

        /* Project is null for workspace root */
        if (project == null) {
            return REJECT;
        }

        final TFSRepository repository =
            TFSEclipseClientPlugin.getDefault().getProjectManager().getRepository(resource.getProject());

        if (repository == null) {
            return repositoryUnavailablePolicy.acceptResourceWithNoRepository(resource) ? ACCEPT : REJECT;
        }

        if (resource.getType() == IResource.FOLDER || resource.getType() == IResource.PROJECT) {
            try {
                return (repository.getWorkspace().isLocalPathMapped(resource.getLocation().toOSString())) ? ACCEPT
                    : REJECT;
            } catch (final InputValidationException e) {
                /*
                 * Thrown when the resource path contains characters that aren't
                 * allowed in TFS server paths (dollar sign at beginning of path
                 * components, etc.).
                 */
                return REJECT;
            }
        } else if (resource.getType() == IResource.FILE) {
            final ResourceDataManager resourceDataManager =
                TFSEclipseClientPlugin.getDefault().getResourceDataManager();
            return (!resourceDataManager.hasCompletedRefresh(resource.getProject())
                || resourceDataManager.hasResourceData(resource)) ? ACCEPT : REJECT;
        } else {
            /*
             * Unknown resource type, can't figure out if it's in the
             * repository.
             */
            return REJECT;
        }
    }
}
