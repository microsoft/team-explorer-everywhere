// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.webservices;

import com.microsoft.tfs.util.BitField;

/**
 * @threadsafety immutable
 * @since TEE-SDK-11.0
 */
public class ReadIdentityOptions extends BitField {
    public static final ReadIdentityOptions NONE = new ReadIdentityOptions(0);

    /**
     * read from IMS store only, or also from external sources such as AD.
     * <p>
     * Reading from the identity provider will add an overhead. An example of
     * when this option may be required, is to assign permission(s) to some
     * identity that may or may not be in the database already.
     */
    public static final ReadIdentityOptions INCLUDE_READ_FROM_SOURCE = new ReadIdentityOptions(1);

    /**
     * whether to map back well known TFS Sids to their well known form, or
     * expose their true value.
     */
    public static final ReadIdentityOptions TRUE_SID = new ReadIdentityOptions(2);

    /**
     * read extended identity properties.
     */
    public static final ReadIdentityOptions EXTENDED_PROPERTIES = new ReadIdentityOptions(4);

    private ReadIdentityOptions(final int flags) {
        super(flags);
    }

    // -- Common Strongly types BitField methods.

    public static ReadIdentityOptions combine(final ReadIdentityOptions[] ReadIdentityOptions) {
        return new ReadIdentityOptions(BitField.combine(ReadIdentityOptions));
    }

    public boolean containsAll(final ReadIdentityOptions other) {
        return containsAllInternal(other);
    }

    public boolean contains(final ReadIdentityOptions other) {
        return containsInternal(other);
    }

    public boolean containsAny(final ReadIdentityOptions other) {
        return containsAnyInternal(other);
    }

    public ReadIdentityOptions remove(final ReadIdentityOptions other) {
        return new ReadIdentityOptions(removeInternal(other));
    }

    public ReadIdentityOptions retain(final ReadIdentityOptions other) {
        return new ReadIdentityOptions(retainInternal(other));
    }

    public ReadIdentityOptions combine(final ReadIdentityOptions other) {
        return new ReadIdentityOptions(combineInternal(other));
    }

}
