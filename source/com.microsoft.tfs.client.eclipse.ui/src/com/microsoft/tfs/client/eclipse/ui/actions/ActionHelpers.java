// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse.ui.actions;

import java.text.MessageFormat;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Shell;

import com.microsoft.tfs.client.common.framework.resources.Resources;
import com.microsoft.tfs.client.common.framework.resources.filter.ResourceFilter;
import com.microsoft.tfs.client.common.framework.resources.filter.ResourceFilters;
import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.common.ui.framework.helper.MessageBoxHelpers;
import com.microsoft.tfs.client.common.ui.framework.helper.SelectionUtils;
import com.microsoft.tfs.client.common.ui.teamexplorer.helpers.PendingChangesHelpers;
import com.microsoft.tfs.client.common.util.Adapters;
import com.microsoft.tfs.client.eclipse.TFSEclipseClientPlugin;
import com.microsoft.tfs.client.eclipse.repository.ResourceRepositoryMap;
import com.microsoft.tfs.client.eclipse.resource.PluginResourceHelpers;
import com.microsoft.tfs.client.eclipse.ui.Messages;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PendingChange;
import com.microsoft.tfs.jni.FileSystemUtils;
import com.microsoft.tfs.util.Check;

public abstract class ActionHelpers {
    private static final Log log = LogFactory.getLog(ActionHelpers.class);

    /**
     * Calculates this action's current enablement by filtering the current
     * array of resources, returning true if <em>any item</em> is accepted by
     * the filter.
     *
     * @param resources
     *        the resources to filter (must not be <code>null</code>)
     * @param filter
     *        the filter to use (must not be <code>null</code>)
     * @return true if this action should be enabled (the filter accepts
     *         <em>any item</em> in the selection), false if it should not be
     *         enabled (the filter rejects <em>all items</em> in the selection)
     */
    public static boolean filterAcceptsAnyResource(final IResource[] resources, final ResourceFilter filter) {
        for (int i = 0; i < resources.length; i++) {
            if (resources[i] == null) {
                continue;
            }

            if (filter.filter(resources[i]).isAccept()) {
                return true;
            }
        }

        return false;
    }

    /**
     * Calculates this action's current enablement by filtering the current
     * selection, returning true if <em>any item</em> is accepted by the filter.
     *
     * @param selection
     *        the selection to filter (must not be <code>null</code>)
     * @param filter
     *        the filter to use (must not be <code>null</code>)
     * @return true if this action should be enabled (the filter accepts
     *         <em>any item</em> in the selection), false if it should not be
     *         enabled (the filter rejects <em>all items</em> in the selection)
     */
    public static boolean filterAcceptsAnyResource(final ISelection selection, final ResourceFilter filter) {
        if (selection instanceof IStructuredSelection == false) {
            return false;
        }

        final IStructuredSelection ss = (IStructuredSelection) selection;

        /*
         * Adapt each item as we encounter it, so we can filter it directly, and
         * possibly return early in a large selection without adapting every
         * element.
         */

        @SuppressWarnings("rawtypes")
        final Iterator it;

        for (it = ss.iterator(); it.hasNext();) {
            final IResource resource = (IResource) Adapters.getAdapter(it.next(), IResource.class);

            if (resource == null) {
                continue;
            }

            if (filter.filter(resource).isAccept()) {
                return true;
            }
        }

        return false;
    }

    /**
     * <p>
     * Adapts the current selection to a set of different classes which are
     * returned in an {@link AdaptedSelectionInfo}:
     * </p>
     * <p>
     * <ul>
     * <li>The {@link IResource}s that matched the current selection</li>
     * <li>The {@link TFSRepository}s that contain the matched resources</li>
     * <li>The {@link PendingChange}s that belong to the matched resources</li>
     * </ul>
     * </p>
     * <p>
     * Pending change information may be excluded from the results to improve
     * execution speed of this method.
     * </p>
     *
     * @param selection
     *        the selection to adapt (must not be <code>null</code>)
     * @param resourceFilter
     *        the {@link ResourceFilter} to use for filtering {@link IResource}s
     *        (must not be <code>null</code>)
     * @param includePendingChanges
     *        if true, pending change information is included in the info, if
     *        false the pending changes in the info will always be empty
     * @return a new {@link AdaptedSelectionInfo} with the resources that
     *         correspond to this object's current {@link ISelection}
     */
    public static AdaptedSelectionInfo adaptSelectionToStandardResources(
        final ISelection selection,
        final ResourceFilter resourceFilter,
        final boolean includePendingChanges) {
        Check.notNull(selection, "selection"); //$NON-NLS-1$
        Check.notNull(resourceFilter, "resourceFilter"); //$NON-NLS-1$

        final long start = System.currentTimeMillis();

        IResource[] resources = (IResource[]) SelectionUtils.adaptSelectionToArray(selection, IResource.class);
        resources = Resources.filter(resources, resourceFilter);

        final ResourceRepositoryMap resourceRepositoryMap = PluginResourceHelpers.mapResources(resources);

        final AdaptedSelectionInfo info = new AdaptedSelectionInfo(
            resources,
            resourceRepositoryMap.getRepositories(),
            (includePendingChanges) ? resourceRepositoryMap.getPendingChangesMap().getAllChanges()
                : new PendingChange[0]);

        final String messageFormat = "adapt info took {0} ms"; //$NON-NLS-1$
        final String message = MessageFormat.format(messageFormat, (System.currentTimeMillis() - start));
        log.trace(message);
        return info;
    }

