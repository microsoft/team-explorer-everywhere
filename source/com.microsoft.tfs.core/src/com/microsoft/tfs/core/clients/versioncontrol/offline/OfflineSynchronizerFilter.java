// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.offline;

import java.io.File;

import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ItemType;

/**
 * <p>
 * {@link OfflineSynchronizerFilter} provides an interface to limiting
 * ("filtering") the resources that will be addressed by the
 * OfflineSynchronizer. This class is an interface to be subclassed by the
 * various clients to handle exclusions or ignores.
 * </p>
 *
 * @since TEE-SDK-10.1
 * @threadsafety thread-compatible
 */
public class OfflineSynchronizerFilter {
    /**
     * Returns true if changes should be pended for the given file.
     *
     * @param file
     *        the File which was detected as changed (won't exist in the case of
     *        {@link OfflineChangeType#DELETE})
     * @param changeType
     *        type of change detected (add, edit, delete)
     * @param serverItemType
     *        if the changeType is {@link OfflineChangeType#DELETE}, this is
     *        type of the server item which corresponds to the given file, it is
     *        <code>null</code> otherwise
     * @return true to pend the change for this file, false to ignore
     */
    public boolean shouldPend(final File file, final OfflineChangeType changeType, final ItemType serverItemType) {
        return true;
    }

    /**
     * Returns true if we should recurse into the given directory when examining
     * changes.
     *
     * @param file
     *        a File representing the directory in question
     * @return true to recurse into the directory, false to skip
     */
    public boolean shouldRecurse(final File file) {
        return true;
    }
}
