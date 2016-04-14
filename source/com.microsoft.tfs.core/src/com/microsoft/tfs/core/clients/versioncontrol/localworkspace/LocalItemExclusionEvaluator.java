// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.localworkspace;

import java.io.File;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import com.microsoft.tfs.core.Messages;
import com.microsoft.tfs.core.clients.versioncontrol.VersionControlClient;
import com.microsoft.tfs.core.clients.versioncontrol.Workstation;
import com.microsoft.tfs.core.clients.versioncontrol.exceptions.ItemNotMappedException;
import com.microsoft.tfs.core.clients.versioncontrol.internal.localworkspace.LocalWorkspaceProperties;
import com.microsoft.tfs.core.clients.versioncontrol.internal.localworkspace.LocalWorkspaceTransaction;
import com.microsoft.tfs.core.clients.versioncontrol.path.LocalPath;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.WorkingFolder;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;
import com.microsoft.tfs.core.config.persistence.PersistenceStoreProvider;
import com.microsoft.tfs.util.Check;

/**
 * @threadsafety thread-compatible
 */
public class LocalItemExclusionEvaluator {
    /**
     * The name of the ignore file (like ".tfignore" but might be configured by
     * the user to be a different name).
     */
    public static final String IGNORE_FILE_NAME;

    /**
     * The name of the ignore file with the appropriate local filesystem path
     * separator prefix (like "/.tfignore" on Unix).
     */
    public static final String IGNORE_FILE_NAME_WITH_SEPARATOR_PREFIX;

    private static final String DEFAULT_IGNORE_FILE_NAME = ".tfignore"; //$NON-NLS-1$

    static {
        // TODO VS does:
        // TFCommonUtil.GetAppSetting("VersionControl.IgnoreFileName",
        // c_defaultIgnoreFileName);
        IGNORE_FILE_NAME = DEFAULT_IGNORE_FILE_NAME;

        IGNORE_FILE_NAME_WITH_SEPARATOR_PREFIX = File.separator + IGNORE_FILE_NAME;
    }

    private final String startLocalItem;
    private final int startLocalItemDepth;
    private final WorkingFolder[] workingFolders;

    /**
     * The workspace root corresponding to the start local item.
     *
     * Set by contructors through {@link #initialize(String[])} and only read
     * after.
     */
    private String startLocalItemWorkspaceRoot;

    /**
     * The ignore file stack. The first entry is the global exclusion list.
     * Subsequent entries represent .tfignore files.
     */
    private final List<IgnoreFile> ignoreFiles = new ArrayList<IgnoreFile>();

    /**
     * The directory for which the stack is currently ready to evaluate
     * exclusions.
     */
    private String currentDirectory;

    /**
     * Whether or not to load .tfignore files from below the start local item.
     */
    private boolean useOnlyStartLocalItemExclusions = false;

    /**
     * Create an LocalItemExclusionEvaluator object to evaluate local item
     * exclusions for the provided Workspace. Path parts at or above the start
     * local item will not be checked for exclusions.
     *
     * @param workspace
     *        the workspace to check (must not be <code>null</code>)
     * @param startLocalItem
     *        the start local item (must not be <code>null</code> or empty)
     */
    public LocalItemExclusionEvaluator(final Workspace workspace, final String startLocalItem) {
        Check.notNull(workspace, "workspace"); //$NON-NLS-1$
        Check.notNullOrEmpty(startLocalItem, "startLocalItem"); //$NON-NLS-1$

        if (!workspace.isLocalPathMapped(startLocalItem)) {
            throw new ItemNotMappedException(
                MessageFormat.format(
                    Messages.getString("LocalItemExclusionEvaluator.ItemNotMappedExceptionFormat"), //$NON-NLS-1$
                    startLocalItem));
        }

        this.workingFolders = workspace.getFolders();
        this.startLocalItem = startLocalItem;
        this.startLocalItemDepth = LocalPath.getFolderDepth(this.startLocalItem);

        initialize(
            Workstation.getCurrent(
                workspace.getClient().getConnection().getPersistenceStoreProvider()).getLocalItemExclusions(
                    workspace.getClient()));
    }

    public LocalItemExclusionEvaluator(final LocalWorkspaceProperties wp, final String startLocalItem) {
        Check.notNull(wp, "wp"); //$NON-NLS-1$
        Check.notNullOrEmpty(startLocalItem, "startLocalItem"); //$NON-NLS-1$

        if (null == WorkingFolder.getServerItemForLocalItem(startLocalItem, wp.getWorkingFolders())) {
            throw new ItemNotMappedException(
                MessageFormat.format(
                    Messages.getString("LocalItemExclusionEvaluator.ItemNotMappedExceptionFormat"), //$NON-NLS-1$
                    startLocalItem));
        }

        this.workingFolders = wp.getWorkingFolders();
        this.startLocalItem = startLocalItem;
        this.startLocalItemDepth = LocalPath.getFolderDepth(this.startLocalItem);

        // Use the current transaction to get the workspace
        final VersionControlClient client = LocalWorkspaceTransaction.getCurrent().getWorkspace().getClient();
        final PersistenceStoreProvider storeProvider = client.getConnection().getPersistenceStoreProvider();
        initialize(Workstation.getCurrent(storeProvider).getLocalItemExclusions(client));
    }

