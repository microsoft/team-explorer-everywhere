// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.internal.localworkspace;

import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.microsoft.tfs.core.clients.versioncontrol.WorkspacePermissionProfile;
import com.microsoft.tfs.core.clients.versioncontrol.exceptions.InvalidWorkspacePropertiesTableException;
import com.microsoft.tfs.core.clients.versioncontrol.exceptions.VersionControlException;
import com.microsoft.tfs.core.clients.versioncontrol.localworkspace.BaselineFolder;
import com.microsoft.tfs.core.clients.versioncontrol.localworkspace.LocalMetadataTable;
import com.microsoft.tfs.core.clients.versioncontrol.path.LocalPath;
import com.microsoft.tfs.core.clients.versioncontrol.path.internal.FileSystemWalker;
import com.microsoft.tfs.core.clients.versioncontrol.path.internal.FileSystemWalker.FileSystemVisitor;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.RecursionType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.WorkingFolder;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.WorkingFolderType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;
import com.microsoft.tfs.jni.FileSystemAttributes;
import com.microsoft.tfs.jni.FileSystemUtils;
import com.microsoft.tfs.jni.PlatformMiscUtils;
import com.microsoft.tfs.jni.WellKnownSID;
import com.microsoft.tfs.jni.helpers.FileCopyHelper;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.FileHelpers;
import com.microsoft.tfs.util.Platform;

public class LocalWorkspaceProperties extends LocalMetadataTable {
    private static final Log log = LogFactory.getLog(LocalWorkspaceProperties.class);

    /**
     * A map from the name of the metadata table (i.e. "localversion" or
     * "pendingchanges") to the BaselineFolder object where that table is
     * currently located. If the string maps to null or is not found in the
     * dictionary, then we expect to find the table in the same location as this
     * table (the C:\ProgramData location, which is a function of the collection
     * GUID, workspace name, and workspace owner).
     */
    private Map<String, BaselineFolder> metadataTableLocations;

    /**
     * The list of baseline folders ($tf folders) for this workspace.
     */
    private List<BaselineFolder> baselineFolders;

    /**
     * The authoritative set of working folders (mappings) for this workspace.
     */
    private List<WorkingFolder> workingFolders;

    /**
     * If team project renames have been received and applied, this member
     * contains the new project revision ID which needs to be acknowledged by
     * the client.
     */
    private int newProjectRevisionId;

    /**
     * The workspace which we are operating on
     */
    private Workspace workspace;

    /**
     * If non-zero, a (potential) write operation on this table resulted in a
     * BaselineFolderCollection associated with this transaction's lock being
     * locked for update. The write lock token is in this member. The write lock
     * must be released once the table is saved to disk.
     */
    private int baselineFoldersWriteLockToken;

    /**
     * The string postfixed to the server path of a mapping with one-level
     * recursion.
     */
    private static final String ONE_LEVEL_MAPPING = "/*"; //$NON-NLS-1$

    /**
     * Magic numbers for binary format.
     */
    private static final short MAGIC = 0x7E3C;
    private static final byte SCHEMA_VERSION2 = 2;

    public LocalWorkspaceProperties(final String tableLocation, final LocalWorkspaceProperties cachedLoadSource)
        throws Exception {
        super(tableLocation, cachedLoadSource);
        /* Don't do anything here, Initialize() is called first. */
    }

    @Override
    protected void initialize() {
        baselineFolders = new ArrayList<BaselineFolder>();
        workingFolders = new ArrayList<WorkingFolder>();
        metadataTableLocations = new HashMap<String, BaselineFolder>();
        workspace = LocalWorkspaceTransaction.getCurrent().getWorkspace();
    }

    public List<BaselineFolder> getBaselineFolders() {
        return baselineFolders;
    }

    @Override
    protected void load(final InputStream is) throws Exception {
        log.trace("--- Entering load ---"); //$NON-NLS-1$
        final BinaryReader br = new BinaryReader(is, "UTF-16LE"); //$NON-NLS-1$

        try {
            try {
                final short magic = br.readInt16();
                log.trace("magic: " + magic); //$NON-NLS-1$

                if (MAGIC != magic) {
                    throw new InvalidWorkspacePropertiesTableException();
                }

                final byte schemaVersion = br.readByte();
                log.trace("version: " + schemaVersion); //$NON-NLS-1$

                if (schemaVersion == SCHEMA_VERSION2) {
                    loadFromVersion2(br, true);
                } else {
                    throw new InvalidWorkspacePropertiesTableException();
                }
            } catch (final Exception e) {
                log.trace("--- leaving load with exception ---", e); //$NON-NLS-1$
                log.trace(""); //$NON-NLS-1$
                if (e instanceof InvalidWorkspacePropertiesTableException) {
                    throw (InvalidWorkspacePropertiesTableException) e;
                } else {
                    // Wrap the exception
                    throw new InvalidWorkspacePropertiesTableException(e);
                }
            }
        } finally {
            br.close();
        }
        log.trace("--- Leaving load ---"); //$NON-NLS-1$
        log.trace(""); //$NON-NLS-1$
    }

