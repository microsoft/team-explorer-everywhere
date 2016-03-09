// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.localworkspace;

import java.io.File;
import java.text.MessageFormat;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.microsoft.tfs.core.clients.versioncontrol.internal.localworkspace.BaselineFolderState;
import com.microsoft.tfs.core.clients.versioncontrol.path.LocalPath;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;
import com.microsoft.tfs.jni.FileSystemAttributes;
import com.microsoft.tfs.jni.FileSystemUtils;
import com.microsoft.tfs.jni.PlatformMiscUtils;
import com.microsoft.tfs.jni.WellKnownSID;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.GUID;
import com.microsoft.tfs.util.Platform;

public class BaselineFolder {
    private static final Log log = LogFactory.getLog(BaselineFolder.class);

    private static String[] PARTITIONING_FOLDERS;
    private static String[] POTENTIAL_BASELINE_FOLDER_NAME;
    private static final int PARTITIONING_FOLDER_COUNT = 16;

    /*
     * On Windows platforms, use $tf for parity with Visual Studio (and set the
     * hidden bit). On Unix platforms, use '.tf' so that Eclipse (and presumably
     * other toolings) will not treat it as a "normal" folder.
     */
    private static final String TF_FOLDER_NAME = Platform.isCurrentPlatform(Platform.WINDOWS) ? "$tf" : ".tf"; //$NON-NLS-1$ //$NON-NLS-2$

    private static final String GZ_EXTENSION = ".gz"; //$NON-NLS-1$
    private static final String RAW_EXTENSION = ".rw"; //$NON-NLS-1$

    static {
        // When building up the paths of baselines, we'll be incorporating the
        // partitioning folders
        // a lot. Let's keep a static cache of these values.
        PARTITIONING_FOLDERS = new String[PARTITIONING_FOLDER_COUNT];

        for (int i = 0; i < PARTITIONING_FOLDER_COUNT; i++) {
            PARTITIONING_FOLDERS[i] = Integer.toString(i);
        }

        // Pre-allocate the potential baseline folder names.
        POTENTIAL_BASELINE_FOLDER_NAME = new String[PARTITIONING_FOLDER_COUNT];
        POTENTIAL_BASELINE_FOLDER_NAME[0] = TF_FOLDER_NAME;

        for (int i = 1; i < PARTITIONING_FOLDER_COUNT; i++) {
            POTENTIAL_BASELINE_FOLDER_NAME[i] = TF_FOLDER_NAME + Integer.toString(i);
        }
    }

    public String partition;
    public String path;
    public BaselineFolderState state;

    public BaselineFolder(final String partition, final String path, final BaselineFolderState state) {
        this.partition = partition;
        this.path = path;
        this.state = state;
    }

    @Override
    public BaselineFolder clone() {
        return new BaselineFolder(this.partition, this.path, this.state);
    }

    /**
     * Given the location at which a baseline folder should be created, creates
     * a baseline folder on disk and returns it.
     *
     *
     * @param localFolder
     * @return
     */
    public static BaselineFolder create(final Workspace workspace, final String localFolder) {
        Check.notNullOrEmpty(localFolder, "localFolder"); //$NON-NLS-1$

        final File file = new File(localFolder);
        if (!file.exists()) {
            return null;
        }

        try {
            // The number of times we'll try to permute the $tf name to get
            // a folder laid down is the same as the number of partitions within
            // $tf folders.
            for (final String proposedFolderName : POTENTIAL_BASELINE_FOLDER_NAME) {
                final String proposedPath = LocalPath.combine(localFolder, proposedFolderName);

                final File proposedFile = new File(proposedPath);
                if (!proposedFile.exists()) {
                    ensureBaselineDirectoryExists(workspace, proposedPath);
                    createBaselineFolderStructure(proposedPath);
                    return new BaselineFolder(
                        getPartitionForPath(localFolder),
                        proposedPath,
                        BaselineFolderState.VALID);
                }
            }
        } catch (final Throwable t) {
        }

        // Unable to create a baseline folder here. The caller should try
        // to establish a baseline folder at a different location.
        return null;
    }

