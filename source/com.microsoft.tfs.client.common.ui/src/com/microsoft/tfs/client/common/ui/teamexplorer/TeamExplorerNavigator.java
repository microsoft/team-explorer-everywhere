// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teamexplorer;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.microsoft.tfs.client.common.ui.framework.helper.UIHelpers;
import com.microsoft.tfs.client.common.ui.framework.telemetry.ClientTelemetryHelper;
import com.microsoft.tfs.client.common.ui.teamexplorer.internal.TeamExplorerNavigationItemConfig;
import com.microsoft.tfs.client.common.ui.teamexplorer.items.ITeamExplorerNavigationItem;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.listeners.SingleListenerFacade;

public class TeamExplorerNavigator {
    // The state to coordinate navigation resets caused by server, repository,
    // or workspace change events. These events arrive on background threads and
    // need to queue a runnable on the UI to process the reset request. See the
    // comment for ResetNavigationRunnable for additional information.
    private final Object pendingResetNavigationLock = new Object();
    private Runnable pendingResetNavigationRunnable = null;
    private TeamExplorerNavigationItemConfig nextNavigation = null;

    // The navigation history buffer and current location.
    private final List<TeamExplorerNavigationItemConfig> navigationBuffer;
    private int currentItemBufferIndex;

    // Event listeners for navigation state changes.
    private final SingleListenerFacade listeners = new SingleListenerFacade(TeamExplorerNavigationListener.class);

    public TeamExplorerNavigator() {
        navigationBuffer = new ArrayList<TeamExplorerNavigationItemConfig>();
        currentItemBufferIndex = -1;
    }

    public void addListener(final TeamExplorerNavigationListener listener) {
        listeners.addListener(listener);
    }

    public void removeListener(final TeamExplorerNavigationListener listener) {
        listeners.removeListener(listener);
    }

    private void fireNavigationBufferChangedEvent() {
        ((TeamExplorerNavigationListener) listeners.getListener()).navigationHistoryChanged();
    }

    private void fireNavigateToEvent(final TeamExplorerNavigationItemConfig item) {
        ((TeamExplorerNavigationListener) listeners.getListener()).navigateToItem(item);
    }

    public boolean hasPrevious() {
        return currentItemBufferIndex > 0;
    }

    public boolean hasNext() {
        return currentItemBufferIndex < navigationBuffer.size() - 1;
    }

    public void navigateBack() {
        Check.isTrue(currentItemBufferIndex > 0, "currentItemBufferIndex > 0"); //$NON-NLS-1$
        currentItemBufferIndex--;

        fireNavigationBufferChangedEvent();
        doNavigation(getCurrentItem());
    }

    /**
     * Delete current navigation item in the navigate buffer and navigate to the
     * most recent page
     *
     * @param currentNavItem
     */
    public void navigateOut(final TeamExplorerNavigationItemConfig currentNavItem) {
        Check.isTrue(currentItemBufferIndex >= 0, "currentItemBufferIndex >= 0"); //$NON-NLS-1$

        final List<TeamExplorerNavigationItemConfig> navItemsToRemove =
            new ArrayList<TeamExplorerNavigationItemConfig>();

        for (final TeamExplorerNavigationItemConfig item : navigationBuffer) {
            if (item.equals(currentNavItem)) {
                navItemsToRemove.add(item);
            }
        }

        navigationBuffer.removeAll(navItemsToRemove);
        currentItemBufferIndex = navigationBuffer.size() - 1;
        nextNavigation = null;

        fireNavigationBufferChangedEvent();
        doNavigation(getCurrentItem());
    }

    public void navigateForward() {
        Check.isTrue(
            currentItemBufferIndex < navigationBuffer.size() - 1,
            "currentItemBufferIndex < navigationBuffer.size() - 1"); //$NON-NLS-1$
        currentItemBufferIndex++;

        fireNavigationBufferChangedEvent();
        doNavigation(getCurrentItem());
    }

    public void navigateToItem(final TeamExplorerNavigationItemConfig item) {
        clearNavigationHistoryFromIndex(currentItemBufferIndex);

        navigationBuffer.add(item);
        currentItemBufferIndex = navigationBuffer.size() - 1;

        fireNavigationBufferChangedEvent();
        doNavigation(item);
    }

    public void clearNavigationHistory() {
        clearNavigationHistoryFromIndex(-1);
        currentItemBufferIndex = -1;

        fireNavigationBufferChangedEvent();
    }

    public TeamExplorerNavigationItemConfig getCurrentItem() {
        if (currentItemBufferIndex >= 0) {
            return navigationBuffer.get(currentItemBufferIndex);
        } else {
            return null;
        }
    }

    private void clearNavigationHistoryFromIndex(final int index) {
        for (int i = navigationBuffer.size() - 1; i > index; i--) {
            navigationBuffer.remove(i);
        }
    }

