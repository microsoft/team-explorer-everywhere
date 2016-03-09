// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.soapextensions;

import com.microsoft.tfs.core.internal.wrappers.EnumerationWrapper;

import ms.tfs.versioncontrol.clientservices._03._WorkingFolderType;

/**
 * Enumerates the types of working folders.
 *
 * @threadsafety immutable
 * @since TEE-SDK-10.1
 */
public class WorkingFolderType extends EnumerationWrapper {
    public static final WorkingFolderType MAP = new WorkingFolderType(_WorkingFolderType.Map);
    public static final WorkingFolderType CLOAK = new WorkingFolderType(_WorkingFolderType.Cloak);

    private WorkingFolderType(final _WorkingFolderType type) {
        super(type);
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
    public static WorkingFolderType fromWebServiceObject(final _WorkingFolderType webServiceObject) {
        return (WorkingFolderType) EnumerationWrapper.fromWebServiceObject(webServiceObject);
    }

    /**
     * Gets the web service object this class wraps. The returned object should
     * not be modified.
     *
     * @return the web service object this class wraps.
     */
    public _WorkingFolderType getWebServiceObject() {
        return (_WorkingFolderType) webServiceObject;
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
