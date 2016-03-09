// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.soapextensions;

import com.microsoft.tfs.core.clients.versioncontrol.path.LocalPath;

import ms.tfs.versioncontrol.clientservices._03._WorkspaceItem;

/**
 * Contains information about a version control item in a workspace.
 *
 * @since TEE-SDK-11.1
 */
public class WorkspaceItem extends Item {
    public WorkspaceItem() {
        this(new _WorkspaceItem());
    }

    public WorkspaceItem(final ItemType itemType, final String serverItem, final int encoding) {
        this(
            new _WorkspaceItem(
                0,
                null,
                0,
                encoding,
                itemType.getWebServiceObject(),
                0,
                serverItem,
                null,
                null,
                null,
                0,
                null,
                false,
                null,
                0,
                0,
                null,
                null,
                null));
    }

    public WorkspaceItem(final _WorkspaceItem item) {
        super(item);
    }

    /**
     * Gets the web service object this class wraps. The returned object should
     * not be modified.
     *
     * @return the web service object this class wraps.
     */
    @Override
    public _WorkspaceItem getWebServiceObject() {
        return (_WorkspaceItem) webServiceObject;
    }

    public ChangeType getChangeType() {
        return ChangeType.fromIntFlags(0, getWebServiceObject().getCt());
    }

    public ChangeType getRecursiveChangeType() {
        return ChangeType.fromIntFlags(0, getWebServiceObject().getRct());
    }

    public String getCommittedServerItem() {
        return getWebServiceObject().getCsi();
    }

    public void setCommittedServerItem(final String value) {
        getWebServiceObject().setCsi(value);
    }

    public String getLocalItem() {
        return LocalPath.tfsToNative(getWebServiceObject().getLi());
    }

    public void setLocalItem(final String value) {
        getWebServiceObject().setLi(LocalPath.nativeToTFS(value));
    }
}
