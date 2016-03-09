// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol;

import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PropertyValue;

public final class PropertyConstants {
    private PropertyConstants() {
    }

    public static final String CREATED_BY_KEY = "Microsoft.TeamFoundation.VersionControl.Shelveset.CreatedBy"; //$NON-NLS-1$

    /**
     * This property is set to indicate whether a file has the Unix execute bit
     * set on it. The absence of this property indicates the execute bit is not
     * set.
     */
    public static final String EXECUTABLE_KEY = "Microsoft.TeamFoundation.VersionControl.Executable"; //$NON-NLS-1$

    /**
     * This property is set to indicate whether a file is a symbolic link. The
     * value indicates the link destination. The absence of this property
     * indicates this file not a symbolic link.
     */
    public static final String SYMBOLIC_KEY = "Microsoft.TeamFoundation.VersionControl.SymbolicLink"; //$NON-NLS-1$

    /**
     * Convenience instance of a {@link PropertyValue} that indicates the Unix
     * execute bit is set.
     */
    public static final PropertyValue EXECUTABLE_ENABLED_VALUE =
        new PropertyValue(PropertyConstants.EXECUTABLE_KEY, "true"); //$NON-NLS-1$

    /**
     * Convenience instance of a {@link PropertyValue} that indicates the Unix
     * execute bit is <b>not</b> set.
     */
    public static final PropertyValue EXECUTABLE_DISABLED_VALUE =
        new PropertyValue(PropertyConstants.EXECUTABLE_KEY, "false"); //$NON-NLS-1$

    /**
     * Convenience instance of a {@link PropertyValue} that indicates symbolic
     * link.
     */
    public static final PropertyValue IS_SYMLINK = new PropertyValue(PropertyConstants.SYMBOLIC_KEY, "true"); //$NON-NLS-1$

    /**
     * Convenience instance of a {@link PropertyValue} that indicates not
     * symbolic link.
     */
    public static final PropertyValue NOT_SYMLINK = new PropertyValue(PropertyConstants.SYMBOLIC_KEY, "false"); //$NON-NLS-1$

    public static final String QUERY_ALL_PROPERTIES_WILDCARD = "*"; //$NON-NLS-1$
    public static final String[] QUERY_ALL_PROPERTIES_FILTERS = new String[] {
        QUERY_ALL_PROPERTIES_WILDCARD
    };
}
