// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.offline;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.microsoft.tfs.core.Messages;
import com.microsoft.tfs.core.clients.versioncontrol.GetItemsOptions;
import com.microsoft.tfs.core.clients.versioncontrol.PropertyConstants;
import com.microsoft.tfs.core.clients.versioncontrol.PropertyUtils;
import com.microsoft.tfs.core.clients.versioncontrol.WebServiceLevel;
import com.microsoft.tfs.core.clients.versioncontrol.path.LocalPath;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ChangeType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.DeletedState;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Item;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ItemSet;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ItemType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PendingChange;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PendingSet;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PropertyValue;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.RecursionType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;
import com.microsoft.tfs.core.clients.versioncontrol.specs.ItemSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.LatestVersionSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.VersionSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.WorkspaceVersionSpec;
import com.microsoft.tfs.jni.FileSystemAttributes;
import com.microsoft.tfs.jni.FileSystemUtils;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.HashUtils;
import com.microsoft.tfs.util.HashUtils.UncheckedNoSuchAlgorithmException;
import com.microsoft.tfs.util.tasks.CanceledException;
import com.microsoft.tfs.util.tasks.TaskMonitor;
import com.microsoft.tfs.util.tasks.TaskMonitorService;

/**
 * <p>
 * {@link OfflineSynchronizer} examines a mapped folder for changes that are not
 * reflected on the server side. Changes that are found locally (by examining
 * writable files or differences, additions or deletions) will be pended to the
 * server.
 * </p>
 *
 * @since TEE-SDK-10.1
 * @threadsafety thread-compatible
 */
public class OfflineSynchronizer {
    private final Workspace workspace;

    private boolean detectAdded = true;
    private boolean detectDeleted = true;

    private RecursionType recursionType = RecursionType.ONE_LEVEL;

    private final OfflineSynchronizerProvider provider;
    private OfflineSynchronizerFilter filter = new OfflineSynchronizerFilter();
    private OfflineSynchronizerMethod method = OfflineSynchronizerMethod.MD5_HASH;

    private Map<String, byte[]> serverFiles = new HashMap<String, byte[]>();
    private PendingChange[] serverChanges = new PendingChange[0];

    private final List<OfflineChange> offlineChanges = new ArrayList<OfflineChange>();
    private final List<String> excludes = new ArrayList<String>();

    private final TaskMonitor taskMonitor = TaskMonitorService.getTaskMonitor();

    /**
     * Takes an OfflineSynchronizerProvider which provides local paths to
     * resources to examine.
     *
     * @param workspace
     *        an AWorkspace holding the mapped resources
     * @param provider
     *        provider of local paths for synchronization
     */
    public OfflineSynchronizer(final Workspace workspace, final OfflineSynchronizerProvider provider) {
        this(workspace, provider, null);
    }

    /**
     * Takes an OfflineSynchronizerProvider which provides local paths to
     * resources to examine.
     *
     * @param workspace
     *        an AWorkspace holding the mapped resources
     * @param provider
     *        provider of local paths for synchronization
     * @param filter
     *        a filter which can limit resources to be pended
     */
    public OfflineSynchronizer(
        final Workspace workspace,
        final OfflineSynchronizerProvider provider,
        final OfflineSynchronizerFilter filter) {
        Check.notNull(workspace, "workspace"); //$NON-NLS-1$
        Check.notNull(provider, "provider"); //$NON-NLS-1$

        this.workspace = workspace;
        this.provider = provider;

        if (filter != null) {
            this.filter = filter;
        }
    }

    /**
     * Toggle whether files added locally (ie, those which exist on the local
     * filesystem but do not exist on the server) will have adds pended for
     * them.
     *
     * @param detectAdded
     *        true to pend adds, false to ignore added resources
     */
    public void setDetectAdded(final boolean detectAdded) {
        this.detectAdded = detectAdded;
    }

