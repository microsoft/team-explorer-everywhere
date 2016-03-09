// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.events;

import java.util.EventListener;

/**
 *
 * Defines an interface for listeners of the {@link ConflictEvent}.
 *
 * @since TEE-SDK-10.1
 */
public interface ConflictListener extends EventListener {
    public void onConflict(ConflictEvent e);
}
