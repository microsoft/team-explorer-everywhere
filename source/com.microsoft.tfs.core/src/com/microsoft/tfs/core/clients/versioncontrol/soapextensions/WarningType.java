// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.soapextensions;

import com.microsoft.tfs.core.internal.wrappers.EnumerationWrapper;

import ms.tfs.versioncontrol.clientservices._03._WarningType;

/**
 * Represents a type of {@link Warning}.
 *
 * @since TEE-SDK-10.1
 */
public class WarningType extends EnumerationWrapper {
    public static final WarningType INVALID = new WarningType(_WarningType.Invalid);
    public static final WarningType RESOURCE_PENDING_CHANGE_WARNING =
        new WarningType(_WarningType.ResourcePendingChangeWarning);
    public static final WarningType NAMESPACE_PENDING_CHANGE_WARNING =
        new WarningType(_WarningType.NamespacePendingChangeWarning);
    public static final WarningType STALE_VERSION_WARNING = new WarningType(_WarningType.StaleVersionWarning);

    private WarningType(final _WarningType warningType) {
        super(warningType);
    }

    /**
     * Gets the correct wrapper type for the given web service object.
     *
     * @param webServiceObject
     *        the web service object (must not be <code>null</code>)
     * @return the correct wrapper type for the given web service object
     * @throws RuntimeException
     *         if no wrapper type is known for the given web service object
     */
    public static WarningType fromWebServiceObject(final _WarningType webServiceObject) {
        return (WarningType) EnumerationWrapper.fromWebServiceObject(webServiceObject);
    }

    /**
     * Gets the web service object this class wraps. The returned object should
     * not be modified.
     *
     * @return the web service object this class wraps.
     */
    public _WarningType getWebServiceObject() {
        return (_WarningType) webServiceObject;
    }

    @Override
    public boolean equals(final Object o) {
        return super.equals(o);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }
}