    private void loadFromVersion2(final BinaryReader br, final boolean isVersionTwo) throws Exception {
        log.trace("--- Entering loadFromVersion2 ---"); //$NON-NLS-1$
        baselineFolders.clear();

        /*
         * The first two bytes of the version 2 schema contain the number of
         * BaselineFolder objects in the table.
         */
        final int count = br.readInt16();
        log.trace("baseline folders count: " + count); //$NON-NLS-1$

        for (int i = 0; i < count; i++) {
            String partition = br.readString();
            log.trace("baseline folder partition: " + partition); //$NON-NLS-1$

            // Null is encoded as a zero-length string in the table.
            if (partition.length() == 0) {
                partition = null;
            }

            String path = br.readString();
            log.trace("baseline folder path: " + path); //$NON-NLS-1$

            // Null is encoded as a zero-length string in the table.
            if (path.length() == 0) {
                path = null;
            }

            final BaselineFolderState state = BaselineFolderState.fromByte(br.readByte());
            log.trace("baseline folder state: " + state); //$NON-NLS-1$
            baselineFolders.add(new BaselineFolder(partition, path, state));
        }

        /*
         * The next two bytes of the version 2 schema contain the number of
         * entries in the baseline folder map.
         */
        final int mapCount = br.readInt16();
        log.trace("baseline folder map count: " + mapCount); //$NON-NLS-1$

        for (int i = 0; i < mapCount; i++) {
            // The name of the table.
            final String tableName = br.readString();
            log.trace("baseline folder map table name: " + tableName); //$NON-NLS-1$

            if (tableName.length() == 0) {
                continue;
            }

            // The index in baselineFolders of the baseline folder which
            // contains the table.
            final short index = br.readInt16();
            log.trace("baseline folder map table index: " + index); //$NON-NLS-1$

            if (index >= 0 && index < count) {
                metadataTableLocations.put(tableName, baselineFolders.get(index));
            } else {
                metadataTableLocations.put(tableName, null);
            }
        }

        /*
         * The next two bytes of the version 2 schema contain the number of
         * working folders in the workspace.
         */
        final int workingFolderCount = br.readInt16();
        log.trace("working folder count: " + workingFolderCount); //$NON-NLS-1$

        for (int i = 0; i < workingFolderCount; i++) {
            WorkingFolderType type = WorkingFolderType.MAP;
            RecursionType recursionType = RecursionType.FULL;

            // The server path of the mapping.
            String serverItem = br.readString();
            log.trace("working folder server item: " + serverItem); //$NON-NLS-1$

            if (serverItem.length() == 0) {
                continue;
            }

            // The local path of the mapping.
            String localItem = br.readString();
            log.trace("working folder local item: " + localItem); //$NON-NLS-1$

            if (localItem.length() == 0) {
                localItem = null;

                // This is a cloak.
                type = WorkingFolderType.CLOAK;
            } else if (serverItem.endsWith(ONE_LEVEL_MAPPING)) {
                // This is a one-level mapping.
                recursionType = RecursionType.ONE_LEVEL;

                // Remove the trailing /*.
                serverItem = serverItem.substring(0, serverItem.length() - ONE_LEVEL_MAPPING.length());
            }

            workingFolders.add(new WorkingFolder(serverItem, localItem, type, recursionType));
        }

        /*
         * Is there a trailing integer on the end of the table? This is a
         * NewProjectRevisionId value.
         */
        if (isVersionTwo && !br.isEOF()) {
            newProjectRevisionId = br.readInt32();
            log.trace("working folder new project revision id: " + newProjectRevisionId); //$NON-NLS-1$
        } else {
            newProjectRevisionId = 0;
        }

        log.trace("--- Leaving loadFromVersion2 ---"); //$NON-NLS-1$
    }

    @Override
    protected boolean cachedLoad(final LocalMetadataTable source) {
        if (source instanceof LocalWorkspaceProperties) {
            final LocalWorkspaceProperties wpCached = (LocalWorkspaceProperties) source;

            baselineFolders = wpCached.baselineFolders;
            metadataTableLocations = wpCached.metadataTableLocations;
            workingFolders = wpCached.workingFolders;
            newProjectRevisionId = wpCached.newProjectRevisionId;

            return true;
        }

        return false;
    }

    @Override
    protected boolean save(final OutputStream os) throws IOException {
        log.trace("--- Entering save ---"); //$NON-NLS-1$
        final BinaryWriter bw = new BinaryWriter(os, "UTF-16LE"); //$NON-NLS-1$

        try {
            bw.write(MAGIC);
            log.trace("magic: " + MAGIC); //$NON-NLS-1$
            writeToVersion2(bw);
        } finally {
            bw.close();
        }

        log.trace("--- Leaving save ---"); //$NON-NLS-1$
        return true;
    }

    @Override
    public void close() throws IOException {
        super.close();

        // If we locked a BaselineFolderCollection instance (which is a cache of
        // this table's contents) because we were updating -- then we need to
        // write the updated data to this object, and release the write lock.
        // This will unblock any other threads which are waiting to read from
        // the BaselineFolderCollection.
        if (0 != baselineFoldersWriteLockToken) {
            final WorkspaceLock wLock = LocalWorkspaceTransaction.getCurrent().getWorkspaceLock();
            final BaselineFolderCollection baselineFolders = wLock.getBaselineFolders();

            if (null != baselineFolders) {
                baselineFolders.updateFrom(this.baselineFolders);
                baselineFolders.unlockForWrite(baselineFoldersWriteLockToken);
                baselineFoldersWriteLockToken = 0;
            }
        }

    }

    @Override
    protected void saveComplete() {
        // Write backup copies of this table into each baseline folder. This is
        // for recoverability in case the data in the ProgramData location
        // disappears.
        final String sourceLocation = getSlotOnePath(getFilename());
        final String fileNamePart = LocalPath.getFileName(sourceLocation);

        for (final BaselineFolder baselineFolder : baselineFolders) {
            try {
                if (BaselineFolderState.VALID != baselineFolder.getState()) {
                    // We *don't* want a backup copy to exist in this folder.
                    // Just make sure it's not there in case we had put one
                    // there previously. (Otherwise autorecovery logic might
                    // read an out of date copy, which we'd like to avoid.)
                    final String potentialOldBackupCopy = LocalPath.combine(baselineFolder.getPath(), fileNamePart);

                    if (new File(potentialOldBackupCopy).exists()) {
                        FileHelpers.deleteFileWithoutException(potentialOldBackupCopy);
                    }
                } else {
                    // Put a backup copy of the table into this baseline folder,
                    // overwriting any previous backup copy that was present at
                    // this location.
                    FileCopyHelper.copy(sourceLocation, LocalPath.combine(baselineFolder.getPath(), fileNamePart));
                }
            } catch (final Exception ex) {
                // Failed to create a backup copy of the properties table in
                // this baseline folder.
                log.trace(ex);
            }
        }
    }