    /**
     * Toggle whether files deleted locally (ie, those which do not exist on the
     * local filesystem but the server expects that we have them) will have
     * deletes pended for them.
     *
     * @param detectDeleted
     *        true to pend deletes, false to ignore deleted resources
     */
    public void setDetectDeleted(final boolean detectDeleted) {
        this.detectDeleted = detectDeleted;
    }

    /**
     * Sets the resource filter used to check resources before pending changes.
     *
     * @param filter
     *        filter to check before pending resource changes
     */
    public void setFilter(final OfflineSynchronizerFilter filter) {
        this.filter = filter;
    }

    /**
     * Sets the method of edited file detection.
     *
     * @param method
     *        method of file detection
     */
    public void setMethod(final OfflineSynchronizerMethod method) {
        this.method = method;
    }

    /**
     * Sets the recursion for this operation.
     *
     * @param recursionType
     *        how deep to recurse for the named paths
     */
    public void setRecursionType(final RecursionType recursionType) {
        this.recursionType = recursionType;
    }

    /**
     * Gets the detected offline changes.
     *
     * @return a List of OfflineChange objects
     */
    public OfflineChange[] getChanges() {
        return offlineChanges.toArray(new OfflineChange[offlineChanges.size()]);
    }

    /**
     * Returns online for the set of resources.
     *
     * @throws Exception
     *         if there was an error communicating with the server
     * @throws CanceledException
     *         if the task monitor was cancelled
     * @return The number of changes to be pended to the server
     */
    public final int detectChanges() throws Exception {
        Check.notNull(workspace, "workspace"); //$NON-NLS-1$
        Check.notNull(provider, "provider"); //$NON-NLS-1$

        offlineChanges.clear();

        taskMonitor.begin(Messages.getString("OfflineSynchronizer.DetectingOfflineChanges"), 400); //$NON-NLS-1$

        try {
            /*
             * STEP 1: scan for remote information -- do a queryItems on the
             * paths to bring online, this provides enough data to know what
             * local resources have been added, deleted, etc. also get pending
             * changes for later.
             */
            getServerState(taskMonitor.newSubTaskMonitor(100));

            /*
             * STEP 2: scan for local information -- walk the filesystem looking
             * for files which have been modified, which do not exist in the
             * serverState (ie, adds) or which exist only in the serverState
             * (ie, deletes)
             */
            scanLocal(taskMonitor.newSubTaskMonitor(100));

            /*
             * STEP 3: create deletes for the paths that the server thinks we
             * have (from step 1) that we don't (ie, weren't removed in step 2)
             * (visually pretend this is part of step 2)
             */
            taskMonitor.setCurrentWorkDescription(
                Messages.getString("OfflineSynchronizer.ExaminingLocalFilesystemForDeletions")); //$NON-NLS-1$
            getDeletions();
            taskMonitor.worked(100);

            /*
             * STEP 4: filter out any changes based on the server's knowledge of
             * pending changes
             */
            taskMonitor.setCurrentWorkDescription(
                Messages.getString("OfflineSynchronizer.ResolvingConflictingChanges")); //$NON-NLS-1$
            resolveChanges();
            taskMonitor.worked(100);

            return offlineChanges.size();
        } finally {
            taskMonitor.done();
        }
    }

    /**
     * Queries the OfflineSynchronizerProvider for local paths to synchronize.
     *
     * @return a List of local paths
     */
    private List<String> getLocalPaths() {
        final List<String> paths = new ArrayList<String>();

        final Object[] resources = provider.getResources();
        for (int i = 0; i < resources.length; i++) {
            final String resourcePath = provider.getLocalPathForResource(resources[i]);

            // note: check for null as some overriding classes may need to bail
            // on some resources. (ie, some IResources may not map to local
            // paths but may have been selected)
            if (resourcePath != null) {
                paths.add(resourcePath);
            }
        }

        return paths;
    }

