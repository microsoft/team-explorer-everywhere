// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.sdk.samples.console;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Date;

import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.core.clients.versioncontrol.GetOptions;
import com.microsoft.tfs.core.clients.versioncontrol.PendChangesOptions;
import com.microsoft.tfs.core.clients.versioncontrol.WorkspaceLocation;
import com.microsoft.tfs.core.clients.versioncontrol.WorkspacePermissionProfile;
import com.microsoft.tfs.core.clients.versioncontrol.exceptions.ServerPathFormatException;
import com.microsoft.tfs.core.clients.versioncontrol.path.LocalPath;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Changeset;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.GetRequest;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.LockLevel;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PendingChange;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PendingSet;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.RecursionType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.WorkingFolder;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;
import com.microsoft.tfs.core.clients.versioncontrol.specs.ItemSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.LatestVersionSpec;
import com.microsoft.tfs.core.util.FileEncoding;
import com.microsoft.tfs.jni.FileSystemAttributes;
import com.microsoft.tfs.jni.FileSystemUtils;

/**
 * This sample demonstrates version control operations such as creating a
 * workspace, creating a working folder mapping, getting files, making changes,
 * checking in changes, displaying history, and deleting a workspace.
 */
public class VersionControlSample {
    public static FileEncoding ENCODING = null;

    public static void main(final String[] args) {
        /*
         * NOTE: This sample omits some recommended SDK initialization steps for
         * clarity. See ConnectionAdvisorSample and LogConfigurationSample for
         * important information on initializing the TFS SDK for Java before
         * usage.
         */

        // Connect to TFS
        final TFSTeamProjectCollection tpc = ConsoleSettings.connectToTFS();

        // Create and map a new workspace
        final Workspace workspace = createAndMapWorkspace(tpc);

        // Listen to the get event
        addGetEventListeners(tpc);

        // Get latest on a project
        getLatest(workspace);

        // Do some pending changes

        // Add a new file
        final File newFile = addFile(workspace);

        // Edit a file
        editFile(workspace, newFile);

        // Rename a file
        final File renamedFile = renameFile(workspace, newFile);

        // Branch a file
        final File branchFile = branchFile(workspace, renamedFile);

        // Delete a file
        deleteFile(workspace, renamedFile);

        // View history on the branched file
        displayFileHistory(workspace, branchFile);

        // Delete the workspace for clean up
        tpc.getVersionControlClient().deleteWorkspace(workspace);
        System.out.println("Deleted the workspace"); //$NON-NLS-1$
    }

    public static Workspace createAndMapWorkspace(final TFSTeamProjectCollection tpc) {
        final String workspaceName = "SampleVCWorkspace" + System.currentTimeMillis(); //$NON-NLS-1$
        Workspace workspace = null;

        // Get the workspace
        workspace = tpc.getVersionControlClient().tryGetWorkspace(ConsoleSettings.MAPPING_LOCAL_PATH);

        // Create and map the workspace if it does not exist
        if (workspace == null) {
            workspace = tpc.getVersionControlClient().createWorkspace(
                null,
                workspaceName,
                "Sample workspace comment", //$NON-NLS-1$
                WorkspaceLocation.SERVER,
                null,
                WorkspacePermissionProfile.getPrivateProfile());

            // Map the workspace
            final WorkingFolder workingFolder = new WorkingFolder(
                ConsoleSettings.MAPPING_SERVER_PATH,
                LocalPath.canonicalize(ConsoleSettings.MAPPING_LOCAL_PATH));
            workspace.createWorkingFolder(workingFolder);
        }

        System.out.println("Workspace '" + workspaceName + "' now exists and is mapped"); //$NON-NLS-1$ //$NON-NLS-2$

        return workspace;
    }

