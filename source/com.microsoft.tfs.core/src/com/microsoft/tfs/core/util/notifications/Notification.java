// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.util.notifications;

import java.util.HashMap;
import java.util.Map;

import com.microsoft.tfs.util.TypesafeEnum;

/**
 * Defintes types of intra- and inter-process notifications for communication
 * with other Visual Studio products on Windows.
 */
public class Notification extends TypesafeEnum {
    /*
     * Ensure these values match the other VS products.
     */

    // This must be initialized first so the other static initializers can use
    // it
    private static final Map<Integer, Notification> instances = new HashMap<Integer, Notification>();

    // Windows WM_USER
    public final static Notification TEAM_FOUNDATION_NOTIFICATION_BEGIN = new Notification(0x400);

    // *********************************************************************************************
    // Team Explorer Notifications
    // *********************************************************************************************
    public final static Notification TEAM_EXPLORER_NOTIFICATION_BEGIN =
        new Notification(TEAM_FOUNDATION_NOTIFICATION_BEGIN.getValue());

    // TeamExplorerFavoriteCreated - new favorite has been created.
    // wParam - server Uri
    // lParam - favorite name
    // NOT YET IMPLEMENTED
    public final static Notification TEAM_EXPLORER_FAVORITE_CREATED =
        new Notification(TEAM_EXPLORER_NOTIFICATION_BEGIN.getValue() + 1);

    // TeamExplorerFavoriteDeleted - a favorite has been deleted.
    // wParam - server Uri
    // lParam - favorite name
    // NOT YET IMPLEMENTED
    public final static Notification TEAM_EXPLORER_FAVORITE_DELETED =
        new Notification(TEAM_EXPLORER_NOTIFICATION_BEGIN.getValue() + 2);

    // TeamExplorerFavoriteRenamed - a favorite has been renamed.
    // wParam - server Uri
    // lParam - favorite name
    // NOT YET IMPLEMENTED
    public final static Notification TEAM_EXPLORER_FAVORITE_RENAMED =
        new Notification(TEAM_EXPLORER_NOTIFICATION_BEGIN.getValue() + 3);

    public final static Notification TEAM_EXPLORER_NOTIFICATION_END =
        new Notification(TEAM_EXPLORER_NOTIFICATION_BEGIN.getValue() + 99);

    // *********************************************************************************************
    // Version Control Notifications
    // *********************************************************************************************
    public final static Notification VERSION_CONTROL_NOTIFICATION_BEGIN =
        new Notification(TEAM_EXPLORER_NOTIFICATION_END.getValue() + 1);

    // VersionControlWorkspaceCreated - new workspace has been created.
    // wParam - server
    // lParam - workspace
    public final static Notification VERSION_CONTROL_WORKSPACE_CREATED =
        new Notification(VERSION_CONTROL_NOTIFICATION_BEGIN.getValue() + 1);

    // VersionControlWorkspaceDeleted - a workspace has been deleted.
    // wParam - server
    // lParam - workspace
    public final static Notification VERSION_CONTROL_WORKSPACE_DELETED =
        new Notification(VERSION_CONTROL_NOTIFICATION_BEGIN.getValue() + 2);

    // VersionControlWorkspaceChanged - the working folder mappings for a
    // workspace have changed.
    // wParam - server
    // lParam - workspace
    public final static Notification VERSION_CONTROL_WORKSPACE_CHANGED =
        new Notification(VERSION_CONTROL_NOTIFICATION_BEGIN.getValue() + 3);

    // VersionControlPendingChangesChanged - the pending changes have changed
    // for a workspace.
    // wParam - server
    // lParam - workspace
    public final static Notification VERSION_CONTROL_PENDING_CHANGES_CHANGED =
        new Notification(VERSION_CONTROL_NOTIFICATION_BEGIN.getValue() + 4);

    // VersionControlGetCompleted - a get operation has completed in a workspace
    // wParam - server
    // lParam - workspace
    public final static Notification VERSION_CONTROL_GET_COMPLETED =
        new Notification(VERSION_CONTROL_NOTIFICATION_BEGIN.getValue() + 5);