    private void writeToVersion2(final BinaryWriter bw) throws IOException {
        log.trace("--- Entering writeToVersion2 ---"); //$NON-NLS-1$
        bw.write(SCHEMA_VERSION2);
        log.trace("version: " + SCHEMA_VERSION2); //$NON-NLS-1$

        /*
         * The first two bytes of the version 2 schema encode the number of
         * BaselineFolder objects in the file.
         */
        bw.write((short) baselineFolders.size());
        log.trace("baseline folders size: " + baselineFolders.size()); //$NON-NLS-1$

        for (final BaselineFolder baselineFolder : baselineFolders) {
            bw.write(baselineFolder.partition == null ? "" : baselineFolder.partition); //$NON-NLS-1$
            log.trace(
                "baseline folder partition: " + (baselineFolder.partition == null ? "" : baselineFolder.partition)); //$NON-NLS-1$ //$NON-NLS-2$
            bw.write(baselineFolder.path == null ? "" : baselineFolder.path); //$NON-NLS-1$
            log.trace("baseline folder path: " + (baselineFolder.path == null ? "" : baselineFolder.path)); //$NON-NLS-1$ //$NON-NLS-2$
            bw.write((byte) baselineFolder.state.getValue());
            log.trace("baseline folder state: " + baselineFolder.state); //$NON-NLS-1$
        }

        /*
         * The next two bytes of the version 2 schema encode the number of
         * entries in the metadata table location map.
         */
        bw.write((short) metadataTableLocations.size());
        log.trace("baseline folder maps count: " + metadataTableLocations.size()); //$NON-NLS-1$

        for (final String key : metadataTableLocations.keySet()) {
            final BaselineFolder value = metadataTableLocations.get(key);
            bw.write(key == null ? "" : key); //$NON-NLS-1$
            log.trace("baseline folder map name: " + (key == null ? "" : key)); //$NON-NLS-1$ //$NON-NLS-2$

            short index = -1;
            if (null != value) {
                index = (short) baselineFolders.indexOf(value);
            }

            bw.write(index);
            log.trace("baseline folder maps index: " + index); //$NON-NLS-1$
        }

        /*
         * The next two bytes of the version 2 schema encode the number of
         * working folders in the workspace.
         */
        bw.write((short) workingFolders.size());
        log.trace("working folders count: " + workingFolders.size()); //$NON-NLS-1$

        for (final WorkingFolder workingFolder : workingFolders) {
            String serverItem = workingFolder.getServerItem();

            if (RecursionType.ONE_LEVEL == workingFolder.getDepth()) {
                // Postfix the one-level mapping terminator.
                serverItem = serverItem.concat(ONE_LEVEL_MAPPING);
            }

            bw.write(serverItem);
            log.trace("working folder server item: " + serverItem); //$NON-NLS-1$

            if (WorkingFolderType.CLOAK == workingFolder.getType()) {
                bw.write(""); //$NON-NLS-1$
                log.trace("working folder local item: " + ""); //$NON-NLS-1$ //$NON-NLS-2$
            } else {
                bw.write(workingFolder.getLocalItem());
                log.trace("working folder local item: " + workingFolder.getLocalItem()); //$NON-NLS-1$
            }
        }

        // Append the new project revision ID to the end of the format
        if (newProjectRevisionId > 0) {
            bw.write(newProjectRevisionId);
            log.trace("working folder new project revision id: " + newProjectRevisionId); //$NON-NLS-1$
        }

        log.trace("--- Leaving writeToVersion2 ---"); //$NON-NLS-1$
        log.trace(""); //$NON-NLS-1$
    }