    public static void addGetEventListeners(final TFSTeamProjectCollection tpc) {
        // Adding a get operation started event listener, this is fired once per
        // get call
        final SampleGetOperationStartedListener getOperationStartedListener = new SampleGetOperationStartedListener();
        tpc.getVersionControlClient().getEventEngine().addOperationStartedListener(getOperationStartedListener);

        // Adding a get event listener, this fired once per get operation(which
        // might be multiple times per get call)
        final SampleGetEventListener getListener = new SampleGetEventListener();
        tpc.getVersionControlClient().getEventEngine().addGetListener(getListener);

        // Adding a get operation completed event listener, this is fired once
        // per get call
        final SampleGetOperationCompletedListener getOperationCompletedListener =
            new SampleGetOperationCompletedListener();
        tpc.getVersionControlClient().getEventEngine().addOperationCompletedListener(getOperationCompletedListener);
    }

    public static void getLatest(final Workspace workspace) {
        final ItemSpec spec = new ItemSpec(ConsoleSettings.MAPPING_LOCAL_PATH, RecursionType.FULL);
        final GetRequest request = new GetRequest(spec, LatestVersionSpec.INSTANCE);
        workspace.get(request, GetOptions.NONE);
    }

    public static File addFile(final Workspace workspace) {

        // Create the file locally
        final File newFile = new File(ConsoleSettings.MAPPING_LOCAL_PATH, "SampleAppFile"); //$NON-NLS-1$
        writeFileContents(newFile, "", "UTF-8"); //$NON-NLS-1$ //$NON-NLS-2$

        // Pend an add
        // The encoding is passed as null and the add will detect the encoding
        // of the file
        workspace.pendAdd(new String[] {
            newFile.getAbsolutePath()
        }, false, ENCODING, LockLevel.UNCHANGED, GetOptions.NONE, PendChangesOptions.NONE);

        // Checkin the pending change
        final int cs = checkinPendingChanges(workspace, "Adding a sample file"); //$NON-NLS-1$

        System.out.println("Added file " + newFile.getAbsolutePath() + " in CS# " + cs); //$NON-NLS-1$ //$NON-NLS-2$

        return newFile;
    }

    public static void editFile(final Workspace workspace, final File file) {
        // Pend edit
        final ItemSpec fileSpec = new ItemSpec(file.getAbsolutePath(), RecursionType.NONE);
        workspace.pendEdit(
            new ItemSpec[] {
                fileSpec
        },
            LockLevel.UNCHANGED,
            ENCODING,

            GetOptions.NONE,
            PendChangesOptions.NONE);

        // Edit the file
        writeFileContents(file, "Edited this file at " + new Date().toString(), "UTF-8"); //$NON-NLS-1$ //$NON-NLS-2$

        // Checkin the pending change
        final int cs = checkinPendingChanges(workspace, "Editing the sample file"); //$NON-NLS-1$

        System.out.println("Edited file " + file.getAbsolutePath() + " in CS# " + cs); //$NON-NLS-1$ //$NON-NLS-2$
    }

    public static File renameFile(final Workspace workspace, final File file) {
        // Pend rename
        final File renamedFile = new File(ConsoleSettings.MAPPING_LOCAL_PATH, "RenamedSampleAppFile"); //$NON-NLS-1$
        workspace.pendRename(
            file.getAbsolutePath(),
            renamedFile.getAbsolutePath(),
            LockLevel.UNCHANGED,
            GetOptions.NONE,
            true,
            PendChangesOptions.NONE);

        // Checkin pending change
        final int cs = checkinPendingChanges(workspace, "Renaming the sample file to " + renamedFile.getName()); //$NON-NLS-1$

        System.out.println("Renamed file '" //$NON-NLS-1$
            + file.getAbsolutePath()
            + "' to '" //$NON-NLS-1$
            + renamedFile.getAbsolutePath()
            + "'  in CS# " //$NON-NLS-1$
            + cs);

        return renamedFile;
    }

