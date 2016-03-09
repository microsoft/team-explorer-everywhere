// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.soapextensions.internal;

import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;
import com.microsoft.tfs.core.clients.workitem.CheckinWorkItemAction;
import com.microsoft.tfs.core.internal.wrappers.WebServiceObjectWrapper;

import ms.tfs.versioncontrol.clientservices._03._CheckinNotificationWorkItemInfo;

/**
 * Associates a work item (by ID) with a {@link CheckinWorkItemAction} which
 * will be performed as part of a check-in.
 *
 * This class is currently unused because {@link Workspace} creates the wrapped
 * web service type ({@link _CheckinNotificationWorkItemInfo}) directly.
 */
public class CheckinNotificationWorkItemInfo extends WebServiceObjectWrapper {
    public CheckinNotificationWorkItemInfo(final int id, final CheckinWorkItemAction action) {
        super(new _CheckinNotificationWorkItemInfo(id, action.getWebServiceObject()));
    }

    public CheckinNotificationWorkItemInfo(final _CheckinNotificationWorkItemInfo info) {
        super(info);
    }

    /**
     * Gets the web service object this class wraps. The returned object should
     * not be modified.
     *
     * @return the web service object this class wraps.
     */
    public _CheckinNotificationWorkItemInfo getWebServiceObject() {
        return (_CheckinNotificationWorkItemInfo) webServiceObject;
    }

    public CheckinWorkItemAction getCheckinAction() {
        return CheckinWorkItemAction.fromWebServiceObject(getWebServiceObject().getCheckinAction());
    }

    public int getID() {
        return getWebServiceObject().getId();
    }

    public void setCheckinAction(final CheckinWorkItemAction value) {
        getWebServiceObject().setCheckinAction(value.getWebServiceObject());
    }

    public void setID(final int value) {
        getWebServiceObject().setId(value);
    }
}