    /**
     * Given a path, creates a baseline folder structure at that path. Example
     * paths: @"D:\workspace\$tf" and
     *
     * "C:\ProgramData\TFS\Offline\[guid]\ws1;[domain/user]
     *
     * @param path
     */
    public static void createBaselineFolderStructure(final String path) {
        // Create the partitioning folders.
        for (int j = 0; j < PARTITIONING_FOLDER_COUNT; j++) {
            final String partitioningFolderPath = LocalPath.combine(path, PARTITIONING_FOLDERS[j]);
            final File directory = new File(partitioningFolderPath);

            if (!directory.exists()) {
                directory.mkdirs();
            }
        }
    }

    /**
     * Ensures that the local metadata directory for the given local workspace
     * exists. The path looks like:
     *
     * "C:\ProgramData\TFS\Offline\<guid>\<workspace>"
     *
     * The method has special logic to place an access control entry on the
     * folder for the owner, and to take ownership from BUILTIN\Administrators.
     * (This logic only runs if the directory is actually created by this
     * method.)
     *
     * @param workspace
     *        Workspace whose local metadata directory's existence should be
     *        ensured
     */
    public static void ensureLocalMetadataDirectoryExists(final Workspace workspace) {
        ensureLocalMetadataDirectoryExists(workspace, null);
    }

    /**
     * Ensures that the local metadata directory for the given local workspace
     * exists. The path looks like:
     * "C:\ProgramData\TFS\Offline\<guid>\<workstation>" on Windows and
     * "~/.microsoft/Team Foundation/4.0/Configuration/TFS-Offline" on Unix.
     *
     * On Windows, the method has special logic to place an access control entry
     * on the folder for the owner, and to take ownership from
     * BUILTIN\Administrators. (This logic only runs if the directory is
     * actually created by this method.)
     *
     * The method will also clone any additional access control entries from the
     * provided sourceDirectoryForAcl directory.
     *
     * @param workspace
     *        Workspace whose local metadata directory's existence should be
     *        ensured
     * @param sourceDirectoryForAcl
     *        Source directory for the access control list
     */
    public static void ensureLocalMetadataDirectoryExists(
        final Workspace workspace,
        final String sourceDirectoryForAcl) {
        final File directory = new File(workspace.getLocalMetadataDirectory());

        if (directory.exists()) {
            return;
        }

        directory.mkdirs();

        /*
         * The baseline metadata directory may be in a common location on
         * Windows, so set permissions on it.
         */
        if (Platform.isCurrentPlatform(Platform.WINDOWS)) {
            try {
                final String ownerSID = FileSystemUtils.getInstance().getOwner(directory.getAbsolutePath());
                final String currentSID = PlatformMiscUtils.getInstance().getCurrentIdentityUser();

                /*
                 * If the directory we just created is owned by
                 * BUILTIN\Administrators because we're running elevated, then
                 * take ownership of it.
                 */
                if (ownerSID.equals(
                    PlatformMiscUtils.getInstance().getWellKnownSID(WellKnownSID.WinBuiltinAdministratorsSid, null))) {
                    try {
                        FileSystemUtils.getInstance().setOwner(directory.getAbsolutePath(), currentSID);
                    } catch (final Exception e) {
                        /*
                         * If we fail to take ownership of the item, we'll
                         * continue on and try to set the ACE for the current
                         * user.
                         */
                        log.warn(MessageFormat.format("Error changing owner on {0} to {1}", directory, currentSID), e); //$NON-NLS-1$
                    }
                }

                String copyExplicitEntriesFromPath = null;
                if (sourceDirectoryForAcl != null
                    && new File(sourceDirectoryForAcl).exists()
                    && new File(sourceDirectoryForAcl).isDirectory()) {
                    copyExplicitEntriesFromPath = sourceDirectoryForAcl;
                }

                FileSystemUtils.getInstance().grantInheritableFullControl(
                    directory.getAbsolutePath(),
                    currentSID,
                    copyExplicitEntriesFromPath);
            } catch (final Exception e) {
                log.warn(MessageFormat.format("Error proper ensuring owner of {0}", directory), e); //$NON-NLS-1$
            }
        }

        // Add the NotContextIndexed bit to the attributes for the directory.
        final FileSystemAttributes attrs = FileSystemUtils.getInstance().getAttributes(directory);
        if (attrs != null) {
            attrs.setNotContentIndexed(true);
            FileSystemUtils.getInstance().setAttributes(directory, attrs);
        }
    }