    /**
     * Gets the various bits of server state needed for offline synchronization.
     */
    private void getServerState(final TaskMonitor taskMonitor) throws Exception {
        taskMonitor.begin(Messages.getString("OfflineSynchronizer.ExaminingServerState"), 2); //$NON-NLS-1$

        try {
            taskMonitor.setCurrentWorkDescription(
                Messages.getString("OfflineSynchronizer.ExaminingServerFileInformation")); //$NON-NLS-1$
            serverFiles = getServerFiles();
            taskMonitor.worked(1);

            taskMonitor.setCurrentWorkDescription(Messages.getString("OfflineSynchronizer.ExaminingServerChanges")); //$NON-NLS-1$
            serverChanges = getPendingChanges();
            taskMonitor.worked(1);
        } finally {
            taskMonitor.done();
        }
    }

    /**
     * Gets the server's ideas of our local paths via queryItems. This is so
     * that we can determine what files the server believes we have, but no
     * longer exist. (Ie, these are files that were locally deleted.)
     *
     * We populate localPaths, which is a HashMap of local file paths, with the
     * file's md5sum as a value.
     *
     * Important note: we also populate localPaths with the parent paths of any
     * path returned from the server, up to the path we queried for.
     *
     * For example, if we QueryItems for /a/b, and the only file in our local
     * workspace is /a/b/c/d/e.java, then we also need to populate localPaths
     * with /a/b, /a/b/c, and /a/b/c/d. Otherwise, we would detect those as adds
     * later.
     *
     * @param path
     *        The (local) directory path to query the server for
     * @return Set A HashSet containing local paths.
     */
    private Map<String, byte[]> getServerFiles() throws Exception {
        if (taskMonitor.isCanceled()) {
            throw new CanceledException();
        }

        final List<String> queryPaths = getLocalPaths();
        final HashMap<String, byte[]> localPaths = new HashMap<String, byte[]>();

        // convert local paths to AItemSpecs
        final ItemSpec[] itemSpecs = new ItemSpec[queryPaths.size()];
        for (int i = 0; i < queryPaths.size(); i++) {
            final String path = queryPaths.get(i);
            itemSpecs[i] = new ItemSpec(LocalPath.canonicalize(path), recursionType);
        }

        final VersionSpec versionSpec = new WorkspaceVersionSpec(workspace);

        final ItemSet[] itemSet =
            workspace.getClient().getItems(itemSpecs, versionSpec, DeletedState.NON_DELETED, ItemType.ANY, false);

        // Sanity-check that we got back an array the same size as what we
        // queried
        Check.isTrue(queryPaths.size() == itemSet.length, "queryPaths.size() == itemSet.length"); //$NON-NLS-1$

        // Iterate over the server's file list
        for (int i = 0; i < itemSet.length; i++) {
            final String queriedItem = itemSpecs[i].getItem();
            final Item[] item = itemSet[i].getItems();

            for (int j = 0; j < item.length; j++) {
                if (taskMonitor.isCanceled()) {
                    throw new CanceledException();
                }

                // Get our local path for this file
                final String mappedPath = workspace.getMappedLocalPath(item[j].getServerItem());

                if (mappedPath != null) {
                    // Add it to the list of files (along with its hash)
                    localPaths.put(mappedPath, item[j].getContentHashValue());

                    // Add all parent paths up to the one we queried for
                    addLocalParents(localPaths, queriedItem, mappedPath);
                }
            }
        }

        return localPaths;
    }

    /**
     * Adds all parents of "path" to the provided list of local paths (stopping
     * at queriedItem.) Will stop if the list of local paths contains an entry
     * for any parent.
     *
     * @param localPaths
     *        List of paths to populate (may not be null)
     * @param baseFile
     *        Local path to stop examining at (may be null)
     * @param localFile
     *        The path to add roots of (may be null)
     */
    private void addLocalParents(final Map<String, byte[]> localPaths, final String baseFile, final String localFile) {
        Check.notNull(localPaths, "localPaths"); //$NON-NLS-1$
        Check.notNull(baseFile, "baseFile"); //$NON-NLS-1$
        Check.notNull(localFile, "localFile"); //$NON-NLS-1$

        // stop recursing once we hit the base of our query
        if (LocalPath.equals(baseFile, localFile)) {
            return;
        }

        final File parentFile = new File(localFile).getParentFile();

        // we reached the end of the filesystem
        if (parentFile == null) {
            return;
        }

        // get the path of this file
        final String parent = canonicalPath(parentFile);

        // if this is already in the local path list, we're done
        if (localPaths.containsKey(parent)) {
            return;
        }

        // otherwise, add a dummy directory entry
        localPaths.put(parent, new byte[0]);

        // recurse
        addLocalParents(localPaths, baseFile, parent);
    }

