// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.build.internal.soapextensions;

import java.util.Calendar;

import com.microsoft.tfs.core.clients.build.flags.ControllerStatus2010;
import com.microsoft.tfs.core.internal.wrappers.WebServiceObjectWrapper;

import ms.tfs.build.buildservice._03._BuildController;

public class BuildController2010 extends WebServiceObjectWrapper {
    private BuildController2010() {
        this(new _BuildController());
    }

    public BuildController2010(final _BuildController value) {
        super(value);
    }

    public BuildController2010(final BuildController controller) {
        this();

        setCustomAssemblyPath(controller.getCustomAssemblyPath());
        setDateUpdated(controller.getDateCreated());
        setDateUpdated(controller.getDateUpdated());
        setDescription(controller.getDescription());
        setEnabled(controller.isEnabled());
        setTags(controller.getTags());
        setMaxConcurrentBuilds(controller.getMaxConcurrentBuilds());
        setName(controller.getName());
        setQueueCount(controller.getQueueCount());
        setServiceHostURI(controller.getServiceHostURI());
        setStatus(TFS2010Helper.convert(controller.getStatus()));
        setStatusMessage(controller.getStatusMessage());
        setURI(controller.getURI());
        setURL(controller.getURL());
    }

    public _BuildController getWebServiceObject() {
        return (_BuildController) webServiceObject;
    }

    public String getCustomAssemblyPath() {
        return getWebServiceObject().getCustomAssemblyPath();
    }

    public Calendar getDateCreated() {
        return getWebServiceObject().getDateCreated();
    }

    public Calendar getDateUpdated() {
        return getWebServiceObject().getDateUpdated();
    }

    public String getDescription() {
        return getWebServiceObject().getDescription();
    }

    public boolean isEnabled() {
        return getWebServiceObject().isEnabled();
    }

    public String[] getTags() {
        return getWebServiceObject().getTags();
    }

    public int getMaxConcurrentBuilds() {
        return getWebServiceObject().getMaxConcurrentBuilds();
    }

    public String getName() {
        return getWebServiceObject().getName();
    }

    public int getQueueCount() {
        return getWebServiceObject().getQueueCount();
    }

    public String getServiceHostURI() {
        return getWebServiceObject().getServiceHostUri();
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

    public String getURL() {
        return getWebServiceObject().getUrl();
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

    public void setURL(final String value) {
        getWebServiceObject().setUrl(value);
    }

    private void setDateUpdated(final Calendar value) {
        getWebServiceObject().setDateUpdated(value);
    }

    private void setTags(final String[] value) {
        getWebServiceObject().setTags(value);
    }

    private void setQueueCount(final int value) {
        getWebServiceObject().setQueueCount(value);
    }

    private void setServiceHostURI(final String value) {
        getWebServiceObject().setServiceHostUri(value);
    }
}
