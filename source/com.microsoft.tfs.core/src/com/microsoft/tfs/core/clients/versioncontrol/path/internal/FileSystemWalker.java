// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.path.internal;

import java.io.File;
import java.io.FileFilter;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Set;
import java.util.Stack;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicReference;

import com.microsoft.tfs.core.Messages;
import com.microsoft.tfs.core.clients.versioncontrol.Workstation;
import com.microsoft.tfs.core.clients.versioncontrol.events.EventSource;
import com.microsoft.tfs.core.clients.versioncontrol.events.NonFatalErrorEvent;
import com.microsoft.tfs.core.clients.versioncontrol.exceptions.ItemCloakedException;
import com.microsoft.tfs.core.clients.versioncontrol.exceptions.ItemNotMappedException;
import com.microsoft.tfs.core.clients.versioncontrol.exceptions.LocalPathFormatException;
import com.microsoft.tfs.core.clients.versioncontrol.exceptions.OnlyOneWorkspaceException;
import com.microsoft.tfs.core.clients.versioncontrol.exceptions.VersionControlException;
import com.microsoft.tfs.core.clients.versioncontrol.localworkspace.BaselineFolder;
import com.microsoft.tfs.core.clients.versioncontrol.localworkspace.LocalItemExclusionEvaluator;
import com.microsoft.tfs.core.clients.versioncontrol.path.ItemPath;
import com.microsoft.tfs.core.clients.versioncontrol.path.LocalPath;
import com.microsoft.tfs.core.clients.versioncontrol.path.ServerPath;
import com.microsoft.tfs.core.clients.versioncontrol.path.Wildcard;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PathTranslation;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;
import com.microsoft.tfs.core.exceptions.InputValidationException;
import com.microsoft.tfs.jni.FileSystemAttributes;
import com.microsoft.tfs.jni.FileSystemUtils;
import com.microsoft.tfs.util.Check;

/**
 *
 * Walks the file system finding files that match a filter criterion and are
 * actively mapped into the user's workspace.
 * <p>
 * <!-- Event Origination Info -->
 * <p>
 * This class is an <b>core event origination point</b>. The {@link EventSource}
 * object that accompanies each event fired by this method describes the
 * execution context (current thread, etc.) when and where this method was
 * invoked.
 *
 * @threadsafety thread-compatible
 */
public class FileSystemWalker {
    /**
     * Defines an interface for objects which can visit files and directories
     * found by {@link FileSystemWalker}.
     *
     * @threadsafety thread-compatible
     */
    public static interface FileSystemVisitor {
        /**
         * Invoked when a file or directory is found.
         *
         * @param path
         *        the absolute local path to the file or directory (must not be
         *        <code>null</code>)
         */
        void visit(final String path);
    }

    private final Workspace workspace;
    private final boolean isRecursive;
    private final boolean includeDirectories;
    private final boolean treatMissingItemsAsFiles;
    private final boolean applyLocalItemExclusions;

