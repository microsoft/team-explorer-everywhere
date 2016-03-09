// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.webservices;

import com.microsoft.tfs.util.TypesafeEnum;

/**
 * @threadsafety immutable
 * @since TEE-SDK-11.0
 */
public class IdentitySearchFactor extends TypesafeEnum {
    /**
     * NT account name (domain\alias)
     */
    public static final IdentitySearchFactor ACCOUNT_NAME = new IdentitySearchFactor(0);

    /**
     * Display name
     */
    public static final IdentitySearchFactor DISPLAY_NAME = new IdentitySearchFactor(1);

    /**
     * Find project admin group
     */
    public static final IdentitySearchFactor ADMINISTRATORS_GROUP = new IdentitySearchFactor(2);

    /**
     * Find the identity using the identifier
     */
    public static final IdentitySearchFactor IDENTIFIER = new IdentitySearchFactor(3);

    /**
     * Email address
     */
    public static final IdentitySearchFactor MAIL_ADDRESS = new IdentitySearchFactor(4);

    /**
     * A general search for an identity. This is the default search factor for
     * shorter overloads of ReadIdentity, and typically the correct choice for
     * user input.
     * <p>
     * Use the general search factor to find one or more identities by one of
     * the following properties:
     * <ul>
     * <li>Display name</li>
     * <li>account name</li>
     * <li>UniqueName</li>
     * </ul>
     * UniqueName may be easier to type than display name. It can also be used
     * to indicate a single identity when two or more identities share the same
     * display name (e.g. "John Smith")
     */
    public static final IdentitySearchFactor GENERAL = new IdentitySearchFactor(5);

    protected IdentitySearchFactor(final int value) {
        super(value);
    }
}
