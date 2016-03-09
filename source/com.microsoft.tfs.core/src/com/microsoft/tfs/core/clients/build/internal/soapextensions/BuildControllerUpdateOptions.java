// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.build.internal.soapextensions;

import com.microsoft.tfs.core.clients.build.flags.BuildControllerUpdate;
import com.microsoft.tfs.core.clients.build.soapextensions.ControllerStatus;
import com.microsoft.tfs.core.internal.wrappers.WebServiceObjectWrapper;
import com.microsoft.tfs.core.internal.wrappers.WrapperUtils;

import ms.tfs.build.buildservice._04._BuildControllerUpdateOptions;
import ms.tfs.build.buildservice._04._PropertyValue;

public class BuildControllerUpdateOptions extends WebServiceObjectWrapper {
    public BuildControllerUpdateOptions() {
        super(new _BuildControllerUpdateOptions());
    }

    public _BuildControllerUpdateOptions getWebServiceObject() {
        return (_BuildControllerUpdateOptions) this.webServiceObject;
    }

    public PropertyValue[] getAttachedProperties() {
        return (PropertyValue[]) WrapperUtils.wrap(PropertyValue.class, getWebServiceObject().getAttachedProperties());
    }

    public void setAttachedProperties(final PropertyValue[] value) {
        getWebServiceObject().setAttachedProperties(
            (_PropertyValue[]) WrapperUtils.unwrap(_PropertyValue.class, value));
    }

    /**
     * Gets or sets the custom assembly URI. Corresponds to
     * <see cref="BuildControllerUpdate.CustomAssemblyPath" />.
     *
     *
     * @return
     */
    public String getCustomAssemblyPath() {
        return getWebServiceObject().getCustomAssemblyPath();
    }

    public void setCustomAssemblyPath(final String value) {
        getWebServiceObject().setCustomAssemblyPath(value);
    }

    /**
     * Gets or sets the description. Corresponds to
     * <see cref="BuildControllerUpdate.Description" />.
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
     * <see cref="BuildControllerUpdate.Enabled" />.
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
    public BuildControllerUpdate getFields() {
        return BuildControllerUpdate.fromWebServiceObject(getWebServiceObject().getFields());
    }

    public void setFields(final BuildControllerUpdate value) {
        getWebServiceObject().setFields(value.getWebServiceObject());
    }

    /**
     * Gets or sets the number of maximum concurrent builds. Corresponds to
     * <see cref="BuildControllerUpdate.MaxConcurrentBuilds" />.
     *
     *
     * @return
     */
    public int getMaxConcurrentBuilds() {
        return getWebServiceObject().getMaxConcurrentBuilds();
    }

    public void setMaxConcurrentBuilds(final int value) {
        getWebServiceObject().setMaxConcurrentBuilds(value);
    }

    /**
     * Gets or sets the display name. Corresponds to
     * <see cref="BuildControllerUpdate.Name" />.
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
     * <see cref="BuildControllerUpdate.Status" />.
     *
     *
     * @return
     */
    public ControllerStatus getStatus() {
        return ControllerStatus.fromWebServiceObject(getWebServiceObject().getStatus());
    }

    public void setStatus(final ControllerStatus value) {
        getWebServiceObject().setStatus(value.getWebServiceObject());
    }

    /**
     * Gets or sets the status message. Corresponds to
     * <see cref="BuildControllerUpdate.StatusMessage" />.
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
     * Gets or sets the URI of the build controller to update.
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