    /**
     * Returns the full path of the directory containing the metadata table
     * provided. Does not return null.
     */
    public String getMetadataTableLocation(final String tableName) {
        Check.notNullOrEmpty(tableName, "tableName"); //$NON-NLS-1$

        BaselineFolder expectedBaselineFolder = null;
        if (metadataTableLocations.containsKey(tableName)) {
            expectedBaselineFolder = metadataTableLocations.get(tableName);
        }

        String expectedLocation;
        if (null == expectedBaselineFolder || null == expectedBaselineFolder.path) {
            // If we have no baseline folder for this table, then we expect the
            // metadata table to be located in the same location as this table.
            expectedLocation = LocalPath.getParent(getFilename());
        } else {
            // We expect the metadata table to be located in the baseline folder
            // specified.
            expectedLocation = expectedBaselineFolder.path;
        }

        // 2. Is the metadata table actually present at that location?
        final String expectedFileName = LocalPath.combine(expectedLocation, tableName);

        // 3. If present, return the root path to the table (without any file
        // extension).
        if (isMetadataTablePresentAtLocation(expectedFileName)) {
            return expectedFileName;
        }

        // 4. The metadata table was not found where we expect it to be. Let's
        // try to find it.
        for (final BaselineFolder eachBaselineFolder : baselineFolders) {
            if (null == eachBaselineFolder.path) {
                continue;
            }

            final String potentialFileName = LocalPath.combine(eachBaselineFolder.path, tableName);

            if (isMetadataTablePresentAtLocation(potentialFileName)) {
                // OK, we found the metadata table
                metadataTableLocations.put(tableName, eachBaselineFolder);
                setDirty(true);

                return potentialFileName;
            }
        }

        // 5. If we haven't already, check the ProgramData location.
        if (null != expectedBaselineFolder && null != expectedBaselineFolder.path) {
            final String programDataLocationFileName =
                LocalPath.combine(workspace.getLocalMetadataDirectory(), tableName);

            if (isMetadataTablePresentAtLocation(programDataLocationFileName)) {
                // OK, we found the metadata table
                metadataTableLocations.put(tableName, null);
                setDirty(true);

                return programDataLocationFileName;
            }
        }

        // 6. Could not find the metadata table. Let's return the path where we
        // would create a new one.
        BaselineFolder selectedBaselineFolder = null;
        for (final BaselineFolder baselineFolder : baselineFolders) {
            if (baselineFolder.path != null && baselineFolder.state == BaselineFolderState.VALID) {
                selectedBaselineFolder = baselineFolder;
                break;
            }
        }

        if (null != selectedBaselineFolder) {
            final BaselineFolder currentMetadataTableFolder = metadataTableLocations.get(tableName);
            if (currentMetadataTableFolder == null || currentMetadataTableFolder != selectedBaselineFolder) {
                metadataTableLocations.put(tableName, selectedBaselineFolder);
                setDirty(true);
            }

            final String path = selectedBaselineFolder.path;
            BaselineFolder.ensureLocalMetadataDirectoryExists(workspace, path);
            return LocalPath.combine(path, tableName);
        } else {
            // Use m_workspace.LocalMetadataDirectory to host the metadata table
            if (!metadataTableLocations.containsKey(tableName) || metadataTableLocations.get(tableName) != null) {
                metadataTableLocations.put(tableName, null);
                setDirty(true);
            }

            BaselineFolder.ensureLocalMetadataDirectoryExists(workspace);
            return LocalPath.combine(workspace.getLocalMetadataDirectory(), tableName);
        }
    }

    /**
     * Given a baseline file GUID, removes it from the baseline folder in which
     * it resides.
     *
     *
     * @param baselineFileGuid
     *        Baseline file GUID to remove
     */
    public void deleteBaseline(final byte[] baselineFileGuid) {
        BaselineFolderCollection.deleteBaseline(workspace, baselineFolders, baselineFileGuid);
    }

    /**
     * Given a local path on disk and the baseline file GUID corresponding to
     * that item, ensures that the baseline file for that item is in the correct
     * BaselineFolder.
     *
     *
     * @param baselineFileGuid
     *        Baseline file GUID of the item
     * @param currentLocalItem
     *        Current local path of the item
     */
    public void updateBaselineLocation(final byte[] baselineFileGuid, final String currentLocalItem) {
        BaselineFolderCollection.updateBaselineLocation(workspace, baselineFolders, baselineFileGuid, currentLocalItem);
    }

    /**
     * Given a baseline file GUID and a target location on disk, copies the
     * baseline from the baseline store to the target location. (The target
     * location always receives a decompressed copy of the baseline, even if it
     * is stored compressed in the baseline folder.)
     *
     *
     * @param baselineFileGuid
     *        Baseline file GUID to copy
     * @param targetLocalItem
     *        Target location for the baseline file
     * @param baselineFileLength
     *        (optional) If provided, the uncompressed baseline length will be
     *        compared against this value and checked after decompression. If
     *        the values do not match, an exception will be thrown.
     * @param baselineHashValue
     *        (optional) If provided, the uncompressed baseline will be hashed
     *        and its hash compared to this value after decompression. If the
     *        values to not match, an exception will be thrown.
     * @return True if the operation succeeded; false if the baseline could not
     *         be located
     * @throws IOException
     */
    public void copyBaselineToTarget(
        final byte[] baselineFileGuid,
        final String targetLocalItem,
        final long baselineFileLength,
        final byte[] baselineHashValue,
        final boolean symlink) {
        BaselineFolderCollection.copyBaselineToTarget(
            workspace,
            baselineFolders,
            baselineFileGuid,
            targetLocalItem,
            baselineFileLength,
            baselineHashValue,
            symlink);
    }

    /**
     * Given a BaselineFolder object, moves its contents to another
     * BaselineFolder, if one can be located; if not, to the ProgramData
     * location. The BaselineFolder specified is then deleted from disk and
     * removed from the baseline folders list for this workspace.
     *
     *
     * @param baselineFolder
     *        BaselineFolder to remove
     */
    public void removeBaselineFolder(final BaselineFolder baselineFolder) {
        Check.notNull(baselineFolder, "baselineFolder"); //$NON-NLS-1$
        Check.notNullOrEmpty(baselineFolder.path, "baselineFolder.Path"); //$NON-NLS-1$

        if (!baselineFolders.contains(baselineFolder)) {
            return;
        }

        // If we find any baselines still in the folder structure, we don't know
        // their local paths.
        // We'll look for a different BaselineFolder on the same partition which
        // is in the Valid state.
        BaselineFolder newBaselineFolder = null;

        for (final BaselineFolder bf : baselineFolders) {
            if (null == bf.path || BaselineFolderState.VALID != bf.state || baselineFolder == bf) {
                continue;
            }

            if (LocalPath.equals(baselineFolder.partition, bf.partition)) {
                // This other BaselineFolder is on the same partition. Let's use
                // that one!
                newBaselineFolder = bf;
                break;
            } else if (null == newBaselineFolder) {
                newBaselineFolder = bf;
            }
        }

        // About to modify the structure of the baseline folders on disk and/or
        // the data in m_baselineFolders, so lock the BaselineFolderCollection
        // cache object which is associated with the lock protecting this
        // transaction (if it exists).
        invalidateBaselineFolderCache();

        if (moveBaselineFolderStructure(baselineFolder.path, newBaselineFolder)) {
            // Delete the baseline folder, which is now void of anything useful.
            // What may be still
            // present are the partitioning folders (see the BaselineFolder
            // class for details) and
            // any lost temporary files (.tmp extension).
            try {
                FileHelpers.deleteDirectory(baselineFolder.path);
            } catch (final Throwable t) {
                log.warn(t);
            }

            // Remove the baseline folder from the list.
            baselineFolders.remove(baselineFolder);
        } else {
            // We couldn't get rid of everything from the baseline folder. We'll
            // mark it as
            // stale, and will try again later.
            baselineFolder.state = BaselineFolderState.STALE;
        }

        setDirty(true);
    }

