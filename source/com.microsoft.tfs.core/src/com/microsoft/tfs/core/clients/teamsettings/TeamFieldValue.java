// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.teamsettings;

import com.microsoft.tfs.core.internal.wrappers.WebServiceObjectWrapper;

import ms.tfs.services.teamconfiguration._01._TeamFieldValue;

public class TeamFieldValue extends WebServiceObjectWrapper {
    public TeamFieldValue(final _TeamFieldValue webServiceObject) {
        super(webServiceObject);
    }

    /**
     * Gets the web service object this class wraps. The returned object should
     * not be modified.
     *
     * @return the web service object this class wraps.
     */
    public _TeamFieldValue getWebServiceObject() {
        return (_TeamFieldValue) webServiceObject;
    }

    public String getValue() {
        return getWebServiceObject().getValue();
    }

    public void setValue(final String value) {
        getWebServiceObject().setValue(value);
    }

    public boolean isIncludeChildren() {
        return getWebServiceObject().isIncludeChildren();
    }

    public void setIncludeChildren(final boolean value) {
        getWebServiceObject().setIncludeChildren(value);
    }
}
