// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.soapextensions;

import com.microsoft.tfs.core.internal.wrappers.EnumerationWrapper;

import ms.tfs.versioncontrol.clientservices._03._SeverityType;

/**
 * Enumerates the severity levels for a {@link Failure}.
 *
 * @since TEE-SDK-10.1
 */
public class SeverityType extends EnumerationWrapper {
    public static final SeverityType ERROR = new SeverityType(_SeverityType.Error);
    public static final SeverityType WARNING = new SeverityType(_SeverityType.Warning);

    private SeverityType(final _SeverityType severityType) {
        super(severityType);
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
    public static SeverityType fromWebServiceObject(final _SeverityType webServiceObject) {
        return (SeverityType) EnumerationWrapper.fromWebServiceObject(webServiceObject);
    }

    /**
     * Gets the web service object this class wraps. The returned object should
     * not be modified.
     *
     * @return the web service object this class wraps.
     */
    public _SeverityType getWebServiceObject() {
        return (_SeverityType) webServiceObject;
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
