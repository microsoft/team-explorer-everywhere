// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.metadata.mapper;

public class MetadataColumnTypes {
    public static final String GUID = "System.Guid"; //$NON-NLS-1$
    public static final String INTEGER = "System.Int32"; //$NON-NLS-1$
    public static final String STRING = "System.String"; //$NON-NLS-1$
    public static final String BOOLEAN = "System.Boolean"; //$NON-NLS-1$
    public static final String DATE = "System.DateTime"; //$NON-NLS-1$
    public static final String LONG = "System.UInt64"; //$NON-NLS-1$

    // TODO, need to add System.Double, which can come back from Work item
    // queries
    // with work item fields of type Double
}
