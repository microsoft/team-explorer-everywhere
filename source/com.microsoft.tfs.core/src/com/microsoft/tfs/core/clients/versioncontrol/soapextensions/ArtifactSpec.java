// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.soapextensions;

import com.microsoft.tfs.core.internal.wrappers.WebServiceObjectWrapper;
import com.microsoft.tfs.util.GUID;

import ms.tfs.versioncontrol.clientservices._03._ArtifactSpec;

/**
 * Represents a Team Foundation property user-defined name (moniker)
 * specification.
 *
 * @since TEE-SDK-10.1
 */
public class ArtifactSpec extends WebServiceObjectWrapper {
    public ArtifactSpec(final _ArtifactSpec webServiceObject) {
        super(webServiceObject);
    }

    /**
     * Gets the web service object this class wraps. The returned object should
     * not be modified.
     *
     * @return the web service object this class wraps.
     */
    public _ArtifactSpec getWebServiceObject() {
        return (_ArtifactSpec) webServiceObject;
    }

    public String getItem() {
        return getWebServiceObject().getItem();
    }

    public int getVersion() {
        return getWebServiceObject().getVer();
    }

    public GUID getKind() {
        return new GUID(getWebServiceObject().getK());
    }

    public byte[] getID() {
        return getWebServiceObject().getId();
    }
}
