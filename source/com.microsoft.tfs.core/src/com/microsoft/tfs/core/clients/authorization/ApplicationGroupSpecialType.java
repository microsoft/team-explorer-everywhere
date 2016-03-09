// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.authorization;

import com.microsoft.tfs.core.internal.wrappers.EnumerationWrapper;

import ms.tfs.services.authorization._03._ApplicationGroupSpecialType;

/**
 * The special application group types are automatically created and have
 * implicit rights associated with them.
 *
 * @threadsafety thread-compatible
 * @since TEE-SDK-10.1
 */
public class ApplicationGroupSpecialType extends EnumerationWrapper {
    public static final ApplicationGroupSpecialType GENERIC =
        new ApplicationGroupSpecialType(_ApplicationGroupSpecialType.Generic);

    public static final ApplicationGroupSpecialType ADMINISTRATIVE_APPLICATION_GROUP =
        new ApplicationGroupSpecialType(_ApplicationGroupSpecialType.AdministrativeApplicationGroup);

    public static final ApplicationGroupSpecialType SERVICE_APPLICATION_GROUP =
        new ApplicationGroupSpecialType(_ApplicationGroupSpecialType.ServiceApplicationGroup);

    public static final ApplicationGroupSpecialType EVERYONE_APPLICATION_GROUP =
        new ApplicationGroupSpecialType(_ApplicationGroupSpecialType.EveryoneApplicationGroup);

    public static final ApplicationGroupSpecialType LICENSEES_APPLICATION_GROUP =
        new ApplicationGroupSpecialType(_ApplicationGroupSpecialType.LicenseesApplicationGroup);

    public ApplicationGroupSpecialType(final _ApplicationGroupSpecialType extendedInfo) {
        super(extendedInfo);
    }

    /**
     * Gets the web service object this class wraps. The returned object should
     * not be modified.
     *
     * @return the web service object this class wraps.
     */
    public _ApplicationGroupSpecialType getWebServiceObject() {
        return (_ApplicationGroupSpecialType) webServiceObject;
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
    public static ApplicationGroupSpecialType fromWebServiceObject(
        final _ApplicationGroupSpecialType webServiceObject) {
        return (ApplicationGroupSpecialType) EnumerationWrapper.fromWebServiceObject(webServiceObject);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return getWebServiceObject().getName();
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
