// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.clc.xml;

/**
 * Defines the canonical names for several XML element and attribute names
 * written as XML by the CLC.
 * <p>
 * It might seem like overkill defining some of these names here, but a name
 * needs to be passed at least twice to SAX when writing an element (once for
 * start, once for end).
 */
public class CommonXMLNames {
    public static final String ID = "id"; //$NON-NLS-1$
    public static final String DATE = "date"; //$NON-NLS-1$
    public static final String NAME = "name"; //$NON-NLS-1$
    public static final String CHANGESET = "changeset"; //$NON-NLS-1$
    public static final String SHELVESET = "shelveset"; //$NON-NLS-1$
    public static final String VERSION = "version"; //$NON-NLS-1$
    public static final String LABEL = "label"; //$NON-NLS-1$
    public static final String COMMENT = "comment"; //$NON-NLS-1$
    public static final String SERVER_ITEM = "server-item"; //$NON-NLS-1$
    public static final String SOURCE_ITEM = "source-item"; //$NON-NLS-1$
    public static final String SOURCE_SERVER_ITEM = "source-server-item"; //$NON-NLS-1$
    public static final String SOURCE_LOCAL_ITEM = "source-local-item"; //$NON-NLS-1$
    public static final String LOCAL_ITEM = "local-item"; //$NON-NLS-1$
    public static final String COMPUTER = "computer"; //$NON-NLS-1$
    public static final String FILE_TYPE = "file-type"; //$NON-NLS-1$
    public static final String USER = "user"; //$NON-NLS-1$
    public static final String OWNER = "owner"; //$NON-NLS-1$
    public static final String OWNER_DISPLAY_NAME = "owner-display-name"; //$NON-NLS-1$
    public static final String COMMITTER = "committer"; //$NON-NLS-1$
    public static final String ITEM = "item"; //$NON-NLS-1$
    public static final String CHANGE_TYPE = "change-type"; //$NON-NLS-1$
    public static final String CHECK_IN_NOTE = "check-in-note"; //$NON-NLS-1$
    public static final String POLICY_OVERRIDE = "policy-override"; //$NON-NLS-1$
    public static final String REASON = "reason"; //$NON-NLS-1$
    public static final String MESSAGE = "message"; //$NON-NLS-1$
    public static final String SERVER = "server"; //$NON-NLS-1$
    public static final String LOCK = "lock"; //$NON-NLS-1$
    public static final String WORKSPACE = "workspace"; //$NON-NLS-1$
    public static final String PENDING_CHANGE = "pending-change"; //$NON-NLS-1$
    public static final String DELETION_ID = "deletion-id"; //$NON-NLS-1$
    public static final String WORKING_FOLDER = "working-folder"; //$NON-NLS-1$
}
