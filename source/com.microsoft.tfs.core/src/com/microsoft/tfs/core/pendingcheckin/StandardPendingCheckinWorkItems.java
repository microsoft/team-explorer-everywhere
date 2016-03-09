// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.pendingcheckin;

import com.microsoft.tfs.core.clients.versioncontrol.events.EventSource;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.WorkItemCheckinInfo;
import com.microsoft.tfs.core.pendingcheckin.events.CheckedWorkItemsChangedEvent;
import com.microsoft.tfs.core.pendingcheckin.events.CheckedWorkItemsChangedListener;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.listeners.SingleListenerFacade;

/**
 * <p>
 * Standard implementation of {@link PendingCheckinWorkItems}.
 * </p>
 *
 * @since TEE-SDK-10.1
 * @threadsafety thread-safe
 */
public class StandardPendingCheckinWorkItems implements PendingCheckinWorkItems {
    private final SingleListenerFacade checkedWorkItemsChangedEventListeners =
        new SingleListenerFacade(CheckedWorkItemsChangedListener.class);

    private WorkItemCheckinInfo[] checkedWorkItems;

    /**
     * Constructs a @ StandardPendingCheckinWorkItems} for the given checked
     * work items.
     *
     * @param checkedWorkItems
     *        the checked work items that will be updated when this checkin
     *        happens (must not be <code>null</code>)
     */
    public StandardPendingCheckinWorkItems(final WorkItemCheckinInfo[] checkedWorkItems) {
        Check.notNull(checkedWorkItems, "checkedWorkItems"); //$NON-NLS-1$
        this.checkedWorkItems = checkedWorkItems;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addCheckedWorkItemsChangedListener(final CheckedWorkItemsChangedListener listener) {
        checkedWorkItemsChangedEventListeners.addListener(listener);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeCheckedWorkItemsChangedListener(final CheckedWorkItemsChangedListener listener) {
        checkedWorkItemsChangedEventListeners.removeListener(listener);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized WorkItemCheckinInfo[] getCheckedWorkItems() {
        return checkedWorkItems;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void setCheckedWorkItems(final WorkItemCheckinInfo[] checkedWorkItems) {
        Check.notNull(checkedWorkItems, "checkedWorkItems"); //$NON-NLS-1$

        this.checkedWorkItems = checkedWorkItems;

        ((CheckedWorkItemsChangedListener) checkedWorkItemsChangedEventListeners.getListener()).onCheckedWorkItemsChangesChanged(
            new CheckedWorkItemsChangedEvent(EventSource.newFromHere()));
    }
}
