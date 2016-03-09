// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol;

import com.microsoft.tfs.util.BitField;

/**
 * Features supported by the version control services of a Team Foundation
 * Server.
 *
 * @since TEE-SDK-10.1
 */
public final class SupportedFeatures extends BitField {
    public static final SupportedFeatures NONE = new SupportedFeatures(0, "None"); //$NON-NLS-1$

    /**
     * The client supports optionally synchronizing to the latest version of an
     * item on Checkout (PendingEdit). If this flag is set but
     * {@link #GET_LATEST_ON_CHECKOUT} is not set in PendChangesOptions, then
     * the client will synchronize only if the team project has the
     * {@link #GET_LATEST_ON_CHECKOUT} setting on.
     */
    public static final SupportedFeatures GET_LATEST_ON_CHECKOUT = new SupportedFeatures(1, "GetLatestOnCheckout"); //$NON-NLS-1$

    /**
     * This means that the server / client supports one level mappings
     */
    public static final SupportedFeatures ONE_LEVEL_MAPPING = new SupportedFeatures(2, "OneLevelMapping"); //$NON-NLS-1$

    /**
     * The destroy feature is available on the server.
     */
    public static final SupportedFeatures DESTROY = new SupportedFeatures(4, "Destroy"); //$NON-NLS-1$

    /**
     * The ability to create a committed branch on the server.
     */
    public static final SupportedFeatures CREATE_BRANCH = new SupportedFeatures(8, "CreateBranch"); //$NON-NLS-1$

    /**
     * The ability to page changes within a changeset.
     */
    public static final SupportedFeatures GET_CHANGES_FOR_CHANGESET =
        new SupportedFeatures(16, "GetChangesForChangeset"); //$NON-NLS-1$

    /**
     * This represents the suite of proxy services: AddProxy(), DeleteProxy(),
     * QueryProxies(), ...
     */
    public static final SupportedFeatures PROXY_SUITE = new SupportedFeatures(32, "ProxySuite"); //$NON-NLS-1$

    /**
     * This represents the ability to query local version information from the
     * server
     */
    public static final SupportedFeatures LOCAL_VERSIONS = new SupportedFeatures(64, "LocalVersions"); //$NON-NLS-1$

    // Bit 128 is reserved for the Orcas Dogfood implementation of workspace
    // permissions.

    /**
     * This represents the ability to page pending changes down from the server,
     * and queue up pending changes for checkin, before committing them.
     */
    public static final SupportedFeatures BATCHED_CHECKINS = new SupportedFeatures(256, "BatchedCheckins"); //$NON-NLS-1$

    /**
     * The server supports workspace permissions
     */
    public static final SupportedFeatures WORKSPACE_PERMISSIONS = new SupportedFeatures(512, "WorkspacePermissions"); //$NON-NLS-1$

    /**
     * The server supports the ability to set checkin dates
     */
    public static final SupportedFeatures CHECKIN_DATES = new SupportedFeatures(1024, "CheckinDates"); //$NON-NLS-1$

    /**
     * This is a combination of all the features which are supported. Subject to
     * change across releases. You can send this value (from client object model
     * to server, or from server to client object model) and mask with it, but
     * you should not test for equality against it.
     */
    public static final SupportedFeatures ALL = new SupportedFeatures(combine(new SupportedFeatures[] {
        GET_LATEST_ON_CHECKOUT,
        ONE_LEVEL_MAPPING,
        DESTROY,
        CREATE_BRANCH,
        GET_CHANGES_FOR_CHANGESET,
        PROXY_SUITE,
        LOCAL_VERSIONS,
        WORKSPACE_PERMISSIONS,
        BATCHED_CHECKINS,
        CHECKIN_DATES
    }).toIntFlags(), "All"); //$NON-NLS-1$

    public static SupportedFeatures combine(final SupportedFeatures[] changeTypes) {
        return new SupportedFeatures(BitField.combine(changeTypes));
    }

    private SupportedFeatures(final int flags, final String name) {
        super(flags);
        registerStringValue(getClass(), flags, name);
    }

    public SupportedFeatures(final int flags) {
        super(flags);
    }

    public boolean containsAll(final SupportedFeatures other) {
        return containsAllInternal(other);
    }

    public boolean contains(final SupportedFeatures other) {
        return containsInternal(other);
    }

    public boolean containsAny(final SupportedFeatures other) {
        return containsAnyInternal(other);
    }

    public SupportedFeatures remove(final SupportedFeatures other) {
        return new SupportedFeatures(removeInternal(other));
    }

    public SupportedFeatures retain(final SupportedFeatures other) {
        return new SupportedFeatures(retainInternal(other));
    }

    public SupportedFeatures combine(final SupportedFeatures other) {
        return new SupportedFeatures(combineInternal(other));
    }
}