    /**
     * Moves anything of value (baselines, metadata tables) from the specified
     * source baseline folder to the specified target baseline folder.
     *
     *
     * @param rootBaselineFolderPath
     *        Source baseline folder to empty
     * @param targetBaselineFolder
     *        Target baseline folder (null if targetBaselineFolderPath is the
     *        ProgramData location)
     * @return True if the folder rootBaselineFolderPath was successfully
     *         cleared
     */
    private boolean moveBaselineFolderStructure(
        final String rootBaselineFolderPath,
        final BaselineFolder targetBaselineFolder) {
        if (!new File(rootBaselineFolderPath).exists()) {
            return true;
        }

        boolean successfullyCleared = true;
        String targetBaselineFolderPath;

        if (null != targetBaselineFolder) {
            targetBaselineFolderPath = targetBaselineFolder.getPath();
            BaselineFolder.ensureBaselineDirectoryExists(workspace, targetBaselineFolder.getPath());
        } else {
            targetBaselineFolderPath = workspace.getLocalMetadataDirectory();
            BaselineFolder.ensureLocalMetadataDirectoryExists(workspace);
        }

        BaselineFolder.createBaselineFolderStructure(targetBaselineFolderPath);

        for (final EnumeratedLocalItem localItem : new LocalItemEnumerable(rootBaselineFolderPath, true, true, null)) {
            if (localItem.isDirectory()) {
                continue;
            }

            String extension = LocalPath.getFileExtension(localItem.getFullPath());
            if (extension == null) {
                extension = ""; //$NON-NLS-1$
            }

            if (extension.equalsIgnoreCase(BaselineFolder.getGzipExtension())
                || extension.equalsIgnoreCase(BaselineFolder.getRawExtension())) {
                // Baseline to move.
                final String relativePath = LocalPath.makeRelative(localItem.getFullPath(), rootBaselineFolderPath);
                final String newPath = LocalPath.combine(targetBaselineFolderPath, relativePath);

                try {
                    LocalWorkspaceProperties.rename(localItem.getFullPath(), newPath);
                } catch (final Exception e) {
                    throw new VersionControlException(e);
                }
            }
            // For metadata tables, we only need to consider slots one and two
            // (.tf1 and .tf2) as these
            // are the only authoritative slots. See LocalMetadataTable.cs.
            else if (extension.equalsIgnoreCase(LocalMetadataTable.FILE_EXTENSION_SLOT_ONE)
                || extension.equalsIgnoreCase(LocalMetadataTable.FILE_EXTENSION_SLOT_TWO)) {
                // Metadata table to move.

                // Trim the extension from the path to get the metadata table
                // name.
                final String metadataTablePath =
                    localItem.getFullPath().substring(0, localItem.getFullPath().length() - extension.length());

                // Update the metadata table location. This method will move
                // both the .tf1 and .tf2 files.
                // (Although it would be very rare for both to be present on
                // disk simultaneously.)
                if (!moveMetadataTable(metadataTablePath, targetBaselineFolder)) {
                    successfullyCleared = false;
                }
            }
        }

        return successfullyCleared;
    }

    /**
     * Given the path to a metadata table (without the extension), i.e.
     * "D:\workspace\$tf\pendingchanges" moves the metadata table to the
     * specified target baseline folder. If the target baseline folder is null,
     * the metadata table is moved to the ProgramData location for this
     * workspace.
     *
     *
     * @param metadataTablePath
     *        Metadata table to move
     * @param targetBaselineFolder
     *        BaselineFolder to move the metadata table to
     * @return True if the metadata table was successfully moved
     */
    private boolean moveMetadataTable(final String metadataTablePath, final BaselineFolder targetBaselineFolder) {
        // The metadata table being moved must not be open.
        Check.isTrue(
            LocalWorkspaceTransaction.getCurrent().getOpenedTables().contains(Tables.WORKSPACE_PROPERTIES),
            "LocalWorkspaceTransaction.getCurrent().getOpenedTables().contains(Tables.WORKSPACE_PROPERTIES)"); //$NON-NLS-1$

        // We cannot move ourselves.
        if (LocalPath.equals(metadataTablePath, getFilename())) {
            return false;
        }

        // About to modify the structure of the baseline folders on disk and/or
        // the data in m_baselineFolders, so lock the BaselineFolderCollection
        // cache object which is associated with the lock protecting this
        // transaction (if it exists).
        invalidateBaselineFolderCache();

        final String slotOnePath = getSlotOnePath(metadataTablePath);
        final String slotTwoPath = getSlotTwoPath(metadataTablePath);
        final String metadataTableName = LocalPath.getFileName(metadataTablePath);

        final String targetMetadataTablePath = LocalPath.combine(
            targetBaselineFolder != null ? targetBaselineFolder.path : workspace.getLocalMetadataDirectory(),
            metadataTableName);

        final String targetSlotOnePath = getSlotOnePath(targetMetadataTablePath);
        final String targetSlotTwoPath = getSlotTwoPath(targetMetadataTablePath);

        final BaselineFolder currentBaselineFolder = metadataTableLocations.get(metadataTableName);

        try {
            setDirty(true);
            metadataTableLocations.put(metadataTableName, targetBaselineFolder);

            // We only care about moving slot one if it exists, and slot two if
            // slot one does not exist.
            // See LocalMetadataTable.cs for details on the atomic write
            // methodology used.
            if (new File(slotOnePath).exists()) {
                LocalWorkspaceProperties.rename(slotOnePath, targetSlotOnePath);

                final FileSystemAttributes attributes = FileSystemUtils.getInstance().getAttributes(targetSlotOnePath);
                attributes.setNotContentIndexed(true);
                FileSystemUtils.getInstance().setAttributes(targetSlotOnePath, attributes);
            } else if (new File(slotTwoPath).exists()) {
                LocalWorkspaceProperties.rename(slotTwoPath, targetSlotTwoPath);

                final FileSystemAttributes attributes = FileSystemUtils.getInstance().getAttributes(targetSlotTwoPath);
                attributes.setNotContentIndexed(true);
                FileSystemUtils.getInstance().setAttributes(targetSlotTwoPath, attributes);
            }
        } catch (final Throwable t) {
            // The move failed; restore the previous metadata table location
            // before throwing.
            metadataTableLocations.put(metadataTableName, currentBaselineFolder);
            throw new VersionControlException(t);
        }

        setDirty(true);
        return true;
    }

