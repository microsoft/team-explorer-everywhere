// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.events;

import java.util.EventListener;

/**
 * Defines an interface for listeners of the
 * {@link WorkstationNonFatalErrorEvent}.
 *
 * @since TEE-SDK-11.0
 */
public interface WorkstationNonFatalErrorListener extends EventListener {
    public void onWorkstationNonFatalError(WorkstationNonFatalErrorEvent e);
}
