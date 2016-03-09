// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.soapextensions;

import com.microsoft.tfs.core.clients.versioncontrol.path.LocalPath;
import com.microsoft.tfs.core.internal.wrappers.WebServiceObjectWrapper;

import ms.tfs.versioncontrol.clientservices._03._LocalVersion;

/**
 * Contains the local version information of an item in a users workspace.
 *
 * @since TEE-SDK-11.0
 */
public final class LocalVersion extends WebServiceObjectWrapper {
    public LocalVersion(final _LocalVersion version) {
        super(version);
    }

    public LocalVersion(final String localItem, final int version) {
        super(new _LocalVersion(localItem, version));
    }

    /**
     * Gets the web service object this class wraps. The returned object should
     * not be modified.
     *
     * @return the web service object this class wraps.
     */
    public _LocalVersion getWebServiceObject() {
        return (_LocalVersion) webServiceObject;
    }

    /**
     * @return the local path of the item
     */
    public String getItem() {
        return LocalPath.tfsToNative(getWebServiceObject().getI());
    }

    /**
     * @return the version which the user has locally.
     */
    public int getVersion() {
        return getWebServiceObject().getV();
    }
}
