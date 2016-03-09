// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.authorization;

import com.microsoft.tfs.core.internal.wrappers.EnumerationWrapper;

import ms.tfs.services.authorization._03._IdentityType;

/**
 * Represents different types of identity.
 *
 * @since TEE-SDK-10.1
 */
public class IdentityType extends EnumerationWrapper {
    public static final IdentityType INVALID_IDENTITY = new IdentityType(_IdentityType.InvalidIdentity);
    public static final IdentityType UNKNOWN_IDENTITY_TYPE = new IdentityType(_IdentityType.UnknownIdentityType);
    public static final IdentityType WINDOWS_USER = new IdentityType(_IdentityType.WindowsUser);
    public static final IdentityType WINDOWS_GROUP = new IdentityType(_IdentityType.WindowsGroup);
    public static final IdentityType APPLICATION_GROUP = new IdentityType(_IdentityType.ApplicationGroup);

    public IdentityType(final _IdentityType extendedInfo) {
        super(extendedInfo);
    }

    /**
     * Gets the web service object this class wraps. The returned object should
     * not be modified.
     *
     * @return the web service object this class wraps.
     */
    public _IdentityType getWebServiceObject() {
        return (_IdentityType) webServiceObject;
    }

    /**
     * Gets the correct wrapper type for the given web service object.
     *
     * @param webServiceObject
     *        the web service object (must not be <code>null</code>)
     * @return the correct wrapper type for the given web service object
     * @throws RuntimeException
     *         if no wrapper type is known for the given web service object
     */
    public static IdentityType fromWebServiceObject(final _IdentityType webServiceObject) {
        return (IdentityType) EnumerationWrapper.fromWebServiceObject(webServiceObject);
    }

    @Override
    public boolean equals(final Object o) {
        return super.equals(o);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }
}
