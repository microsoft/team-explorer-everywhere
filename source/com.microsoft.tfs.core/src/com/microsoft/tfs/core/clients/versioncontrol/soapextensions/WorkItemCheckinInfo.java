// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.soapextensions;

import java.text.MessageFormat;

import com.microsoft.tfs.core.Messages;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.internal.CheckinNotificationWorkItemInfo;
import com.microsoft.tfs.core.clients.versioncontrol.workspacecache.WorkItemCheckedInfo;
import com.microsoft.tfs.core.clients.workitem.CheckinWorkItemAction;
import com.microsoft.tfs.core.clients.workitem.WorkItem;
import com.microsoft.tfs.core.clients.workitem.WorkItemActions;
import com.microsoft.tfs.core.clients.workitem.WorkItemClient;
import com.microsoft.tfs.util.Check;

/**
 * Represents information about a work item checkin.
 *
 * @since TEE-SDK-10.1
 */
public class WorkItemCheckinInfo {
    private final WorkItem workItem;
    private CheckinWorkItemAction action;

    public WorkItemCheckinInfo(final WorkItem workItem) {
        this.workItem = workItem;
    }

    public WorkItemCheckinInfo(final WorkItem workItem, final CheckinWorkItemAction action) {
        this.workItem = workItem;
        this.action = action;
    }

    @Override
    public String toString() {
        final String messageFormat = Messages.getString("WorkItemCheckinInfo.WorkItemCheckinInfoFormat"); //$NON-NLS-1$
        return MessageFormat.format(messageFormat, Integer.toString(workItem.getFields().getID()), action.toString());
    }

    public CheckinWorkItemAction getAction() {
        return action;
    }

    public void clearAction() {
        action = null;
    }

    public void setActionToDefault() {
        action = getAvailableActions()[0];
    }

    public void setActionFromString(final String s) {
        action = CheckinWorkItemAction.fromString(s);
    }

    public void setAction(final CheckinWorkItemAction newAction) {
        action = newAction;
    }

    public CheckinNotificationWorkItemInfo getNotification() {
        if (action == null) {
            action = CheckinWorkItemAction.ASSOCIATE;
        }
        return new CheckinNotificationWorkItemInfo(workItem.getFields().getID(), action);
    }

    public WorkItem getWorkItem() {
        return workItem;
    }

    public String getActionString() {
        return action == null ? "" : action.toUIString(); //$NON-NLS-1$
    }

    public String[] getAvailableActionStrings() {
        final CheckinWorkItemAction[] actions = getAvailableActions();
        final String[] strings = new String[actions.length];
        for (int i = 0; i < actions.length; i++) {
            strings[i] = actions[i].toString();
        }
        return strings;
    }

    public boolean isResolveSupported() {
        return isActionSupported(CheckinWorkItemAction.RESOLVE);
    }

    public boolean isActionSupported(final CheckinWorkItemAction action) {
        final CheckinWorkItemAction[] available = getAvailableActions();

        for (int i = 0; i < available.length; i++) {
            if (available[i].equals(action)) {
                return true;
            }
        }

        return false;
    }

    public CheckinWorkItemAction[] getAvailableActions() {
        if (workItem.getNextState(WorkItemActions.VS_CHECKIN) != null) {
            return new CheckinWorkItemAction[] {
                CheckinWorkItemAction.RESOLVE,
                CheckinWorkItemAction.ASSOCIATE
            };
        } else {
            return new CheckinWorkItemAction[] {
                CheckinWorkItemAction.ASSOCIATE
            };
        }
    }

    /**
     * Converts {@link WorkItemCheckedInfo}s to {@link WorkItemCheckinInfo}. A
     * {@link WorkItemClient} is required to create the {@link WorkItem}
     * isntances.
     * <p>
     * This operation may contact the server to populate the {@link WorkItem}.
     *
     * @param workItemClient
     *        a work item client to resolve information with (must not be
     *        <code>null</code>)
     * @param checkedInfo
     *        the {@link WorkItemCheckedInfo} to convert (may be
     *        <code>null</code>)
     * @return the converted {@link WorkItemCheckinInfo}s, <code>null</code> if
     *         a <code>null</code> {@link WorkItemCheckedInfo} was given
     */
    public static WorkItemCheckinInfo[] fromWorkItemCheckedInfo(
        final WorkItemClient workItemClient,
        final WorkItemCheckedInfo[] checkedInfo) {
        Check.notNull(workItemClient, "workItemClient"); //$NON-NLS-1$

        WorkItemCheckinInfo[] ret = null;

        if (checkedInfo != null) {
            ret = new WorkItemCheckinInfo[checkedInfo.length];

            for (int i = 0; i < checkedInfo.length; i++) {
                final WorkItem wi = workItemClient.getWorkItemByID(checkedInfo[i].getID());

                ret[i] = new WorkItemCheckinInfo(wi, checkedInfo[i].getCheckinAction());
            }
        }

        return ret;
    }
}
