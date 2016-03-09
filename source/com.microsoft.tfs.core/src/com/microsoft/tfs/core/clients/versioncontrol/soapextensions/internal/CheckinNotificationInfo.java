// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.soapextensions.internal;

import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;
import com.microsoft.tfs.core.internal.wrappers.WebServiceObjectWrapper;
import com.microsoft.tfs.core.internal.wrappers.WrapperUtils;

import ms.tfs.versioncontrol.clientservices._03._CheckinNotificationInfo;
import ms.tfs.versioncontrol.clientservices._03._CheckinNotificationWorkItemInfo;

/**
 * Represents work items to be associated with or resolved by a check-in.
 *
 * This class is currently unused because {@link Workspace} creates the wrapped
 * web service type ({@link _CheckinNotificationInfo}) directly.
 */
public class CheckinNotificationInfo extends WebServiceObjectWrapper {
    public CheckinNotificationInfo(final CheckinNotificationWorkItemInfo[] workItemInfos) {
        super(
            new _CheckinNotificationInfo(
                (_CheckinNotificationWorkItemInfo[]) WrapperUtils.unwrap(
                    _CheckinNotificationWorkItemInfo.class,
                    workItemInfos)));
    }

    public CheckinNotificationInfo(final _CheckinNotificationInfo info) {
        super(info);
    }

    /**
     * Gets the web service object this class wraps. The returned object should
     * not be modified.
     *
     * @return the web service object this class wraps.
     */
    public _CheckinNotificationInfo getWebServiceObject() {
        return (_CheckinNotificationInfo) webServiceObject;
    }

    public CheckinNotificationWorkItemInfo[] getWorkItemInfo() {
        return (CheckinNotificationWorkItemInfo[]) WrapperUtils.wrap(
            CheckinNotificationWorkItemInfo.class,
            getWebServiceObject().getWorkItemInfo());
    }

    public void setWorkItemInfo(final CheckinNotificationWorkItemInfo[] value) {
        getWebServiceObject().setWorkItemInfo(
            (_CheckinNotificationWorkItemInfo[]) WrapperUtils.unwrap(_CheckinNotificationWorkItemInfo.class, value));
    }

}
