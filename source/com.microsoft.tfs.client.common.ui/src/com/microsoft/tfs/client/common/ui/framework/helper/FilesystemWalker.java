// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.framework.helper;

import java.io.File;
import java.io.FileFilter;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import com.microsoft.tfs.jni.FileSystemAttributes;
import com.microsoft.tfs.jni.FileSystemUtils;

/**
 * A class to visit files and directories in a filesystem, optionally recursing
 * through subdirectories.
 *
 * This class defines a public interface, FilesystemVisitor, which clients of
 * this class must implement. The implementation of FilesystemVisitor is called
 * back to perform specific processing for files and directories being visited.
 *
 * You can specify whether the visitor should receive callbacks for files,
 * directories, or both. The default behavior is that the visitor will receive
 * callbacks for both.
 *
 * The default behavior is to recurse through subdirectories, after visiting the
 * directories and files in a directory, but the recursing can be disabled.
 *
 * If all that's needed is to collect a list of visited files, there are
 * convenience overloads of the walk() methods that do not take a
 * FilesystemVisitor. These convenience methods simply return a list of the
 * files and/or directories that were visited.
 */
public class FilesystemWalker {
    private final File startingDirectory;
    private boolean visitFiles = true;
    private boolean visitDirectories = true;
    private boolean recurse = true;

    /**
     * Create a new FilesystemWalker that will walk beginning at the given
     * directory.
     *
     * @param startingDirectory
     *        full path of starting directory
     */
    public FilesystemWalker(final String startingDirectory) {
        this(startingDirectory != null ? new File(startingDirectory) : null);
    }

    /**
     * Create a new FilesystemWalker that will walk beginning at the given
     * directory.
     *
     * @param startingDirectory
     *        a File object representing a directory to begin at
     */
    public FilesystemWalker(final File startingDirectory) {
        if (startingDirectory == null) {
            throw new IllegalArgumentException("startingDirectory must be specified"); //$NON-NLS-1$
        }
        if (!startingDirectory.isDirectory()) {
            final String messageFormat = "the specified starting directory [{0}] is not a directory"; //$NON-NLS-1$
            final String message = MessageFormat.format(messageFormat, startingDirectory.getAbsolutePath());
            throw new IllegalArgumentException(message);
        }
        this.startingDirectory = startingDirectory;
    }

    /**
     * Defines a visitor object that will be called back when files and/or
     * directories are encountered by a FilesystemWalker.
     */
    public interface FilesystemVisitor {
        /**
         * Visit a file or directory. Any processing is optional.
         *
         * @param file
         *        file or directory currently being visited
         */
        public void visit(File file);
    }

    /**
     * Walk the filesystem, collecting files and/or directories that are
     * visited.
     *
     * @return the List of files and/or directories visited
     */
    public List walk() {
        return walk((FileFilter) null);
    }

    /**
     * Walk the filesystem, collecting files and/or directories that are
     * visited.
     *
     * @param filter
     *        FileFilter used to filter the files and directories looked at
     * @return the List of files and/or directories visited
     */
    public List walk(final FileFilter filter) {
        final FileCollector collector = new FileCollector();
        walk(collector, filter);
        return collector.getFiles();
    }

    /**
     * Walk the filesystem with the given visitor.
     *
     * @param visitor
     *        FilesystemVisitor to receive callbacks
     */
    public void walk(final FilesystemVisitor visitor) {
        walk(visitor, null);
    }

    /**
     * Walk the filesystem with the given visitor.
     *
     * @param visitor
     *        FilesystemVisitor to receive callbacks
     * @param filter
     *        FileFilter used to filter the files and directories looked at
     */
    public void walk(final FilesystemVisitor visitor, final FileFilter filter) {
        walkRecursive(startingDirectory, visitor, filter);
    }

    /**
     * Sets whether or not the visitor will receive callbacks when directories
     * are encountered.
     *
     * @param visitDirectories
     *        true to receive directory callbacks
     * @return this FilesystemWalker
     */
    public FilesystemWalker setVisitDirectories(final boolean visitDirectories) {
        this.visitDirectories = visitDirectories;
        return this;
    }

    /**
     * Sets whether or not the visitor will receive callbacks when files are
     * encountered.
     *
     * @param visitFiles
     *        true to receive file callbacks
     * @return this FilesystemWalker
     */
    public FilesystemWalker setVisitFiles(final boolean visitFiles) {
        this.visitFiles = visitFiles;
        return this;
    }

    /**
     * Sets whether or not this FilesystemWalker will recurse into
     * subdirectories
     *
     * @param recurse
     *        true to recurse into subdirectories
     */
    public void setRecurse(final boolean recurse) {
        this.recurse = recurse;
    }

    private void walkRecursive(final File directory, final FilesystemVisitor visitor, final FileFilter filter) {
        // list files and directories in directory
        final File[] files = (filter == null ? directory.listFiles() : directory.listFiles(filter));

        // if visitFiles is on, callback visitor for each file
        if (visitFiles) {
            for (int i = 0; i < files.length; i++) {
                if (files[i].isFile()) {
                    visitor.visit(files[i]);
                }
            }
        }

        if (visitDirectories || recurse) {
            for (int i = 0; i < files.length; i++) {
                final FileSystemAttributes attrs = FileSystemUtils.getInstance().getAttributes(files[i]);

                if (attrs.isDirectory()) {
                    // ... if visit directories is on, callback visitor
                    if (visitDirectories) {
                        visitor.visit(files[i]);
                    }

                    // ... if recurse is on, recurse into directory unless
                    // symbolic link
                    if (recurse && !attrs.isSymbolicLink()) {
                        walkRecursive(files[i], visitor, filter);
                    }
                }
            }
        }
    }

    private static class FileCollector implements FilesystemVisitor {
        private final List files = new ArrayList();

        @Override
        public void visit(final File file) {
            files.add(file);
        }

        public List getFiles() {
            return files;
        }
    }
}
