// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse.resourcechange;

import java.io.File;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.team.core.Team;

import com.microsoft.tfs.client.common.framework.resources.filter.ResourceFilter;
import com.microsoft.tfs.client.common.framework.resources.filter.ResourceFilterResult;
import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.eclipse.TFSEclipseClientPlugin;
import com.microsoft.tfs.client.eclipse.TFSRepositoryProvider;
import com.microsoft.tfs.client.eclipse.util.TeamUtils;
import com.microsoft.tfs.core.clients.versioncontrol.WorkspaceLocation;
import com.microsoft.tfs.core.clients.versioncontrol.path.LocalPath;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ChangeType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PendingChange;
import com.microsoft.tfs.core.util.CodePageMapping;
import com.microsoft.tfs.core.util.FileEncoding;

public class DeltaVisitor implements IResourceDeltaVisitor {
    private static final Log log = LogFactory.getLog(TFSResourceChangeListener.class);

    private final ResourceFilter localWorkspaceBaselineFilter;
    private final ResourceFilter ignoreFilter;
    private final ResourceFilter inRepositoryFilter;

    private final IStatus status = Status.OK_STATUS;
    private final Set<String> filesToAddByLocalPath = new HashSet<String>();
    private final Map<TFSRepository, Set<PendingChange>> convertToEditPendingChanges =
        new HashMap<TFSRepository, Set<PendingChange>>();
    private final Map<TFSRepository, TFSResourceChangeSet> filesToAddByRepository =
        new HashMap<TFSRepository, TFSResourceChangeSet>();
    private final Map<TFSRepository, Set<String>> filesNeedingScan = new HashMap<TFSRepository, Set<String>>();
    private final Map<TFSRepository, Set<IFile>> filesNeedingResourceData = new HashMap<TFSRepository, Set<IFile>>();

    public DeltaVisitor(
        final ResourceFilter localWorkspaceBaselineFilter,
        final ResourceFilter ignoreFilter,
        final ResourceFilter inRepositoryFilter) {
        this.localWorkspaceBaselineFilter = localWorkspaceBaselineFilter;
        this.ignoreFilter = ignoreFilter;
        this.inRepositoryFilter = inRepositoryFilter;
    }

    @Override
    public boolean visit(final IResourceDelta delta) throws CoreException {
        final IResource resource = delta.getResource();

        /*
         * Local workspace baseline folders can move around the disk somewhat
         * unpredictably, as mappings change, so we should detect them whenever
         * they pass through RCL and mark them "team private."
         *
         * Do this before testing the ignoreFilter, because that filter will
         * reject baselines and return for added safety (so we never pend
         * changes for them).
         */
        if (localWorkspaceBaselineFilter.filter(resource).isReject()) {
            resource.setTeamPrivateMember(true);

            /*
             * You might see resources marked "team private" by this method
             * remain visible the package explorer until you manually refresh
             * the view. This is because marking them doesn't fire events that
             * cause views to redraw.
             *
             * Forcing a local refresh on the resource doesn't seem to work
             * either (even if run as a deferred job). I dont know of a
             * work-around as of Eclipse 3.7.
             */

            return false;
        }

        /*
         * Use the big ignore filter first, quickly returning if we're rejecting
         * the resource.
         */
        final ResourceFilterResult result = ignoreFilter.filter(resource, ResourceFilter.FILTER_FLAG_TREE_OPTIMIZATION);

        if (result.isReject()) {
            return !result.isRejectChildren();
        }

        /*
         * Detect whether the resource is in the repository so we can do smart
         * things.
         */
        final ResourceFilterResult inRepositoryResult =
            inRepositoryFilter.filter(resource, ResourceFilter.FILTER_FLAG_TREE_OPTIMIZATION);

        /*
         * If it's in the repository, check it for changes.
         */
        if (inRepositoryResult.isAccept()) {
            /*
             * This file is in the repository, this change is either a
             * filesystem-level change that we did not drive or a metadata
             * change (like encoding) that we should examine.
             */
            return visitResourceInRepository(delta);
        } else {
            /* Wasn't in the repository, see if it needs added. */
            return visitResourceNotInRepository(delta);
        }
    }

