// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol;

import com.microsoft.tfs.util.BitField;

/**
 * {@link DestroyFlags} is used to control the options of a destroy operation.
 *
 * @since TEE-SDK-10.1
 */
public class DestroyFlags extends BitField {
    /**
     * No destroy flags. The default destroy options will be used.
     */
    public static final DestroyFlags NONE = new DestroyFlags(0, "None"); //$NON-NLS-1$

    /**
     * When this flag is specified to destroy, the server does not actually
     * destroy the indicated items. Instead, it returns the items that would be
     * destroyed if this flag were not specified.
     */
    public static final DestroyFlags PREVIEW = new DestroyFlags(1, "Preview"); //$NON-NLS-1$

    /**
     * When this flag is specified to destroy, the server immediately starts an
     * asynchronous cleanup of the versioned item file content data. Normally,
     * destroy immediately removes history data, but defers cleanup of file
     * content to a periodic maintenance job.
     */
    public static final DestroyFlags START_CLEANUP = new DestroyFlags(2, "StartCleanup"); //$NON-NLS-1$

    /**
     * Keeps history. When this flag is specified, item version history is
     * preserved. This means that the item will still appear in directory
     * listings, history queries, etc. When this flag is specified, a "stopAt"
     * version spec can be specified to the destroy operation that controls how
     * much of the file content data is destroyed.
     */
    public static final DestroyFlags KEEP_HISTORY = new DestroyFlags(4, "KeepHistory"); //$NON-NLS-1$

    /**
     * When this flag is specified to destroy, the items that were destroyed are
     * not returned to the client. Normally, the server returns the list of
     * items that were destroyed as a result of the operation.
     */
    public static final DestroyFlags SILENT = new DestroyFlags(8, "Silent"); //$NON-NLS-1$

    /**
     * This will return pended and shelved changes that will be destroyed and
     * not return the entire set of items that will be destroyed.
     */
    public static final DestroyFlags AFFECTED_CHANGES = new DestroyFlags(16, "AffectedChanges"); //$NON-NLS-1$

    /**
     * Instructs the server to delete local version rows, working folder
     * mappings pending adds & branches, for any affected workspaces. This
     * should be used only for move cases.
     */
    public static final DestroyFlags DELETE_WORKSPACE_STATE = new DestroyFlags(32, "DeleteWorkspaceState"); //$NON-NLS-1$

    public static DestroyFlags combine(final DestroyFlags[] values) {
        return new DestroyFlags(BitField.combine(values));
    }

    private DestroyFlags(final int flags, final String name) {
        super(flags);
        registerStringValue(getClass(), flags, name);
    }

    private DestroyFlags(final int flags) {
        super(flags);
    }

    public boolean containsAll(final DestroyFlags other) {
        return containsAllInternal(other);
    }

    public boolean contains(final DestroyFlags other) {
        return containsInternal(other);
    }

    public boolean containsAny(final DestroyFlags other) {
        return containsAnyInternal(other);
    }

    public DestroyFlags remove(final DestroyFlags other) {
        return new DestroyFlags(removeInternal(other));
    }

    public DestroyFlags retain(final DestroyFlags other) {
        return new DestroyFlags(retainInternal(other));
    }

    public DestroyFlags combine(final DestroyFlags other) {
        return new DestroyFlags(combineInternal(other));
    }
}