    public static void ensureBaselineDirectoryExists(final Workspace workspace, final String directoryPath) {
        // We're going to copy access control entries from the local metadata
        // directory's access
        // control list, so make sure it exists on the disk.
        ensureLocalMetadataDirectoryExists(workspace);

        final File directory = new File(directoryPath);

        if (directory.exists()) {
            return;
        }

        directory.mkdirs();

        /*
         * The baseline metadata directory may be in a common location on
         * Windows, so set permissions on it.
         */
        if (Platform.isCurrentPlatform(Platform.WINDOWS)) {
            try {
                final String ownerSID = FileSystemUtils.getInstance().getOwner(directory.getAbsolutePath());
                final String currentSID = PlatformMiscUtils.getInstance().getCurrentIdentityUser();

                /*
                 * If the directory we just created is owned by
                 * BUILTIN\Administrators because we're running elevated, then
                 * take ownership of it.
                 */
                if (ownerSID.equals(
                    PlatformMiscUtils.getInstance().getWellKnownSID(WellKnownSID.WinBuiltinAdministratorsSid, null))) {
                    try {
                        FileSystemUtils.getInstance().setOwner(directory.getAbsolutePath(), currentSID);
                    } catch (final Exception e) {
                        /*
                         * If we fail to take ownership of the item, we'll
                         * continue on and try to set the ACE for the current
                         * user.
                         */
                        log.warn(MessageFormat.format("Error changing owner on {0} to {1}", directory, currentSID), e); //$NON-NLS-1$
                    }
                }

                FileSystemUtils.getInstance().copyExplicitDACLEntries(
                    workspace.getLocalMetadataDirectory(),
                    directory.getAbsolutePath());
            } catch (final Exception e) {
                log.warn(MessageFormat.format("Error proper ensuring owner of {0}", directory), e); //$NON-NLS-1$
            }
        }

        // Add the NotContextIndexed and Hidden bits to the attributes for the
        // directory.
        final FileSystemAttributes attrs = FileSystemUtils.getInstance().getAttributes(directory);
        if (attrs != null) {
            attrs.setNotContentIndexed(true);
            attrs.setHidden(true);
            FileSystemUtils.getInstance().setAttributes(directory, attrs);
        }
    }

