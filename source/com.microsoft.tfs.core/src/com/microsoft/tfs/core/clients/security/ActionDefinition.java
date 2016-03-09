// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.security;

import com.microsoft.tfs.core.internal.wrappers.WebServiceObjectWrapper;

import ms.ws._ActionDefinition;

public class ActionDefinition extends WebServiceObjectWrapper {
    public ActionDefinition(final _ActionDefinition webServiceObject) {
        super(webServiceObject);
    }

    /**
     * Creates and ActionDefinition based on the inputs.
     *
     *
     * @param bit
     *        The bit mask integer for this action. Must be a power of 2.
     * @param name
     *        The non-localized name for this action.
     * @param displayName
     *        The localized display name for this action
     */
    public ActionDefinition(final int bit, final String name, final String displayName) {
        super(new _ActionDefinition(bit, name, displayName));
    }

    /**
     * Gets the web service object this class wraps. The returned object should
     * not be modified.
     *
     * @return the web service object this class wraps.
     */
    public _ActionDefinition getWebServiceObject() {
        return (_ActionDefinition) webServiceObject;
    }

    /**
     * @return the bit mask integer for this action. Must be a power of 2.
     */
    public int getBit() {
        return getWebServiceObject().getBit();
    }

    /**
     * @return the non-localized name for this action.
     */
    public String getName() {
        return getWebServiceObject().getName();
    }

    /**
     * @return the localized display name for this action.
     */
    public String getDisplayName() {
        return getWebServiceObject().getDisplayName();
    }
}