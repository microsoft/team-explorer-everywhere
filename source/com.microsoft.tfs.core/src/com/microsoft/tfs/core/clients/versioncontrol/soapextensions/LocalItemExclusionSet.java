// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.soapextensions;

import com.microsoft.tfs.core.internal.wrappers.WebServiceObjectWrapper;
import com.microsoft.tfs.util.GUID;

import ms.tfs.versioncontrol.clientservices._03._LocalItemExclusionSet;

/**
 * Wrapper for the _LocalItemExclusionSet proxy object.
 *
 *
 * @threadsafety unknown
 */
public class LocalItemExclusionSet extends WebServiceObjectWrapper {
    public LocalItemExclusionSet() {
        this(new _LocalItemExclusionSet());
    }

    public LocalItemExclusionSet(final _LocalItemExclusionSet webServiceObject) {
        super(webServiceObject);

        if (getWebServiceObject().getWatermark() == null) {
            setWatermark(GUID.EMPTY);
        }
    }

    private _LocalItemExclusionSet getWebServiceObject() {
        return (_LocalItemExclusionSet) webServiceObject;
    }

    public GUID getWatermark() {
        return new GUID(getWebServiceObject().getWatermark());
    }

    public void setWatermark(final GUID value) {
        getWebServiceObject().setWatermark(value.getGUIDString());
    }

    public String[] getExclusions() {
        return getWebServiceObject().getExclusions();
    }

    public void setExclusions(final String[] value) {
        getWebServiceObject().setExclusions(value);
    }
}
