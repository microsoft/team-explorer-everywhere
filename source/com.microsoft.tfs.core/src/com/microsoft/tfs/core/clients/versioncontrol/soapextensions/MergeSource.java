// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.soapextensions;

import com.microsoft.tfs.core.internal.wrappers.WebServiceObjectWrapper;

import ms.tfs.versioncontrol.clientservices._03._MergeSource;

/**
 * Represents the source of a merge.
 *
 * @since TEE-SDK-10.1
 */
public class MergeSource extends WebServiceObjectWrapper {
    public MergeSource(final _MergeSource webServiceObject) {
        super(webServiceObject);
    }

    /**
     * Gets the web service object this class wraps. The returned object should
     * not be modified.
     *
     * @return the web service object this class wraps.
     */
    public _MergeSource getWebServiceObject() {
        return (_MergeSource) webServiceObject;
    }

    public String getServerItem() {
        return getWebServiceObject().getS();
    }

    public int getVersionFrom() {
        return getWebServiceObject().getVf();
    }

    public int getVersionTo() {
        return getWebServiceObject().getVt();
    }

    public boolean isRename() {
        return getWebServiceObject().isR();
    }
}
