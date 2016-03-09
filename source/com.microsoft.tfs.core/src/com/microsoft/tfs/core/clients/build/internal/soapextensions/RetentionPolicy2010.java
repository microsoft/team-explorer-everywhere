// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.build.internal.soapextensions;

import com.microsoft.tfs.core.clients.build.flags.BuildReason2010;
import com.microsoft.tfs.core.clients.build.flags.BuildStatus2010;
import com.microsoft.tfs.core.clients.build.flags.DeleteOptions2010;
import com.microsoft.tfs.core.internal.wrappers.WebServiceObjectWrapper;

import ms.tfs.build.buildservice._03._RetentionPolicy;

public class RetentionPolicy2010 extends WebServiceObjectWrapper {
    private RetentionPolicy2010() {
        this(new _RetentionPolicy());
    }

    public RetentionPolicy2010(final _RetentionPolicy value) {
        super(value);
    }

    public RetentionPolicy2010(final RetentionPolicy policy) {
        this();

        setBuildReason(TFS2010Helper.convert(policy.getBuildReason()));
        setBuildStatus(TFS2010Helper.convert(policy.getBuildStatus()));
        setDeleteOptions(TFS2010Helper.convert(policy.getDeleteOptions()));
        setNumberToKeep(policy.getNumberToKeep());
    }

    public _RetentionPolicy getWebServiceObject() {
        return (_RetentionPolicy) webServiceObject;
    }

    public BuildReason2010 getBuildReason() {
        return BuildReason2010.fromWebServiceObject(getWebServiceObject().getBuildReason());
    }

    public BuildStatus2010 getBuildStatus() {
        return BuildStatus2010.fromWebServiceObject(getWebServiceObject().getBuildStatus());
    }

    public DeleteOptions2010 getDeleteOptions() {
        return DeleteOptions2010.fromWebServiceObject(getWebServiceObject().getDeleteOptions());
    }

    public int getNumberToKeep() {
        return getWebServiceObject().getNumberToKeep();
    }

    public void setBuildReason(final BuildReason2010 value) {
        getWebServiceObject().setBuildReason(value.getWebServiceObject());
    }

    public void setBuildStatus(final BuildStatus2010 value) {
        getWebServiceObject().setBuildStatus(value.getWebServiceObject());
    }

    public void setDeleteOptions(final DeleteOptions2010 value) {
        getWebServiceObject().setDeleteOptions(value.getWebServiceObject());
    }

    public void setNumberToKeep(final int value) {
        getWebServiceObject().setNumberToKeep(value);
    }
}
