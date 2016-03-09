// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.build.internal.soapextensions;

import com.microsoft.tfs.core.clients.build.flags.QueuedBuildRetryOption;
import com.microsoft.tfs.core.clients.build.flags.QueuedBuildUpdate;
import com.microsoft.tfs.core.clients.build.soapextensions.QueuePriority;
import com.microsoft.tfs.core.internal.wrappers.WebServiceObjectWrapper;
import com.microsoft.tfs.util.GUID;
import com.microsoft.tfs.util.GUID.GUIDStringFormat;

import ms.tfs.build.buildservice._04._QueuedBuildUpdateOptions;

public class QueuedBuildUpdateOptions extends WebServiceObjectWrapper {
    public QueuedBuildUpdateOptions() {
        super(new _QueuedBuildUpdateOptions());
    }

    public _QueuedBuildUpdateOptions getWebServiceObject() {
        return (_QueuedBuildUpdateOptions) this.webServiceObject;
    }

    /**
     * Gets or sets the batch ID.
     *
     *
     * @return
     */
    public GUID getBatchID() {
        return new GUID(getWebServiceObject().getBatchId());
    }

    public void setBatchID(final GUID value) {
        getWebServiceObject().setBatchId(value.getGUIDString(GUIDStringFormat.NONE));
    }

    /**
     * Gets or sets the fields which should be updated. Only values included
     * here will be extracted from this object during an update.
     *
     *
     * @return
     */
    public QueuedBuildUpdate getFields() {
        return QueuedBuildUpdate.fromWebServiceObject(getWebServiceObject().getFields());
    }

    public void setFields(final QueuedBuildUpdate value) {
        getWebServiceObject().setFields(value.getWebServiceObject());
    }

    /**
     * Gets or sets the postponed state.
     *
     *
     * @return
     */
    public boolean isPostponed() {
        return getWebServiceObject().isPostponed();
    }

    public void setPostponed(final boolean value) {
        getWebServiceObject().setPostponed(value);
    }

    /**
     * Gets or sets the priority.
     *
     *
     * @return
     */
    public QueuePriority getPriority() {
        return QueuePriority.fromWebServiceObject(getWebServiceObject().getPriority());
    }

    public void setPriority(final QueuePriority value) {
        getWebServiceObject().setPriority(value.getWebServiceObject());
    }

    /**
     * Gets or sets the ID of the target queued build.
     *
     *
     * @return
     */
    public int getQueueID() {
        return getWebServiceObject().getQueueId();
    }

    public void setQueueID(final int value) {
        getWebServiceObject().setQueueId(value);
    }

    /**
     * Gets or sets the retry state.
     *
     *
     * @return
     */
    public boolean isRetry() {
        return getWebServiceObject().isRetry();
    }

    public void setRetry(final boolean value) {
        getWebServiceObject().setRetry(value);
    }

    /**
     * Gets or sets the retry option.
     *
     *
     * @return
     */
    public QueuedBuildRetryOption getRetryOption() {
        return QueuedBuildRetryOption.fromWebServiceObject(getWebServiceObject().getRetryOption());
    }

    public void setRetryOption(final QueuedBuildRetryOption value) {
        getWebServiceObject().setRetryOption(value.getWebServiceObject());
    }
}
