// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.microsoft.tfs.core.clients.workitem.internal.wiqlparse.DateTime;
import com.microsoft.tfs.core.clients.workitem.project.Project;
import com.microsoft.tfs.util.Check;

/**
 * Utilities for working with work item queries.
 *
 * @since TEE-SDK-10.1
 */
public class WorkItemQueryUtils {
    public static Map<String, Object> makeContext(final Project project, final String teamName) {
        return makeContext((project == null ? null : project.getName()), teamName);
    }

    public static Map<String, Object> makeContext(final String projectName, final String teamName) {
        final Map<String, Object> queryContext = new HashMap<String, Object>();

        if (projectName != null) {
            queryContext.put(WorkItemQueryConstants.PROJECT_MACRO_NAME, projectName);
        }

        if (teamName != null) {
            queryContext.put(WorkItemQueryConstants.TEAM_CONTEXT_NAME, teamName);
        }

        return queryContext;
    }

    public static String formatDate(final Date date) {
        return DateTime.formatRoundTripUniversal(date);
    }

    public static String escapeValue(final String value) {
        Check.notNull(value, "value"); //$NON-NLS-1$

        return value.replaceAll(
            "\\" + WorkItemQueryConstants.VALUE_SINGLE_QUOTE, //$NON-NLS-1$
            WorkItemQueryConstants.VALUE_SINGLE_QUOTE_ESCAPED);
    }

    public static String quoteValue(final String value) {
        Check.notNull(value, "value"); //$NON-NLS-1$

        return WorkItemQueryConstants.VALUE_SINGLE_QUOTE
            + escapeValue(value)
            + WorkItemQueryConstants.VALUE_SINGLE_QUOTE;
    }

    public static String formatValueList(final String[] values) {
        Check.notNull(values, "values"); //$NON-NLS-1$

        final StringBuffer buffer = new StringBuffer();

        buffer.append(WorkItemQueryConstants.VALUE_LIST_OPEN);

        for (int i = 0; i < values.length; i++) {
            buffer.append(quoteValue(values[i]));
            if (i < values.length - 1) {
                buffer.append(WorkItemQueryConstants.VALUE_SEPARATOR);
            }
        }

        buffer.append(WorkItemQueryConstants.VALUE_LIST_CLOSE);

        return buffer.toString();
    }

    public static String bracketFieldName(final String fieldName) {
        Check.notNull(fieldName, "fieldName"); //$NON-NLS-1$

        return WorkItemQueryConstants.FIELD_NAME_OPEN_BRACKET
            + fieldName
            + WorkItemQueryConstants.FIELD_NAME_CLOSE_BRACKET;
    }

    public static String formatFieldList(final String[] fieldNames) {
        Check.notNull(fieldNames, "fieldNames"); //$NON-NLS-1$

        final StringBuffer buffer = new StringBuffer();

        for (int i = 0; i < fieldNames.length; i++) {
            buffer.append(bracketFieldName(fieldNames[i]));
            if (i < fieldNames.length - 1) {
                buffer.append(WorkItemQueryConstants.FIELD_SEPARATOR);
                buffer.append(" "); //$NON-NLS-1$
            }
        }

        return buffer.toString();
    }
}
