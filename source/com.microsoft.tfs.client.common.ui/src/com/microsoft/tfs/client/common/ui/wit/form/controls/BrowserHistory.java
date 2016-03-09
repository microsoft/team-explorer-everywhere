// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.wit.form.controls;

import java.util.ArrayList;

import com.microsoft.tfs.util.Check;

/**
 * Maintains a set of URLs which represent a browser history. An index into the
 * set of URLs represents the current URL being viewed. Methods to move
 * backward/forward in the history are provide.
 *
 *
 * @threadsafety unknown
 */
public class BrowserHistory {
    private final String EMPTY_URL = ""; //$NON-NLS-1$

    ArrayList<String> history = new ArrayList<String>();
    int currentIndex = -1;

    /**
     * Add a new URL to the browsing history. Adding a new URL causes all items
     * beyond the current position to be eliminated from the history. The newly
     * added URL will be at the end of the browser history upon return.
     *
     *
     * @param url
     *        The URL to add.
     */
    public void addURL(final String url) {
        while (currentIndex < history.size() - 1) {
            history.remove(currentIndex + 1);
        }

        history.add(url);
        currentIndex = history.size() - 1;
    }

    /**
     * Returns the current URL which represents the URL currently being viewed.
     */
    public String getCurrentURL() {
        if (currentIndex >= 0 && currentIndex < history.size()) {
            return history.get(currentIndex);
        } else {
            return EMPTY_URL;
        }
    }

    /**
     * Return the original URL in the browsing history.
     */
    public String getOriginalURL() {
        if (history.size() > 0) {
            return history.get(0);
        } else {
            return EMPTY_URL;
        }
    }

    /**
     * Return the previous URL in the list (relative to the current position)
     * and moves the index back to make this the now current position.
     */
    public String moveBack() {
        Check.isTrue(currentIndex > 0, "currentIndex > 0"); //$NON-NLS-1$
        currentIndex--;
        return history.get(currentIndex);
    }

    /**
     * Return the next URL in the list (relative to the current position) and
     * moves the index back to make this the now current position.
     */
    public String moveForward() {
        Check.isTrue(currentIndex < history.size() - 1, "currentIndex < history.size() - 1"); //$NON-NLS-1$
        currentIndex++;
        return history.get(currentIndex);
    }

    /**
     * Returns true is there is a next URL in the list relative to the current
     * position.
     */
    public boolean canMoveBack() {
        return currentIndex > 0;
    }

    /**
     * Return true if there is a previous URL in the list relative to the
     * current position.
     */
    public boolean canMoveForward() {
        return currentIndex >= 0 && currentIndex < (history.size() - 1);
    }

    /**
     * Returns true if there is any URL in the history.
     */
    public boolean hasCurrentURL() {
        return currentIndex >= 0;
    }
}
