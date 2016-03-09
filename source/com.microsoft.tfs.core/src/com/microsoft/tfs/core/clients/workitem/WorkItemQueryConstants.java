// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem;

/**
 * Defines constants used with work item queries.
 *
 * @since TEE-SDK-10.1
 */
public class WorkItemQueryConstants {
    public static final String PROJECT_MACRO_NAME = "project"; //$NON-NLS-1$
    public static final String TEAM_CONTEXT_NAME = "team"; //$NON-NLS-1$
    public static final char VALUE_SINGLE_QUOTE = '\'';
    public static final String VALUE_SINGLE_QUOTE_ESCAPED = "\'\'"; //$NON-NLS-1$
    public static final String VALUE_LIST_OPEN = "("; //$NON-NLS-1$
    public static final String VALUE_LIST_CLOSE = ")"; //$NON-NLS-1$
    public static final String FIELD_NAME_OPEN_BRACKET = "["; //$NON-NLS-1$
    public static final String FIELD_NAME_CLOSE_BRACKET = "]"; //$NON-NLS-1$
    public static final String FIELD_SEPARATOR = ","; //$NON-NLS-1$
    public static final String VALUE_SEPARATOR = ","; //$NON-NLS-1$
    public static final String KW_SELECT = "SELECT"; //$NON-NLS-1$
    public static final String KW_WHERE = "WHERE"; //$NON-NLS-1$
    public static final String KW_AND = "AND"; //$NON-NLS-1$
    public static final String OP_IN = "IN"; //$NON-NLS-1$
    public static final String OP_GREATER_THAN = ">"; //$NON-NLS-1$

    public static final int MAX_PAGE_SIZE = 200;
    public static final int MIN_PAGE_SIZE = 50;
    public static final int DEFAULT_PAGE_SIZE = 50;
}
