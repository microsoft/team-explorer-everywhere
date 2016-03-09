// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.fields;

/**
 * Corresponds to "PsFieldDefinitionTypeEnum" in MS code.
 */
public class PSFieldDefinitionTypeEnum {
    public static final int SINGLE_VALUED_BOOLEAN = FieldTypeConstants.TYPE_BIT; // 224
    public static final int SINGLE_VALUED_DATE_TIME = FieldTypeConstants.TYPE_DATETIME; // 48
    public static final int SINGLE_VALUED_DOUBLE = FieldTypeConstants.TYPE_DOUBLE; // 240
    public static final int SINGLE_VALUED_INTEGER = FieldTypeConstants.TYPE_INTEGER; // 32
    public static final int SINGLE_VALUED_INTEGER_TREEID =
        FieldTypeConstants.TYPE_INTEGER | FieldTypeConstants.SUBTYPE_TREE_ID; // 288
    public static final int SINGLE_VALUED_KEYWORD = FieldTypeConstants.TYPE_STRING; // 16
    public static final int SINGLE_VALUED_KEYWORD_TREE_NODE_NAME =
        FieldTypeConstants.TYPE_STRING | FieldTypeConstants.SUBTYPE_TREE_NODE_NAME; // 528
    public static final int SINGLE_VALUED_KEYWORD_TREE_NODE_TYPE =
        FieldTypeConstants.TYPE_STRING | FieldTypeConstants.SUBTYPE_TREE_NODE_TYPE; // 784
    public static final int SINGLE_VALUED_KEYWORD_TREEPATH =
        FieldTypeConstants.TYPE_STRING | FieldTypeConstants.SUBTYPE_TREEPATH; // 272
    public static final int SINGLE_VALUED_KEYWORD_PERSON = FieldTypeConstants.TYPE_PERSON; // 24
    public static final int SINGLE_VALUED_LARGE_TEXT_HISTORY =
        FieldTypeConstants.TYPE_LONGTEXT | FieldTypeConstants.SUBTYPE_HISTORY; // 320
    public static final int SINGLE_VALUED_LARGE_TEXT_HTML =
        FieldTypeConstants.TYPE_LONGTEXT | FieldTypeConstants.SUBTYPE_HTML; // 576
    public static final int SINGLE_VALUED_LARGE_TEXT_PLAINTEXT = FieldTypeConstants.TYPE_LONGTEXT; // 64
    public static final int TREE_NODE = FieldTypeConstants.TYPE_TREENODE; // 160
    public static final int SINGLE_VALUED_GUID = FieldTypeConstants.TYPE_GUID; // 208
}
