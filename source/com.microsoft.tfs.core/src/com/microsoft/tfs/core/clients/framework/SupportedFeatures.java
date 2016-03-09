// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.framework;

import com.microsoft.tfs.util.BitField;

/**
 * Features supported by the framework (identity, catalog, location) services of
 * a Team Foundation Server.
 *
 * @since TEE-SDK-11.0
 */
public final class SupportedFeatures extends BitField {
    public static final SupportedFeatures NONE = new SupportedFeatures(0, "None"); //$NON-NLS-1$

    /**
     * The client supports strongly-typed identity properties.
     */
    public static final SupportedFeatures IDENTITY_PROPERTIES = new SupportedFeatures(1, "IdentityProperties"); //$NON-NLS-1$

    /**
     * This is a combination of all the features which are supported. Subject to
     * change across releases. You can send this value (from client object model
     * to server, or from server to client object model) and mask with it, but
     * you should not test for equality against it.
     */
    public static final SupportedFeatures ALL = new SupportedFeatures(combine(new SupportedFeatures[] {
        IDENTITY_PROPERTIES

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