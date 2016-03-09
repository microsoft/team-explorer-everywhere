// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.events;

import java.util.EventListener;

import com.microsoft.tfs.core.clients.versioncontrol.DestroyFlags;
import com.microsoft.tfs.core.clients.versioncontrol.VersionControlClient;
import com.microsoft.tfs.core.clients.versioncontrol.specs.ItemSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.VersionSpec;

/**
 * <p>
 * When a call is made to
 * {@link VersionControlClient#destroy(ItemSpec, VersionSpec, VersionSpec, DestroyFlags)}
 * , a {@link DestroyEvent} is sent to each registered {@link DestroyListener}
 * for each item that was destroyed as a result of the operation.
 * </p>
 *
 * <p>
 * {@link DestroyListener}s can be registered by calling
 * {@link VersionControlEventEngine#addDestroyListener(DestroyListener)}. Each
 * {@link VersionControlClient} has an {@link VersionControlEventEngine}.
 * </p>
 *
 * @since TEE-SDK-10.1
 */
public interface DestroyListener extends EventListener {
    /**
     * Called when an item is destroyed as the result of a call to
     * {@link VersionControlClient#destroy(ItemSpec, VersionSpec, VersionSpec, DestroyFlags)}
     * .
     *
     * @param event
     *        the {@link DestroyEvent} that represents the item that was
     *        destroyed (must not be <code>null</code>)
     */
    public void onDestroy(DestroyEvent event);
}
