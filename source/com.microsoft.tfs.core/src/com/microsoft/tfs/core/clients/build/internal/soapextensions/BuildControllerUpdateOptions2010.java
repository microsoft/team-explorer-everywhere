// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.build.internal.soapextensions;

import com.microsoft.tfs.core.clients.build.flags.BuildControllerUpdate2010;
import com.microsoft.tfs.core.clients.build.flags.ControllerStatus2010;
import com.microsoft.tfs.core.internal.wrappers.WebServiceObjectWrapper;

import ms.tfs.build.buildservice._03._BuildControllerUpdateOptions;

public class BuildControllerUpdateOptions2010 extends WebServiceObjectWrapper {
    private BuildControllerUpdateOptions2010() {
        this(new _BuildControllerUpdateOptions());
    }

    public BuildControllerUpdateOptions2010(final _BuildControllerUpdateOptions value) {
        super(value);
    }

    public BuildControllerUpdateOptions2010(final BuildControllerUpdateOptions updateOptions) {
        this();

        setCustomAssemblyPath(updateOptions.getCustomAssemblyPath());
        setDescription(updateOptions.getDescription());
        setEnabled(updateOptions.isEnabled());
        setFields(TFS2010Helper.convert(updateOptions.getFields()));
        setMaxConcurrentBuilds(updateOptions.getMaxConcurrentBuilds());
        setName(updateOptions.getName());
        setStatus(TFS2010Helper.convert(updateOptions.getStatus()));
        setStatusMessage(updateOptions.getStatusMessage());
        setURI(updateOptions.getURI());
    }

    public _BuildControllerUpdateOptions getWebServiceObject() {
        return (_BuildControllerUpdateOptions) webServiceObject;
    }

    public String getCustomAssemblyPath() {
        return getWebServiceObject().getCustomAssemblyPath();
    }

    public String getDescription() {
        return getWebServiceObject().getDescription();
    }

    public boolean isEnabled() {
        return getWebServiceObject().isEnabled();
    }

    public BuildControllerUpdate2010 getFields() {
        return BuildControllerUpdate2010.fromWebServiceObject(getWebServiceObject().getFields());
    }

    public int getMaxConcurrentBuilds() {
        return getWebServiceObject().getMaxConcurrentBuilds();
    }

    public String getName() {
        return getWebServiceObject().getName();
    }

    public ControllerStatus2010 getStatus() {
        return ControllerStatus2010.fromWebServiceObject(getWebServiceObject().getStatus());
    }

    public String getStatusMessage() {
        return getWebServiceObject().getStatusMessage();
    }

    public String getURI() {
        return getWebServiceObject().getUri();
    }

    public void setCustomAssemblyPath(final String value) {
        getWebServiceObject().setCustomAssemblyPath(value);
    }

    public void setDescription(final String value) {
        getWebServiceObject().setDescription(value);
    }

    public void setEnabled(final boolean value) {
        getWebServiceObject().setEnabled(value);
    }

    public void setFields(final BuildControllerUpdate2010 value) {
        getWebServiceObject().setFields(value.getWebServiceObject());
    }

    public void setMaxConcurrentBuilds(final int value) {
        getWebServiceObject().setMaxConcurrentBuilds(value);
    }

    public void setName(final String value) {
        getWebServiceObject().setName(value);
    }

    public void setStatus(final ControllerStatus2010 value) {
        getWebServiceObject().setStatus(value.getWebServiceObject());
    }

    public void setStatusMessage(final String value) {
        getWebServiceObject().setStatusMessage(value);
    }

    public void setURI(final String value) {
        getWebServiceObject().setUri(value);
    }
}
