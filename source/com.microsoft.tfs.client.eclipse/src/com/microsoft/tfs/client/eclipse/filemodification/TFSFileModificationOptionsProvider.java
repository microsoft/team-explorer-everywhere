// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse.filemodification;

import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.LockLevel;

/**
 * Provides {@link TFSFileModificationOptions} to control how a file
 * modification validation event is processed.
 *
 * @threadsafety unknown
 */
public interface TFSFileModificationOptionsProvider {
    /**
     * Gets options which control how a file modification event is processed.
     *
     * @param serverPaths
     *        the server paths being modified (must not be <code>null</code>)
     * @param forcedLockLevel
     *        the lock level the team project's configuration is forcing for
     *        this event (<code>null</code> if no lock level is forced)
     * @return the options
     */
    TFSFileModificationOptions getOptions(String[] serverPaths, LockLevel forcedLockLevel);
}
