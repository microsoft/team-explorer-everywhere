// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.commonstructure;

import com.microsoft.tfs.core.internal.wrappers.WebServiceObjectWrapper;

import ms.tfs.services.classification._03._Property;

/**
 * <p>
 * A property defined on a {@link NodeInfo}.
 * </p>
 *
 * @since TEE-SDK-10.1
 * @threadsafety thread-compatible
 */
public class Property extends WebServiceObjectWrapper {
    public Property(final String name, final String value) {
        super(new _Property(name, value));
    }

    public Property(final _Property property) {
        super(property);
    }

    /**
     * Gets the web service object this class wraps. The returned object should
     * not be modified.
     *
     * @return the web service object this class wraps.
     */
    public _Property getWebServiceObject() {
        return (_Property) webServiceObject;
    }

    public String getName() {
        return getWebServiceObject().getName();
    }

    public String getValue() {
        return getWebServiceObject().getValue();
    }
}
