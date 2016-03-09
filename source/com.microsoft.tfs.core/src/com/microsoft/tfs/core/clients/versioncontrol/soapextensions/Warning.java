// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.soapextensions;

import com.microsoft.tfs.core.internal.wrappers.WebServiceObjectWrapper;

import ms.tfs.versioncontrol.clientservices._03._Warning;

/**
 * Represents a warning encountered during a version control operation.
 *
 * @since TEE-SDK-10.1
 */
public class Warning extends WebServiceObjectWrapper {
    public Warning(final _Warning warning) {
        super(warning);

        final String displayName = warning.getUserdisp();
        if (displayName == null || displayName.length() == 0) {
            warning.setUserdisp(warning.getUser());
        }
    }

    /**
     * Gets the web service object this class wraps. The returned object should
     * not be modified.
     *
     * @return the web service object this class wraps.
     */
    public _Warning getWebServiceObject() {
        return (_Warning) webServiceObject;
    }

    public String getUser() {
        return getWebServiceObject().getUser();
    }

    public String getUserDisplayName() {
        return getWebServiceObject().getUserdisp();
    }

    public ChangeType getChangeType() {
        return new ChangeType(getWebServiceObject().getChg(), getWebServiceObject().getChgEx());
    }

    public String getWarningMessage() {
        return getWebServiceObject().getWs();
    }

    public WarningType getWarningType() {
        return WarningType.fromWebServiceObject(getWebServiceObject().getWrn());
    }

    public String getParentOrChildPath() {
        return getWebServiceObject().getCpp();
    }

    /**
     * @return the name of the workspace where the conflicting change is pended.
     */
    public String getWorkspace() {
        return getWebServiceObject().getWs();
    }
}
