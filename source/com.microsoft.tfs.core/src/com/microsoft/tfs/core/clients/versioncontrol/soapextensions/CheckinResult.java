// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.soapextensions;

import java.util.Calendar;

import com.microsoft.tfs.core.internal.wrappers.WebServiceObjectWrapper;
import com.microsoft.tfs.core.internal.wrappers.WrapperUtils;

import ms.tfs.versioncontrol.clientservices._03._CheckinResult;

/**
 * This represents the result of a call to the checkin method.
 *
 * @since TEE-SDK-11.0
 */
public class CheckinResult extends WebServiceObjectWrapper {
    public CheckinResult(final _CheckinResult result) {
        super(result);
    }

    /**
     * Gets the web service object this class wraps. The returned object should
     * not be modified.
     *
     * @return the web service object this class wraps.
     */
    public _CheckinResult getWebServiceObject() {
        return (_CheckinResult) webServiceObject;
    }

    public int getChangeset() {
        return getWebServiceObject().getCset();
    }

    public Calendar getDate() {
        return getWebServiceObject().getDate();
    }

    public CheckinState getState() {
        return CheckinState.fromInteger(getWebServiceObject().getState());
    }

    public int getTicket() {
        return getWebServiceObject().getTicket();
    }

    public String[] getUndoneServerItems() {
        return getWebServiceObject().getUndoneServerItems();
    }

    public GetOperation[] getLocalVersionUpdates() {
        return (GetOperation[]) WrapperUtils.wrap(GetOperation.class, getWebServiceObject().getLocalVersionUpdates());
    }

    public Calendar getCreationDate() {
        return getWebServiceObject().getDate();
    }
}