    /**
     * Checks that the given {@link EclipsePluginAction.AdaptedSelectionInfo}
     * contains one or more resources contained in exactly one repository which
     * is online, showing an error message if these conditions are not met.
     *
     * @param info
     *        the {@link EclipsePluginAction.AdaptedSelectionInfo} to check
     *        (must not be <code>null</code>)
     * @param shell
     *        the shell to use to display error dialogs (must not be
     *        <code>null</code>)
     * @return true if the {@link EclipsePluginAction.AdaptedSelectionInfo}
     *         contains one or more resources and exactly one repository, false
     *         otherwise (an error was shown to the user)
     */
    public static boolean ensureNonZeroResourceCountAndSingleRepository(
        final AdaptedSelectionInfo info,
        final Shell shell) {
        Check.notNull(info, "info"); //$NON-NLS-1$
        Check.notNull(shell, "shell"); //$NON-NLS-1$

        if (info.getResources().length == 0) {
            MessageBoxHelpers.messageBox(
                shell,
                Messages.getString("ActionHelpers.NoEligibleResourceDialogTitle"), //$NON-NLS-1$
                Messages.getString("ActionHelpers.NoEligibleResourceDialogText")); //$NON-NLS-1$

            return false;
        }

        if (info.getRepositories().length == 0) {
            MessageBoxHelpers.messageBox(
                shell,
                Messages.getString("ActionHelpers.NoTfsRepositoryDialogTitle"), //$NON-NLS-1$
                Messages.getString("ActionHelpers.NoTfsRepositoryDialogText")); //$NON-NLS-1$

            return false;
        }

        if (info.getRepositories().length > 1) {
            MessageBoxHelpers.messageBox(
                shell,
                Messages.getString("ActionHelpers.MultTfsRepositoriesDialogTitle"), //$NON-NLS-1$
                Messages.getString("ActionHelpers.MultTfsRepositoriesDialogText")); //$NON-NLS-1$

            return false;
        }

        // TODO check info.getRepositories()[0].isOffline() and error about it

        return true;
    }

    /**
     * Checks that the resources in the given
     * {@link EclipsePluginAction.AdaptedSelectionInfo} reside in exactly one
     * {@link IProject}.
     *
     * @param info
     *        the {@link EclipsePluginAction.AdaptedSelectionInfo} to check
     *        (must not be <code>null</code>)
     * @param shell
     *        the shell to use to display error dialogs (must not be
     *        <code>null</code>)
     * @return true if the {@link EclipsePluginAction.AdaptedSelectionInfo}
     *         contains resources from exactly one {@link IProject}, false
     *         otherwise (an error was shown to the user)
     */
    public static boolean ensureSingleProject(final AdaptedSelectionInfo info, final Shell shell) {
        Check.notNull(info, "info"); //$NON-NLS-1$
        Check.notNull(shell, "shell"); //$NON-NLS-1$

        final IResource[] allResources = info.getResources();

        if (allResources.length == 0) {
            MessageBoxHelpers.messageBox(
                shell,
                Messages.getString("ActionHelpers.NoEligibleResourceDialogTitle"), //$NON-NLS-1$
                Messages.getString("ActionHelpers.NoEligibleResourceDialogText")); //$NON-NLS-1$

            return false;
        }

        /*
         * The workspace root has a null project and we want to error about that
         * case later.
         */
        if (allResources.length == 1 && allResources[0].getProject() != null) {
            return true;
        }

        final IProject firstProject = allResources[0].getProject();

        if (firstProject == null) {
            MessageBoxHelpers.messageBox(
                shell,
                Messages.getString("ActionHelpers.NoEligibleResourceDialogTitle"), //$NON-NLS-1$
                Messages.getString("ActionHelpers.WorkspaceRootCannotBeUsed")); //$NON-NLS-1$
        }

        // Start at the second item
        for (int i = 1; i < allResources.length; i++) {
            if (firstProject.equals(allResources[i].getProject()) == false) {
                MessageBoxHelpers.messageBox(
                    shell,
                    Messages.getString("ActionHelpers.MultProjectsDialogTitle"), //$NON-NLS-1$
                    Messages.getString("ActionHelpers.MultProjectsDialogText")); //$NON-NLS-1$

                return false;
            }
        }

        return true;
    }