    /**
     * Gets the list of pending changes for this workspace.
     *
     * @return list of APendingChanges for this workspace
     */
    private PendingChange[] getPendingChanges() {
        if (taskMonitor.isCanceled()) {
            throw new CanceledException();
        }

        final PendingSet pendingSet = workspace.getPendingChanges();

        if (pendingSet != null && pendingSet.getPendingChanges() != null) {
            return pendingSet.getPendingChanges();
        }

        return new PendingChange[0];
    }

    /**
     * Scans all the paths specified for changes
     *
     * @throws Exception
     *         if an error occurs reading a file on disk
     */
    private void scanLocal(final TaskMonitor taskMonitor) throws Exception {
        if (taskMonitor.isCanceled()) {
            throw new CanceledException();
        }

        final List<String> paths = getLocalPaths();

        taskMonitor.begin(Messages.getString("OfflineSynchronizer.ExaminingLocalFilesystem"), paths.size()); //$NON-NLS-1$

        try {
            for (final Iterator<String> i = paths.iterator(); i.hasNext();) {
                final String localPath = i.next();

                taskMonitor.setCurrentWorkDescription(
                    MessageFormat.format(
                        Messages.getString("OfflineSynchronizer.ExaminingLocalPathFormat"), //$NON-NLS-1$
                        localPath));

                final File file = new File(localPath);
                scanLocal(file, 0);

                taskMonitor.worked(1);
            }
        } finally {
            taskMonitor.done();
        }
    }