    private LocalItemExclusionEvaluator m_localItemExclusionEvaluator;
    private final Set<String> m_appliedExclusions = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);

    private final Stack<String> dirStack;
    private final String[] fileSpecs;
    private boolean fileSpecIsDir;
    private int fileSpecIndex;
    private String fileNamePattern;
    private String currentFileSpec;
    private boolean fileSpecHasMatch;

    /**
     * Creates a new file system walker. Repeated calls to Walk() return the
     * matching files.
     *
     * @param workspace
     *        the workspace in which files are being added
     * @param fileSpecs
     *        the file spec filter(s)
     * @param isRecursive
     *        if false, process only the directory specified by fileSpec; if
     *        true, walk the file system recursively starting at fileSpec
     * @param includeDirectories
     *        if true, include directories in the results; if false, include
     *        only files
     * @param treatMissingItemsAsFiles
     *        if false, treats missing files as non fatal errors
     * @param applyLocalItemExclusions
     *        if true, applies local item exclusions when scanning, if false no
     *        local item exclusions are applied
     */
    public FileSystemWalker(
        final Workspace workspace,
        final String[] fileSpecs,
        final boolean isRecursive,
        final boolean includeDirectories,
        final boolean treatMissingItemsAsFiles,
        final boolean applyLocalItemExclusions) {
        this.workspace = workspace;
        this.isRecursive = isRecursive;
        this.includeDirectories = includeDirectories;
        this.treatMissingItemsAsFiles = treatMissingItemsAsFiles;
        this.applyLocalItemExclusions = applyLocalItemExclusions;

        // Save the file specs.
        this.fileSpecIndex = 0;
        this.fileSpecs = new String[fileSpecs.length];
        System.arraycopy(fileSpecs, 0, this.fileSpecs, 0, fileSpecs.length);

        // The directories are queued starting from the file spec and then as
        // encountered.
        this.dirStack = new Stack<String>();

        // Start with the first file spec.
        nextFileSpec();
    }

    /**
     * Walks the files
     *
     * Only files that are actively mapped in the user's workspace (determined
     * by the directory being processed) are included in the results. Non-fatal
     * error events are generated for cases where file attributes are not
     * accessible due to security restrictions. Any directories without working
     * folder mappings are skipped with non-fatal error events generated.
     *
     * @param visitor
     *        the visitor to invoke each time a matching file is found (must not
     *        be <code>null</code>)
     *
     * @throws OnlyOneWorkspaceException
     *         if more than one workspace is encountered
     */
    public void walk(final FileSystemVisitor visitor) throws OnlyOneWorkspaceException {
        Check.notNull(visitor, "visitor"); //$NON-NLS-1$

        // Process each directory.
        while (dirStack.size() > 0) {
            String dirName = dirStack.pop();

            // Get the directory name from the file system so that the casing is
            // correct.
            dirName = LocalPath.canonicalize(dirName);

            final Workspace workspaceForFolder = workspace.getClient().getWorkspace(dirName);
            Check.notNull(workspaceForFolder, MessageFormat.format("workspace for {0}", dirName)); //$NON-NLS-1$

            // The workspace must be the same for all files processed.
            if (null != workspaceForFolder && !workspaceForFolder.equals(workspace)) {
                throw new OnlyOneWorkspaceException(workspaceForFolder, dirName);
            }

            // If this is recursive on a directory and this is the first time
            // we've processed it,
            // include the directory itself in the results and then clear the
            // m_fileSpecIsDir
            // flag to indicate that we've done it for this file spec.
            if (fileSpecIsDir && (isRecursive || fileNamePattern == null)) {
                fileSpecIsDir = false;
                fileSpecHasMatch = true;

                if (includeDirectories) {
                    visitor.visit(dirName);
                }
            }

            // Find each file and directory that matches the specified
            // m_fileNamePattern (might be a wildcard).
            String[] fileNameList;

            if (fileNamePattern != null) {
                fileNameList = getFullPathsForSubFilesAndDirectories(dirName, fileNamePattern);
            } else {
                fileNameList = new String[0];
            }

            if (null == m_localItemExclusionEvaluator
                && workspace.isLocalPathMapped(dirName)
                && applyLocalItemExclusions) {
                m_localItemExclusionEvaluator = new LocalItemExclusionEvaluator(workspace, dirName);
            }

            // Sort them so that the get displayed in the proper order.
            Arrays.sort(fileNameList, LocalPath.TOP_DOWN_COMPARATOR);

            for (final String fileName : fileNameList) {
                // Filter out hidden and system files/directories.
                FileSystemAttributes attributes;
                try {
                    attributes = FileSystemUtils.getInstance().getAttributes(fileName);
                    if (!attributes.exists()) {
                        throw new VersionControlException(
                            MessageFormat.format(
                                Messages.getString("FileSystemWalker.FileOrFolderNotFoundFormat"), //$NON-NLS-1$
                                fileName));
                    }
                } catch (final Exception exception) {
                    // If the file or directory appears in the list, but we
                    // don't have access to the file
                    // system attributes, let the user know but keep going. This
                    // should be extremely rare.
                    workspace.getClient().getEventEngine().fireNonFatalError(
                        new NonFatalErrorEvent(EventSource.newFromHere(), workspace, exception));
                    continue;
                }

                if (attributes.isHidden() || attributes.isSystem()) {
                    continue;
                }

                final boolean isDirectory = attributes.isDirectory();

                if (Wildcard.isWildcard(fileNamePattern) && isIgnored(fileName, isDirectory)) {
                    // ignored items, are skipped silently
                    continue;
                }

                // Add this to the list of matches unless it's a directory and
                // directories are not included.
                if (!isDirectory || includeDirectories) {
                    try {
                        checkForIllegalDollarInPath(fileName);
                    } catch (final InputValidationException exception) {
                        // Report the error unless it will be reported below
                        // when we add the
                        // directories to the recursion stack.
                        if (!isDirectory || !isRecursive) {
                            workspace.getClient().getEventEngine().fireNonFatalError(
                                new NonFatalErrorEvent(EventSource.newFromHere(), workspace, exception));
                        }
                        continue;
                    }

                    fileSpecHasMatch = true;
                    visitor.visit(fileName);
                }
            }

            // If processing recursively, queue up subdirectories. Do not
            // combine with the code above
            // because that prevents going down into subdirectories unless the
            // subdirectory names match
            // the patterns.
            if (isRecursive) {
                final String[] dirs = getFullPathsForSubDirectories(dirName);

                // Sort them so that the get displayed in the proper order.
                Arrays.sort(dirs, LocalPath.TOP_DOWN_COMPARATOR);

                for (int i = dirs.length; i-- > 0;) {
                    FileSystemAttributes attributes;
                    try {
                        attributes = FileSystemUtils.getInstance().getAttributes(dirs[i]);
                    } catch (final Exception exception) {
                        continue;
                    }

                    if (attributes.isHidden() || attributes.isSystem() || attributes.isSymbolicLink()) {
                        continue;
                    }

                    if (isIgnored(dirs[i], true /* isFolder */)) {
                        // ignored items, are skipped silently
                        continue;
                    }

                    // Paths containing parts that start with a dollar sign are
                    // not allowed in
                    // the repository.
                    // Do this before checking to see whether it is cloaked,
                    // which would throw.
                    try {
                        checkForIllegalDollarInPath(dirs[i]);
                    } catch (final InputValidationException exception) {
                        workspace.getClient().getEventEngine().fireNonFatalError(
                            new NonFatalErrorEvent(EventSource.newFromHere(), workspace, exception));
                        continue;
                    }

                    // Skip cloaked folders.
                    if (!workspaceForFolder.isLocalPathMapped(dirs[i])) {
                        continue;
                    }

                    dirStack.push(dirs[i]);
                }
            }

            // When the directory queue is empty, move to the next file spec.
            if (dirStack.size() == 0) {
                if (hasNoFileMatches() && treatMissingItemsAsFiles) {
                    fileSpecHasMatch = true;
                    visitor.visit(currentFileSpec);
                }

                nextFileSpec();
            }
        }
    }

    /**
     * Return true if fileName represents special item which should be ignored
     * during add, e.g. $tf
     */
    private boolean isIgnored(final String fileName, final boolean isFolder) {
        if (BaselineFolder.isPotentialBaselineFolderName(LocalPath.getFileName(fileName))) {
            return true;
        }

        if (null != m_localItemExclusionEvaluator) {
            final AtomicReference<String> appliedExclusion = new AtomicReference<String>();

            if (m_localItemExclusionEvaluator.isExcluded(fileName, isFolder, appliedExclusion, null)) {
                m_appliedExclusions.add(appliedExclusion.get());

                return true;
            }
        }

        return false;
    }

    public String[] getExclusionsApplied() {
        return m_appliedExclusions.toArray(new String[m_appliedExclusions.size()]);
    }

    /**
     * Throws a {@link InputValidationException} if the server path would
     * contains an illegal dollar sign.
     */
    private void checkForIllegalDollarInPath(final String localPath) throws InputValidationException {
        try {
            LocalPath.checkForIllegalDollarInPath(localPath);
            return;
        } catch (final InputValidationException e) {
            // IGNORE
            // we found an illegal dollar sign in a local path but let's
            // convert it to a server path to see if the illegal char
            // is only part of a mapping (in which case we're ok with it)
        }

        // this will throw InputValidationException if the path contains an
        // illegal $ but it will not throw if the item is not mapped (will
        // return null)
        workspace.translateLocalPathToServerPath(localPath);
    }

    /**
     * Gets a flag indicating the existence on a no match found condition.
     */
    private boolean hasNoFileMatches() {
        return (currentFileSpec != null && !fileSpecHasMatch);
    }

    /**
     * Checks to see if no file matched the current file spec. If no file
     * matched, this method generates a non-fatal error event.
     */
    private void checkNoFileMatches() {
        // Once the current file spec is set, generate a non fatal error if it
        // didn't match anything.
        if (hasNoFileMatches()) {
            workspace.getClient().getEventEngine().fireNonFatalError(
                new NonFatalErrorEvent(
                    EventSource.newFromHere(),
                    workspace,
                    new Exception(MessageFormat.format(
                        Messages.getString("FileSystemWalker.NoFileMatchesFormat"), //$NON-NLS-1$
                        currentFileSpec))));
        }
    }

    /**
     * Moves to the next file spec in the file spec queue (dequeue) and enqueues
     * the starting directory for the file spec. Specs which are not usable
     * (cloaked, not mapped, invalid path, other errors) are skipped and the
     * method tries to
     *
     * If {@link #dirStack} is empty when this method returns, no file specs in
     * the remainder of the spec queue were usable.
     */
    private void nextFileSpec() {
        // Generate a non-fatal error if no file matched the previous file spec.
        checkNoFileMatches();

        fileSpecHasMatch = false;
        m_localItemExclusionEvaluator = null;

        while (true) {
            // Nothing to do if file spec queue is empty.
            if (fileSpecIndex == fileSpecs.length) {
                currentFileSpec = null;
                return;
            }

            // Get the next file spec. Note that the exception handler below
            // expects that the file spec index has already been incremented.
            currentFileSpec = fileSpecs[fileSpecIndex];
            fileSpecIndex++;

            FileSystemAttributes attrs = null;

            // Server paths must first be converted to local paths.
            if (ServerPath.isServerPath(currentFileSpec)) {
                final PathTranslation translation = workspace.translateServerPathToLocalPath(currentFileSpec);

                if (translation == null) {
                    // Report the problem and move to the next file spec.
                    workspace.getClient().getEventEngine().fireNonFatalError(
                        new NonFatalErrorEvent(
                            EventSource.newFromHere(),
                            workspace,
                            new ItemNotMappedException(
                                MessageFormat.format(
                                    Messages.getString("VersionControlClient.NoWorkingFolderForFormat"), //$NON-NLS-1$
                                    currentFileSpec))));
                    continue;
                }

                if (translation.isCloaked()) {
                    // Report the problem and move to the next file spec.
                    workspace.getClient().getEventEngine().fireNonFatalError(
                        new NonFatalErrorEvent(
                            EventSource.newFromHere(),
                            workspace,
                            new ItemCloakedException(
                                MessageFormat.format(
                                    Messages.getString("FileSystemWalker.ItemIsCloakedFormat"), //$NON-NLS-1$
                                    currentFileSpec))));
                    continue;
                }

                currentFileSpec = translation.getTranslatedPath();
            } else {
                final String nonCanonicalPath = currentFileSpec;

                // Get the fully symlink guarded canonicalized path.
                currentFileSpec = LocalPath.canonicalize(currentFileSpec);

                try {
                    // Reset the attrs to be read again later (if needed) if the
                    // canonical path differed in case or because there's a
                    // symbolic link in the ancestry
                    if (!nonCanonicalPath.equals(currentFileSpec)) {
                        attrs = null;
                    }

                    // also check that the path does not have an illegal dollar
                    // in the server path.
                    checkForIllegalDollarInPath(currentFileSpec);
                } catch (final LocalPathFormatException exception) {
                    // If there is a problem with the file spec, such as illegal
                    // chars, etc., report the error and continue to the next
                    // file spec.
                    workspace.getClient().getEventEngine().fireNonFatalError(
                        new NonFatalErrorEvent(EventSource.newFromHere(), workspace, exception));
                    continue;
                }
            }

            // Determine whether the file spec is a directory.
            boolean filespecIsDirectory = false;
            if (!ItemPath.isWildcard(currentFileSpec)) {
                if (attrs == null) {
                    attrs = FileSystemUtils.getInstance().getAttributes(currentFileSpec);
                }

                if (attrs.exists() && !attrs.isSymbolicLink()) {
                    filespecIsDirectory = attrs.isDirectory();
                }
            }

            /*
             * Get the directory name and file name/wildcard to match. If
             * fileSpec is a directory and if recursive is set, we're going to
             * process the directory, contents, and all descendants. When not
             * recursive, we are simply going to process the directory itself.
             */
            String dirName;
            if (filespecIsDirectory) {
                dirName = currentFileSpec;

                if (isRecursive) {
                    fileNamePattern = "*"; //$NON-NLS-1$
                } else {
                    fileNamePattern = null;
                }

                fileSpecIsDir = true;
            } else if (attrs != null && attrs.exists()) {
                // getParent() only returns null if given the root of a
                // filesystem, and this is a file so it can't be the root dir
                dirName = LocalPath.getParent(currentFileSpec);
                fileNamePattern = LocalPath.getFileName(currentFileSpec);

                fileSpecIsDir = false;
            } else {
                continue;
            }

            // If the directory is not mapped, generate a non-fatal error and
            // carry on (checking here prevents getting redundant message about
            // nothing matching).
            if (!Workstation.getCurrent(workspace.getClient().getConnection().getPersistenceStoreProvider()).isMapped(
                dirName)) {
                workspace.getClient().getEventEngine().fireNonFatalError(
                    new NonFatalErrorEvent(
                        EventSource.newFromHere(),
                        workspace,
                        new ItemNotMappedException(
                            MessageFormat.format(
                                Messages.getString("VersionControlClient.NoWorkingFolderForFormat"), //$NON-NLS-1$
                                dirName))));

                continue;
            }

            // Set the starting directory and exit the loop.
            dirStack.push(dirName);
            break;
        }
    }

    /**
     * Gets full paths for all the files inside the given directory that match
     * the pattern.
     */
    private String[] getFullPathsForSubFilesAndDirectories(final String directory, final String pattern) {
        Check.notNull(directory, "directory"); //$NON-NLS-1$
        Check.notNull(pattern, "pattern"); //$NON-NLS-1$

        return getAbsolutePaths(new File(directory).listFiles(new FileFilter() {
            @Override
            public boolean accept(final File file) {
                // accept both symbolic links and file.isFile() ||
                // file.isDirectory()
                final boolean symlink = FileSystemUtils.getInstance().getAttributes(file.getPath()).isSymbolicLink();
                return (file.isFile() || symlink || file.isDirectory())
                    && ItemPath.matchesWildcardFile(file.getName(), pattern);
            }
        }));
    }

    /**
     * Gets full paths for all the directories inside the given directory.
     */
    private String[] getFullPathsForSubDirectories(final String directory) {
        Check.notNull(directory, "directory"); //$NON-NLS-1$

        return getAbsolutePaths(new File(directory).listFiles(new FileFilter() {
            @Override
            public boolean accept(final File file) {
                // file.isDirectory() won't match symbolic links
                return file.isDirectory();
            }
        }));
    }

    private String[] getAbsolutePaths(final File[] files) {
        // files will be null if the caller's File() was constructed with a
        // non-existing path
        if (files == null) {
            return new String[0];
        }

        final String[] paths = new String[files.length];
        for (int i = 0; i < files.length; i++) {
            paths[i] = files[i].getAbsolutePath();
        }

        return paths;
    }
}