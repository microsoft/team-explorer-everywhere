// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.internal.localworkspace;

import com.microsoft.tfs.core.clients.versioncontrol.PropertyConstants;
import com.microsoft.tfs.util.BitField;

public class WorkspaceLocalItemStates extends BitField {
    private static final long serialVersionUID = -2707500209874398268L;

    public static final WorkspaceLocalItemStates NONE = new WorkspaceLocalItemStates(0);

    /**
     * Indicates whether this WorkspaceLocalItem is in the deleted state. Such
     * items have no local item.
     */
    public static final WorkspaceLocalItemStates DELETED = new WorkspaceLocalItemStates(1);

    /**
     * Indicates whether the data in this entry should be reconciled to the
     * server at the next reconcile.
     */
    public static final WorkspaceLocalItemStates PENDING_RECONCILE = new WorkspaceLocalItemStates(2);

    /**
     * Bit which is set by the scanner. Indicates whether the local item for
     * this entry exists on disk. Certain Reconcile operations (currently only
     * the reconcile for Get) will delete all LocalItemMissing rows from the
     * table. This allows Get to generate GetOperations for these items so they
     * can be re-downloaded.
     */
    public static final WorkspaceLocalItemStates LOCAL_ITEM_MISSING = new WorkspaceLocalItemStates(4);

    /**
     * Ephemeral bit. Used as temporary state during the execution of the
     * scanner.
     *
     * Indicates whether the first pass (mapped local space) over the workspace
     * hit this item or not. If not, the second pass (local version table) needs
     * to pick it up.
     */
    public static final WorkspaceLocalItemStates SCANNED = new WorkspaceLocalItemStates(8);

    /**
     * Inidicates whether the {@link WorkspaceLocalItem} has the
     * {@link PropertyConstants#EXECUTABLE_KEY} property set on it. There is
     * currently no dedicated local storage for local workspace property values
     * (strings, byte arrays, etc.), so this bit stands in for one property.
     */
    public static final WorkspaceLocalItemStates EXECUTABLE = new WorkspaceLocalItemStates(16);

    /**
     * Inidicates whether the {@link WorkspaceLocalItem} has the
     * {@link PropertyConstants#SYMLINK} property set on it. There is currently
     * no dedicated local storage for local workspace property values (strings,
     * byte arrays, etc.), so this bit stands in for one property.
     */
    public static final WorkspaceLocalItemStates SYMLINK = new WorkspaceLocalItemStates(32);

    private WorkspaceLocalItemStates(final int flags) {
        super(flags);
    }
}
