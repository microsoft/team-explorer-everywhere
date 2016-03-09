// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse.resourcechange;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFileModificationValidator;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.team.IMoveDeleteHook;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;

import com.microsoft.tfs.client.common.commands.vc.AddCommand;
import com.microsoft.tfs.client.common.commands.vc.EditCommand;
import com.microsoft.tfs.client.common.commands.vc.QueryItemsCommand;
import com.microsoft.tfs.client.common.commands.vc.ScanLocalWorkspaceCommand;
import com.microsoft.tfs.client.common.commands.vc.UndoCommand;
import com.microsoft.tfs.client.common.framework.command.CommandExecutor;
import com.microsoft.tfs.client.common.framework.command.CommandList;
import com.microsoft.tfs.client.common.framework.command.JobCommandExecutor;
import com.microsoft.tfs.client.common.framework.command.JobOptions;
import com.microsoft.tfs.client.common.framework.resources.command.ResourceChangingCommand;
import com.microsoft.tfs.client.common.framework.resources.filter.CompositeResourceFilter;
import com.microsoft.tfs.client.common.framework.resources.filter.CompositeResourceFilter.CompositeResourceFilterType;
import com.microsoft.tfs.client.common.framework.resources.filter.ResourceFilter;
import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.eclipse.Messages;
import com.microsoft.tfs.client.eclipse.TFSEclipseClientPlugin;
import com.microsoft.tfs.client.eclipse.TFSRepositoryProvider;
import com.microsoft.tfs.client.eclipse.resource.ConfiguredTeamProviderFilter;
import com.microsoft.tfs.client.eclipse.resource.InRepositoryFilter;
import com.microsoft.tfs.client.eclipse.resource.PluginResourceFilters;
import com.microsoft.tfs.client.eclipse.resource.RepositoryUnavailablePolicy;
import com.microsoft.tfs.client.eclipse.resourcedata.ResourceData;
import com.microsoft.tfs.client.eclipse.resourcedata.ResourceDataUpdate;
import com.microsoft.tfs.core.clients.versioncontrol.GetItemsOptions;
import com.microsoft.tfs.core.clients.versioncontrol.GetOptions;
import com.microsoft.tfs.core.clients.versioncontrol.PendChangesOptions;
import com.microsoft.tfs.core.clients.versioncontrol.VersionControlConstants;
import com.microsoft.tfs.core.clients.versioncontrol.WorkspaceLocation;
import com.microsoft.tfs.core.clients.versioncontrol.events.EventSource;
import com.microsoft.tfs.core.clients.versioncontrol.events.NonFatalErrorEvent;
import com.microsoft.tfs.core.clients.versioncontrol.exceptions.VersionControlException;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.DeletedState;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Failure;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Item;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ItemSet;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ItemType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.LockLevel;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PathTranslation;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PendingChange;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.RecursionType;
import com.microsoft.tfs.core.clients.versioncontrol.specs.ItemSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.WorkspaceVersionSpec;
import com.microsoft.tfs.core.util.FileEncoding;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.FileHelpers;