    private void initialize(final String[] globalExclusions) {
        // Find the workspace root of m_startLocalItem.
        for (final String workspaceRoot : WorkingFolder.getWorkspaceRoots(workingFolders)) {
            if (LocalPath.isChild(workspaceRoot, startLocalItem)) {
                startLocalItemWorkspaceRoot = workspaceRoot;
                break;
            }
        }

        if (null == startLocalItemWorkspaceRoot) {
            throw new ItemNotMappedException(
                MessageFormat.format(
                    Messages.getString("LocalItemExclusionEvaluator.ItemNotMappedExceptionFormat"), //$NON-NLS-1$
                    startLocalItem));
        }

        // From the m_startLocalItem up to the workspace root, insert the
        // IgnoreFile objects into the stack.
        // (This is in reverse order.)

        String currentItem = LocalPath.getParent(startLocalItem);
        String itemParent;

        if (currentItem != null && !LocalPath.equals(currentItem, startLocalItem)) {
            while (LocalPath.isChild(startLocalItemWorkspaceRoot, currentItem)) {
                final IgnoreFile ignoreFile = IgnoreFile.load(currentItem);

                // Putting null entries into the stack here is not necessary
                // since these
                // are not poppable (they are part of the base set of IgnoreFile
                // objects on this stack)
                if (null != ignoreFile) {
                    ignoreFiles.add(ignoreFile);
                }

                itemParent = LocalPath.getParent(currentItem);

                // It turned out that LocalPath.getParent might return null.
                if (currentItem == null || LocalPath.equals(itemParent, currentItem)) {
                    break;
                }

                currentItem = itemParent;
            }

            // Set the current directory.
            currentDirectory = currentItem;
        }

        // Add the global exclusion list to the stack.
        final IgnoreFile globalExclusionList = new IgnoreFile(""); //$NON-NLS-1$

        for (final String globalExclusion : globalExclusions) {
            globalExclusionList.addEntry(new IgnoreEntry(globalExclusion));
        }

        ignoreFiles.add(globalExclusionList);

        // Reverse the list, putting the global exclusion list at the bottom of
        // the stack
        Collections.reverse(ignoreFiles);
    }

    /**
     * If true, ignore files below the start local item will not be processed
     * when evaluating exclusions. The default value for this flag is false.
     */
    public boolean isUseOnlyStartLocalItemExclusions() {
        return useOnlyStartLocalItemExclusions;
    }

    public void setUseOnlyStartLocalItemExclusions(final boolean value) {
        useOnlyStartLocalItemExclusions = value;
    }

    /**
     * Check the provided local item against the LocalItemExclusionEvaluator to
     * see if it should be excluded from addition to version control.
     *
     * @param localItem
     *        the local item to check (must not be <code>null</code>)
     * @return <code>true</code> if the item is excluded; <code>false</code>
     *         otherwise
     */
    public boolean isExcluded(final String localItem) {
        return isExcluded(localItem, false /* isFolder */, null /* appliedExclusion */, null /* ignoreFilePath */);
    }

    /**
     * Check the provided local item against the LocalItemExclusionEvaluator to
     * see if it should be excluded from addition to version control.
     *
     * @param localItem
     *        the local item to check (must not be <code>null</code>)
     * @param isFolder
     *        <code>true</code> if the local item to check is a folder;
     *        <code>false</code> otherwise
     * @return <code>true</code> if the item is excluded; <code>false</code>
     *         otherwise
     */
    public boolean isExcluded(final String localItem, final boolean isFolder) {
        return isExcluded(localItem, isFolder, null /* appliedExclusion */, null /* ignoreFilePath */);
    }

