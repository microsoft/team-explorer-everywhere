// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.build.soapextensions;

import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.WorkingFolderType;
import com.microsoft.tfs.core.internal.wrappers.EnumerationWrapper;

import ms.tfs.build.buildservice._04._WorkspaceMappingType;

/**
 * Describes the type of a workspace mapping.
 *
 * @since TEE-SDK-10.1
 */
public class WorkspaceMappingType extends EnumerationWrapper {
    public static final WorkspaceMappingType MAP = new WorkspaceMappingType(_WorkspaceMappingType.Map);
    public static final WorkspaceMappingType CLOAK = new WorkspaceMappingType(_WorkspaceMappingType.Cloak);

    private WorkspaceMappingType(final _WorkspaceMappingType mappingType) {
        // Visual Studio's implementation defaults to MAP when no mapping type
        // passed.
        super(getDefaultForConstruction(mappingType));
    }

    private static _WorkspaceMappingType getDefaultForConstruction(final _WorkspaceMappingType mappingType) {
        return (mappingType != null) ? mappingType : _WorkspaceMappingType.Map;
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
    public static WorkspaceMappingType fromWebServiceObject(final _WorkspaceMappingType webServiceObject) {
        if (webServiceObject == null) {
            return null;
        }
        return (WorkspaceMappingType) EnumerationWrapper.fromWebServiceObject(webServiceObject);
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

    /**
     * @return the working folder type that corresponds to this mapping type.
     */
    public WorkingFolderType getWorkingFolderType() {
        if (WorkspaceMappingType.CLOAK == this) {
            return WorkingFolderType.CLOAK;
        }

        return WorkingFolderType.MAP;
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
