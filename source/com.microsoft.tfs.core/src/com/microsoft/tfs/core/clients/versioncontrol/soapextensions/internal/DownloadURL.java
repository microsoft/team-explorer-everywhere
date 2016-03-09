// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.soapextensions.internal;

import com.microsoft.tfs.core.clients.versioncontrol.VersionControlConstants;

/**
 * This class does not implement the "compression" done in the VS
 * implementation, only parsing for file ID and destroy detection.
 *
 * @threadsafety thread-safe
 */
public class DownloadURL {
    /**
     * The string "&fid=" to search for in the download URL.
     */
    private final static String FILE_ID_SEARCH_STRING = "&fid="; //$NON-NLS-1$

    /**
     * The download URL's path part.
     */
    private final String queryString;

    /**
     * The file ID parsed from the query arguments.
     */
    private final int fileID;

    /**
     * Creates a new {@link DownloadURL} instance from the query string part of
     * a TFS download URL.
     *
     * @param queryString
     *        the query string to parse (may be <code>null</code> or empty)
     */
    public DownloadURL(final String queryString) {
        // Query string is always saved untouched
        this.queryString = queryString;

        if (queryString == null || queryString.length() == 0) {
            this.fileID = 0;
            return;
        }

        // Parse the file ID

        // Find the first character of the file ID (number).
        int pos;

        /*
         * Look for the "fid=" part of "&fid=" at the beginning of the string,
         * since there is no & separator at the beginning.
         */
        if (queryString.startsWith(FILE_ID_SEARCH_STRING.substring(1))) {
            pos = FILE_ID_SEARCH_STRING.length() - 1;
        } else {
            // Look "&fid=" in the entire url
            pos = queryString.indexOf(FILE_ID_SEARCH_STRING);

            if (pos < 0) {
                // String did not contain a fid= key/value pair.
                this.fileID = 0;
                return;
            }

            pos += FILE_ID_SEARCH_STRING.length();
        }

        if (pos == queryString.length()) {
            // String ended in "fid=".
            this.fileID = 0;
            return;
        }

        /*
         * Find the first character after the file ID. It is legal for the index
         * to be just off the end of the string.
         */
        int endPos = queryString.indexOf('&', pos);

        if (endPos < 0) {
            endPos = queryString.length();
        }

        // Get and store the file ID.
        final String fileIdString = queryString.substring(pos, endPos);

        if (fileIdString == null || fileIdString.length() == 0) {
            // Value of fid= key/value pair was not a number.
            this.fileID = 0;
            return;
        }

        int id = 0;
        try {
            id = Integer.parseInt(fileIdString);
        } catch (final NumberFormatException e) {
            id = 0;
        }

        this.fileID = id;
    }

    /**
     * @return true if the content represented by this download url has been
     *         destroyed.
     */
    public boolean isContentDestroyed() {
        return (null != queryString && fileID == VersionControlConstants.DESTROYED_FILE_ID /* 1023 */);
    }

    public int getFileID() {
        return fileID;
    }

    /**
     * Returns a string representation of the download URL which is equal to the
     * one provided at construction.
     */
    public String getURL() {
        return queryString;
    }
}
