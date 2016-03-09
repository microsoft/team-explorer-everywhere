// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal;

import com.microsoft.tfs.core.clients.workitem.CoreFields;

/**
 * <p>
 * The field IDs defined in this class can be assumed to be valid for any TFS
 * server installation. They are hardcoded into the TFS installation scripts.
 * However, these IDs are not exposed as part of the public work item object
 * model. Object model clients must use field names and not IDs.
 * </p>
 * <p>
 * See also CoreFieldReferenceNames.
 * </p>
 */
public class WorkItemFieldIDs {
    // from PsDatastoreItemTypeEnum
    public static final int WORK_ITEM = -100;
    public static final int WORK_ITEM_LINK = -101;
    public static final int WORK_ITEM_TREE_ID = -102;

    // From CoreFields (kept here for code back-compat)
    public static final int AREA_PATH = CoreFields.AREA_PATH;
    public static final int ITERATION_PATH = CoreFields.ITERATION_PATH;
    public static final int TITLE = CoreFields.TITLE;
    public static final int HISTORY = CoreFields.HISTORY;
    public static final int EXTERNAL_LINK_COUNT = CoreFields.EXTERNAL_LINK_COUNT;
    public static final int HYPERLINK_COUNT = CoreFields.HYPER_LINK_COUNT;
    public static final int ATTACHED_FILE_COUNT = CoreFields.ATTACHED_FILE_COUNT;
    public static final int RELATED_LINK_COUNT = CoreFields.RELATED_LINK_COUNT;
    public static final int WORK_ITEM_TYPE = CoreFields.WORK_ITEM_TYPE;
    public static final int CREATED_BY = CoreFields.CREATED_BY;
    public static final int AREA_ID = CoreFields.AREA_ID;
    public static final int ITERATION_ID = CoreFields.ITERATION_ID;
    public static final int CHANGED_DATE = CoreFields.CHANGED_DATE;
    public static final int REVISED_DATE = CoreFields.REVISED_DATE;
    public static final int AUTHORIZED_AS = CoreFields.AUTHORIZED_AS;
    public static final int CHANGED_BY = CoreFields.CHANGED_BY;
    public static final int ID = CoreFields.ID;
    public static final int STATE = CoreFields.STATE;
    public static final int TEAM_PROJECT = CoreFields.TEAM_PROJECT;
    public static final int NODE_NAME = CoreFields.NODE_NAME;
    public static final int REVISION = CoreFields.REV;
    public static final int CREATED_DATE = CoreFields.CREATED_DATE;
    public static final int REASON = CoreFields.REASON;
    public static final int AUTHORIZED_DATE = CoreFields.AUTHORIZED_DATE;
    public static final int WATERMARK = CoreFields.WATERMARK;

    // Misc
    public static final int WORK_ITEM_FORM_ID = -14;
    public static final int ATTACHED_FILES = 50;
    public static final int LINKED_FILES = 51;
    public static final int BIS_LINKS = 58;
    public static final int NODE_TYPE = -11;
    public static final int PERSON_ID = -6;
}
