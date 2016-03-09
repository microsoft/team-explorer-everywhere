// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.build.internal.soapextensions;

import com.microsoft.tfs.core.clients.build.flags.QueuePriority2010;
import com.microsoft.tfs.core.clients.build.flags.QueuedBuildUpdate2010;
import com.microsoft.tfs.core.internal.wrappers.WebServiceObjectWrapper;

import ms.tfs.build.buildservice._03._QueuedBuildUpdateOptions;

public class QueuedBuildUpdateOptions2010 extends WebServiceObjectWrapper {
    private QueuedBuildUpdateOptions2010() {
        this(new _QueuedBuildUpdateOptions());
    }

    public QueuedBuildUpdateOptions2010(final _QueuedBuildUpdateOptions value) {
        super(value);
    }

    public QueuedBuildUpdateOptions2010(final QueuedBuildUpdateOptions updateOptions) {
        this();

        final _QueuedBuildUpdateOptions o = getWebServiceObject();
        o.setFields(TFS2010Helper.convert(updateOptions.getFields()).getWebServiceObject());
        o.setPostponed(updateOptions.isPostponed());
        o.setPriority(TFS2010Helper.convert(updateOptions.getPriority()).getWebServiceObject());
        o.setQueueId(updateOptions.getQueueID());
    }

    public _QueuedBuildUpdateOptions getWebServiceObject() {
        return (_QueuedBuildUpdateOptions) webServiceObject;
    }

    public QueuedBuildUpdate2010 getFields() {
        return QueuedBuildUpdate2010.fromWebServiceObject(getWebServiceObject().getFields());
    }

    public boolean isPostponed() {
        return getWebServiceObject().isPostponed();
    }

    public QueuePriority2010 getPriority() {
        return QueuePriority2010.fromWebServiceObject(getWebServiceObject().getPriority());
    }

    public int getQueueID() {
        return getWebServiceObject().getQueueId();
    }

    public void setFields(final QueuedBuildUpdate2010 value) {
        getWebServiceObject().setFields(value.getWebServiceObject());
    }

    public void setPostponed(final boolean value) {
        getWebServiceObject().setPostponed(value);
    }

    public void setPriority(final QueuePriority2010 value) {
        getWebServiceObject().setPriority(value.getWebServiceObject());
    }

    public void setQueueID(final int value) {
        getWebServiceObject().setQueueId(value);
    }
}