    /**
     * Scan the local filesystem for writable files. If the given File is a
     * directory, recurse over it. If it's a file, examine it.
     *
     * @param file
     *        File referencing file or folder to examine
     * @param serverState
     *        Set of files we know to exist on the server Note: server state
     *        will be mutated
     * @param excludes
     * @param depth
     *        Depth of recursive call (for recursion)
     */
    private void scanLocal(final File file, final int depth) throws Exception {
        if (taskMonitor.isCanceled()) {
            throw new CanceledException();
        }

        // Check symlink attribute on the non-canonicalized name
        final FileSystemUtils util = FileSystemUtils.getInstance();
        final FileSystemAttributes attrs = util.getAttributes(file);

        String path = file.getAbsolutePath();
        if (!attrs.isSymbolicLink()) {
            path = canonicalPath(file);
        }

        // symlink not supported for previous versions, directly skip and return
        if (workspace.getClient().getServiceLevel().getValue() < WebServiceLevel.TFS_2012_2.getValue()
            && attrs.isSymbolicLink()) {
            serverFiles.remove(path);
            return;
        }

        final boolean exists = serverFiles.containsKey(path);
        final byte[] hashCode = serverFiles.remove(path);
        final ItemType serverItemType = (hashCode == null || hashCode.length == 0) ? ItemType.FOLDER : ItemType.FILE;

        /*
         * The file may not actually exist locally. This may happen if this is
         * the root and was based on user input. For example, the user can
         * select "Go Online" for a single file in Explorer that's pended for a
         * delete. In this case, we push the hashCode back into the serverFiles
         * list.
         */
        if (!attrs.isSymbolicLink() && !file.exists()) {
            serverFiles.put(path, hashCode);
        } else if (file.isFile() || attrs.isSymbolicLink()) {
            OfflineChangeType type = null;
            OfflineChangeType propertyType = null;

            // if the file does not exist on the server, we should pend an add
            if (!exists && detectAdded) {
                type = OfflineChangeType.ADD;
                if (attrs.isSymbolicLink()) {
                    propertyType = OfflineChangeType.SYMLINK;
                } else if (attrs.isExecutable()) {
                    propertyType = OfflineChangeType.EXEC;
                }
            } else if (exists) {
                /**
                 * For symbolic link, compute the MD5 hash based on the targeted
                 * link and compare with server hash
                 */
                if (attrs.isSymbolicLink()) {
                    final String targetLink = util.getSymbolicLink(file.getPath());
                    final byte[] localHashByLink = HashUtils.hashString(targetLink, null, HashUtils.ALGORITHM_MD5);
                    if (!Arrays.equals(localHashByLink, hashCode)) {
                        type = OfflineChangeType.EDIT;
                    }
                } else if (isChanged(file, hashCode)) {
                    type = OfflineChangeType.EDIT;
                }

                if (workspace.getClient().getServiceLevel().getValue() >= WebServiceLevel.TFS_2012.getValue()) {
                    // query server for symlink and exec property, detect
                    // property change on local disk
                    boolean symlinkOnServer = false;
                    boolean executable = false;
                    final ItemSet[] items = workspace.getClient().getItems(
                        new ItemSpec[] {
                            new ItemSpec(path, RecursionType.NONE)
                    },
                        LatestVersionSpec.INSTANCE,
                        DeletedState.ANY,
                        ItemType.ANY,
                        GetItemsOptions.NONE,
                        PropertyConstants.QUERY_ALL_PROPERTIES_FILTERS);

                    if (items != null
                        && items.length > 0
                        && items[0].getItems() != null
                        && items[0].getItems().length > 0) {
                        final PropertyValue[] propertyValues = items[0].getItems()[0].getPropertyValues();
                        symlinkOnServer = PropertyConstants.IS_SYMLINK.equals(
                            PropertyUtils.selectMatching(propertyValues, PropertyConstants.SYMBOLIC_KEY));
                        executable = PropertyConstants.EXECUTABLE_ENABLED_VALUE.equals(
                            PropertyUtils.selectMatching(propertyValues, PropertyConstants.EXECUTABLE_KEY));
                    }

                    if (symlinkOnServer != attrs.isSymbolicLink()) {
                        propertyType =
                            attrs.isSymbolicLink() ? OfflineChangeType.SYMLINK : OfflineChangeType.NOT_SYMLINK;
                    } else if (!attrs.isSymbolicLink() && executable != attrs.isExecutable()) {
                        propertyType = attrs.isExecutable() ? OfflineChangeType.EXEC : OfflineChangeType.NOT_EXEC;
                    }
                }
            }

            OfflineChange newChange = null;
            if (type != null && filter.shouldPend(file, type, serverItemType)) {
                newChange = new OfflineChange(path, type, serverItemType);
                if (propertyType != null) {
                    newChange.addChangeType(propertyType);
                }
            } else if (propertyType != null && filter.shouldPend(file, propertyType, serverItemType)) {
                newChange = new OfflineChange(path, propertyType, serverItemType);
            }

            if (newChange != null) {
                offlineChanges.add(newChange);
            }
        } else if (file.isDirectory()) {
            // pend an add if this directory didn't exist
            if (!exists && filter.shouldPend(file, OfflineChangeType.ADD, serverItemType)) {
                offlineChanges.add(new OfflineChange(path, OfflineChangeType.ADD, serverItemType));
            }

            if (filter.shouldRecurse(file)) {
                // recurse into this directory further
                if (shouldRecurse(file, depth)) {
                    final String[] contents = file.list();

                    for (int i = 0; i < contents.length; i++) {
                        final File child = new File(path + File.separatorChar + contents[i]);
                        scanLocal(child, depth + 1);
                    }
                }
            }
            // this directory was excluded - mark it as such so that we don
            // pend deletes against it later (since we're not scanning it
            // locally)
            else {
                excludes.add(path);
            }
        }
    }

