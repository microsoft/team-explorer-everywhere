// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol;

import com.microsoft.tfs.core.clients.versioncontrol.exceptions.ItemNotFoundException;
import com.microsoft.tfs.util.BitField;

/**
 * Options that affect how pending changes are created.
 *
 * @threadsafety thread-safe
 * @since TEE-SDK-10.1
 */
public final class PendChangesOptions extends BitField {
    /*
     * Integer values used in these options are not sent to the server (if these
     * options go to the server, they're usually as booleans). They only have
     * meaning on the client side.
     */

    public static final PendChangesOptions NONE = new PendChangesOptions(0, "None"); //$NON-NLS-1$

    /**
     * The client requests to synchronize to the latest version of an item on
     * Checkout (PendingEdit). In this case the server will download the latest
     * version of the file to the client.
     */
    public static final PendChangesOptions GET_LATEST_ON_CHECKOUT = new PendChangesOptions(1, "GetLatestOnCheckout"); //$NON-NLS-1$

    /**
     * This instructs the server not to return GetOps for the operation. In the
     * case when {@link #GET_LATEST_ON_CHECKOUT} and {@link #SILENT} are
     * enabled, you may still get back a GetOp if you need to GetLatest.
     */

    public static final PendChangesOptions SILENT = new PendChangesOptions(2, "Silent"); //$NON-NLS-1$

    /**
     * This instructs the server to force "CheckOut Local Version" behavior.
     * This flag will supercede the {@link #GET_LATEST_ON_CHECKOUT} flag above
     * if both are specified. This flag will supercede the
     * {@link #GET_LATEST_ON_CHECKOUT} team project annotation.
     */
    public static final PendChangesOptions FORCE_CHECK_OUT_LOCAL_VERSION =
        new PendChangesOptions(4, "ForceCheckOutLocalVersion"); //$NON-NLS-1$

    /**
     * This flag instructs the client not to report any Failures returned by the
     * server of type {@link ItemNotFoundException} as NonFatalErrors.
     */
    public static final PendChangesOptions SUPPRESS_ITEM_NOT_FOUND_FAILURES =
        new PendChangesOptions(8, "SuppressItemNotFoundFailures"); //$NON-NLS-1$

    /**
     * Whether items missing on disk should be assumed to be files.
     */
    public static final PendChangesOptions TREAT_MISSING_ITEMS_AS_FILES =
        new PendChangesOptions(16, "TreatMissingItemsAsFiles"); //$NON-NLS-1$

    /**
     * If true, applies the local item exlusion lists (see {@link Workstation})
     * for items found by searching in a directory or matching a wildcard.
     */
    public static final PendChangesOptions APPLY_LOCAL_ITEM_EXCLUSIONS =
        new PendChangesOptions(32, "ApplyLocalItemExclusions"); //$NON-NLS-1$

    public static PendChangesOptions combine(final PendChangesOptions[] changeTypes) {
        return new PendChangesOptions(BitField.combine(changeTypes));
    }

    private PendChangesOptions(final int flags, final String name) {
        super(flags);
        registerStringValue(getClass(), flags, name);
    }

    private PendChangesOptions(final int flags) {
        super(flags);
    }

    public boolean containsAll(final PendChangesOptions other) {
        return containsAllInternal(other);
    }

    public boolean contains(final PendChangesOptions other) {
        return containsInternal(other);
    }

    public boolean containsAny(final PendChangesOptions other) {
        return containsAnyInternal(other);
    }

    public PendChangesOptions remove(final PendChangesOptions other) {
        return new PendChangesOptions(removeInternal(other));
    }

    public PendChangesOptions retain(final PendChangesOptions other) {
        return new PendChangesOptions(retainInternal(other));
    }

    public PendChangesOptions combine(final PendChangesOptions other) {
        return new PendChangesOptions(combineInternal(other));
    }
}