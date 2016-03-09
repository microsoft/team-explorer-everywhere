// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.registration;

import com.microsoft.tfs.core.internal.wrappers.WebServiceObjectWrapper;

import ms.tfs.services.registration._03._RegistrationExtendedAttribute2;

/**
 * A name/value pair that represents one extended attribute of a
 * {@link RegistrationEntry}.
 *
 * @since TEE-SDK-10.1
 * @threadsafety thread-compatible
 */
public class RegistrationExtendedAttribute extends WebServiceObjectWrapper {
    public RegistrationExtendedAttribute(final String name, final String value) {
        super(new _RegistrationExtendedAttribute2(name, value));
    }

    public RegistrationExtendedAttribute(final _RegistrationExtendedAttribute2 eventType) {
        super(eventType);
    }

    /**
     * Gets the web service object this class wraps. The returned object should
     * not be modified.
     *
     * @return the web service object this class wraps.
     */
    public _RegistrationExtendedAttribute2 getWebServiceObject() {
        return (_RegistrationExtendedAttribute2) webServiceObject;
    }

    public String getName() {
        return getWebServiceObject().getName();
    }

    public String getValue() {
        return getWebServiceObject().getValue();
    }
}
