// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.build.internal.soapextensions;

import com.microsoft.tfs.core.clients.build.soapextensions.Agent2008Status;

import ms.tfs.build.buildservice._03._BuildAgent2008;

public class BuildAgent2008 extends BuildGroupItem2010 {
    public BuildAgent2008(final _BuildAgent2008 webServiceObject) {
        super(webServiceObject);
    }

    @Override
    public _BuildAgent2008 getWebServiceObject() {
        return (_BuildAgent2008) this.webServiceObject;
    }

    public String getBuildDirectory() {
        return getWebServiceObject().getBuildDirectory();
    }

    public String getDescription() {
        return getWebServiceObject().getDescription();
    }

    public String getMachineName() {
        return getWebServiceObject().getMachineName();
    }

    public int getMaxProcesses() {
        return getWebServiceObject().getMaxProcesses();
    }

    public int getPort() {
        return getWebServiceObject().getPort();
    }

    public int getQueueCount() {
        return getWebServiceObject().getQueueCount();
    }

    public boolean isRequireSecureChannel() {
        return getWebServiceObject().isRequireSecureChannel();
    }

    public Agent2008Status getStatus() {
        return Agent2008Status.fromWebServiceObject(getWebServiceObject().getStatus());
    }

    public String getStatusMessage() {
        return getWebServiceObject().getStatusMessage();
    }
}
