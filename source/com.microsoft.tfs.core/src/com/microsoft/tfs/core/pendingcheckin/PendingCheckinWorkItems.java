// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.pendingcheckin;

import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.WorkItemCheckinInfo;
import com.microsoft.tfs.core.pendingcheckin.events.CheckedWorkItemsChangedListener;

/**
 * <p>
 * Contains the work items associated with (or set to be resolved by) the
 * current pending checkin.
 * </p>
 *
 * @since TEE-SDK-10.1
 * @threadsafety thread-safe
 */
public interface PendingCheckinWorkItems {
    /**
     * Adds a listener that's invoked whenever the work items that are checked
     * in the user interface are changed (via
     * {@link #setCheckedWorkItems(WorkItemCheckinInfo[])}).
     *
     * @param listener
     *        the listener to add (must not be <code>null</code>)
     */
    public void addCheckedWorkItemsChangedListener(CheckedWorkItemsChangedListener listener);

    /**
     * Removes a listener that was previously added by
     * {@link #addCheckedWorkItemsChangedListener(CheckedWorkItemsChangedListener)}
     * .
     *
     * @param listener
     *        the listener to remove (must not be <code>null</code>)
     */
    public void removeCheckedWorkItemsChangedListener(CheckedWorkItemsChangedListener listener);

    /**
     * @return the work items that are "checked" in the user interface (will be
     *         changed during this checkin).
     */
    public WorkItemCheckinInfo[] getCheckedWorkItems();

    /**
     * Sets the work items that are "checked" in the user interface.
     *
     * @param checkedWorkItems
     *        the checked work items (must not be <code>null</code>)
     */
    public void setCheckedWorkItems(WorkItemCheckinInfo[] checkedWorkItems);
}
