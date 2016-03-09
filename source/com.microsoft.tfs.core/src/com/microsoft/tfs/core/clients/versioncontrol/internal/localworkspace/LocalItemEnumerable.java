// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.internal.localworkspace;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;

import com.microsoft.tfs.core.clients.versioncontrol.path.LocalPath;
import com.microsoft.tfs.core.clients.versioncontrol.path.ServerPath;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.RecursionType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.WorkingFolder;
import com.microsoft.tfs.util.Check;

/**
 * Iterator that walks the file system starting at the specified directory or
 * file.
 *
 * @threadsafety unknown
 */
public class LocalItemEnumerable implements Iterable<EnumeratedLocalItem> {
    private final File startPath;
    private final boolean recurse;
    private final List<String> excludedPaths;
    private final boolean enumerateHiddenAndSystem;

    public LocalItemEnumerable(final String path, final boolean recurse, final List<String> excludedPaths) {
        this(path, recurse, false, excludedPaths);
    }

    public LocalItemEnumerable(
        final String path,
        final boolean recurse,
        final boolean enumerateHiddenAndSystem,
        final List<String> excludedPaths) {
        Check.notNullOrEmpty(path, "path"); //$NON-NLS-1$

        this.startPath = new File(path);
        this.recurse = recurse;
        this.excludedPaths = excludedPaths;
        this.enumerateHiddenAndSystem = enumerateHiddenAndSystem;
    }

    @Override
    public Iterator<EnumeratedLocalItem> iterator() {
        return new LocalItemEnumerator(startPath, recurse, enumerateHiddenAndSystem, excludedPaths, null);
    }

    // / <summary>
    // / Returns a set of LocalItemEnumerator objects which if enumerated over,
    // cover the entire mapped local
    // / space of the provided working folder set.
    // / </summary>
    // / <param name="folders">The WorkingFolder set for the workspace whose
    // mapped local space should be traversed</param>
    // / <returns>The set of LocalItemEnumerator objects needed to traverse the
    // workspace</returns>
    public static Iterable<LocalItemEnumerator> getEnumeratorsForWorkingFolders(final WorkingFolder[] folders) {
        // Sort the working folders depth-first by server item.
        Arrays.sort(folders, new Comparator<WorkingFolder>() {
            @Override
            public int compare(final WorkingFolder x, final WorkingFolder y) {
                return ServerPath.compareTopDown(x.getServerItem(), y.getServerItem());
            }
        });

        final Stack<LocalItemEnumerator> enumStack = new Stack<LocalItemEnumerator>();
        LocalItemEnumerator current = null;

        // Pass 1: By server item
        final List<LocalItemEnumerator> toReturn = new ArrayList<LocalItemEnumerator>();

        for (final WorkingFolder wf : folders) {
            while (null != current
                && !ServerPath.isChild(((WorkingFolder) (current.getTag())).getServerItem(), wf.getServerItem())) {
                toReturn.add(current);

                if (enumStack.size() > 0) {
                    current = enumStack.pop();
                } else {
                    current = null;
                }
            }

            if (!wf.isCloaked()) {
                if (null != current) {
                    enumStack.push(current);
                }

                current = new LocalItemEnumerator(
                    new File(wf.getLocalItem()),
                    RecursionType.FULL == wf.getDepth() /* recurse */,
                    false /* enumerateHiddenAndSystem */,
                    null /* excludedPaths */,
                    wf /* tag */);
            } else if (null != current) {
                // Use the current WorkingFolder to get the local item that this
                // cloak would have.
                current.addExcludedPath(
                    ((WorkingFolder) (current.getTag())).translateServerItemToLocalItem(wf.getServerItem()));
            }
        }

        while (enumStack.size() > 0) {
            toReturn.add(enumStack.pop());
        }

        if (null != current) {
            toReturn.add(current);
        }

        // Pass 2: By local item (for map-outs). This pass is to ensure that
        // each local item is
        // enumerated by exactly one LocalItemEnumerator, and that the
        // LocalItemEnumerator which
        // enumerates it is the closest mapping by local path.
        enumStack.clear();
        current = null;

        // Sort the results of the server item pass depth-first by local item.
        Collections.sort(toReturn, new Comparator<LocalItemEnumerator>() {
            @Override
            public int compare(final LocalItemEnumerator x, final LocalItemEnumerator y) {
                return LocalPath.compareTopDown(x.getStartPath(), y.getStartPath());
            }
        });

        for (final LocalItemEnumerator lie : toReturn) {
            while (null != current && !LocalPath.isChild(current.getStartPath(), lie.getStartPath())) {
                if (enumStack.size() > 0) {
                    current = enumStack.pop();
                } else {
                    current = null;
                }
            }

            if (null != current) {
                // Exclude the child's local item from the parent.
                current.addExcludedPath(lie.getStartPath());
                enumStack.push(current);
            }

            current = lie;
        }

        return toReturn;
    }
}
