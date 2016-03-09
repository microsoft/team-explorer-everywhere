// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.internal.localworkspace;

import java.util.HashSet;

import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ChangeRequest;

/**
 * Class that keep track of WorkspaceLocalItem objects that we updated during
 * single PendChange request and notifies us if we try to process the same entry
 * for the second time.
 *
 *
 * @threadsafety unknown
 */
public class DuplicateRequestsController {
    public DuplicateRequestsController(final ChangeRequest[] changeRequests) {
        // TODO we can do better job here of detecting when we can't have
        // duplicates
        // e.g. "edit a b" can create duplicates, "edit c:\w\a $/tp/a"
        // can
        if (changeRequests != null && changeRequests.length > 1) {
            accumulator = new HashSet<String>();
        }
    }

    public boolean process(final WorkspaceLocalItem lvEntry) {
        if (accumulator == null) {
            return false;
        }

        final String localItem = lvEntry.getLocalItem();
        if (localItem == null || localItem.length() == 0) {
            return false;
        }
        if (accumulator.contains(localItem)) {
            return true;
        } else {
            accumulator.add(localItem);
            return false;
        }
    }

    private HashSet<String> accumulator;
}