    /**
     * Determines if we should examine this directory. This exists for extending
     * classes.
     *
     * @param path
     *        A String representing the folder to examine
     * @return true if we should examine the files (and folders) beneath this
     *         directory, false otherwise
     */
    private boolean shouldRecurse(final File directory, final int depth) {
        // depth == 0 means first level
        if (depth == 0) {
            return recursionType != RecursionType.NONE;
        }

        return recursionType == RecursionType.FULL;
    }

    /**
     * Get the list of deletions - this is basically the remainder of the
     * serverState hash. (ie, everything the server thinks we have but that we
     * have not located on disk.)
     *
     * @param changes
     *        List of changes to append to
     * @param serverState
     *        Map of files/hash values from the server
     */
    private void getDeletions() {
        if (!detectDeleted) {
            return;
        }

        for (final Iterator<String> i = serverFiles.keySet().iterator(); i.hasNext();) {
            if (taskMonitor.isCanceled()) {
                throw new CanceledException();
            }

            final String filename = i.next();

            final byte[] hash = serverFiles.get(filename);
            final ItemType serverItemType = (hash == null || hash.length == 0) ? ItemType.FOLDER : ItemType.FILE;

            /*
             * Directories always have an empty hash.
             */
            if (!isExcluded(filename)
                && filter.shouldPend(new File(filename), OfflineChangeType.DELETE, serverItemType)) {
                offlineChanges.add(new OfflineChange(filename, OfflineChangeType.DELETE, serverItemType));
            }
        }
    }

