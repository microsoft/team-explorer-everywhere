// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.security;

import com.microsoft.tfs.core.internal.wrappers.WrapperUtils;

import ms.ws._AccessControlEntryDetails;
import ms.ws._AccessControlListDetails;

public class AccessControlListDetails extends AccessControlList {
    public AccessControlListDetails(final _AccessControlListDetails webServiceObject) {
        super(webServiceObject);

        setAccessControlEntries(getAcesFromWeb(webServiceObject), false);
    }

    public AccessControlListDetails(
        final boolean inheritPermissions,
        final String token,
        final boolean includeExtendedInfo,
        final AccessControlEntryDetails[] accessControlEntries) {
        this(
            new _AccessControlListDetails(
                inheritPermissions,
                token,
                includeExtendedInfo,
                (_AccessControlEntryDetails[]) WrapperUtils.unwrap(
                    _AccessControlEntryDetails.class,
                    accessControlEntries)));
    }

    public AccessControlListDetails(final String token, final boolean inheritPermissions) {
        // An empty ACS list matches the built-in private workspace
        // permission profile
        this(new _AccessControlListDetails(inheritPermissions, token, false, new _AccessControlEntryDetails[0]));
    }

    /**
     * Gets the web service object this class wraps. The returned object should
     * not be modified.
     *
     * @return the web service object this class wraps.
     */
    @Override
    public _AccessControlListDetails getWebServiceObject() {
        return (_AccessControlListDetails) super.getWebServiceObject();
    }

    @Override
    public boolean isInheritPermissions() {
        return getWebServiceObject().isInheritPermissions();
    }

    @Override
    public void setInheritPermissions(final boolean value) {
        getWebServiceObject().setInheritPermissions(value);
    }

    @Override
    public String getToken() {
        return getWebServiceObject().getToken();
    }

    @Override
    public void setToken(final String value) {
        getWebServiceObject().setToken(value);
    }

    @Override
    public AccessControlEntryDetails[] getAccessControlEntries() {
        return getAcesFromWeb(getWebServiceObject());
    }

    private AccessControlEntryDetails[] getAcesFromWeb(final _AccessControlListDetails webServiceObject) {
        if (webServiceObject.getAccessControlEntries() == null) {
            return null;
        } else {
            return (AccessControlEntryDetails[]) WrapperUtils.wrap(
                AccessControlEntryDetails.class,
                webServiceObject.getAccessControlEntries());
        }
    }
}
