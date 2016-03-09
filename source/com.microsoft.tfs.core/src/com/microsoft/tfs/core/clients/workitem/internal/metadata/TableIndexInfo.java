// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.metadata;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class TableIndexInfo {
    private static Map<String, Set<IndexDescription>> map = new HashMap<String, Set<IndexDescription>>();

    static {
        addIndexDescription(MetadataTableNames.CONSTANT_SETS, "ParentID"); //$NON-NLS-1$
        addIndexDescription(MetadataTableNames.CONSTANT_SETS, "ConstID"); //$NON-NLS-1$
    }

    private static void addIndexDescription(final String tableName, final String columnName) {
        Set<IndexDescription> set = map.get(tableName);
        if (set == null) {
            set = new HashSet<IndexDescription>();
            map.put(tableName, set);
        }
        set.add(new IndexDescription(columnName));
    }

    public static IndexDescription[] getIndexesForTable(final String tableName) {
        if (!map.containsKey(tableName)) {
            return new IndexDescription[] {};
        }
        return map.get(tableName).toArray(new IndexDescription[] {});
    }

    public static class IndexDescription {
        private final String columnName;
        private final String indexName;

        IndexDescription(final String columnName) {
            this.columnName = columnName;
            indexName = columnName + "_ix"; //$NON-NLS-1$
        }

        public String getColumnName() {
            return columnName;
        }

        public String getIndexName() {
            return indexName;
        }
    }
}
