// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.sharepoint;

/**
 * Known list types for Windows Sharepoint Services.
 *
 * @since TEE-SDK-10.1
 * @threadsafety immutable
 */
public interface WSSListType {
    public static final String ANNOUNCEMENTS = "104"; //$NON-NLS-1$
    public static final String CONTACTS = "105"; //$NON-NLS-1$
    public static final String CUSTOM_LIST = "100"; //$NON-NLS-1$
    public static final String CUSTOM_LIST_IN_DATA_SOURCE_VIEW = "120"; //$NON-NLS-1$
    public static final String DATASOURCES = "110"; //$NON-NLS-1$
    public static final String DISCUSSION_BOARD = "108"; //$NON-NLS-1$

    /**
     * Document library lists - used to populate Documents folder in Team
     * Explorer.
     */
    public static final String DOCUMENT_LIBRARY = "101"; //$NON-NLS-1$

    public static final String EVENTS = "106"; //$NON-NLS-1$
    public static final String FORM_LIBRARY = "115"; //$NON-NLS-1$
    public static final String ISSUES = "1100"; //$NON-NLS-1$
    public static final String LINKS = "103"; //$NON-NLS-1$
    public static final String SURVEY = "102"; //$NON-NLS-1$
    public static final String TASKS = "107"; //$NON-NLS-1$
    public static final String PICTURE_LIBRARY = "109"; //$NON-NLS-1$
}
