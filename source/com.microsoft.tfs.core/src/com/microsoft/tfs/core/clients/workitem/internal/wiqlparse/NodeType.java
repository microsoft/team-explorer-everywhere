// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.wiqlparse;

/**
 * Defines possible node types in the syntax tree. Every class inherited from
 * Node must have its own type.
 */
public class NodeType {
    public static final NodeType NUMBER = new NodeType("Number", 0); //$NON-NLS-1$
    public static final NodeType NAME = new NodeType("Name", 1); //$NON-NLS-1$
    public static final NodeType STRING = new NodeType("String", 2); //$NON-NLS-1$
    public static final NodeType OPERATION = new NodeType("Operation", 3); //$NON-NLS-1$
    public static final NodeType SELECT = new NodeType("Select", 4); //$NON-NLS-1$
    public static final NodeType FIELD_NAME = new NodeType("FieldName", 5); //$NON-NLS-1$
    public static final NodeType FIELD_LIST = new NodeType("FieldList", 6); //$NON-NLS-1$
    public static final NodeType ORDER_FIELD_LIST = new NodeType("OrderFieldList", 7); //$NON-NLS-1$
    public static final NodeType GROUP_FIELD_LIST = new NodeType("GroupFieldList", 8); //$NON-NLS-1$
    public static final NodeType TABLE_NAME = new NodeType("TableName", 9); //$NON-NLS-1$
    public static final NodeType FIELD_CONDITION = new NodeType("FieldCondition", 10); //$NON-NLS-1$
    public static final NodeType VALUE_LIST = new NodeType("ValueList", 11); //$NON-NLS-1$
    public static final NodeType BOOL_CONST = new NodeType("BoolConst", 12); //$NON-NLS-1$
    public static final NodeType BOOL_VALUE = new NodeType("BoolValue", 13); //$NON-NLS-1$
    public static final NodeType NOT = new NodeType("Not", 14); //$NON-NLS-1$
    public static final NodeType EVER = new NodeType("Ever", 15); //$NON-NLS-1$
    public static final NodeType AND = new NodeType("And", 16); //$NON-NLS-1$
    public static final NodeType OR = new NodeType("Or", 17); //$NON-NLS-1$
    public static final NodeType VARIABLE = new NodeType("Variable", 18); //$NON-NLS-1$
    public static final NodeType ARITHMETIC = new NodeType("Arithmetic", 19); //$NON-NLS-1$
    public static final NodeType MODE = new NodeType("Mode", 20); //$NON-NLS-1$

    private final String type;
    private final int value;

    private NodeType(final String type, final int value) {
        this.type = type;
        this.value = value;
    }

    @Override
    public String toString() {
        return type;
    }

    public int getValue() {
        return value;
    }

}