    // VersionControlChangesetReconciled - a process is reconciling a workspace
    // to a changeset
    // wParam - server
    // lParam - changesetId
    public final static Notification VERSION_CONTROL_CHANGESET_RECONCILED =
        new Notification(VERSION_CONTROL_NOTIFICATION_BEGIN.getValue() + 6);

    // VersionControlFolderContentChanged - a process has changed the content of
    // one or more server folders
    // without creating pending changes. Examples are creating a committed
    // branch
    // and destroying item.
    // wParam - server
    // lParam - changesetId (if available)
    public final static Notification VERSION_CONTROL_FOLDER_CONTENT_CHANGED =
        new Notification(VERSION_CONTROL_NOTIFICATION_BEGIN.getValue() + 7);

    // VersionControlConflictResolved - used to syncronize the use of the inVS
    // merge tool. When the in VS
    // merge tool resolves a conflict or cancels the resolution of one it will
    // notify other processes of the
    // event (mainly tf.exe)
    // wParam - cookie that identifies the merge window (tf.exe recieves when
    // creating the window)
    // lParam - status of the window (Accepted or Cancelled)
    public final static Notification VERSION_CONTROL_MANUAL_MERGE_CLOSED =
        new Notification(VERSION_CONTROL_NOTIFICATION_BEGIN.getValue() + 8);

    // VersionControlLocalWorkspaceScan - the asynchronous scanner for the local
    // workspace ran and found
    // some changes
    // wParam - server
    // lParam - workspace
    public final static Notification VERSION_CONTROL_LOCAL_WORKSPACE_SCAN =
        new Notification(VERSION_CONTROL_NOTIFICATION_BEGIN.getValue() + 9);

    // Eventually we'll probably want to add support for destroy, label,
    // permissions...

    public final static Notification VERSION_CONTROL_NOTIFICATION_END =
        new Notification(VERSION_CONTROL_NOTIFICATION_BEGIN.getValue() + 99);

    // *********************************************************************************************
    // Work Item Tracking Notifications
    // *********************************************************************************************
    public final static Notification WORK_ITEM_TRACKING_NOTIFICATION_BEGIN =
        new Notification(VERSION_CONTROL_NOTIFICATION_END.getValue() + 1);

    // WorkItemTrackingUserQueryCreated - a user query was created
    // wParam - server Uri
    // lParam - query name
    // NOT YET IMPLEMENTED
    public final static Notification WORK_ITEM_TRACKING_USER_QUERY_CREATED =
        new Notification(WORK_ITEM_TRACKING_NOTIFICATION_BEGIN.getValue() + 1);

    // WorkItemTrackingTeamQueryCreated - a team query was created
    // wParam - server Uri
    // lParam - query name
    // NOT YET IMPLEMENTED
    public final static Notification WORK_ITEM_TRACKING_TEAM_QUERY_CREATED =
        new Notification(WORK_ITEM_TRACKING_NOTIFICATION_BEGIN.getValue() + 2);

    // WorkItemTrackingUserQueryDeleted - a user query was deleted
    // wParam - server Uri
    // lParam - query name
    // NOT YET IMPLEMENTED
    public final static Notification WORK_ITEM_TRACKING_USER_QUERY_DELETED =
        new Notification(WORK_ITEM_TRACKING_NOTIFICATION_BEGIN.getValue() + 3);

    // WorkItemTrackingTeamQueryDeleted - a team query was deleted
    // wParam - server Uri
    // lParam - query name
    // NOT YET IMPLEMENTED
    public final static Notification WORK_ITEM_TRACKING_TEAM_QUERY_DELETED =
        new Notification(WORK_ITEM_TRACKING_NOTIFICATION_BEGIN.getValue() + 4);