    public void applyAceToWorkingFolders(final String sidString, final boolean addOrRemove) {
        // Check.notNullOrEmpty(sidString, "sidString"); //$NON-NLS-1$

        final List<String> foldersToReceiveAce = new ArrayList<String>(workingFolders.size());

        for (final WorkingFolder workingFolder : workingFolders) {
            final String path = workingFolder.getLocalItem();
            if (path != null) {
                foldersToReceiveAce.add(new File(path).getAbsolutePath());
            }
        }

        if (Platform.isCurrentPlatform(Platform.WINDOWS)) {
            for (final String folderToReceiveAce : foldersToReceiveAce) {
                applyAceToFileOrFolder(folderToReceiveAce, sidString, addOrRemove);
            }
        } else {
            final FileSystemWalker walker = new FileSystemWalker(
                workspace,
                foldersToReceiveAce.toArray(new String[foldersToReceiveAce.size()]),
                true,
                true,
                true,
                true);

            walker.walk(new FileSystemVisitor() {
                @Override
                public void visit(final String path) {
                    applyAceToFileOrFolder(path, sidString, addOrRemove);
                }
            });
        }
    }

    public void applyAceToBaselineFolders(final String sidString, final boolean addOrRemove) {
        // Check.notNullOrEmpty(sidString, "sidString"); //$NON-NLS-1$

        BaselineFolder.ensureLocalMetadataDirectoryExists(workspace);

        final List<File> foldersToReceiveAce = new ArrayList<File>(baselineFolders.size() + 1);
        foldersToReceiveAce.add(new File(workspace.getLocalMetadataDirectory()));

        for (final BaselineFolder baselineFolder : baselineFolders) {
            foldersToReceiveAce.add(new File(baselineFolder.getPath()));
        }

        for (int i = 0; i < foldersToReceiveAce.size(); i++) {
            final File folderToReceiveAce = foldersToReceiveAce.get(i);
            applyAceToFileOrFolder(folderToReceiveAce.getAbsolutePath(), sidString, addOrRemove);

            if (!Platform.isCurrentPlatform(Platform.WINDOWS)) {
                final File[] subFolders = folderToReceiveAce.listFiles(new FileFilter() {
                    @Override
                    public boolean accept(final File file) {
                        if (file.isDirectory()) {
                            return true;
                        } else {
                            applyAceToFileOrFolder(file.getAbsolutePath(), sidString, addOrRemove);
                            return false;
                        }
                    }
                });

                foldersToReceiveAce.addAll(Arrays.asList(subFolders));
            }
        }
    }

    public void applyAceToFileOrFolder(final String path, final String sidString, final boolean addOrRemove) {
        Check.notNullOrEmpty(path, "path"); //$NON-NLS-1$
        // Check.notNullOrEmpty(sidString, "sidString"); //$NON-NLS-1$

        try {
            final File file = new File(path);

            if (file.exists()) {
                if (Platform.isCurrentPlatform(Platform.WINDOWS)) {
                    if (addOrRemove) {
                        // Adds one explicit "allow" entry for this user
                        FileSystemUtils.getInstance().grantInheritableFullControl(
                            file.getAbsolutePath(),
                            sidString,
                            null);
                    } else {
                        // Removes all explicit "allow" entries for this user
                        FileSystemUtils.getInstance().removeExplicitAllowEntries(file.getAbsolutePath(), sidString);
                    }
                } else {
                    final FileSystemAttributes attrs = FileSystemUtils.getInstance().getAttributes(file);

                    if (attrs.isDirectory()) {
                        attrs.setExecutable(true);
                    }
                    attrs.setPublicWritable(addOrRemove);

                    FileSystemUtils.getInstance().setAttributes(file, attrs);
                }
            }
        } catch (final Exception e) {
            log.warn(MessageFormat.format("Error applying access control rules to {0}", path), e); //$NON-NLS-1$
        }

    }