/**
 * <p>
 * The purpose of the TFS resource changed listener is to do only two things:
 * </p>
 * <ol>
 * <li>Pick up newly added resources in TFS-managed projects and pend them as
 * adds to the TFS server (most important in server workspaces, but done in
 * local workspaces too).</li>
 * <li>React to content and path changes in local workspaces and run the local
 * workspace scanner to determine whether to pend changes.</li>
 * </ol>
 * <p>
 * Since the resource changed listener only exists for the 2 above purposes (and
 * a few other cases), the main challenge in writing it is ignoring all of the
 * other scenarios in which it will be invoked.
 * </p>
 * <p>
 * There are some resource-related operations that should not be handled by the
 * resource change listener:
 * </p>
 * <ul>
 * <li>Moving (renaming) and deleting of resources is handled by the
 * {@link IMoveDeleteHook}, unless the source of the rename is NOT a TFS-managed
 * project (in which case, this is an add operation.) The resource change
 * listener must ignore events that are triggered by moves and deletes.</li>
 * <li>Pending edits for server workspace resources is handled by the
 * {@link IFileModificationValidator}. Local workspace items are already
 * writable so {@link IFileModificationValidator} does nothing and
 * {@link TFSResourceChangeListener} notifies the scanner.</li>
 * </ul>
 * <p>
 * One potential "gotcha" scenario to avoid is when the resource changed
 * listener incorrectly pends an add for items that already exist on the server.
 * For example:
 * </p>
 * <ol>
 * <li>The end user performs a get latest on a folder in a TFS-managed project
 * </li>
 * <li>There is a new item on the server since the last update, so the client
 * gets that item and creates a new local file</li>
 * <li>When the Eclipse workspace refreshes, it sees the new local file and
 * issues a resource changed event for it</li>
 * <li>The resource changed listener reacts to the event by attempting to pend
 * an add for the new file (which fails)</li>
 * </ol>
 * <p>
 * In order to be correct, the resource changed listener must ignore most
 * events:
 * </p>
 * <ul>
 * <li>Events for non-TFS-managed resources should be ignored</li>
 * <li>If the delta is not ADDED, the delta can be ignored. Process the delta's
 * sub-tree if it is not a file and it is CHANGED.</li>
 * <li>If the delta is ADDED, but it has the MOVED_FROM flag, it can be ignored.
 * This represents a move/rename operation.</li>
 * </ul>
 * <p>
 * However, there are some events which pass the above criteria but still must
 * be ignored. The "new file on get latest" scenario given above is one such
 * case. For these, a secondary mechanism is needed. The secondary mechanism
 * works by ignoring all events originating from a particular thread for a
 * period of time. For example:
 * </p>
 * <ol>
 * <li>A thread that will generate resource change events adds itself to the
 * resource changed listener's ignore list before calling the API that will
 * generate the events.</li>
 * <li>When an event is received by the resource change listener, the listener
 * first checks the current thread to see if it is in the ignore list. If so,
 * the event is ignored.</li>
 * <li>The thread removes itself from the ignore list inside a finally block
 * </li>
 * </ol>
 *
 */
public class TFSResourceChangeListener implements IResourceChangeListener {
    private static final Log log = LogFactory.getLog(TFSResourceChangeListener.class);

    private static final String STATUS_REPORTERS_EXTENSION_POINT_ID =
        "com.microsoft.tfs.client.eclipse.resourceChangeStatusReporters"; //$NON-NLS-1$

    private final Object statusReporterLock = new Object();
    private TFSResourceChangeStatusReporter[] statusReporters;

    private final ResourceFilter localWorkspaceBaselineFilter;
    private final ResourceFilter ignoreFilter;
    private final ResourceFilter inRepositoryFilter;

    private final Set<Thread> ignoreThreads = new HashSet<Thread>();

    public TFSResourceChangeListener() {
        /*
         * This filter rejects local workspace baseline folders ($tf/.tf) so we
         * can set them as team private members.
         */
        localWorkspaceBaselineFilter = PluginResourceFilters.LOCAL_WORKSPACE_BASELINE_FILTER;

        /*
         * This filter rejects items not managed by our team provider, ignores
         * items that should be ignored, and allows items that are not already
         * in the repository (notice the inverse of the "in repository" filter
         * is used).
         */
        final CompositeResourceFilter.Builder builder =
            new CompositeResourceFilter.Builder(CompositeResourceFilterType.ALL_MUST_ACCEPT);
        builder.addFilter(new ConfiguredTeamProviderFilter(TFSRepositoryProvider.PROVIDER_ID));
        builder.addFilter(PluginResourceFilters.STANDARD_FILTER);
        ignoreFilter = builder.build();

        inRepositoryFilter = new InRepositoryFilter(RepositoryUnavailablePolicy.REJECT_RESOURCE);
    }

    /**
     * Causes this {@link TFSResourceChangeListener} to ignore change events
     * which were caused by this thread until
     * {@link #stopIgnoreThreadResourceChangeEvents()} is called.
     */
    public void startIgnoreThreadResourceChangeEvents() {
        final Thread t = Thread.currentThread();
        synchronized (ignoreThreads) {
            ignoreThreads.add(t);
        }
    }

