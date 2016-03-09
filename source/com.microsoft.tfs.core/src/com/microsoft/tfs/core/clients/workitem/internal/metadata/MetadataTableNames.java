// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.metadata;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class MetadataTableNames {
    public static final String HIERARCHY = "Hierarchy"; //$NON-NLS-1$
    public static final String FIELDS = "Fields"; //$NON-NLS-1$
    public static final String HIERARCHY_PROPERTIES = "HierarchyProperties"; //$NON-NLS-1$
    public static final String CONSTANTS = "Constants"; //$NON-NLS-1$
    public static final String RULES = "Rules"; //$NON-NLS-1$
    public static final String CONSTANT_SETS = "ConstantSets"; //$NON-NLS-1$
    public static final String FIELD_USAGES = "FieldUsages"; //$NON-NLS-1$
    public static final String WORK_ITEM_TYPES = "WorkItemTypes"; //$NON-NLS-1$
    public static final String WORK_ITEM_TYPE_USAGES = "WorkItemTypeUsages"; //$NON-NLS-1$
    public static final String ACTIONS = "Actions"; //$NON-NLS-1$
    public static final String LINKTYPES = "LinkTypes"; //$NON-NLS-1$
    public static final String WORK_ITEM_TYPE_CATEGORIES = "WorkItemTypeCategories"; //$NON-NLS-1$
    public static final String WORK_ITEM_TYPE_CATEGORY_MEMBERS = "WorkItemTypeCategoryMembers"; //$NON-NLS-1$

    public static final Set<String> allTableNamesVersion2;
    public static final Set<String> allTableNamesVersion3;

    static {
        final Set<String> s2 = new HashSet<String>();
        s2.add(HIERARCHY);
        s2.add(FIELDS);
        s2.add(HIERARCHY_PROPERTIES);
        s2.add(CONSTANTS);
        s2.add(RULES);
        s2.add(CONSTANT_SETS);
        s2.add(FIELD_USAGES);
        s2.add(WORK_ITEM_TYPES);
        s2.add(WORK_ITEM_TYPE_USAGES);
        s2.add(ACTIONS);

        allTableNamesVersion2 = Collections.unmodifiableSet(s2);

        final Set<String> s3 = new HashSet<String>();
        s3.add(HIERARCHY);
        s3.add(FIELDS);
        s3.add(HIERARCHY_PROPERTIES);
        s3.add(CONSTANTS);
        s3.add(RULES);
        s3.add(CONSTANT_SETS);
        s3.add(FIELD_USAGES);
        s3.add(WORK_ITEM_TYPES);
        s3.add(WORK_ITEM_TYPE_USAGES);
        s3.add(ACTIONS);
        s3.add(LINKTYPES);
        s3.add(WORK_ITEM_TYPE_CATEGORIES);
        s3.add(WORK_ITEM_TYPE_CATEGORY_MEMBERS);

        allTableNamesVersion3 = Collections.unmodifiableSet(s3);
    }
}