    /**
     *
     * Checks exclusion list and filters to see if we should pend a deletion for
     * the provided filename.
     *
     * @param localFilename
     *        The filename to question
     * @return true if the file should be deleted, false otherwise
     */
    private boolean isExcluded(final String filename) {
        // make sure a parent isn't in the exclusion list
        for (final Iterator<String> j = excludes.iterator(); j.hasNext();) {
            final String base = j.next();

            if (filename.startsWith(base + File.separatorChar)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Determine if a local file differs from the server file. When the method
     * is WritableFiles, we simply check for the writable bit. When the method
     * is Md5Hash, we compare the file's current md5 hash with the server's.
     *
     * @param file
     *        the File to compare
     * @param expectedHash
     *        the server's idea of the hash code
     * @return true if the file has been modified, false otherwise
     * @throws IOException
     *         if the (local) file cannot be read
     * @throws UncheckedNoSuchAlgorithmException
     *         if the method is Md5Hash but md5 is not available
     * @throws FileNotFoundException
     *         if the (local) file cannot be opened
     * @throws canceledexception
     *         if the hash calculation was canceled via {@link #taskMonitor}
     */
    private boolean isChanged(final File file, final byte[] expectedHash)
        throws FileNotFoundException,
            UncheckedNoSuchAlgorithmException,
            IOException,
            CanceledException {
        // file has changed if it is writable. (pend edits for any
        // writable files, even if we're in md5hash mode, since these
        // would otherwise become writable conflicts.
        if (file.canWrite()) {
            return true;
        }

        // file has changed if md5 hash differs
        if (method == OfflineSynchronizerMethod.MD5_HASH) {
            final byte[] actualHash = HashUtils.hashFile(file, HashUtils.ALGORITHM_MD5, taskMonitor);
            return !Arrays.equals(actualHash, expectedHash);
        }

        return false;
    }

    /**
     * Resolve conflicting changes. This includes:
     *
     * If an item is pended for edit and is locally modified, do not pend an
     * edit.
     *
     * If an item is pended for deletion, and the local file exists, undo the
     * deletion and pend an edit.
     *
     * If an item is pended for add, and the local file does not exist, undo the
     * add.
     */
    private void resolveChanges() {
        final List<OfflineChange> undoChanges = new ArrayList<OfflineChange>();

        for (final Iterator<OfflineChange> i = offlineChanges.iterator(); i.hasNext();) {
            final OfflineChange change = i.next();

            for (int j = 0; j < serverChanges.length; j++) {
                if (taskMonitor.isCanceled()) {
                    throw new CanceledException();
                }

                if (serverChanges[j].getLocalItem() == null
                    || !LocalPath.equals(serverChanges[j].getLocalItem(), change.getLocalPath())) {
                    continue;
                }

                final ChangeType changeType = serverChanges[j].getChangeType();

                /* CONFLICT DETECTION */

                /*
                 * If the server has an add or edit already pended for this
                 * item, do not pend. In the add case, this is a server error;
                 * in the edit case, this is merely redundant.
                 */
                if (change.hasChangeType(OfflineChangeType.EDIT)
                    && (changeType.contains(ChangeType.ADD) || changeType.contains(ChangeType.EDIT))) {
                    i.remove();
                    continue;
                }

                /*
                 * If the server has an add pended for this item and the file
                 * does not exist locally (ie, we deleted it), then we need to
                 * undo the add.
                 */
                if (change.hasChangeType(OfflineChangeType.DELETE) && changeType.contains(ChangeType.ADD)) {
                    change.setChangeType(OfflineChangeType.UNDO);
                    continue;
                }

                /*
                 * If the server has an edit pended for this item, and it has
                 * been locally deleted, then we need to undo the edit and pend
                 * a delete.
                 */
                if (change.hasChangeType(OfflineChangeType.DELETE) && changeType.contains(ChangeType.EDIT)) {
                    change.setChangeType(OfflineChangeType.UNDO);
                    change.addChangeType(OfflineChangeType.DELETE);

                    continue;
                }

                /*
                 * If the server has a delete pended for this item, and it has
                 * been locally modified (ie, we detected it as an add), then we
                 * need to undo the delete and pend an edit instead.
                 */
                if (change.hasChangeType(OfflineChangeType.ADD) && changeType.contains(ChangeType.DELETE)) {
                    change.setChangeType(OfflineChangeType.UNDO);
                    change.addChangeType(OfflineChangeType.EDIT);

                    continue;
                }

                /*
                 * The target of a rename has been removed, undo the rename. If
                 * the source of the rename still exists locally, pend an edit
                 * of the source. If it does not, we pend a delete.
                 */
                if (change.hasChangeType(OfflineChangeType.DELETE) && changeType.contains(ChangeType.RENAME)) {
                    change.setChangeType(OfflineChangeType.UNDO);

                    final String sourceLocalPath = workspace.getMappedLocalPath(serverChanges[j].getSourceServerItem());
                    if (sourceLocalPath == null) {
                        continue;
                    }

                    final File sourceLocalFile = new File(sourceLocalPath);

                    /*
                     * The source of the rename no longer exists, pend a delete
                     */
                    if (!sourceLocalFile.exists()) {
                        if (!isExcluded(sourceLocalPath)
                            && filter.shouldPend(
                                sourceLocalFile,
                                OfflineChangeType.DELETE,
                                change.getServerItemType())) {
                            change.addChangeType(OfflineChangeType.DELETE);
                            change.setSourceLocalPath(sourceLocalPath);
                        }
                    }

                    /* The source of the rename still exists, pend an edit */
                    else {
                        /*
                         * The source of the rename will have been detected as
                         * an offline add (above.) We need to forget about that
                         * change, since we've got a better one.
                         */
                        undoChanges.add(
                            new OfflineChange(sourceLocalPath, OfflineChangeType.ADD, change.getServerItemType()));

                        if (!isExcluded(sourceLocalPath)
                            && filter.shouldPend(sourceLocalFile, OfflineChangeType.EDIT, null)) {
                            change.addChangeType(OfflineChangeType.EDIT);
                            change.setSourceLocalPath(sourceLocalPath);
                        }
                    }

                    continue;
                }
            }
        }

        offlineChanges.removeAll(undoChanges);
    }

    /**
     * Get the canonical local path for a file (if possible) or its absolute
     * (otherwise.)
     *
     * @param file
     *        File to get path for
     * @return a String representing the local path
     */
    private String canonicalPath(final File file) {
        try {
            return file.getCanonicalPath();
        } catch (final IOException e) {
            return file.getAbsolutePath();
        }
    }
}