    /**
     * Resumes processing of change events caused by this thread which were
     * ignored since {@link #startIgnoreThreadResourceChangeEvents()} was
     * called.
     */
    public void stopIgnoreThreadResourceChangeEvents() {
        final Thread t = Thread.currentThread();
        synchronized (ignoreThreads) {
            ignoreThreads.remove(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void resourceChanged(final IResourceChangeEvent event) {
        final Thread t = Thread.currentThread();
        synchronized (ignoreThreads) {
            if (ignoreThreads.contains(t)) {
                return;
            }
        }

        /*
         * Deadlock avoidance: we may be called on the UI thread before the
         * workbench has started (and thus before the ProjectRepositoryManager
         * has started.) In this case, we need to exit early without doing any
         * work, because the workbench will start by posting a syncExec to the
         * UI thread. Thus we can't just wait for project repository manager to
         * start.
         *
         * We cannot kick off a worker thread to handle this as the
         * IResourceChangeEvent given to us (and the delta contained therein) is
         * valid only for the invocation of this method.
         *
         * This means that it's possible to not pend adds that were detected
         * before the workbench started.
         */
        if (!TFSEclipseClientPlugin.getDefault().getProjectManager().isStarted()) {
            log.warn("Resource change event called before workbench has started, ignoring resource changes"); //$NON-NLS-1$
            return;
        }

        final DeltaVisitor visitor = new DeltaVisitor(localWorkspaceBaselineFilter, ignoreFilter, inRepositoryFilter);

        try {
            event.getDelta().accept(visitor);
        } catch (final CoreException e) {
            TFSEclipseClientPlugin.getDefault().getLog().log(e.getStatus());
            return;
        }

        final IStatus visitStatus = visitor.getStatus();

        if (!visitStatus.isOK()) {
            reportNonOKVisitorStatus(visitStatus);
        }

        /*
         * Some changes in the resource delta may conflict with existing pending
         * changes. Convert those changes into a type compatible with the
         * resource event.
         */

        final Map<TFSRepository, Set<PendingChange>> convertToEditPendingChangesByRepository =
            visitor.getConvertToEditPendingChangesSet();

        for (final Entry<TFSRepository, Set<PendingChange>> convertToEditChangesForRepository : convertToEditPendingChangesByRepository.entrySet()) {
            final TFSRepository repository = convertToEditChangesForRepository.getKey();
            final Set<PendingChange> changesSet = convertToEditChangesForRepository.getValue();

            final TFSResourceChangeStatus status = convertPendingChangesToEdit(repository, changesSet);

            reportConvertToEditStatus(status.getStatus(), status.getNonFatals());
        }

        /*
         * Pend adds for matching files. This class is really just about add
         * operations. Other classes handle rename/move, delete, etc. See this
         * class's Javadoc for details.
         */

        final Map<TFSRepository, TFSResourceChangeSet> filesToAddByRepository = visitor.getFilesToAdd();

        for (final Entry<TFSRepository, TFSResourceChangeSet> additionsForRepository : filesToAddByRepository.entrySet()) {
            final TFSRepository repository = additionsForRepository.getKey();
            final TFSResourceChangeSet additionSet = additionsForRepository.getValue();

            final TFSResourceChangeStatus status = pendAdditions(repository, additionSet);

            reportAdditionStatus(status.getStatus(), status.getNonFatals());
        }

        /*
         * Scan files that were edited in order to pend a scan for a local
         * workspace.
         */
        final Map<TFSRepository, Set<String>> filesNeedingScan = visitor.getFilesNeedingScan();

        for (final Entry<TFSRepository, Set<String>> scansForRepository : filesNeedingScan.entrySet()) {
            final TFSRepository repository = scansForRepository.getKey();
            final Set<String> localPaths = scansForRepository.getValue();

            final TFSResourceChangeStatus status = scanItems(repository, localPaths);

            reportScanStatus(status.getStatus(), status.getNonFatals());
        }

        /*
         * Queue background resource data updates for files that were modified
         * outside of our purview.
         */
        final Map<TFSRepository, Set<IFile>> filesNeedingResourceData = visitor.getFilesNeedingResourceData();

        for (final Entry<TFSRepository, Set<IFile>> refreshesForRepository : filesNeedingResourceData.entrySet()) {
            final TFSRepository repository = refreshesForRepository.getKey();
            final Set<IFile> fileSet = refreshesForRepository.getValue();

            if (fileSet.size() == 0) {
                continue;
            }

            TFSEclipseClientPlugin.getDefault().getResourceDataManager().refreshAsync(
                repository,
                fileSet.toArray(new IFile[fileSet.size()]));
        }
    }

    private void reportNonOKVisitorStatus(final IStatus status) {
        for (final TFSResourceChangeStatusReporter reporter : getStatusReporters()) {
            reporter.reportNonOKVisitorStatus(status);
        }
    }

    private void reportConvertToEditStatus(final IStatus status, final NonFatalErrorEvent[] nonFatal) {
        for (final TFSResourceChangeStatusReporter reporter : getStatusReporters()) {
            reporter.reportConvertToEditStatus(status, nonFatal);
        }
    }

    private void reportAdditionStatus(final IStatus status, final NonFatalErrorEvent[] nonFatal) {
        for (final TFSResourceChangeStatusReporter reporter : getStatusReporters()) {
            reporter.reportAdditionStatus(status, nonFatal);
        }
    }

    private void reportScanStatus(final IStatus status, final NonFatalErrorEvent[] nonFatal) {
        for (final TFSResourceChangeStatusReporter reporter : getStatusReporters()) {
            reporter.reportScanStatus(status, nonFatal);
        }
    }

    /*
     * Converts the specified pending changes that would prevent an add or edit
     * operation from being pended correctly. This is most commonly a delete
     * change that Eclipse created during a resource "paste" that overwrites an
     * existing file, but we want the change to be "edit" after the paste
     * completes.
     */
    private TFSResourceChangeStatus convertPendingChangesToEdit(
        final TFSRepository repository,
        final Set<PendingChange> pendingChanges) {
        Check.notNull(repository, "repository"); //$NON-NLS-1$
        Check.notNull(pendingChanges, "pendingChanges"); //$NON-NLS-1$

        final List<NonFatalErrorEvent> nonFatals = new ArrayList<NonFatalErrorEvent>();

        /*
         * Because the "paste" operation has since put content where the
         * "delete" change is, we can't simply undo the changes: that would fail
         * because there's content at the local item. We must move the files to
         * temporary names, undo the changes, and move them back.
         */

        final Map<PendingChange, File> tempFiles = new HashMap<PendingChange, File>();
        for (final PendingChange change : pendingChanges) {
            final File localFile = new File(change.getLocalItem());

            if (localFile.exists()) {
                File tempFile = null;
                try {
                    // Mix the file's name into the temp file to assist manual
                    // recovery
                    tempFile = File.createTempFile(
                        "convertToEdit-" + localFile.getName() + "-", //$NON-NLS-1$ //$NON-NLS-2$
                        ".tmp", //$NON-NLS-1$
                        localFile.getParentFile());
                    tempFile.delete();
                } catch (final IOException e) {
                    log.error("Error creating temp file", e); //$NON-NLS-1$
                    nonFatals.add(
                        new NonFatalErrorEvent(
                            EventSource.newFromHere(),
                            repository.getWorkspace(),
                            new VersionControlException(MessageFormat.format(
                                //@formatter:off
                                Messages.getString("TFSResourceChangeListener.ConvertToEditCouldNotCreateTemporaryFileErrorFormat"), //$NON-NLS-1$
                                //@formatter:on
                                tempFile,
                                e.getLocalizedMessage(),
                                localFile), e)));
                    continue;
                }

                try {
                    FileHelpers.rename(localFile, tempFile);
                } catch (final IOException e) {
                    log.error("Error renaming to temp file", e); //$NON-NLS-1$
                    nonFatals.add(
                        new NonFatalErrorEvent(
                            EventSource.newFromHere(),
                            repository.getWorkspace(),
                            new VersionControlException(MessageFormat.format(
                                //@formatter:off
                                Messages.getString("TFSResourceChangeListener.ConvertToEditCouldNotRenameToTempErrorFormat"), //$NON-NLS-1$
                                //@formatter:on
                                localFile,
                                tempFile,
                                e.getLocalizedMessage()), e)));

                    tempFile.delete();

                    continue;
                }

                tempFiles.put(change, tempFile);
            }
        }

        if (tempFiles.size() == 0) {
            return new TFSResourceChangeStatus(
                new Status(
                    Status.ERROR,
                    TFSEclipseClientPlugin.PLUGIN_ID,
                    Messages.getString("TFSResourceChangeListener.ConvertToEditAllRenamesFailedError")), //$NON-NLS-1$
                nonFatals.toArray(new NonFatalErrorEvent[nonFatals.size()]));
        }

        IStatus status = null;

        try {
            /*
             * Undo the changes. For server workspaces, we also need to pend
             * edits.
             */

            final ItemSpec[] specs = ItemSpec.fromStrings(
                PendingChange.toLocalItems(pendingChanges.toArray(new PendingChange[pendingChanges.size()])),
                RecursionType.NONE);

            final CommandList commands =
                new CommandList(
                    Messages.getString("TFSResourceChangeListener.ConvertingPendingChangesCommandName"), //$NON-NLS-1$
                    Messages.getString("TFSResourceChangeListener.ConvertingPendingChangesCommandErrorText")); //$NON-NLS-1$
            commands.setCancellable(false);

            commands.addCommand(new UndoCommand(repository, specs, GetOptions.NO_DISK_UPDATE));

            if (repository.getWorkspace().getLocation() == WorkspaceLocation.SERVER) {
                commands.addCommand(new EditCommand(repository, specs, LockLevel.UNCHANGED));
            }

            // This status may get replaced by a MultiStatus below
            status = new CommandExecutor().execute(commands);
        } finally {
            // Move the temporary files back.

            for (final Entry<PendingChange, File> entry : tempFiles.entrySet()) {
                final PendingChange change = entry.getKey();
                final File tempFile = entry.getValue();

                final File localFile = new File(change.getLocalItem());

                /*
                 * There should not be any content at the local item because we
                 * did a NO_DISK_UPDATE undo, but delete it just in case. The
                 * content could only be a baseline file and our temp file is
                 * more valuable.
                 */
                if (localFile.exists()) {
                    localFile.delete();
                }

                try {
                    FileHelpers.rename(tempFile, localFile);
                } catch (final IOException e) {
                    /*
                     * This error case should be quite rare, but we need to give
                     * a good error message if it happens.
                     */
                    log.error("Error renaming from temp file", e); //$NON-NLS-1$
                    final MultiStatus multiStatus = new MultiStatus(
                        TFSEclipseClientPlugin.PLUGIN_ID,
                        0,
                        Messages.getString("TFSResourceChangeListener.ConvertToEditRenameFromTempMultiStatusError"), //$NON-NLS-1$
                        null);

                    // Add the UndoCommand's status if there was one
                    if (status != null) {
                        multiStatus.add(status);
                    }

                    multiStatus.add(
                        new Status(
                            Status.ERROR,
                            TFSEclipseClientPlugin.PLUGIN_ID,
                            MessageFormat.format(
                                //@formatter:off
                                Messages.getString("TFSResourceChangeListener.ConvertToEditRenameFromTempSingleItemErrorFormat"), //$NON-NLS-1$
                                //@formatter:on
                                tempFile,
                                localFile),
                            e));

                    status = multiStatus;
                }
            }
        }

        return new TFSResourceChangeStatus(status, nonFatals.toArray(new NonFatalErrorEvent[nonFatals.size()]));
    }

    private TFSResourceChangeStatus pendAdditions(
        final TFSRepository repository,
        final TFSResourceChangeSet additionSet) {
        Check.notNull(repository, "repository"); //$NON-NLS-1$
        Check.notNull(additionSet, "additionSet"); //$NON-NLS-1$

        final String[] localPaths = additionSet.getPathList().toArray(new String[additionSet.getPathList().size()]);
        final Map<String, FileEncoding> encodingHints = new HashMap<String, FileEncoding>();

        for (int i = 0; i < localPaths.length; i++) {
            encodingHints.put(localPaths[i], additionSet.getPathToEncodingMap().get(localPaths[i]));
        }

        final AddCommand addCommand = new AddCommand(
            repository,
            localPaths,
            false,
            LockLevel.UNCHANGED,
            GetOptions.NONE,
            PendChangesOptions.NONE,
            encodingHints,
            null);
        addCommand.setIgnoreNonFatals(true);

        final IStatus addStatus = new CommandExecutor().execute(addCommand);

        /*
         * Setting the ignore non-fatals option (above) means that errors were
         * hard errors, eg server could not be contacted. Continue to report
         * those up.
         */
        if (!addStatus.isOK()) {
            return new TFSResourceChangeStatus(addStatus);
        }

        int expected = localPaths.length;
        final int added = addCommand.getAddCount();
        final NonFatalErrorEvent[] nonFatals = addCommand.getNonFatalErrors();

        /* No errors */
        if (added >= expected && nonFatals.length == 0) {
            return TFSResourceChangeStatus.OK_STATUS;
        }

        /* Report warnings */
        else if (added == expected) {
            return new TFSResourceChangeStatus(Status.OK_STATUS, nonFatals);
        }

        /*
         * Do additional processing to handle non-fatals. Do a query items to
         * see if these items exist in our tfs workspace already - they may have
         * been added by another client and Eclipse is merely notifying us of
         * their existence.
         *
         * Build a map of server items that we're querying on to non fatals so
         * that we can remove the appropriate ones.
         */
        final Map<String, List<NonFatalErrorEvent>> nonFatalMap = new HashMap<String, List<NonFatalErrorEvent>>();
        final List<ItemSpec> queryItems = new ArrayList<ItemSpec>();

        for (int i = 0; i < nonFatals.length; i++) {
            final Failure failure = nonFatals[i].getFailure();
            String serverPath = null;

            if (failure != null
                && failure.getCode() != null
                && failure.getCode().equals(VersionControlConstants.ITEM_EXISTS_EXCEPTION)) {
                // The failure can have a server item, a local item, or neither.
                if (failure.getServerItem() != null) {
                    serverPath = failure.getServerItem();
                } else if (failure.getLocalItem() != null) {
                    final PathTranslation translation =
                        repository.getWorkspace().translateLocalPathToServerPath(failure.getLocalItem());

                    if (translation != null) {
                        serverPath = translation.getTranslatedPath();
                    }
                }

                if (serverPath != null) {
                    queryItems.add(new ItemSpec(serverPath, RecursionType.NONE));
                }
            }

            List<NonFatalErrorEvent> nonFatalsForPath = nonFatalMap.get(serverPath);

            if (nonFatalsForPath == null) {
                nonFatalsForPath = new ArrayList<NonFatalErrorEvent>();
                nonFatalMap.put(serverPath, nonFatalsForPath);
            }

            nonFatalsForPath.add(nonFatals[i]);
        }

        /* Nothing to do, simply report to the user. */
        if (queryItems.size() == 0) {
            return new TFSResourceChangeStatus(
                new Status(
                    IStatus.WARNING,
                    TFSEclipseClientPlugin.PLUGIN_ID,
                    Messages.getString("TFSResourceChangeListener.SomeFilesCouldNotBeAdded")), //$NON-NLS-1$
                nonFatals);
        }

        /* See if we have these items in our workspace */
        final QueryItemsCommand queryCommand = new QueryItemsCommand(
            repository,
            queryItems.toArray(new ItemSpec[queryItems.size()]),
            new WorkspaceVersionSpec(repository.getWorkspace()),
            DeletedState.NON_DELETED,
            ItemType.FILE,
            GetItemsOptions.NONE);

        final IStatus queryStatus = new CommandExecutor().execute(queryCommand);

        if (!queryStatus.isOK() || queryCommand.getItemSets().length != queryItems.size()) {
            return new TFSResourceChangeStatus(
                new Status(
                    IStatus.WARNING,
                    TFSEclipseClientPlugin.PLUGIN_ID,
                    Messages.getString("TFSResourceChangeListener.SomeFilesCouldNotBeAdded")), //$NON-NLS-1$
                nonFatals);
        }

        final ItemSet[] itemSets = queryCommand.getItemSets();
        final List<ResourceDataUpdate> resourceDataUpdates = new ArrayList<ResourceDataUpdate>();

        /*
         * For any item that exists already in our tfs workspace, we assume that
         * another client (VisualStudio, for example) added and checked in the
         * file. We do not warn the user that the item already exists, instead
         * just update the resource data.
         */
        for (int i = 0; i < itemSets.length; i++) {
            final Item[] items = itemSets[i].getItems();

            if (items == null || items.length != 1) {
                continue;
            }

            final String serverPath = items[0].getServerItem();
            final String localPath = repository.getWorkspace().getMappedLocalPath(serverPath);

            if (localPath == null) {
                continue;
            }

            final IResource resource = additionSet.getPathToResourceMap().get(localPath);

            /* Remove this from the list of non-fatals */
            nonFatalMap.remove(serverPath);

            /*
             * Decrement the expected count - with this new knowledge, we should
             * not have expected this add to succeed, so treat it as such for
             * error reporting.
             */
            expected--;

            /* Update the resource data */
            if (resource != null) {
                resourceDataUpdates.add(
                    new ResourceDataUpdate(resource, new ResourceData(serverPath, items[0].getChangeSetID())));
            }
        }

        /* Schedule a resource data update for these resources */
        TFSEclipseClientPlugin.getDefault().getResourceDataManager().update(
            resourceDataUpdates.toArray(new ResourceDataUpdate[resourceDataUpdates.size()]));

        /* All of the non-fatals came from an outside process add. */
        if (added == expected) {
            return TFSResourceChangeStatus.OK_STATUS;
        }

        /* Update the list of non-fatals */
        final List<NonFatalErrorEvent> updatedNonFatals = new ArrayList<NonFatalErrorEvent>();
        for (final Iterator<List<NonFatalErrorEvent>> i = nonFatalMap.values().iterator(); i.hasNext();) {
            updatedNonFatals.addAll(i.next());
        }

        return new TFSResourceChangeStatus(
            new Status(
                IStatus.WARNING,
                TFSEclipseClientPlugin.PLUGIN_ID,
                Messages.getString("TFSResourceChangeListener.SomeFilesCouldNotBeAdded")), //$NON-NLS-1$
            updatedNonFatals.toArray(new NonFatalErrorEvent[updatedNonFatals.size()]));
    }

    private TFSResourceChangeStatus scanItems(final TFSRepository repository, final Set<String> localPaths) {
        Check.notNull(repository, "repository"); //$NON-NLS-1$
        Check.notNull(localPaths, "localPaths"); //$NON-NLS-1$

        /* For local workspaces, run a scan. */
        if (WorkspaceLocation.LOCAL.equals(repository.getWorkspace().getLocation())) {
            /*
             * Scanning must be done outside of this ResourceChangeListener
             * because the post-save IResourceChangeEvent disallows modifying
             * the resource, and the scanner may modify file attributes (to
             * restore "check in" file modification time).
             */

            final ScanLocalWorkspaceCommand command = new ScanLocalWorkspaceCommand(repository, localPaths);
            new JobCommandExecutor(new JobOptions().setSystem(true)).execute(new ResourceChangingCommand(command));
        }

        return new TFSResourceChangeStatus(Status.OK_STATUS);
    }

    private final TFSResourceChangeStatusReporter[] getStatusReporters() {
        synchronized (statusReporterLock) {
            /*
             * If we do not have any TFSResourceChangeStatusNotifiers, query
             * extension points. This allows UI-aware plugins to provide
             * enhanced reporting and prompting functionality.
             */
            if (statusReporters == null) {
                final List<TFSResourceChangeStatusReporter> reporterList =
                    new ArrayList<TFSResourceChangeStatusReporter>();

                /* Add the logging provider (required) */
                reporterList.add(new TFSResourceChangeLoggingStatusReporter());

                final IExtensionRegistry registry = Platform.getExtensionRegistry();
                final IExtensionPoint extensionPoint = registry.getExtensionPoint(STATUS_REPORTERS_EXTENSION_POINT_ID);

                final IConfigurationElement[] elements = extensionPoint.getConfigurationElements();

                for (int i = 0; i < elements.length; i++) {
                    try {
                        final TFSResourceChangeStatusReporter reporter =
                            (TFSResourceChangeStatusReporter) elements[i].createExecutableExtension("class"); //$NON-NLS-1$

                        if (reporter != null) {
                            reporterList.add(reporter);
                        }
                    } catch (final CoreException e) {
                        log.warn(MessageFormat.format(
                            "Could not create {0} class", //$NON-NLS-1$
                            STATUS_REPORTERS_EXTENSION_POINT_ID), e);
                    }
                }

                statusReporters = reporterList.toArray(new TFSResourceChangeStatusReporter[reporterList.size()]);
            }

            return statusReporters;
        }
    }

    private static final class TFSResourceChangeStatus {
        public static final TFSResourceChangeStatus OK_STATUS = new TFSResourceChangeStatus(Status.OK_STATUS);

        private final IStatus status;
        private final NonFatalErrorEvent[] nonFatals;

        public TFSResourceChangeStatus(final IStatus status) {
            this(status, new NonFatalErrorEvent[0]);
        }

        public TFSResourceChangeStatus(final IStatus status, final NonFatalErrorEvent[] nonFatals) {
            Check.notNull(status, "status"); //$NON-NLS-1$
            Check.notNull(nonFatals, "nonFatals"); //$NON-NLS-1$

            this.status = status;
            this.nonFatals = nonFatals;
        }

        public IStatus getStatus() {
            return status;
        }

        public NonFatalErrorEvent[] getNonFatals() {
            return nonFatals;
        }
    }

    /**
     * Writes to log files the statuses that resulted from pending changes on
     * Eclipse resources.
     */
    private final class TFSResourceChangeLoggingStatusReporter implements TFSResourceChangeStatusReporter {
        @Override
        public void reportNonOKVisitorStatus(final IStatus status) {
            final String message =
                MessageFormat.format(
                    Messages.getString("TFSResourceChangeListener.VisitorErrorMessageFormat"), //$NON-NLS-1$
                    status.getMessage());

            if (status.getSeverity() == IStatus.ERROR) {
                log.error(message, status.getException());
            } else if (status.getSeverity() == IStatus.WARNING) {
                log.warn(message, status.getException());
            } else {
                log.info(message, status.getException());
            }
        }

        @Override
        public void reportConvertToEditStatus(final IStatus status, final NonFatalErrorEvent[] nonFatals) {
            /*
             * Ignore status - the command executor should have logged any
             * execution problems
             */
            reportNonFatals(Messages.getString("TFSResourceChangeListener.ConvertToEditErrorMessageFormat"), nonFatals); //$NON-NLS-1$
        }

        @Override
        public void reportAdditionStatus(final IStatus status, final NonFatalErrorEvent[] nonFatals) {
            /*
             * Ignore status - the command executor should have logged any
             * execution problems
             */
            reportNonFatals(Messages.getString("TFSResourceChangeListener.AdditionErrorMessageFormat"), nonFatals); //$NON-NLS-1$
        }

        @Override
        public void reportScanStatus(final IStatus status, final NonFatalErrorEvent[] nonFatals) {
            /*
             * Ignore status - the command executor should have logged any
             * execution problems
             */
            reportNonFatals(Messages.getString("TFSResourceChangeListener.ScanningErrorMessageFormat"), nonFatals); //$NON-NLS-1$
        }

        private void reportNonFatals(final String messageFormat, final NonFatalErrorEvent[] nonFatals) {
            if (nonFatals == null || nonFatals.length == 0) {
                return;
            }

            for (int i = 0; i < nonFatals.length; i++) {
                log.warn(MessageFormat.format(messageFormat, nonFatals[i].getMessage()));
            }
        }
    }
}