    public void resetNavigation(final TeamExplorerContext context) {
        final TeamExplorerNavigationItemConfig navTo = getCurrentItem();
        clearNavigationHistory();

        // Attempt to navigate to the previously active
        // page. Note the previously active page may no
        // longer be available due to a change in the
        // context (such as the active project changing).
        final ITeamExplorerNavigationItem navItem = navTo == null ? null : navTo.createInstance();

        if (navItem != null && navItem.isVisible(context)) {
            navigateToItem(navTo);
        } else {
            // Return to the home page if we couldn't
            // navigate back to the current page (for
            // example, the documents page might not be
            // available after switching projects).
            doNavigation(null);
        }
    }

    /**
     * Calls to this method are queued to be run on the UI thread. This method
     * is called in response to repository, project, team, or workspace changes
     * that require the currently active page to change content (or simple
     * navigation from Team Explorer). These can occur in rapid succession and
     * should *not* be queued separately to be run on the UI thread. This is
     * because the as we begin building the content for one page on the UI
     * thread and block for some reason, the next navigation request in the UI
     * queue can start running, and as a result, would dispose the contents of
     * the page being constructed in the paused runnable. When this happens the
     * paused runnable will throw a widget disposed exception when it is resumed
     * and attempts to finish building its content (because it's content was
     * disposed by the next navigation runnable in the queue). This scenario
     * occurs, for example, when a server is changed and the default repository
     * changes to null then then another value in rapid succession.
     *
     * To prevent the problem described above, this method will not queue a new
     * runnable if one is already active. Instead, that queued runnable will
     * check to see if another navigation request is pending before terminating.
     *
     * @param item
     *        The configuration item for the target of the navigation.
     */
    private void doNavigation(final TeamExplorerNavigationItemConfig item) {
        ClientTelemetryHelper.sendPageView(item);

        Runnable queueRunnable = null;

        // Grab the lock to coordinate with any existing runnable which may be
        // currently processing another reset request.
        synchronized (pendingResetNavigationLock) {
            if (pendingResetNavigationRunnable == null) {
                pendingResetNavigationRunnable = new NavigationRunnable();
                queueRunnable = pendingResetNavigationRunnable;
            }

            // This is always set to ensure a currently executing runnable will
            // continue to run and process this this reset request in the case
            // that we don't create a new runnable.
            nextNavigation = item;
        }

        if (queueRunnable != null) {
            UIHelpers.runOnUIThread(true, queueRunnable);
        }
    }

    /**
     * A runnable to process a reset navigation request.
     *
     * Runnables of this type are always queued to the UI thread and we should
     * only ever have one queued at a time. If this runnable is in progress and
     * another navigation reset comes in on another thread, this currently
     * executing runnable will continue and process the next request instead of
     * having another runnable created and queued to the UI thread. Shared state
     * is used to coordinate this runnable on the UI thread with other
     * navigation reset requests coming in on other threads.
     *
     * The reason we need to avoid having two runnables queued to the UI thread
     * is because the entire new UI content is being created for Team Explorer
     * and the runnable may pause while the content is being created. If this
     * happens and another reset navigation runnable is started by the
     * UIAsyncWaiter while the previous in-progress reset runnable is paused,
     * the second one will dispose of the partial content created by the first,
     * which will cause a Widget Disposed exception when first runnable resumes.
     *
     * The problem described above occurs when a server, project, or workspace
     * change occurs and the default repository events fire in rapid succession
     * (first changing to null then to a new value).
     */
    private final class NavigationRunnable implements Runnable {
        private final Log log = LogFactory.getLog(NavigationRunnable.class);

        @Override
        public void run() {
            try {
                while (true) {
                    TeamExplorerNavigationItemConfig navTo = null;

                    synchronized (pendingResetNavigationLock) {
                        navTo = nextNavigation;

                        // Clear for now since we're about to handle the reset
                        // request. Another thread can change this value back to
                        // true before we decide to exit the loop and we'll keep
                        // looping to process additional incoming requests.
                        nextNavigation = null;
                    }

                    fireNavigateToEvent(navTo);

                    synchronized (pendingResetNavigationLock) {
                        // If pending navigation context is still null, then no
                        // other thread has requested a reset while processed
                        // the
                        // previous reset and so this runnable can now
                        // terminate.
                        if (nextNavigation == null) {
                            // This runnable is about to go away, so allow
                            // another one to be created when another reset is
                            // requested.
                            pendingResetNavigationRunnable = null;
                            return;
                        }
                    }
                }
            } catch (final Exception e) {
                // log the exception.
                log.error("Exception during navigation: ", e); //$NON-NLS-1$

                // Clear the shared state in the case of an exception.
                synchronized (pendingResetNavigationLock) {
                    pendingResetNavigationRunnable = null;
                    nextNavigation = null;
                }

            }
        }
    }
}
