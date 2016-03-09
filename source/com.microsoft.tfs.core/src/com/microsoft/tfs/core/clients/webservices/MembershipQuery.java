// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.webservices;

import com.microsoft.tfs.util.TypesafeEnum;

/**
 * An index for IMS identity caches.
 *
 * @threadsafety immutable
 * @since TEE-SDK-11.0
 */
public class MembershipQuery extends TypesafeEnum {
    // These enumeration values should run from zero to N, with no gaps.
    // IdentityHostCache uses these values as indexes.

    /**
     * Query will not return any membership data
     */
    public static final MembershipQuery NONE = new MembershipQuery(0);

    /**
     * Query will return only direct membership data
     */
    public static final MembershipQuery DIRECT = new MembershipQuery(1);

    /**
     * Query will return expanded membership data
     */
    public static final MembershipQuery EXPANDED = new MembershipQuery(2);

    // Dev10 had the public value "Last = 3", as an indicator of the end of the
    // enumeration.

    protected MembershipQuery(final int value) {
        super(value);
    }
}
