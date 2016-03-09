// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.build.internal.soapextensions;

import com.microsoft.tfs.core.clients.build.flags.BuildAgentUpdate;
import com.microsoft.tfs.core.clients.build.soapextensions.AgentStatus;
import com.microsoft.tfs.core.internal.wrappers.WebServiceObjectWrapper;
import com.microsoft.tfs.core.internal.wrappers.WrapperUtils;

import ms.tfs.build.buildservice._04._BuildAgentUpdateOptions;
import ms.tfs.build.buildservice._04._PropertyValue;

public class BuildAgentUpdateOptions extends WebServiceObjectWrapper {
    public BuildAgentUpdateOptions() {
        super(new _BuildAgentUpdateOptions());
    }

    public _BuildAgentUpdateOptions getWebServiceObject() {
        return (_BuildAgentUpdateOptions) this.webServiceObject;
    }

    public PropertyValue[] getAttachedProperties() {
        return (PropertyValue[]) WrapperUtils.wrap(PropertyValue.class, getWebServiceObject().getAttachedProperties());
    }

    public void setAttachedProperties(final PropertyValue[] value) {
        getWebServiceObject().setAttachedProperties(
            (_PropertyValue[]) WrapperUtils.unwrap(_PropertyValue.class, value));
    }

    /**
     * Gets or sets the build directory. Corresponds to
     * <see cref="BuildAgentUpdate.BuildDirectory" />.
     *
     *
     * @return
     */
    public String getBuildDirectory() {
        return getWebServiceObject().getBuildDirectory();
    }

    public void setBuildDirectory(final String value) {
        getWebServiceObject().setBuildDirectory(value);
    }

    /**
     * Gets or sets the build controller URI. Corresponds to
     * <see cref="BuildAgentUpdate.ControllerUri" />.
     *
     *
     * @return
     */
    public String getControllerURI() {
        return getWebServiceObject().getControllerUri();
    }

    public void setControllerURI(final String value) {
        getWebServiceObject().setControllerUri(value);
    }

    /**
     * Gets or sets the description. Corresponds to
     * <see cref="BuildAgentUpdate.Description" />.
     *
     *
     * @return
     */
    public String getDescription() {
        return getWebServiceObject().getDescription();
    }

    public void setDescription(final String value) {
        getWebServiceObject().setDescription(value);
    }

    /**
     * Gets or sets the enabled state. Corresponds to
     * <see cref="BuildAgentUpdate.Enabled" />.
     *
     *
     * @return
     */
    public boolean isEnabled() {
        return getWebServiceObject().isEnabled();
    }

    public void setEnabled(final boolean value) {
        getWebServiceObject().setEnabled(value);
    }

    /**
     * Gets or sets the fields which should be updated. Only values included
     * here will be extracted from this object during an update.
     *
     *
     * @return
     */
    public BuildAgentUpdate getFields() {
        return BuildAgentUpdate.fromWebServiceObject(getWebServiceObject().getFields());
    }

    public void setFields(final BuildAgentUpdate value) {
        getWebServiceObject().setFields(value.getWebServiceObject());
    }

    /**
     * Gets or sets the display name. Corresponds to
     * <see cref="BuildAgentUpdate.Name" />.
     *
     *
     * @return
     */
    public String getName() {
        return getWebServiceObject().getName();
    }

    public void setName(final String value) {
        getWebServiceObject().setName(value);
    }

    /**
     * Gets or sets the status. Corresponds to
     * <see cref="BuildAgentUpdate.Status" />.
     *
     *
     * @return
     */
    public AgentStatus getStatus() {
        return AgentStatus.fromWebServiceObject(getWebServiceObject().getStatus());
    }

    public void setStatus(final AgentStatus value) {
        getWebServiceObject().setStatus(value.getWebServiceObject());
    }

    /**
     * Gets or sets the status message. Corresponds to
     * <see cref="BuildAgentUpdate.StatusMessage" />.
     *
     *
     * @return
     */
    public String getStatusMessage() {
        return getWebServiceObject().getStatusMessage();
    }

    public void setStatusMessage(final String value) {
        getWebServiceObject().setStatusMessage(value);
    }

    /**
     * Gets the list of tags. Corresponds to
     * <see cref="BuildAgentUpdate.Tags" />.
     *
     *
     * @return
     */
    public String[] getTags() {
        return getWebServiceObject().getTags();
    }

    public void setTags(final String[] value) {
        getWebServiceObject().setTags(value);
    }

    /**
     * Gets or sets the URI of the build agent to update.
     *
     *
     * @return
     */
    public String getURI() {
        return getWebServiceObject().getUri();
    }

    public void setURI(final String value) {
        getWebServiceObject().setUri(value);
    }
}