    public void applyPermissionsProfileToBaselineFolders(final WorkspacePermissionProfile profile) {
        final String usersSIDString = Platform.isCurrentPlatform(Platform.WINDOWS)
            ? PlatformMiscUtils.getInstance().getWellKnownSID(WellKnownSID.WinBuiltinUsersSid, null) : null;

        if (profile.getName().equals(WorkspacePermissionProfile.BUILTIN_PROFILE_NAME_PUBLIC)
            || profile.getName().equals(WorkspacePermissionProfile.BUILTIN_PROFILE_NAME_PUBLIC_LIMITED)) {
            // Create an ACE for the Everyone SID.
            applyAceToBaselineFolders(usersSIDString, true);
        } else if (profile.getName().equals(WorkspacePermissionProfile.BUILTIN_PROFILE_NAME_PRIVATE)) {
            // Remove any ACE for the Everyone SID.
            applyAceToBaselineFolders(usersSIDString, false);
        }
    }

    public void applyPermissionsProfileToWorkingFolders(final WorkspacePermissionProfile profile) {
        final String usersSIDString = Platform.isCurrentPlatform(Platform.WINDOWS)
            ? PlatformMiscUtils.getInstance().getWellKnownSID(WellKnownSID.WinBuiltinUsersSid, null) : null;

        if (profile.getName().equals(WorkspacePermissionProfile.BUILTIN_PROFILE_NAME_PUBLIC)
            || profile.getName().equals(WorkspacePermissionProfile.BUILTIN_PROFILE_NAME_PUBLIC_LIMITED)) {
            // Create an ACE for the Everyone SID.
            applyAceToWorkingFolders(usersSIDString, true);
        } else if (profile.getName().equals(WorkspacePermissionProfile.BUILTIN_PROFILE_NAME_PRIVATE)) {
            // Remove any ACE for the Everyone SID.
            applyAceToWorkingFolders(usersSIDString, false);
        }
    }

    /**
     * Invoked by the client object model at various times to perform
     * maintenance on the baseline folders. Maintenance actions include: 1.
     * Creating baseline folders on partitions which previously did not have a
     * folder which could parent a baseline folder. 2. Trying to empty and
     * remove stale baseline folders (into valid ones). 3. Trying to empty the
     * ProgramData location into a baseline folder, which is always preferred.
     */
    public void doBaselineFolderMaintenance() {
        // Grab the latest working folders for the workspace.
        final WorkingFolder[] workingFolders = getWorkingFolders();

        // Map from each partition to the set of workspace roots on that
        // partition
        final Map<String, List<String>> partitions = new HashMap<String, List<String>>();

        for (final String workspaceRoot : WorkingFolder.getWorkspaceRoots(workingFolders)) {
            final String partition = BaselineFolder.getPartitionForPath(workspaceRoot);

            List<String> partitionRoots = partitions.get(partition);
            if (partitionRoots == null) {
                partitionRoots = new ArrayList<String>();
                partitions.put(partition, partitionRoots);
            }

            partitionRoots.add(workspaceRoot);
        }

        // Map from each partition to the set of extant baseline folders on that
        // partition
        final Map<String, List<BaselineFolder>> baselineMap = new HashMap<String, List<BaselineFolder>>();

        for (final BaselineFolder bf : baselineFolders) {
            if (!partitions.containsKey(bf.partition)) {
                // This BaselineFolder is on a partition that has no workspace
                // root.
                setDirty(true);
                invalidateBaselineFolderCache();
                bf.state = BaselineFolderState.STALE;
            }

            List<BaselineFolder> partitionFolders = baselineMap.get(bf.partition);
            if (partitionFolders == null) {
                partitionFolders = new ArrayList<BaselineFolder>();
                baselineMap.put(bf.partition, partitionFolders);
            }

            partitionFolders.add(bf);
        }

        final List<String> partitionsNeedingBaselineFolders = new ArrayList<String>();

        for (final String key : partitions.keySet()) {
            final List<BaselineFolder> extantBaselineFoldersOnPartition = baselineMap.get(key);
            if (extantBaselineFoldersOnPartition == null) {
                // No baseline folder on this partition; try to create one
                partitionsNeedingBaselineFolders.add(key);
            } else {
                BaselineFolder extantValidBf = null;
                for (final BaselineFolder baselineFolder : extantBaselineFoldersOnPartition) {
                    if (baselineFolder.state == BaselineFolderState.VALID) {
                        extantValidBf = baselineFolder;
                        break;
                    }
                }

                if (null != extantValidBf) {
                    final String baselineFolderParent = LocalPath.getParent(extantValidBf.path);

                    final List<String> partitionValue = partitions.get(key);
                    if (!partitionValue.contains(baselineFolderParent)) {
                        // We have an extant, valid baseline folder for this
                        // partition, but the parenting folder is not
                        // a workspace root. Maybe one of the stale baseline
                        // folders could be made valid?
                        BaselineFolder staleBf = null;

                        for (final BaselineFolder baselineFolder : extantBaselineFoldersOnPartition) {
                            if (baselineFolder.state == BaselineFolderState.STALE
                                && partitionValue.contains(LocalPath.getParent(baselineFolder.path))) {
                                staleBf = baselineFolder;
                                break;
                            }
                        }

                        if (null != staleBf) {
                            // Awesome, we don't have to create a new baseline
                            // folder; a stale one that we already have will
                            // work fine.
                            setDirty(true);
                            invalidateBaselineFolderCache();
                            staleBf.state = BaselineFolderState.VALID;
                        } else {
                            partitionsNeedingBaselineFolders.add(key);
                        }

                        setDirty(true);
                        invalidateBaselineFolderCache();
                        extantValidBf.state = BaselineFolderState.STALE;
                    }
                }
            }
        }

        // OK, we've calculated our work to perform at this point:
        // 1. We should try to create baseline folders on the partitions in the
        // list partitionsNeedingBaselineFolders.
        // 2. After that, we should try to empty and remove all stale baseline
        // folders from m_baselineFolders.
        // 3. After that, we should try to empty the ProgramData location into
        // any valid baseline folder.

        // 1. We should try to create baseline folders on the partitions in the
        // list partitionsNeedingBaselineFolders.
        for (final String partition : partitionsNeedingBaselineFolders) {
            final List<String> partitionRoots = partitions.get(partition);
            BaselineFolder baselineFolder = null;

            for (final String partitionRoot : partitionRoots) {
                // This will return null if the baseline folder could not be
                // created at the target location (for example if the directory
                // does not exist on the local disk, or if the workspace root is
                // not a directory).
                baselineFolder = BaselineFolder.create(workspace, partitionRoot);

                if (null != baselineFolder) {
                    break;
                }
            }

            if (null != baselineFolder) {
                if (0 == baselineFolders.size()) {
                    // We're transitioning from 0 to 1 baseline folders. To save
                    // a write later, update the expected metadata table
                    // locations for LV and PC to point to this new location.
                    metadataTableLocations.put("localversion", baselineFolder); //$NON-NLS-1$
                    metadataTableLocations.put("pendingchanges", baselineFolder); //$NON-NLS-1$
                }

                baselineFolders.add(baselineFolder);
                invalidateBaselineFolderCache();
                setDirty(true);
            }
        }

        // 2. We should try to empty and remove all stale baseline folders from
        // m_baselineFolders. We would prefer to empty stale baseline folders
        // into valid baseline folders on the same partition. Otherwise, we'll
        // empty them into any valid baseline folder that we find. If we can't
        // find a valid folder in which to empty, we'll empty into the
        // ProgramData location.
        final List<BaselineFolder> staleFolders = new ArrayList<BaselineFolder>();

        for (final BaselineFolder folder : baselineFolders) {
            if (folder.state == BaselineFolderState.STALE) {
                staleFolders.add(folder);
            }
        }

        for (final BaselineFolder staleBf : staleFolders) {
            removeBaselineFolder(staleBf);
        }

        // 3. We should try to empty the ProgramData location into any valid
        // baseline folder.
        BaselineFolder validBaselineFolder = null;
        for (final BaselineFolder baselineFolder : baselineFolders) {
            if (baselineFolder.state == BaselineFolderState.VALID) {
                validBaselineFolder = baselineFolder;
                break;
            }
        }

        if (null != validBaselineFolder) {
            moveBaselineFolderStructure(workspace.getLocalMetadataDirectory(), validBaselineFolder);
        }

        // If the scanner is asynchronous, then this is a good place in the code
        // path to ask it to make sure all his PathWatchers are running. Get may
        // have created directories that the scanner wants to watch, but
        // couldn't earlier, when updating the working folders called
        // Scanner.Invalidate.
        workspace.getWorkspaceWatcher().ensureWatching();
    }

