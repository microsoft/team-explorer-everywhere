// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.events;

import com.microsoft.tfs.core.clients.CoreClientEvent;
import com.microsoft.tfs.core.clients.versioncontrol.DestroyFlags;
import com.microsoft.tfs.core.clients.versioncontrol.VersionControlClient;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Item;
import com.microsoft.tfs.core.clients.versioncontrol.specs.ItemSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.VersionSpec;
import com.microsoft.tfs.util.Check;

/**
 * A {@link DestroyEvent} represents a single item that was destroyed as a
 * result of a call to
 * {@link VersionControlClient#destroy(ItemSpec, VersionSpec, VersionSpec, DestroyFlags)}
 * . {@link DestroyEvent}s are sent to {@link DestroyListener}s.
 *
 * @since TEE-SDK-10.1
 */
public class DestroyEvent extends CoreClientEvent {
    private final Item destroyedItem;
    private final VersionSpec stopAt;
    private final DestroyFlags flags;

    /**
     * Creates a new {@link DestroyEvent}.
     *
     * @param source
     *        the {@link EventSource} (must not be <code>null</code>)
     * @param destroyedItem
     *        the item that was destroyed (must not be <code>null</code>)
     * @param stopAt
     *        the stop at version spec, or <code>null</code> if no stop at was
     *        specified
     * @param flags
     *        the {@link DestroyFlags} that were used in the destroy operation
     *        (must not be <code>null</code>)
     */
    public DestroyEvent(
        final EventSource source,
        final Item destroyedItem,
        final VersionSpec stopAt,
        final DestroyFlags flags) {
        super(source);

        Check.notNull(destroyedItem, "destroyedItem"); //$NON-NLS-1$
        Check.notNull(flags, "flags"); //$NON-NLS-1$

        this.destroyedItem = destroyedItem;
        this.stopAt = stopAt;
        this.flags = flags;
    }

    /**
     * @return the destroyed item that this event was sent for (never
     *         <code>null</code>)
     */
    public Item getDestroyedItem() {
        return destroyedItem;
    }

    /**
     * @return the stop at version spec that was specified to destroy, or
     *         <code>null</code> if no stop at version spec was specified
     */
    public VersionSpec getStopAt() {
        return stopAt;
    }

    /**
     * @return the {@link DestroyFlags} that were specified to destroy (never
     *         <code>null</code>)
     */
    public DestroyFlags getFlags() {
        return flags;
    }
}
