// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.build.internal.soapextensions;

import com.microsoft.tfs.core.clients.build.flags.BuildServiceHostUpdate;
import com.microsoft.tfs.core.internal.wrappers.WebServiceObjectWrapper;

import ms.tfs.build.buildservice._04._BuildServiceHostUpdateOptions;

public class BuildServiceHostUpdateOptions extends WebServiceObjectWrapper {
    public BuildServiceHostUpdateOptions() {
        super(new _BuildServiceHostUpdateOptions());
    }

    public _BuildServiceHostUpdateOptions getWebServiceObject() {
        return (_BuildServiceHostUpdateOptions) this.webServiceObject;
    }

    /**
     * Gets or sets the base URL. Corresponds to
     * <see cref="BuildServiceHostUpdate.BaseUrl" />.
     *
     *
     * @return
     */
    public String getBaseURL() {
        return getWebServiceObject().getBaseUrl();
    }

    public void setBaseURL(final String value) {
        getWebServiceObject().setBaseUrl(value);
    }

    /**
     * Gets or sets the fields which should be updated. Only values included
     * here will be extracted from this object during an update.
     *
     *
     * @return
     */
    public BuildServiceHostUpdate getFields() {
        return BuildServiceHostUpdate.fromWebServiceObject(getWebServiceObject().getFields());
    }

    public void setFields(final BuildServiceHostUpdate value) {
        getWebServiceObject().setFields(value.getWebServiceObject());
    }

    /**
     * Gets or sets the display name. Corresponds to
     * <see cref="BuildServiceHostUpdate.Name" />.
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

    public boolean isRequireClientCertificates() {
        return getWebServiceObject().isRequireClientCertificates();
    }

    public void setRequireClientCertificates(final boolean value) {
        getWebServiceObject().setRequireClientCertificates(value);
    }

    /**
     * Gets or sets the URI of the build service host to update.
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
