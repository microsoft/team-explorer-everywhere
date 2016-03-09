// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.security;

import com.microsoft.tfs.core.clients.webservices.IdentityDescriptor;
import com.microsoft.tfs.core.internal.wrappers.WebServiceObjectWrapper;

import ms.ws._AccessControlEntry;

/**
 * Describes the levels of access allowed for a given identity (described by an
 * {@link IdentityDescriptor}.
 *
 * @threadsafety thread-compatible
 * @since TEE-SDK-10.1
 * @see {@link AccessControlEntryDetails}
 */
public abstract class AccessControlEntry extends WebServiceObjectWrapper {
    protected AccessControlEntry(final _AccessControlEntry webServiceObject) {
        super(webServiceObject);
    }

    /**
     * Gets the web service object this class wraps. The returned object should
     * not be modified.
     *
     * @return the web service object this class wraps.
     */
    public _AccessControlEntry getWebServiceObject() {
        return (_AccessControlEntry) webServiceObject;
    }

    public int getAllow() {
        return getWebServiceObject().getAllow();
    }

    public void setAllow(final int value) {
        getWebServiceObject().setAllow(value);
    }

    public int getDeny() {
        return getWebServiceObject().getDeny();
    }

    public void setDeny(final int value) {
        getWebServiceObject().setDeny(value);
    }
}