    public void invalidateBaselineFolderCache() {
        final WorkspaceLock wLock = LocalWorkspaceTransaction.getCurrent().getWorkspaceLock();
        final BaselineFolderCollection baselineFolders = wLock.getBaselineFolders();

        if (null != baselineFolders && 0 == baselineFoldersWriteLockToken) {
            baselineFoldersWriteLockToken = baselineFolders.lockForWrite();
        }
    }

    /**
     * Given the file name for a metadata table (full path minus the .tf*
     * extension), returns true if the metadata table is present and false if it
     * is missing.
     *
     *
     * @param fileName
     *        The potential location for the metadata table on disk
     * @return True if the metadata table is present on disk at the location
     *         specified.
     */
    private boolean isMetadataTablePresentAtLocation(final String fileName) {
        // Example values for fileName:
        // @"D:\workspace\$tf\pendingchanges"
        // @"C:\ProgramData\TFS\Offline\11c92875-fac4-4277-afba-d16f6eeb2189\ws1;domain;username\pendingchanges"
        Check.notNullOrEmpty(fileName, "fileName"); //$NON-NLS-1$

        boolean presentAtLocation = false;

        try {
            // This is only a valid check if the table in question is not
            // currently open. Make sure that only the workspace properties
            // table is open right now.
            Check.isTrue(
                LocalWorkspaceTransaction.getCurrent().getOpenedTables().contains(Tables.WORKSPACE_PROPERTIES),
                "LocalWorkspaceTransaction.getCurrent().getOpenedTables().contains(Tables.WORKSPACE_PROPERTIES)"); //$NON-NLS-1$

            if (new File(getSlotOnePath(fileName)).exists() || new File(getSlotTwoPath(fileName)).exists()) {
                presentAtLocation = true;
            }
        } catch (final Exception e) {
        }

        return presentAtLocation;
    }

    public WorkingFolder[] getWorkingFolders() {
        return workingFolders.toArray(new WorkingFolder[workingFolders.size()]);
    }

    public void setWorkingFolders(final WorkingFolder[] workingFolders) {
        this.workingFolders = new ArrayList<WorkingFolder>(workingFolders.length);
        final WorkingFolder[] newWorkingFolders = WorkingFolder.clone(workingFolders);

        for (final WorkingFolder workingFolder : newWorkingFolders) {
            this.workingFolders.add(workingFolder);
        }

        setDirty(true);
    }

    public int getNewProjectRevisionId() {
        return newProjectRevisionId;
    }

    public void setNewProjectRevisionId(final int newProjectRevisionId) {
        if (this.newProjectRevisionId != newProjectRevisionId) {
            setDirty(true);
            this.newProjectRevisionId = newProjectRevisionId;
        }
    }

    private static void rename(final String source, final String target) throws FileNotFoundException, IOException {
        try {
            // Will throw IOException if source and target are on different file
            // systems on UNIX.
            FileHelpers.rename(source, target);
        } catch (final IOException e) {
            FileCopyHelper.copy(source, target);
            final File fSource = new File(source);
            fSource.delete();
        }
    }
}
