// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.build.internal.soapextensions;

import com.microsoft.tfs.core.internal.wrappers.WebServiceObjectWrapper;

import ms.tfs.build.buildservice._04._BuildTeamProjectPermission;

public class BuildTeamProjectPermission extends WebServiceObjectWrapper {
    public BuildTeamProjectPermission() {
        super(new _BuildTeamProjectPermission());
    }

    public _BuildTeamProjectPermission getWebServiceObject() {
        return (_BuildTeamProjectPermission) webServiceObject;
    }

    public String[] getAllows() {
        return getWebServiceObject().getAllows().clone();
    }

    public void setAllows(final String[] value) {
        getWebServiceObject().setAllows(value);
    }

    public String[] getDenies() {
        return getWebServiceObject().getDenies().clone();
    }

    public String getIdentityName() {
        return getWebServiceObject().getIdentityName();
    }

    public void setIdentityName(final String value) {
        getWebServiceObject().setIdentityName(value);
    }
}