    public static File branchFile(final Workspace workspace, final File file) {
        final File branchFile = new File(file.getAbsolutePath() + "-branch"); //$NON-NLS-1$
        workspace.pendBranch(
            file.getAbsolutePath(),
            branchFile.getAbsolutePath(),
            LatestVersionSpec.INSTANCE,
            LockLevel.UNCHANGED,
            RecursionType.NONE,
            GetOptions.NONE,
            PendChangesOptions.NONE);

        final int cs = checkinPendingChanges(workspace, "branched the sample file"); //$NON-NLS-1$

        System.out.println("Branched file '" //$NON-NLS-1$
            + file.getAbsolutePath()
            + "' to '" //$NON-NLS-1$
            + branchFile.getAbsolutePath()
            + "'  in CS# " //$NON-NLS-1$
            + cs);

        return branchFile;
    }

    public static void deleteFile(final Workspace workspace, final File file) {
        final ItemSpec renamedFileSpec = new ItemSpec(file.getAbsolutePath(), RecursionType.NONE);
        workspace.pendDelete(new ItemSpec[] {
            renamedFileSpec
        }, LockLevel.UNCHANGED, GetOptions.NONE, PendChangesOptions.NONE);

        final int cs = checkinPendingChanges(workspace, "Deleted the sample file"); //$NON-NLS-1$

        System.out.println("Deleted file " + file.getAbsolutePath() + " in CS# " + cs); //$NON-NLS-1$ //$NON-NLS-2$
    }

    public static int checkinPendingChanges(final Workspace workspace, final String comment) {
        final PendingSet pendingSet = workspace.getPendingChanges();
        int cs = 0;

        if (pendingSet != null) {
            final PendingChange[] pendingChanges = pendingSet.getPendingChanges();
            if (pendingChanges != null) {
                cs = workspace.checkIn(pendingChanges, comment);
            }

        }

        return cs;
    }

    public static void displayFileHistory(final Workspace workspace, final File file) {
        System.out.println("History for file " + file.getAbsolutePath()); //$NON-NLS-1$

        Changeset[] cs = null;
        try {
            cs = workspace.queryHistory(
                file.getCanonicalPath(),
                LatestVersionSpec.INSTANCE,
                0,
                RecursionType.NONE,
                null,
                null,
                LatestVersionSpec.INSTANCE,
                Integer.MAX_VALUE,
                true,
                false,
                false,
                false);
        } catch (final ServerPathFormatException e) {

            e.printStackTrace();
        } catch (final IOException e) {

            e.printStackTrace();
        }

        for (final Changeset changeset : cs) {
            System.out.println("CS#:" //$NON-NLS-1$
                + changeset.getChangesetID()
                + " ChangeType:" //$NON-NLS-1$
                + changeset.getChanges()[0].getChangeType().toUIString(true, changeset.getChanges()[0]));

        }
    }

    /**
     * Write the specified contents to the given file
     *
     * @param file
     *        File to write to
     *
     * @param contents
     *        Contents to write to the file
     *
     * @param encoding
     *        File encoding to use (null for default)
     *
     * @throws IOException
     */
    public static void writeFileContents(final File file, final String contents, final String encoding) {
        if (file.exists()) {
            setReadOnly(file, false);
        }

        BufferedWriter bw = null;
        try {
            try {
                bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), encoding));
                bw.write(contents);
            } finally {
                bw.close();
            }
        } catch (final Throwable t) {
            throw new RuntimeException("Failed to write contents to file " + file, t); //$NON-NLS-1$
        }
    }

    /**
     * Set or clear the readonly bit of the specified file
     *
     * @param file
     *        File to set/unset as readonly
     *
     * @param readOnly
     *        If true, make read-only. If false, make writable
     */
    public static void setReadOnly(final File file, final boolean readOnly) {
        final FileSystemAttributes attr = FileSystemUtils.getInstance().getAttributes(file);

        if (readOnly != attr.isReadOnly()) {
            attr.setReadOnly(readOnly);
            FileSystemUtils.getInstance().setAttributes(file, attr);
        }
    }
}
