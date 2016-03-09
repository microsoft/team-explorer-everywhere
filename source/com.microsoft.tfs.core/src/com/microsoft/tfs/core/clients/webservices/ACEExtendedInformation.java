// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.webservices;

import com.microsoft.tfs.core.clients.security.AccessControlEntry;
import com.microsoft.tfs.core.internal.wrappers.WebServiceObjectWrapper;

import ms.ws._AceExtendedInformation;

/**
 * Holds the inherited and effective permission information for a given
 * {@link AccessControlEntry}.
 *
 * @since TEE-SDK-10.1
 */
public class ACEExtendedInformation extends WebServiceObjectWrapper {
    public ACEExtendedInformation(final _AceExtendedInformation extendedInfo) {
        super(extendedInfo);
    }

    public ACEExtendedInformation(
        final int inheritedAllow,
        final int inheritedDeny,
        final int effectiveAllow,
        final int effectiveDeny) {
        this(new _AceExtendedInformation(inheritedAllow, inheritedDeny, effectiveAllow, effectiveDeny));
    }

    /**
     * Gets the web service object this class wraps. The returned object should
     * not be modified.
     *
     * @return the web service object this class wraps.
     */
    public _AceExtendedInformation getWebServiceObject() {
        return (_AceExtendedInformation) webServiceObject;
    }

    public int getEffectiveAllow() {
        return getWebServiceObject().getEffectiveAllow();
    }

    public int getEffectiveDeny() {
        return getWebServiceObject().getEffectiveDeny();
    }

    public int getInheritedAllow() {
        return getWebServiceObject().getInheritedAllow();
    }

    public int getInheritedDeny() {
        return getWebServiceObject().getInheritedDeny();
    }
}
