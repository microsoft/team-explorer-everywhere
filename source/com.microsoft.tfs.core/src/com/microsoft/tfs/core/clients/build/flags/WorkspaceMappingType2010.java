// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.build.flags;

import com.microsoft.tfs.core.internal.wrappers.EnumerationWrapper;

import ms.tfs.build.buildservice._03._WorkspaceMappingType;

public class WorkspaceMappingType2010 extends EnumerationWrapper {
    public static final WorkspaceMappingType2010 MAP = new WorkspaceMappingType2010(_WorkspaceMappingType.Map);
    public static final WorkspaceMappingType2010 CLOAK = new WorkspaceMappingType2010(_WorkspaceMappingType.Cloak);

    private WorkspaceMappingType2010(final _WorkspaceMappingType type) {
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
    public static WorkspaceMappingType2010 fromWebServiceObject(final _WorkspaceMappingType webServiceObject) {
        if (webServiceObject == null) {
            return null;
        }
        return (WorkspaceMappingType2010) EnumerationWrapper.fromWebServiceObject(webServiceObject);
    }

    /**
     * Gets the web service object this class wraps. The returned object should
     * not be modified.
     *
     * @return the web service object this class wraps.
     */
    public _WorkspaceMappingType getWebServiceObject() {
        return (_WorkspaceMappingType) webServiceObject;
    }
}
