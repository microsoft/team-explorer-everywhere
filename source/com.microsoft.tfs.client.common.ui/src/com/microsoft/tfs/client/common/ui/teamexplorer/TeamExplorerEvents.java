// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teamexplorer;

import java.util.HashMap;
import java.util.Map;

import com.microsoft.tfs.client.common.ui.TeamExplorerEventArg;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.listeners.SingleListenerFacade;

public class TeamExplorerEvents {
    public static final int MY_WORK_ITEM_FAVORITES_CHANGED = 1000;
    public static final int TEAM_WORK_ITEM_FAVORITES_CHANGED = 1010;
    public static final int MY_BUILD_FAVORITES_CHANGED = 1020;
    public static final int TEAM_BUILD_FAVORITES_CHANGED = 1030;
    public static final int BUILD_DEFINITION_ADDED = 1040;
    public static final int BUILD_DEFINITION_CHANGED = 1045;
    public static final int BUILD_DEFINITION_DELETED = 1050;
    public static final int QUERY_ITEM_DELETED = 1060;
    public static final int QUERY_ITEM_UPDATED = 1070;
    public static final int QUERY_ITEM_RENAMED = 1080;
    public static final int QUERY_FOLDER_CHILDREN_UPDATED = 1090;
    public static final int FORM_RESIZED = 1100;

    public Map<Integer, SingleListenerFacade> events = new HashMap<Integer, SingleListenerFacade>();

    public TeamExplorerEvents() {
        events.put(MY_WORK_ITEM_FAVORITES_CHANGED, new SingleListenerFacade(TeamExplorerEventListener.class));
        events.put(TEAM_WORK_ITEM_FAVORITES_CHANGED, new SingleListenerFacade(TeamExplorerEventListener.class));
        events.put(MY_BUILD_FAVORITES_CHANGED, new SingleListenerFacade(TeamExplorerEventListener.class));
        events.put(TEAM_BUILD_FAVORITES_CHANGED, new SingleListenerFacade(TeamExplorerEventListener.class));
        events.put(BUILD_DEFINITION_ADDED, new SingleListenerFacade(TeamExplorerEventListener.class));
        events.put(BUILD_DEFINITION_CHANGED, new SingleListenerFacade(TeamExplorerEventListener.class));
        events.put(BUILD_DEFINITION_DELETED, new SingleListenerFacade(TeamExplorerEventListener.class));
        events.put(QUERY_ITEM_DELETED, new SingleListenerFacade(TeamExplorerEventListener.class));
        events.put(QUERY_ITEM_UPDATED, new SingleListenerFacade(TeamExplorerEventListener.class));
        events.put(QUERY_ITEM_RENAMED, new SingleListenerFacade(TeamExplorerEventListener.class));
        events.put(QUERY_FOLDER_CHILDREN_UPDATED, new SingleListenerFacade(TeamExplorerEventListener.class));
        events.put(FORM_RESIZED, new SingleListenerFacade(TeamExplorerEventListener.class));
    }

    public synchronized void registerEvent(final int eventType) {
        // verify the value does not already exist.
        // add an entry to the events map
    }

    public synchronized void addListener(final int eventType, final TeamExplorerEventListener listener) {
        Check.isTrue(events.containsKey(eventType), "Event not registered"); //$NON-NLS-1$
        Check.notNull(listener, "listener"); //$NON-NLS-1$

        final SingleListenerFacade listeners = events.get(eventType);
        listeners.addListener(listener);
    }

    public synchronized void removeListener(final int eventType, final TeamExplorerEventListener listener) {
        Check.isTrue(events.containsKey(eventType), "Event not registered"); //$NON-NLS-1$
        Check.notNull(listener, "listener"); //$NON-NLS-1$

        final SingleListenerFacade listeners = events.get(eventType);
        listeners.removeListener(listener);
    }

    public synchronized void notifyListener(final int eventType) {
        notifyListener(eventType, new TeamExplorerEventArg());
    }

    public synchronized void notifyListener(final int eventType, final TeamExplorerEventArg arg) {
        Check.isTrue(events.containsKey(eventType), "Event not registered"); //$NON-NLS-1$
        Check.notNull(arg, "arg"); //$NON-NLS-1$

        final SingleListenerFacade listeners = events.get(eventType);
        ((TeamExplorerEventListener) listeners.getListener()).onEvent(arg);
    }

    public synchronized int getListenerCount(final int eventType) {
        final SingleListenerFacade listeners = events.get(eventType);
        return listeners.getListenerList().size();
    }
}
