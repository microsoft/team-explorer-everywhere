// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.wiqlparse;

/**
 * Constants used for WIQL
 */
public class WIQLConstants {
    // Tables
    public static final String WORK_ITEM_TABLE = "WorkItems"; //$NON-NLS-1$
    public static final String WORK_ITEM_LINK_TABLE = "WorkItemLinks"; //$NON-NLS-1$

    // Prefixes
    public static final String SOURCE_PREFIX = "Source"; //$NON-NLS-1$
    public static final String TARGET_PREFIX = "Target"; //$NON-NLS-1$

    // Query Modes
    public static final String DOES_NOT_CONTAIN = "DoesNotContain"; //$NON-NLS-1$
    public static final String MAY_CONTAIN = "MayContain"; //$NON-NLS-1$
    public static final String MUST_CONTAIN = "MustContain"; //$NON-NLS-1$
    public static final String RECURSIVE = "Recursive"; //$NON-NLS-1$

    // Query Xml Constants
    public static final String LINKS_QUERY = "LinksQuery"; //$NON-NLS-1$
    public static final String QUERY = "Query"; //$NON-NLS-1$
    public static final String WIQL = "Wiql"; //$NON-NLS-1$
    public static final String CONTEXT = "Context"; //$NON-NLS-1$
    public static final String KEY = "Key"; //$NON-NLS-1$
    public static final String VALUE = "Value"; //$NON-NLS-1$
    public static final String DAY_PRECISION = "DayPrecision"; //$NON-NLS-1$
    public static final String VALUE_TYPE = "ValueType"; //$NON-NLS-1$
    public static final String PRODUCT = "Product"; //$NON-NLS-1$

}