    /**
     * Returns the {@link IProject}s that are contained in this selection.
     *
     * @param selection
     *        The selected resources (not <code>null</code>)
     * @return All {@link IProject}s contained by the selection (never
     *         <code>null</code>)
     */
    public static IProject[] getProjectsFromSelection(final ISelection selection) {
        Check.notNull(selection, "selection"); //$NON-NLS-1$

        if (!(selection instanceof IStructuredSelection)) {
            return new IProject[0];
        }

        final Set<IProject> projectSet = new HashSet<IProject>();

        @SuppressWarnings("rawtypes")
        final Iterator i;

        for (i = ((IStructuredSelection) selection).iterator(); i.hasNext();) {
            final IResource resource = (IResource) i.next();

            final IProject project = resource.getProject();

            if (project != null) {
                projectSet.add(project);
            }
        }

        return projectSet.toArray(new IProject[projectSet.size()]);
    }

    /**
     * Gets the {@link TFSRepository}s contained in the selection, if any are
     * connected.
     *
     * @param selection
     *        The selected {@link IResource}s (not <code>null/code>)
     * @return An array of {@link TFSRepository}s containing the selection
     *         (never <code>null</code>)
     */
    public static TFSRepository[] getRepositoriesFromSelection(final ISelection selection) {
        Check.notNull(selection, "selection"); //$NON-NLS-1$

        if (!(selection instanceof IStructuredSelection)) {
            return new TFSRepository[0];
        }

        final Set<TFSRepository> repositorySet = new HashSet<TFSRepository>();

        @SuppressWarnings("rawtypes")
        final Iterator i;

        for (i = ((IStructuredSelection) selection).iterator(); i.hasNext();) {
            final IResource resource = (IResource) i.next();
            final IProject project = resource.getProject();

            if (project != null) {
                final TFSRepository repository =
                    TFSEclipseClientPlugin.getDefault().getProjectManager().getRepository(project);

                if (repository != null) {
                    repositorySet.add(repository);
                }
            }
        }

        return repositorySet.toArray(new TFSRepository[repositorySet.size()]);
    }

    public static String getFirstSelectedPath(final ISelection selection) {
        final AdaptedSelectionInfo selectionInfo =
            ActionHelpers.adaptSelectionToStandardResources(selection, ResourceFilters.LINKED_RESOURCES_FILTER, false);

        if (selectionInfo == null
            || selectionInfo.getRepositories() == null
            || selectionInfo.getRepositories().length == 0) {
            return null;
        }

        final IResource resources[] = selectionInfo.getResources();

        if (resources == null || resources.length == 0) {
            return null;
        }

        return resources[0] == null ? null : resources[0].getLocationURI().getPath();
    }

    /**
     * Test whether the selection is symlink
     *
     * @param selection
     * @return
     */
    public static boolean linkSelected(final ISelection selection) {
        final String path = getFirstSelectedPath(selection);
        if (path == null) {
            return false;
        } else {
            return FileSystemUtils.getInstance().getAttributes(path).isSymbolicLink();
        }
    }

    /**
     * Test whether the selection is pending add
     *
     * @param selection
     * @return
     */
    public static boolean pendingAddSelected(final ISelection selection) {
        final AdaptedSelectionInfo selectionInfo =
            ActionHelpers.adaptSelectionToStandardResources(selection, ResourceFilters.LINKED_RESOURCES_FILTER, false);

        if (selectionInfo == null
            || selectionInfo.getRepositories() == null
            || selectionInfo.getRepositories().length == 0) {
            return false;
        }

        final TFSRepository repository = selectionInfo.getRepositories()[0];

        final String path = getFirstSelectedPath(selection);
        if (path == null || repository == null) {
            return false;
        } else {
            return PendingChangesHelpers.isPendingAdd(repository, path);
        }
    }
}
