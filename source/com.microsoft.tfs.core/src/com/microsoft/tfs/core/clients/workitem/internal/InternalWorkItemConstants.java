// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal;

/**
 * This class is the internal equivalent of WorkItemConstants.
 *
 * Constants defined here are not to be exposed as public API.
 */
public class InternalWorkItemConstants {
    public static final String NULL_DATE_STRING = "9999-01-01T00:00:00.000"; //$NON-NLS-1$

    public static final String WORK_ITEM_ARTIFACT_TYPE = "WorkItem"; //$NON-NLS-1$

    public static final String CLASSIFICATION_TOOL_ID = "Classification"; //$NON-NLS-1$
    public static final String TEAM_PROJECT_NODE_ARTIFACT_TYPE = "TeamProject"; //$NON-NLS-1$
    public static final String NODE_ARTIFACT_TYPE = "Node"; //$NON-NLS-1$

    public static final String ATTACHMENT_SERVER_URL_EXTENDED_ATTRIBUTE_NAME = "AttachmentServerUrl"; //$NON-NLS-1$

    public static final int TFS_EVERYONE_CONSTANT_SET_ID = -1;
    public static final int AUTHENTICATED_USERS_CONSTANT_SET_ID = -2;
}