    /*
     * Visit a resource that is in the repository, but has changed on disk for
     * some reason. This is generally used to update resource data for resources
     * that have had metadata (ie, encoding) changes or were modified by another
     * client.
     */
    private boolean visitResourceInRepository(final IResourceDelta delta) {
        final IResource resource = delta.getResource();

        /*
         * If this isn't a file, simply return true to recurse down this
         * container.
         */
        if (resource.getType() != IResource.FILE || resource.getProject() == null) {
            return true;
        }

        /*
         * Ignore marker-only changes. Huge perf improvement ignoring markers in
         * a large build since they can't cause a pending change.
         *
         * We could ignore SYNC also, but our team provider doesn't make use of
         * sync data on resources.
         */
        if (delta.getKind() == IResourceDelta.CHANGED && delta.getFlags() == IResourceDelta.MARKERS) {
            return true;
        }

        final IProject project = resource.getProject();
        final TFSRepository repository = TFSEclipseClientPlugin.getDefault().getProjectManager().getRepository(project);

        final String localPath = resource.getLocation().toOSString();

        if (repository != null && localPath != null) {
            getOrCreateFilesNeedingScanSet(repository).add(localPath);

            /*
             * Update resource data for this resource. For server workspaces,
             * this probably means that a resource was modified outside of
             * Eclipse - it may have been modified in Visual Studio, so we
             * should try to update resource data for this resource.
             */

            /* Ignore encoding-only changes. */
            if (delta.getKind() == IResourceDelta.CHANGED && delta.getFlags() == IResourceDelta.ENCODING) {
                return true;
            }

            /*
             * If there are any pending changes for this resources, ensure
             * they're compatible with the delta change.
             */
            final PendingChange pendingChange =
                repository.getPendingChangeCache().getPendingChangeByLocalPath(localPath);

            if (pendingChange != null) {
                /*
                 * It's a common operation in Eclipse to paste a file over an
                 * existing file. JDT completes this operation by:
                 *
                 * 1. Validating an edit on the target (we pend an edit, or the
                 * file is already editable)
                 *
                 * 2. Deleting the target (we pend a delete, which replaces the
                 * edit pending change)
                 *
                 * 3. Copying the source to the target (IResourceChangeListener
                 * fires and we end up right here), the file is actually on
                 * disk, but pending an add would fail because it's incompatible
                 * with delete
                 *
                 * So we need to mark this change as needing "converted" to an
                 * edit change.
                 *
                 * We can't reliably detect the context of a paste operation
                 * versus some other similar sequence of edit -> delete -> add,
                 * so we handle that sequence in general.
                 *
                 * Only do this pending change conversion if the local item
                 * exists, since that won't be true in a plain user-driven
                 * "delete" operation (no conversion required).
                 */
                if (new File(localPath).exists() && pendingChange.getChangeType().contains(ChangeType.DELETE)) {
                    getOrCreateConvertToEditPendingChangesSet(repository).add(pendingChange);

                    // Queue a scan for local workspaces (so the "edit" happens
                    // if the contents really are different).
                    if (repository.getWorkspace().getLocation() == WorkspaceLocation.LOCAL) {
                        getOrCreateFilesNeedingScanSet(repository).add(localPath);
                    }

                    return true;
                }

                /* Ignore this, keep processing */
                return true;
            }

            /*
             * Otherwise, add this to the list of resources that need resource
             * data refreshes
             */
            getOrCreateFilesNeedingResourceDataSet(repository).add((IFile) resource);
        }

        /* Keep processing */
        return true;
    }

