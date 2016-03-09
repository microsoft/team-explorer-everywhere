// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.events;

import java.util.EventListener;

import com.microsoft.tfs.core.clients.versioncontrol.WorkspaceOptions;
import com.microsoft.tfs.core.clients.versioncontrol.localworkspace.LocalWorkspaceScanner;
import com.microsoft.tfs.core.clients.versioncontrol.localworkspace.WorkspaceWatcher;

/**
 * Interface for listeners of an event fired when a {@link WorkspaceWatcher}'s
 * scanner changed the on-disk attributes of items that were scanned. This can
 * happen if {@link WorkspaceOptions#SET_FILE_TO_CHECKIN} is enabled for a
 * workspace.
 *
 * @threadsafety thread-compatible
 */
public interface ScannerModifiedFilesListener extends EventListener {
    /**
     * Called when one or more local items' attributes were modified by the
     * {@link LocalWorkspaceScanner}.
     *
     * @param event
     *        the event args (must not be <code>null</code>)
     */
    void onScannerModifiedFiles(ScannerModifiedFilesEvent event);
}
