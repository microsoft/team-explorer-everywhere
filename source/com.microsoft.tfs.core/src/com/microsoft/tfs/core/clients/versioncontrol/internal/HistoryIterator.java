// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.internal;

import java.util.Iterator;
import java.util.NoSuchElementException;

import com.microsoft.tfs.core.clients.versioncontrol.exceptions.VersionControlException;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Changeset;
import com.microsoft.tfs.core.clients.versioncontrol.specs.ItemSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.ChangesetVersionSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.VersionSpec;
import com.microsoft.tfs.util.Check;

import ms.tfs.versioncontrol.clientservices._03._Changeset;

/**
 * <p>
 * {@link HistoryIterator} implements a paging algorithm for TFS history data
 * returned from the QueryHistory web service method, providing an
 * {@link Iterator}-based view of results. Like other TFS history APIs,
 * changesets are returned from this {@link Iterator} in reverse chronological
 * order.
 * </p>
 *
 * <p>
 * {@link HistoryIterator} pages in sets of {@link _Changeset}s from the TFS
 * server, up to {@link #PAGE_SIZE} at a time. The {@link Iterator} methods will
 * transparently page in the next set if needed.
 * </p>
 *
 * <p>
 * This {@link Iterator} implementation does not support the optional
 * {@link Iterator#remove()} method.
 * </p>
 */
public class HistoryIterator implements Iterator<Changeset> {
    /**
     * The maximum number of changesets we will page in during a single call to
     * QueryHistory. This number is chosen because it is what Visual Studio's
     * object model uses for a max page size.
     */
    private static final int PAGE_SIZE = 256;

    /**
     * Proxy for the web service we call to page in changesets.
     */
    private final WebServiceLayer webServiceLayer;

    /**
     * Invariant parameters we pass to the QueryHistory web service method.
     * These values do not change over the lifetime of this class (they are the
     * same for every page).
     */
    private final String workspaceName;
    private final String workspaceOwner;
    private final ItemSpec itemSpec;
    private final VersionSpec versionItem;
    private final String user;
    private final VersionSpec versionFrom;
    private final boolean includeFiles;
    private final boolean generateDownloadUrls;
    private final boolean slotMode;
    private final boolean sortAscending;

    /**
     * The top of the next page we will page in (could be <code>null</code>
     * indicating "end of time"). This is initialized at class construction to
     * "versionTo". Each time we page in, this is set to the changeset number of
     * the last (chronologically earliest) changeset in the page minus 1.
     */
    private VersionSpec endingVersionSpec;

    /**
     * The number of changesets we have left to page in. This is initialized at
     * class construction to "max count". Each time we page in, this is
     * decremented by the size of the page. If it reaches 0, we know there are
     * no more pages we need to get.
     */
    private int numLeft;

    /**
     * The number of changesets we requested for the current page. May not be
     * the number we actually got back if the current page is the last page.
     * Initialized to -1. Set just before we pull in each page from the server.
     * Will be set to either {@link #numLeft} (before the current page) or
     * {@link #PAGE_SIZE}, whichever is smaller.
     */
    private int numRequested = -1;

    /**
     * The current page of {@link Changeset}s we've paged in from the server. A
     * <code>null</code> value indicates that we've never pulled in any pages at
     * all yet. An empty array indicates that the server returned no more
     * changesets, which means there are no more pages.
     */
    private Changeset[] changesets;

    /**
     * An index into the current page ({@link #changesets}). Initialized to -1.
     * Each time we page in, this is set to 0. Each time the next element is
     * requested from the current page ({@link #next()}) , this is incremented.
     * When it reaches the page length, we've reached the end of the page and
     * there are either no more changesets or we need to pull in the next page.
     */
    private int changesetsIndex = -1;

    /**
     * Creates a new {@link HistoryIterator}. Instantiating a
     * {@link HistoryIterator} does not perform any round trips to the server.
     * The first call to QueryHistory will happen when one of the
     * {@link Iterator} methods is called for the first time, or if the
     * {@link #prime()} method is called.
     *
     * @param webServiceLayer
     *        the web service proxy class to use to call the QueryHistory web
     *        service method (must not be <code>null</code>)
     * @param workspaceName
     *        the workspace name parameter to pass to the QueryHistory web
     *        service method
     * @param workspaceOwner
     *        the workspace owner parameter to pass to the QueryHistory web
     *        service method
     * @param itemSpec
     *        the itemSpec parameter to pass to the QueryHistory web service
     *        method
     * @param versionItem
     *        the versionItem parameter to pass to the QueryHistory web service
     *        method
     * @param user
     *        the user parameter to pass to the QueryHistory web service method
     * @param versionFrom
     *        the versionFrom parameter to pass to the QueryHistory web service
     *        method
     * @param versionTo
     *        the versionTo that specifies the more recent version to query up
     *        to
     * @param maxCount
     *        the maximum number of changesets this {@link Iterator} should
     *        return
     * @param includefiles
     *        the includefiles parameter to pass to the QueryHistory web service
     *        method
     * @param generateDownloadUrls
     *        the generateDownloadUrls parameter to pass to the QueryHistory web
     *        service method
     * @param slotMode
     *        the slotMode parameter to pass to the QueryHistory web service
     *        method
     */
    public HistoryIterator(
        final WebServiceLayer webServiceLayer,
        final String workspaceName,
        final String workspaceOwner,
        final ItemSpec itemSpec,
        final VersionSpec versionItem,
        final String user,
        final VersionSpec versionFrom,
        final VersionSpec versionTo,
        final int maxCount,
        final boolean includefiles,
        final boolean generateDownloadUrls,
        final boolean slotMode,
        final boolean sortAscending) {
        Check.notNull(webServiceLayer, "webServiceLayer"); //$NON-NLS-1$

        this.webServiceLayer = webServiceLayer;
        this.workspaceName = workspaceName;
        this.workspaceOwner = workspaceOwner;
        this.itemSpec = itemSpec;
        this.versionItem = versionItem;
        this.user = user;
        this.versionFrom = versionFrom;
        endingVersionSpec = versionTo;
        numLeft = maxCount;
        includeFiles = includefiles;
        this.generateDownloadUrls = generateDownloadUrls;
        this.slotMode = slotMode;
        this.sortAscending = sortAscending;
    }