    /**
     * Check the provided local item against the LocalItemExclusionEvaluator to
     * see if it should be excluded from addition to version control.
     *
     * @param localItem
     *        the local item to check (must not be <code>null</code>)
     * @param isFolder
     *        <code>true</code> if the local item to check is a folder;
     *        <code>false</code> otherwise
     * @param appliedExclusion
     *        if the item is excluded, the exclusion which was applied (may be
     *        <code>null</code>)
     * @param ignoreFilePath
     *        if the item is excluded, the name of the ignore file on disk which
     *        contains the applied exclusion. If the applied exclusion came from
     *        the global exclusion list for the Team Project Collection, the
     *        value is the empty string (may be <code>null</code>)
     * @return <code>true</code> if the item is excluded; <code>false</code>
     *         otherwise
     */
    public boolean isExcluded(
        final String localItem,
        final boolean isFolder,
        final AtomicReference<String> appliedExclusion,
        final AtomicReference<String> ignoreFilePath) {
        if (appliedExclusion != null) {
            appliedExclusion.set(null);
        }
        if (ignoreFilePath != null) {
            ignoreFilePath.set(null);
        }

        Check.isTrue(LocalPath.isChild(startLocalItem, localItem), "localItem must be a child of the startLocalItem"); //$NON-NLS-1$

        prepareStackForLocalItem(localItem);

        /*
         * Walk the stack of IgnoreFile objects in reverse. Entries in the stack
         * may be null -- this means that for that directory in the stack, there
         * is no ignore file. The top entry in the stack (m_ignoreFiles[0]) is
         * the global exclusion list.
         */
        for (int i = ignoreFiles.size() - 1; i >= 0; i--) {
            final IgnoreFile ignoreFile = ignoreFiles.get(i);

            if (null != ignoreFile) {
                final AtomicReference<String> innerAppliedExclusion = new AtomicReference<String>();
                final Boolean isExcluded =
                    ignoreFile.isExcluded(localItem, isFolder, startLocalItem, innerAppliedExclusion);

                if (isExcluded != null) {
                    if (appliedExclusion != null) {
                        appliedExclusion.set(innerAppliedExclusion.get());
                    }

                    if (ignoreFilePath != null) {
                        if (0 == i) {
                            // Global exclusion list
                            ignoreFilePath.set(""); //$NON-NLS-1$
                        } else {
                            // .tfignore file
                            ignoreFilePath.set(ignoreFile.getFullPath());
                        }
                    }

                    return isExcluded.booleanValue();
                }
            }
        }

        return false;
    }

    /**
     * Alters the stack of .tfignore files
     */
    private void prepareStackForLocalItem(final String localItemToCheck) {
        if (null == currentDirectory
            && (useOnlyStartLocalItemExclusions || LocalPath.equals(localItemToCheck, startLocalItem))) {
            // When m_currentDirectory is null, the stack is prepared to check
            // the start local item only.
            // If UseOnlyStartLocalItemExclusions is enabled, then we're
            // prepared to check any item at or beneath the start local item.
            return;
        }

        if (null != currentDirectory && LocalPath.isDirectChild(currentDirectory, localItemToCheck)) {
            // When m_currentDirectory is non-null, the stack is prepared to
            // check the immediate children of the
            // current directory.
            return;
        }

        // OK, we'll be changing directories. Get the new desired current
        // directory, which will be the parent of the local item to check.
        String newCurrentDirectory = LocalPath.getParent(localItemToCheck);

        if (LocalPath.equals(newCurrentDirectory, localItemToCheck)) {
            newCurrentDirectory = null;
        }

        // Get the path which is common to our current directory and our
        // destination. We'll pop off the stack until we reach the common path.
        String commonPath = null;

        if (null != currentDirectory && null != newCurrentDirectory) {
            commonPath = LocalPath.getCommonPathPrefix(localItemToCheck, currentDirectory);
        }

        int currentFolderDepth = 0, commonFolderDepth = 0;

        if (null != currentDirectory) {
            currentFolderDepth = LocalPath.getFolderDepth(currentDirectory);
        }

        if (null != commonPath) {
            commonFolderDepth = LocalPath.getFolderDepth(commonPath);
        }

        // Pop the number of frames we need to pop to reach the common folder.
        final int popCount = currentFolderDepth - commonFolderDepth;

        if (popCount > 0) {
            for (int i = 0; i < popCount; i++) {
                ignoreFiles.remove(ignoreFiles.size() - 1);
            }
        }

        currentDirectory = commonPath;

        // Advance from the common path to the target directory.
        if (null != newCurrentDirectory) {
            final List<IgnoreFile> ignoreFilesToAdd = new ArrayList<IgnoreFile>();

            String currentItem = newCurrentDirectory;
            String itemParent;

            while (true) {
                if (null != currentDirectory && currentItem.length() == currentDirectory.length()) {
                    break;
                }

                if (!LocalPath.isChild(startLocalItemWorkspaceRoot, currentItem)) {
                    break;
                }

                ignoreFilesToAdd.add(IgnoreFile.load(currentItem));

                itemParent = LocalPath.getParent(currentItem);

                if (LocalPath.equals(itemParent, currentItem)) {
                    break;
                }

                currentItem = itemParent;
            }

            Collections.reverse(ignoreFilesToAdd);
            ignoreFiles.addAll(ignoreFilesToAdd);

            currentDirectory = newCurrentDirectory;
        }
    }

}
