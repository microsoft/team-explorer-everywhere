// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.security;

import com.microsoft.tfs.core.clients.webservices.ACEExtendedInformation;
import com.microsoft.tfs.core.clients.webservices.IdentityDescriptor;

import ms.ws._AccessControlEntryDetails;

/**
 * Extends {@link AccessControlEntry} with more details.
 *
 * @threadsafety thread-compatible
 */
public class AccessControlEntryDetails extends AccessControlEntry implements Cloneable {
    public AccessControlEntryDetails(final _AccessControlEntryDetails webServiceObject) {
        super(webServiceObject);
    }

    public AccessControlEntryDetails(
        final IdentityDescriptor descriptor,
        final String token,
        final int allow,
        final int deny,
        final ACEExtendedInformation extendedInfo) {
        this(
            new _AccessControlEntryDetails(
                allow,
                deny,
                token,
                extendedInfo != null ? extendedInfo.getWebServiceObject() : null,
                descriptor != null ? descriptor.getWebServiceObject() : null));
    }

    public AccessControlEntryDetails(final IdentityDescriptor descriptor, final int allow, final int deny) {
        this(descriptor, null, allow, deny, null);
    }

    /**
     * Gets the web service object this class wraps. The returned object should
     * not be modified.
     *
     * @return the web service object this class wraps.
     */
    @Override
    public _AccessControlEntryDetails getWebServiceObject() {
        return (_AccessControlEntryDetails) super.getWebServiceObject();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object clone() {
        return new AccessControlEntryDetails(
            getSerializableDescriptor(),
            getToken(),
            getAllow(),
            getDeny(),
            getExtendedInformation());
    }

    public String getToken() {
        return getWebServiceObject().getToken();
    }

    public void setToken(final String value) {
        getWebServiceObject().setToken(value);
    }

    public ACEExtendedInformation getExtendedInformation() {
        if (getWebServiceObject().getExtendedInformation() != null) {
            return new ACEExtendedInformation(getWebServiceObject().getExtendedInformation());
        } else {
            return null;
        }
    }

    public void setExtendedInformation(final ACEExtendedInformation value) {
        getWebServiceObject().setExtendedInformation(value != null ? value.getWebServiceObject() : null);
    }

    public IdentityDescriptor getSerializableDescriptor() {
        if (getWebServiceObject().getSerializableDescriptor() != null) {
            return new IdentityDescriptor(getWebServiceObject().getSerializableDescriptor());
        } else {
            return null;
        }
    }

    public void setSerializableDescriptor(final IdentityDescriptor value) {
        getWebServiceObject().setSerializableDescriptor(value != null ? value.getWebServiceObject() : null);
    }
}
