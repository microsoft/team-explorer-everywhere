// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol;

import com.microsoft.tfs.util.BitField;

/**
 * Options which control how item information is retrieved from the server.
 *
 * @since TEE-SDK-10.1
 */
public class GetItemsOptions extends BitField {
    /**
     * Default behavior. Sorting is performed and no download information is
     * generated.
     */
    public static final GetItemsOptions NONE = new GetItemsOptions(0, "None"); //$NON-NLS-1$

    /**
     * If set, causes download information to be generated. Set this only if you
     * plan to download the file's contents after calling GetItems().
     */
    public static final GetItemsOptions DOWNLOAD = new GetItemsOptions(1, "Download"); //$NON-NLS-1$

    /**
     * Speeds up GetItems() by not sorting the result items.
     */
    public static final GetItemsOptions UNSORTED = new GetItemsOptions(2, "Unsorted"); //$NON-NLS-1$

    /**
     * Include information on whether an item is a branch object in the system
     *
     * @since TFS 2010
     */
    public static final GetItemsOptions INCLUDE_BRANCH_INFO = new GetItemsOptions(4, "IncludeBranchInfo"); //$NON-NLS-1$

    /**
     * Include items which are the source of a rename.
     *
     * @since TFS 2010
     */
    public static final GetItemsOptions INCLUDE_SOURCE_RENAMES = new GetItemsOptions(8, "IncludeSourceRenames"); //$NON-NLS-1$

    /**
     * Only available in local workspaces. Will cause the retrieval of items
     * from the local table, without making a server call.
     *
     * @since TFS 2012
     */
    public static final GetItemsOptions LOCAL_ONLY = new GetItemsOptions(16, "LocalOnly"); //$NON-NLS-1$

    /**
     * Instructs the server to return items which are affected by a Recursive
     * Delete
     *
     * @since TFS 2012
     */
    public static final GetItemsOptions INCLUDE_RECURSIVE_DELETES = new GetItemsOptions(32, "IncludeRecursiveDeletes"); //$NON-NLS-1$

    public static GetItemsOptions combine(final GetItemsOptions[] values) {
        return new GetItemsOptions(BitField.combine(values));
    }

    private GetItemsOptions(final int flags, final String name) {
        super(flags);
        registerStringValue(getClass(), flags, name);
    }

    private GetItemsOptions(final int flags) {
        super(flags);
    }

    public boolean containsAll(final GetItemsOptions other) {
        return containsAllInternal(other);
    }

    public boolean contains(final GetItemsOptions other) {
        return containsInternal(other);
    }

    public boolean containsAny(final GetItemsOptions other) {
        return containsAnyInternal(other);
    }

    public GetItemsOptions remove(final GetItemsOptions other) {
        return new GetItemsOptions(removeInternal(other));
    }

    public GetItemsOptions retain(final GetItemsOptions other) {
        return new GetItemsOptions(retainInternal(other));
    }

    public GetItemsOptions combine(final GetItemsOptions other) {
        return new GetItemsOptions(combineInternal(other));
    }
}
