// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.soapextensions;

import com.microsoft.tfs.core.internal.wrappers.WebServiceObjectWrapper;

import ms.tfs.versioncontrol.clientservices._03._ItemMerge;

/**
 * Contains information about an item that was merged in the past.
 *
 * @since TEE-SDK-10.1
 */
public class ItemMerge extends WebServiceObjectWrapper {
    public ItemMerge(final _ItemMerge itemMerge) {
        super(itemMerge);
    }

    /**
     * Gets the web service object this class wraps. The returned object should
     * not be modified.
     *
     * @return the web service object this class wraps.
     */
    public _ItemMerge getWebServiceObject() {
        return (_ItemMerge) webServiceObject;
    }

    public int getSourceItemID() {
        return getWebServiceObject().getSid();
    }

    public String getSourceServerItem() {
        return getWebServiceObject().getSsi();
    }

    public int getSourceVersionFrom() {
        return getWebServiceObject().getSvf();
    }

    public int getTargetItemID() {
        return getWebServiceObject().getTid();
    }

    public String getTargetServerItem() {
        return getWebServiceObject().getTsi();
    }

    public int getTargetVersionFrom() {
        return getWebServiceObject().getTvf();
    }
}
