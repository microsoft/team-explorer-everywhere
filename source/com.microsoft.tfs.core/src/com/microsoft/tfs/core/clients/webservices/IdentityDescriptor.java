// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.webservices;

import com.microsoft.tfs.core.clients.authorization.IdentityType;
import com.microsoft.tfs.core.internal.wrappers.WebServiceObjectWrapper;
import com.microsoft.tfs.util.Check;

import ms.ws._IdentityDescriptor;

/**
 * Wrapper for an identity type and a unique identifier.
 *
 * @threadsafety thread-compatible
 * @since TEE-SDK-10.1
 */
public class IdentityDescriptor extends WebServiceObjectWrapper {
    private Object data;

    /**
     * Constructs an {@link IdentityDescriptor} of the given type for the given
     * identity.
     *
     * @param identityType
     *        the type of identity (must be one of the types from
     *        {@link IdentityType}) (must not be <code>null</code> or empty)
     * @param identifier
     *        the identifier, usually a SID from
     *        {@link GroupWellKnownSIDConstants} (must not be <code>null</code>
     *        or empty)
     */
    public IdentityDescriptor(final String identityType, final String identifier) {
        this(new _IdentityDescriptor(identityType, identifier));

        Check.notNullOrEmpty(identityType, "identityType"); //$NON-NLS-1$
        Check.notNullOrEmpty(identifier, "identifier"); //$NON-NLS-1$
    }

    public IdentityDescriptor(final _IdentityDescriptor id) {
        super(id);
    }

    /**
     * Gets the web service object this class wraps. The returned object should
     * not be modified.
     *
     * @return the web service object this class wraps.
     */
    public _IdentityDescriptor getWebServiceObject() {
        return (_IdentityDescriptor) webServiceObject;
    }

    /**
     * @return the type of identifier
     */
    public String getIdentityType() {
        return getWebServiceObject().getIdentityType();
    }

    /**
     * @return the identifier
     */
    public String getIdentifier() {
        return getWebServiceObject().getIdentifier();
    }

    /**
     * @return the miscellaneous data not sent to TFS
     */
    public Object getData() {
        return data;
    }

    /**
     * Sets miscellaenous data not sent to TFS.
     *
     * @param data
     *        the data (may be null)
     */
    public void setData(final Object data) {
        this.data = data;
    }

    @Override
    public boolean equals(final Object o) {
        if (o == this) {
            return true;
        }
        if (o instanceof IdentityDescriptor == false) {
            return false;
        }

        return ((IdentityDescriptor) o).getIdentifier().equals(getIdentifier());
    }

    @Override
    public int hashCode() {
        return getIdentifier().hashCode();
    }

}
