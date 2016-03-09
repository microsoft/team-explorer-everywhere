// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.commonstructure;

import com.microsoft.tfs.core.internal.wrappers.WebServiceObjectWrapper;

import ms.tfs.services.classification._03._ProjectProperty;

/**
 * <p>
 * A single property configured on a team project.
 * </p>
 *
 * @since TEE-SDK-10.1
 * @threadsafety thread-compatible
 */
public class ProjectProperty extends WebServiceObjectWrapper {
    public ProjectProperty(final String name, final String value) {
        super(new _ProjectProperty(name, value));
    }

    public ProjectProperty(final _ProjectProperty property) {
        super(property);
    }

    /**
     * Gets the web service object this class wraps. The returned object should
     * not be modified.
     *
     * @return the web service object this class wraps.
     */
    public _ProjectProperty getWebServiceObject() {
        return (_ProjectProperty) webServiceObject;
    }

    public String getName() {
        return getWebServiceObject().getName();
    }

    public String getValue() {
        return getWebServiceObject().getValue();
    }
}