    /*
     * Visit a resource not in the repository (or more accurately, a resource
     * that lacks resourcedata, and is thus believed to not be in the
     * repository.)
     *
     * This is generally used to handle the file added case.
     */
    private boolean visitResourceNotInRepository(final IResourceDelta delta) {
        final IResource resource = delta.getResource();

        /*
         * If it wasn't an ADD, return true for changed containers (to recurse).
         */
        if (delta.getKind() != IResourceDelta.ADDED) {
            return delta.getKind() == IResourceDelta.CHANGED && resource.getType() != IResource.FILE;
        }

        final IProject project = resource.getProject();
        final TFSRepository repository = TFSEclipseClientPlugin.getDefault().getProjectManager().getRepository(project);

        /*
         * If a file was moved from a project where we are the team provider, it
         * won't need pended as an add (it was already pended as a rename).
         */
        IFile originalFile = null;
        if ((delta.getFlags() & IResourceDelta.MOVED_FROM) != 0) {
            originalFile = ResourcesPlugin.getWorkspace().getRoot().getFile(delta.getMovedFromPath());

            if (TeamUtils.isConfiguredWith(originalFile, TFSRepositoryProvider.PROVIDER_ID)) {
                /*
                 * If this resource is in a local workspace, it may have been
                 * edited as a result of the move (Java refactor > rename), so
                 * we should add the item to the "edit" set so it gets scanned
                 * later.
                 */
                if (repository != null && repository.getWorkspace().getLocation() == WorkspaceLocation.LOCAL) {
                    final String localPath = resource.getLocation().toOSString();

                    if (localPath != null) {
                        getOrCreateFilesNeedingScanSet(repository).add(localPath);
                        return true;
                    }
                }

                return false;
            }
        }

        /* Add it. */
        if (resource.getType() == IResource.FILE) {
            final String localPath = resource.getLocation().toOSString();

            if (repository != null && localPath != null && !filesToAddByLocalPath.contains(localPath)) {
                /*
                 * This file may already be pended for addition. (In particular,
                 * if a user has a file imported twice in their workspace. See
                 * bug 4332.)
                 */
                final PendingChange pendingChange =
                    repository.getPendingChangeCache().getPendingChangeByLocalPath(localPath);

                /*
                 * We want to ignore adds iff there is a pending change and it
                 * is of a type that creates new resources (ie, add, target of a
                 * rename, branch, undelete.)
                 */
                final ChangeType[] conflictingChangeTypes = new ChangeType[] {
                    ChangeType.ADD,
                    ChangeType.BRANCH,
                    ChangeType.UNDELETE
                };

                if (pendingChange != null) {
                    for (int i = 0; i < conflictingChangeTypes.length; i++) {
                        if (pendingChange.getChangeType().contains(conflictingChangeTypes[i])) {
                            log.info(MessageFormat.format(
                                "Ignoring add notification for {0} (already pended as {1})", //$NON-NLS-1$
                                resource,
                                conflictingChangeTypes[i].toUIString(true, pendingChange)));
                            return true;
                        }
                    }

                    if (pendingChange != null
                        && pendingChange.getChangeType().contains(ChangeType.RENAME)
                        && LocalPath.equals(pendingChange.getLocalItem(), localPath)) {
                        log.info(
                            MessageFormat.format(
                                "Ignoring add notification for {0} (already pended as target of rename)", //$NON-NLS-1$
                                resource));
                        return true;
                    }
                }

                if (originalFile != null) {
                    log.info(
                        MessageFormat.format(
                            "Rename detected from file {0} to {1} (source not managed by TFS, target will be added)", //$NON-NLS-1$
                            originalFile,
                            resource));
                } else {
                    log.info(MessageFormat.format("Detected new file {0}", resource)); //$NON-NLS-1$
                }

                /* Add to the list of files to add for this repository */
                getOrCreateFilesToAddSet(repository).add(localPath, getEncodingForResource(resource), resource);

                filesToAddByLocalPath.add(localPath);
            }
        }

        /*
         * Keep processing.
         */

        return true;
    }

    public Map<TFSRepository, Set<PendingChange>> getConvertToEditPendingChangesSet() {
        return convertToEditPendingChanges;
    }

    public Map<TFSRepository, TFSResourceChangeSet> getFilesToAdd() {
        return filesToAddByRepository;
    }

    public Map<TFSRepository, Set<String>> getFilesNeedingScan() {
        return filesNeedingScan;
    }

    public Map<TFSRepository, Set<IFile>> getFilesNeedingResourceData() {
        return filesNeedingResourceData;
    }

    private Set<PendingChange> getOrCreateConvertToEditPendingChangesSet(final TFSRepository repository) {
        Set<PendingChange> changesSet = convertToEditPendingChanges.get(repository);

        if (changesSet == null) {
            changesSet = new HashSet<PendingChange>();
            convertToEditPendingChanges.put(repository, changesSet);
        }

        return changesSet;
    }

    private TFSResourceChangeSet getOrCreateFilesToAddSet(final TFSRepository repository) {
        TFSResourceChangeSet additionSet = filesToAddByRepository.get(repository);

        if (additionSet == null) {
            additionSet = new TFSResourceChangeSet();
            filesToAddByRepository.put(repository, additionSet);
        }

        return additionSet;
    }

    private Set<String> getOrCreateFilesNeedingScanSet(final TFSRepository repository) {
        Set<String> scansForRepository = filesNeedingScan.get(repository);

        if (scansForRepository == null) {
            scansForRepository = new HashSet<String>();
            filesNeedingScan.put(repository, scansForRepository);
        }

        return scansForRepository;
    }

    private Set<IFile> getOrCreateFilesNeedingResourceDataSet(final TFSRepository repository) {
        Set<IFile> filesForRepository = filesNeedingResourceData.get(repository);

        if (filesForRepository == null) {
            filesForRepository = new HashSet<IFile>();
            filesNeedingResourceData.put(repository, filesForRepository);
        }

        return filesForRepository;
    }

    public IStatus getStatus() {
        return status;
    }

    private static FileEncoding getEncodingForResource(final IResource resource) {
        if (resource.getType() != IResource.FILE) {
            return null;
        }

        final IFile file = (IFile) resource;

        /*
         * Use the deprecated method for Eclipse 3.0 compatibility
         */
        final int teamType = Team.getType(file);

        if (teamType == Team.BINARY) {
            return FileEncoding.BINARY;
        }

        String encoding;

        try {
            encoding = file.getCharset();
        } catch (final CoreException e) {
            encoding = ResourcesPlugin.getEncoding();
        }

        final int codePage = CodePageMapping.getCodePage(encoding, false);

        if (codePage == 0) {
            return null;
        }

        return new FileEncoding(codePage);
    }
}