    /**
     * Primes this {@link HistoryIterator} by reading in the first page of
     * results from the server. This method is intended to be called immediately
     * after construction, and can only be called if no pages have been paged in
     * yet.
     */
    public void prime() {
        if (changesets == null) {
            pageIn();
        } else {
            throw new IllegalStateException();
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see java.util.Iterator#hasNext()
     */
    @Override
    public boolean hasNext() {
        if (!currentPageHasNext() && areMorePages()) {
            pageIn();
        }

        return currentPageHasNext();
    }

    /*
     * (non-Javadoc)
     *
     * @see java.util.Iterator#next()
     */
    @Override
    public Changeset next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }

        return changesets[changesetsIndex++];
    }

    /*
     * (non-Javadoc)
     *
     * @see java.util.Iterator#remove()
     */
    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }

    /**
     * @return <code>true</code> if we have a current page ({@link #changesets}
     *         is non-<code>null</code>), and the current page index (
     *         {@link #changesetsIndex}) is a valid index into the current page
     */
    private boolean currentPageHasNext() {
        return changesets != null && changesetsIndex < changesets.length;
    }

    /**
     * Checks to see if there are more pages to page in. If and only if this
     * method returns <code>true</code>, it is safe to then call the
     * {@link #pageIn()} method.
     *
     * @return <code>true</code> if it is appropriate to try paging in another
     *         page
     */
    private boolean areMorePages() {
        if (changesets == null) {
            /*
             * We've never paged at all yet, so we can't say that there are no
             * more pages.
             */
            return true;
        }

        if (changesets.length == 0) {
            /*
             * The last page was an empty array, so there are no more pages.
             */
            return false;
        }

        if (changesets[changesets.length - 1].getChangesetID() == 1) {
            /*
             * The last page ended with the very first changeset, so there are
             * no more pages.
             */
            return false;
        }

        if (changesets.length < numRequested) {
            /*
             * The last page was smaller than what we requested, so there are no
             * more pages.
             */
            return false;
        }

        if (numLeft == 0) {
            /*
             * We've paged in the number of changesets that were originally
             * requested, so there are no more pages.
             */
            return false;
        }

        /*
         * If we get here, the best we can say is there may be another page.
         * Attempting to page in is appropriate.
         */
        return true;
    }

    /**
     * Pulls in the next page from the server.
     *
     * @throws VersionControlException
     *         if the web service threw an exception during history query.
     */
    private void pageIn() throws VersionControlException {
        /*
         * Figure out how many changesets to request for the the new page. It is
         * either numLeft or PAGE_SIZE, whichever is smaller.
         */
        numRequested = Math.min(numLeft, PAGE_SIZE);

        /*
         * Pull in the next page by calling the server.
         */

        changesets = webServiceLayer.queryHistory(
            workspaceName,
            workspaceOwner,
            itemSpec,
            versionItem,
            user,
            versionFrom,
            endingVersionSpec,
            numRequested,
            includeFiles,
            generateDownloadUrls,
            slotMode,
            sortAscending);

        /*
         * Decrement the numLeft field, subtracting the number of changesets in
         * the page we just pulled in.
         */
        numLeft -= changesets.length;

        /*
         * Assuming the page was non-empty, we can calculate where the top of
         * the next page would be. Note that this does not necessarily mean that
         * there will be a next page. This calcuation simply figures out the top
         * of the next page if there will be one.
         */
        if (changesets.length > 0) {
            // if the user asked for the changes, sort them
            if (includeFiles) {
                for (final Changeset set : changesets) {
                    set.sortChanges();
                }
            }

            final int lastChangesetNumber = changesets[changesets.length - 1].getChangesetID();
            endingVersionSpec = new ChangesetVersionSpec(lastChangesetNumber - 1);
        }

        /*
         * Set the current page index to 0 (beginning of the page).
         */
        changesetsIndex = 0;
    }
}
