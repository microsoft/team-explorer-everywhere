// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.metadata;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

public class TablePrimaryKeys {
    private static final Map<String, String> KEYS = new HashMap<String, String>();

    static {
        KEYS.put(MetadataTableNames.HIERARCHY_PROPERTIES, "PropID"); //$NON-NLS-1$
        KEYS.put(MetadataTableNames.CONSTANTS, "ConstID"); //$NON-NLS-1$
        KEYS.put(MetadataTableNames.RULES, "RuleID"); //$NON-NLS-1$
        KEYS.put(MetadataTableNames.CONSTANT_SETS, "RuleSetID"); //$NON-NLS-1$
        KEYS.put(MetadataTableNames.FIELD_USAGES, "FldUsageID"); //$NON-NLS-1$
        KEYS.put(MetadataTableNames.WORK_ITEM_TYPES, "WorkItemTypeID"); //$NON-NLS-1$
        KEYS.put(MetadataTableNames.WORK_ITEM_TYPE_USAGES, "WorkItemTypeUsageID"); //$NON-NLS-1$
        KEYS.put(MetadataTableNames.ACTIONS, "ActionID"); //$NON-NLS-1$
        KEYS.put(MetadataTableNames.LINKTYPES, "ReferenceName"); //$NON-NLS-1$
        KEYS.put(MetadataTableNames.WORK_ITEM_TYPE_CATEGORIES, "WorkItemTypeCategoryID"); //$NON-NLS-1$
        KEYS.put(MetadataTableNames.WORK_ITEM_TYPE_CATEGORY_MEMBERS, "WorkItemTypeCategoryMemberID"); //$NON-NLS-1$

        /*
         * TODO why is this here? from what I can see, it isn't needed
         */
        KEYS.put("StoredQueries", "ID"); //$NON-NLS-1$ //$NON-NLS-2$

        KEYS.put(MetadataTableNames.FIELDS, "FldID"); //$NON-NLS-1$
        KEYS.put(MetadataTableNames.HIERARCHY, "AreaID"); //$NON-NLS-1$
    }

    public static String getPrimaryKeyColumnForTableName(final String tableName) {
        if (KEYS.containsKey(tableName)) {
            return KEYS.get(tableName);
        }
        throw new RuntimeException(
            MessageFormat.format(
                "unable to obtain the primary key for table [{0}] because the table is unknown", //$NON-NLS-1$
                tableName));
    }
}
