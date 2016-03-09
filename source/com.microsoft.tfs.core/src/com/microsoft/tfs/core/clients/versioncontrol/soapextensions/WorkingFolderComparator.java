// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.soapextensions;

import java.util.Comparator;

import com.microsoft.tfs.core.clients.versioncontrol.path.LocalPath;
import com.microsoft.tfs.core.clients.versioncontrol.path.ServerPath;
import com.microsoft.tfs.util.Check;

/**
 * <p>
 * Compare engine for {@link WorkingFolder} items, to be used with
 * {@link Comparable#compareTo(Object)}. Supports multiple sort strategies (see
 * {@link WorkingFolderType}).
 * </p>
 * <p>
 * Only server path sorting is implemented, because local path sorting is
 * complicated by cloaked mappings. Cloaks have no local path component, so they
 * cannot be sorted with normal mappings. In theory, the empty local path
 * mappings could be sorted to one side, but a sorted list of these mappings is
 * not currently useful for working folder lookups.
 * </p>
 *
 * @threadsafety immutable
 * @since TEE-SDK-10.1
 */
public final class WorkingFolderComparator implements Comparator<WorkingFolder> {
    private final WorkingFolderComparatorType sortType;

    /**
     * Creates a {@link WorkingFolderComparator} that compares using the
     * specified strategy.
     *
     * @param sortType
     *        the type of comparison to perform (must not be <code>null</code>)
     */
    public WorkingFolderComparator(final WorkingFolderComparatorType sortType) {
        Check.notNull(sortType, "sortType"); //$NON-NLS-1$
        this.sortType = sortType;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int compare(final WorkingFolder first, final WorkingFolder second) {
        /*
         * See the class Javadoc before adding local path sorting.
         */

        if (sortType == WorkingFolderComparatorType.SERVER_PATH) {
            return ServerPath.compareTopDown(first.getServerItem(), second.getServerItem());
        } else if (sortType == WorkingFolderComparatorType.SERVER_PATH_REVERSE) {
            return ServerPath.compareBottomUp(first.getServerItem(), second.getServerItem());
        } else if (sortType == WorkingFolderComparatorType.LOCAL_PATH) {
            final String firstLocal = first.getLocalItem();
            final String secondLocal = second.getLocalItem();

            // The local item of a cloaked working folder is null and must be
            // accounted for. A null will sort to the top of the list.
            if (firstLocal == null || secondLocal == null) {
                if (firstLocal == null && secondLocal == null) {
                    return 0;
                } else {
                    return firstLocal == null ? -1 : 1;
                }
            } else {
                return LocalPath.compareTopDown(firstLocal, secondLocal);
            }
        } else {
            throw new RuntimeException("Unsupported sort type"); //$NON-NLS-1$
        }
    }
}
