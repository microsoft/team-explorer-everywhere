// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.authorization;

import com.microsoft.tfs.core.internal.wrappers.WebServiceObjectWrapper;

import ms.tfs.services.authorization._03._Identity;

/**
 * An {@link Identity} structure represents a user, group, or Team Foundation
 * Server application group, along with some of their attributes.
 *
 * @threadsafety thread-compatible
 * @since TEE-SDK-10.1
 */
public class Identity extends WebServiceObjectWrapper {
    public Identity(final _Identity webServiceObject) {
        super(webServiceObject);
    }

    /**
     * Gets the web service object this class wraps. The returned object should
     * not be modified.
     *
     * @return the web service object this class wraps.
     */
    public _Identity getWebServiceObject() {
        return (_Identity) webServiceObject;
    }

    public IdentityType getType() {
        return IdentityType.fromWebServiceObject(getWebServiceObject().getType());
    }

    public String getSID() {
        return getWebServiceObject().getSid();
    }

    public String getDisplayName() {
        return getWebServiceObject().getDisplayName();
    }

    public String getDescription() {
        return getWebServiceObject().getDescription();
    }

    public String getDomain() {
        return getWebServiceObject().getDomain();
    }

    public String getAccountName() {
        return getWebServiceObject().getAccountName();
    }

    public String getDistinguishedName() {
        return getWebServiceObject().getDistinguishedName();
    }

    public String getMailAddress() {
        return getWebServiceObject().getMailAddress();
    }

    public ApplicationGroupSpecialType getSpecialType() {
        return new ApplicationGroupSpecialType(getWebServiceObject().getSpecialType());
    }

    public boolean isDeleted() {
        return getWebServiceObject().isDeleted();
    }

    public String[] getMembers() {
        return getWebServiceObject().getMembers();
    }

    public String[] getMemberOf() {
        return getWebServiceObject().getMemberOf();
    }

    public boolean isSecurityGroup() {
        return getWebServiceObject().isSecurityGroup();
    }
}