    // WorkItemTrackingUserQueryRenamed - a user query was renamed
    // wParam - server Uri
    // lParam - query name
    // NOT YET IMPLEMENTED
    public final static Notification WORK_ITEM_TRACKING_USER_QUERY_RENAMED =
        new Notification(WORK_ITEM_TRACKING_NOTIFICATION_BEGIN.getValue() + 5);

    // WorkItemTrackingTeamQueryRenamed - a team query was renamed
    // wParam - server Uri
    // lParam - query name
    // NOT YET IMPLEMENTED
    public final static Notification WORK_ITEM_TRACKING_TEAM_QUERY_RENAMED =
        new Notification(WORK_ITEM_TRACKING_NOTIFICATION_BEGIN.getValue() + 6);

    public final static Notification WORK_ITEM_TRACKING_NOTIFICATION_END =
        new Notification(WORK_ITEM_TRACKING_NOTIFICATION_BEGIN.getValue() + 99);

    // *********************************************************************************************
    // Reporting Notifications
    // *********************************************************************************************
    public final static Notification REPORTING_NOTIFICATION_BEGIN =
        new Notification(WORK_ITEM_TRACKING_NOTIFICATION_END.getValue() + 1);

    public final static Notification REPORTING_NOTIFICATION_END =
        new Notification(REPORTING_NOTIFICATION_BEGIN.getValue() + 99);

    // *********************************************************************************************
    // Documents Notifications
    // *********************************************************************************************
    public final static Notification DOCUMENTS_NOTIFICATION_BEGIN =
        new Notification(REPORTING_NOTIFICATION_END.getValue() + 1);

    public final static Notification DOCUMENTS_NOTIFICATION_END =
        new Notification(DOCUMENTS_NOTIFICATION_BEGIN.getValue() + 99);

    // *********************************************************************************************
    // Build Notifications
    // *********************************************************************************************
    public final static Notification BUILD_NOTIFICATION_BEGIN =
        new Notification(DOCUMENTS_NOTIFICATION_END.getValue() + 1);

    // BuildNotificationBuildCreated - a new build type was created
    // wParam - server Uri
    // lParam - team project
    // NOT YET IMPLEMENTED
    public final static Notification BUILD_NOTIFICATION_BUILD_CREATED =
        new Notification(BUILD_NOTIFICATION_BEGIN.getValue() + 1);

    // BuildNotificationBuildDeleted - a build type was deleted
    // wParam - server Uri
    // lParam - team project
    // NOT YET IMPLEMENTED
    public final static Notification BUILD_NOTIFICATION_BUILD_DELETED =
        new Notification(BUILD_NOTIFICATION_BEGIN.getValue() + 2);

    // BuildNotificationBuildRenamed - a build type was renamed
    // wParam - server Uri
    // lParam - team project
    // NOT YET IMPLEMENTED
    public final static Notification BUILD_NOTIFICATION_BUILD_RENAMED =
        new Notification(BUILD_NOTIFICATION_BEGIN.getValue() + 3);

    // BuildNotificationBuildStarted - a build type was started
    // wParam - server Uri
    // lParam - team project
    // NOT YET IMPLEMENTED
    public final static Notification BUILD_NOTIFICATION_BUILD_STARTED =
        new Notification(BUILD_NOTIFICATION_BEGIN.getValue() + 4);

    // BuildNotificationBuildStopped - a build type was stopped
    // wParam - server Uri
    // lParam - team project
    // NOT YET IMPLEMENTED
    public final static Notification BUILD_NOTIFICATION_BUILD_STOPPED =
        new Notification(BUILD_NOTIFICATION_BEGIN.getValue() + 5);

    public final static Notification BUILD_NOTIFICATION_END =
        new Notification(BUILD_NOTIFICATION_BEGIN.getValue() + 99);

    public final static Notification TEAM_FOUNDATION_NOTIFICATION_END =
        new Notification(BUILD_NOTIFICATION_END.getValue() + 1);

    private Notification(final int value) {
        super(value);
        instances.put(value, this);
    }

    public static Notification fromValue(final int value) {
        return instances.get(value);
    }
};
