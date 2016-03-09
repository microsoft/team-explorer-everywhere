// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.internal.conflict;

import java.util.ArrayList;
import java.util.List;

import com.microsoft.tfs.core.clients.versioncontrol.OperationStatus;
import com.microsoft.tfs.core.clients.versioncontrol.events.GetEvent;
import com.microsoft.tfs.core.clients.versioncontrol.events.GetListener;
import com.microsoft.tfs.core.clients.versioncontrol.path.LocalPath;

public class WritableConflictOnSourcePathListener implements GetListener {
    private final List<String> writableConflictPaths = new ArrayList<String>();

    @Override
    public void onGet(final GetEvent e) {
        if (OperationStatus.SOURCE_WRITABLE.equals(e.getStatus())
            || OperationStatus.TARGET_WRITABLE.equals(e.getStatus())) {
            if (e.getSourceLocalItem() == null || LocalPath.equals(e.getSourceLocalItem(), e.getTargetLocalItem())) {
                /*
                 * We don't need to record these paths, because they are the
                 * same as target.
                 */
            } else {
                writableConflictPaths.add(e.getSourceLocalItem());
            }
        }
    }

    public String[] getMovedPaths() {
        return writableConflictPaths.toArray(new String[writableConflictPaths.size()]);
    }
}
