// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.webservices;

import com.microsoft.tfs.core.internal.wrappers.WebServiceObjectWrapper;

import ms.ws._KeyValueOfStringString;

/**
 * Wrapper class for the {@link _KeyValueOfStringString} proxy object.
 *
 * @since TEE-SDK-10.1
 */
public class KeyValueOfStringString extends WebServiceObjectWrapper {
    /**
     * Wrapper constructor
     */
    public KeyValueOfStringString(final _KeyValueOfStringString webServiceObject) {
        super(webServiceObject);
    }

    /**
     * Returns the wrapped proxy object.
     */
    public _KeyValueOfStringString getWebServiceObject() {
        return (_KeyValueOfStringString) webServiceObject;
    }

    public String getKey() {
        return getWebServiceObject().getKey();
    }

    public String getValue() {
        return getWebServiceObject().getValue();
    }
}
