// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.build.internal.soapextensions;

import com.microsoft.tfs.core.internal.wrappers.WebServiceObjectWrapper;

import ms.tfs.build.buildservice._03._BuildTeamProjectPermission;

public class BuildTeamProjectPermission2010 extends WebServiceObjectWrapper {
    private BuildTeamProjectPermission2010() {
        this(new _BuildTeamProjectPermission());
    }

    public BuildTeamProjectPermission2010(final _BuildTeamProjectPermission value) {
        super(value);
    }

    public BuildTeamProjectPermission2010(final BuildTeamProjectPermission permission) {
        this();
        setAllows(permission.getAllows());
        setDenies(permission.getDenies());
        setIdentityName(permission.getIdentityName());
    }

    public _BuildTeamProjectPermission getWebServiceObject() {
        return (_BuildTeamProjectPermission) webServiceObject;
    }

    public String[] getAllows() {
        return getWebServiceObject().getAllows();
    }

    public String[] getDenies() {
        return getWebServiceObject().getDenies();
    }

    public String getIdentityName() {
        return getWebServiceObject().getIdentityName();
    }

    public void setAllows(final String[] value) {
        getWebServiceObject().setAllows(value);
    }

    public void setDenies(final String[] value) {
        getWebServiceObject().setDenies(value);
    }

    public void setIdentityName(final String value) {
        getWebServiceObject().setIdentityName(value);
    }
}
