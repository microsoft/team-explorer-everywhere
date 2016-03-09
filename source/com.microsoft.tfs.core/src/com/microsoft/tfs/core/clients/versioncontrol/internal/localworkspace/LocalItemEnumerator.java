// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.internal.localworkspace;

import java.io.File;
import java.text.MessageFormat;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.Set;
import java.util.Stack;
import java.util.TreeSet;

import com.microsoft.tfs.core.clients.versioncontrol.localworkspace.BaselineFolder;
import com.microsoft.tfs.core.clients.versioncontrol.path.LocalPath;
import com.microsoft.tfs.jni.FileSystemAttributes;
import com.microsoft.tfs.jni.FileSystemUtils;
import com.microsoft.tfs.util.Check;

/**
 * Private implementation of the iterator.
 *
 * Handles a directory or file as the root of iteration and returns files and
 * directories in a breadth first order. Directory names '.' and '..' are
 * skipped as are hidden files.
 *
 * @threadsafety unknown
 */
public class LocalItemEnumerator implements Iterator<EnumeratedLocalItem> {
    private final boolean recurse;
    private final boolean enumerateHiddenAndSystem;
    private final String startPath;
    private final Object tag;
    private Set<String> excludedPaths;

    private File[] currentFileList;
    private int currentFileListIndex;

    private EnumeratedLocalItem currentLocalItem;
    private final Stack<Queue<File>> stateStack;

    public LocalItemEnumerator(
        final File startPath,
        final boolean recurse,
        final boolean enumerateHiddenAndSystem,
        final List<String> excludedPaths,
        final Object tag) {
        Check.notNull(startPath, "startPath"); //$NON-NLS-1$

        this.recurse = recurse;
        this.enumerateHiddenAndSystem = enumerateHiddenAndSystem;
        this.startPath = startPath.getPath();
        this.tag = tag;

        if (null != excludedPaths && excludedPaths.size() > 0) {
            this.excludedPaths = new TreeSet<String>(LocalPath.TOP_DOWN_COMPARATOR);
            this.excludedPaths.addAll(excludedPaths);
        }

        if (!startPath.exists()) {
            currentFileList = new File[0];
        } else if (startPath.isDirectory()) {
            currentFileList = startPath.listFiles();
        } else if (startPath.isFile()) {
            currentFileList = new File[] {
                startPath
            };
        } else {
            throw new IllegalArgumentException(
                MessageFormat.format(
                    "Don''t know how to handle startPath {0} which is not a file or directory", //$NON-NLS-1$
                    startPath));
        }

        currentFileListIndex = 0;

        stateStack = new Stack<Queue<File>>();
        stateStack.push(new LinkedList<File>());
    }

    public String getStartPath() {
        return startPath;
    }

    public Object getTag() {
        return tag;
    }

    public void addExcludedPath(final String localItem) {
        if (null == excludedPaths) {
            excludedPaths = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
        }
        excludedPaths.add(localItem);
    }

    private boolean moveNext() {
        while (true) {
            if (currentFileList != null && currentFileListIndex < currentFileList.length) {
                final File currentFile = currentFileList[currentFileListIndex++];

                if (currentFile.getName().equals(".") || currentFile.getName().equals("..")) //$NON-NLS-1$ //$NON-NLS-2$
                {
                    continue;
                }

                // Defer loading these attributes
                FileSystemAttributes fileAttrs = null;

                // The $tf directory is marked hidden on Windows
                if (!enumerateHiddenAndSystem) {
                    // Check to see if the file is hidden. Skip and log a
                    // warning if the file attributes cannot be read.
                    try {
                        fileAttrs = FileSystemUtils.getInstance().getAttributes(currentFile.getPath());

                        if (fileAttrs.isHidden()) {
                            continue;
                        }
                    } catch (final RuntimeException e) {
                        // Ignore this file if we can't read attributes.
                        continue;
                    }
                }

                // On non-Windows platforms $tf/.tf cannot be marked hidden,
                // so test for it here
                if (BaselineFolder.isPotentialBaselineFolderName(currentFile.getName())) {
                    continue;
                }

                if (excludedPaths != null && excludedPaths.contains(currentFile.getPath())) {
                    continue;
                }

                if (fileAttrs == null) {
                    fileAttrs = FileSystemUtils.getInstance().getAttributes(currentFile.getPath());
                }

                if (recurse && fileAttrs.isDirectory() && !fileAttrs.isSymbolicLink()) {
                    // put on the directories to be searched stack
                    stateStack.peek().add(currentFile);
                }

                currentLocalItem = new EnumeratedLocalItem(currentFile, fileAttrs);
                return true;
            } else {
                while (stateStack.size() > 0) {
                    if (stateStack.peek().size() > 0) {
                        final File subDir = stateStack.peek().poll();
                        stateStack.push(new LinkedList<File>());
                        currentFileList = subDir.listFiles();
                        currentFileListIndex = 0;
                        break;
                    } else {
                        stateStack.pop();
                    }
                }

                if (stateStack.size() == 0) {
                    currentLocalItem = null;
                    return false;
                }
            }
        }
    }

    @Override
    public boolean hasNext() {
        if (currentLocalItem != null) {
            return true;
        }
        return moveNext();
    }

    @Override
    public EnumeratedLocalItem next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }

        final EnumeratedLocalItem toReturn = currentLocalItem;
        moveNext();
        return toReturn;
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }
}
