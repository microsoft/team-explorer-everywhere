// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.fields;

public class FieldTypeConstants {
    public static final int MASK_FIELD_TYPE_AND_SUBTYPE = 0xFF0;
    public static final int MASK_FIELD_TYPE_ONLY = 0xF0;
    public static final int MASK_FIELD_SUBTYPE_ONLY = 0xF00;

    public static final int FLAG_READONLY_TYPE = 1;
    public static final int FLAG_IGNORE_TYPE = 2;
    public static final int FLAG_STORE_AS_REFERENCE = 8;

    public static final int TYPE_STRING = 16;
    public static final int TYPE_PERSON = 24;
    public static final int TYPE_INTEGER = 32;
    public static final int TYPE_DATETIME = 48;
    public static final int TYPE_LONGTEXT = 64;
    public static final int TYPE_TREENODE = 160;
    public static final int TYPE_GUID = 208;
    public static final int TYPE_BIT = 224;
    public static final int TYPE_DOUBLE = 240;

    public static final int SUBTYPE_TREEPATH = 256; // used with type String
    // (16)
    public static final int SUBTYPE_TREE_NODE_NAME = 512; // used with type
    // String (16)
    public static final int SUBTYPE_TREE_NODE_TYPE = 768; // used with type
    // String (16)
    public static final int SUBTYPE_PLAINTEXT = 0; // used with type LongText
    // (64)
    public static final int SUBTYPE_HISTORY = 256; // used with type LongText
    // (64)
    public static final int SUBTYPE_HTML = 512; // used with type LongText (64)
    public static final int SUBTYPE_TREE_ID = 256; // used with type Integer
    // (32)
}
