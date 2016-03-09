// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.registration;

import com.microsoft.tfs.core.internal.wrappers.WebServiceObjectWrapper;

import ms.tfs.services.registration._03._RegistrationEventType;

/**
 * Describes a configured event.
 *
 * @threadsafety thread-compatible
 * @since TEE-SDK-10.1
 */
public class EventType extends WebServiceObjectWrapper {
    public EventType(final String name, final String schema) {
        super(new _RegistrationEventType(name, schema));
    }

    public EventType(final _RegistrationEventType eventType) {
        super(eventType);
    }

    /**
     * Gets the web service object this class wraps. The returned object should
     * not be modified.
     *
     * @return the web service object this class wraps.
     */
    public _RegistrationEventType getWebServiceObject() {
        return (_RegistrationEventType) webServiceObject;
    }

    public String getName() {
        return getWebServiceObject().getName();
    }

    public String getSchema() {
        return getWebServiceObject().getSchema();
    }
}