    /**
     * Returns true is if the specified folderName is that of a potential
     * baseline folder name.
     *
     *
     * @param folderName
     *        The folder name to test (just the name not the full path).
     */
    public static boolean isPotentialBaselineFolderName(final String folderName) {
        for (final String potentialFolderName : POTENTIAL_BASELINE_FOLDER_NAME) {
            if (folderName.equalsIgnoreCase(potentialFolderName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Given a baseline file GUID, calculates the path that this baseline file
     * GUID would have in this baseline folder, without the extension (.rw or
     * .gz).
     *
     *
     * @param baselineFileGuid
     * @return
     */
    public String getPathFromGUID(final byte[] baselineFileGuid) {
        Check.notNull(this.path, "Should not call BaselineFolder.GetPathFromGuid on a BaselineFolder with a null Path"); //$NON-NLS-1$
        return getPathFromGUID(this.path, baselineFileGuid);
    }

    /**
     * Given the root baseline folder and a baseline file GUID, calculates the
     * path that this baseline file GUID would have in that root folder, without
     * the extension (.rw or .gz).
     *
     * Example values for baselineFolderRootPath: @"D:\workspace\$tf" -- from
     * the instance method GetPathFromGuid immediately above and
     *
     * "C:\ProgramData\TFS\Offline\[guid]\ws1;[domain/user]
     *
     *
     * @param baselineFolderRootPath
     *        Root folder of the baseline folder structure
     * @param baselineFileGuid
     *        Baseline file GUID whose path should be computed
     * @return
     */
    public static String getPathFromGUID(final String baselineFolderRootPath, final byte[] baselineFileGuid) {
        checkForValidBaselineFileGUID(baselineFileGuid);

        final AtomicReference<String> outIndividualBaselineFolder = new AtomicReference<String>();

        return getPathFromGUID(baselineFolderRootPath, baselineFileGuid, outIndividualBaselineFolder);
    }

    /**
     * Given the root baseline folder and a baseline file GUID, calculates the
     * path that this baseline file GUID would have in that root folder, without
     * the extension (.rw or .gz).
     *
     * Example values for baselineFolderRootPath: "D:\workspace\$tf" -- from the
     * instance method GetPathFromGuid immediately above
     * "C:\ProgramData\TFS\Offline\<guid>\<workspace>"
     *
     * @param baselineFolderRootPath
     *        Root folder of the baseline folder structure
     * @param baselineFileGuid
     *        Baseline file GUID whose path should be computed
     * @param String
     *        [out] A value equal to Path.GetDirectoryName(retval)
     * @return
     */
    public static String getPathFromGUID(
        final String baselineFolderRootPath,
        final byte[] baselineFileGuid,
        final AtomicReference<String> individualBaselineFolder) {
        checkForValidBaselineFileGUID(baselineFileGuid);

        // i.e. @"D:\workspace\$tf\1"
        individualBaselineFolder.set(baselineFolderRootPath
            + File.separator
            + PARTITIONING_FOLDERS[((char) baselineFileGuid[0]) % PARTITIONING_FOLDER_COUNT]);

        // i.e. @"D:\workspace\$tf\1\408bed21-9023-47c3-8280-b1ec3ffacd94"
        return individualBaselineFolder.get() + File.separator + new GUID(baselineFileGuid).getGUIDString();
    }

    /**
     * Throws an ArgumentException if the baseline file GUID provided is null or
     * has a length != 16 bytes.
     *
     *
     * @param baselineFileGuid
     *        Baseline file GUID to check
     */
    public static void checkForValidBaselineFileGUID(final byte[] baselineFileGuid) {
        Check.notNull(baselineFileGuid, "baselineFileGuid"); //$NON-NLS-1$
        Check.isTrue(baselineFileGuid.length == 16, "baselineFileGuid"); //$NON-NLS-1$
    }

    public String getPath() {
        return path;
    }

    public BaselineFolderState getState() {
        return state;
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj instanceof BaselineFolder) {
            final BaselineFolder other = (BaselineFolder) obj;

            return this.partition.equals(other.partition) && this.path.equals(other.path) && this.state == other.state;
        }

        return false;
    }

    @Override
    public int hashCode() {
        return partition.hashCode() * 17 + path.hashCode() * 11 + state.getValue() * 3;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("BaselineFolder Partition('"); //$NON-NLS-1$
        sb.append(partition);
        sb.append("'), State('"); //$NON-NLS-1$
        sb.append(state);
        sb.append("'), + Path('"); //$NON-NLS-1$
        sb.append(path);
        sb.append("')"); //$NON-NLS-1$
        return sb.toString();
    }

    public static String getPartitionForPath(final String localItem) {
        Check.notNullOrEmpty(localItem, "localItem"); //$NON-NLS-1$

        if (!LocalPath.isPathRooted(localItem)) {
            throw new IllegalArgumentException("getPartitionForPath: The path was not rooted."); //$NON-NLS-1$
        }

        return LocalPath.getPathRoot(localItem);
    }

    public static String getBaselineFolderName() {
        return TF_FOLDER_NAME;
    }

    public static String getGzipExtension() {
        return GZ_EXTENSION;
    }

    public static String getRawExtension() {
        return RAW_EXTENSION;
    }

    public static int getPartitioningFolderCount() {
        return PARTITIONING_FOLDER_COUNT;
    }
}
