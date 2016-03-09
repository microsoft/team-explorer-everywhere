// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.clc.vc.diff.launch;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

import com.microsoft.tfs.client.clc.exceptions.CLCException;
import com.microsoft.tfs.client.clc.vc.diff.DiffItem;
import com.microsoft.tfs.core.util.FileEncoding;

/**
 * Contains information about an item about to be launched into a diff program
 * and some methods to generate nice labels.
 * <p>
 * {@link DiffItem} is better suited for gathering and comparing information
 * about files that need to be diffed, this interface is for items that are
 * about to be displayed or passed to an external program.
 * <p>
 * Implementations must generate a non-null default label for retrieval via
 * {@link #getLabel()} after construction. They may allow
 * {@link #setLabel(String)} to override that label.
 */
public interface DiffLaunchItem {
    /**
     * The standard date/time formatter for these launch item labels.
     */
    public final static DateFormat SHORT_DATE_TIME_FORMATTER =
        SimpleDateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);

    /**
     * @return the TFS code page for the item.
     * @throws CLCException
     *         if an exception happened getting the item information to get the
     *         encoding.
     * @see FileEncoding
     */
    public int getEncoding() throws CLCException;

    /**
     * @return the path to the local file.
     * @throws CLCException
     *         if an exception happened managing temporary files.
     */
    public String getFilePath() throws CLCException;

    /**
     * @return true if the file {@link #getFilePath()} returns is a temporary
     *         file, false if it is not.
     */
    public boolean isTemporary();

    /**
     * @return the label to use to display this {@link DiffLaunchItem}.
     * @throws CLCException
     *         if an exception happened managing temporary files.
     */
    public String getLabel() throws CLCException;

    /**
     * Overrides the default label string on the {@link DiffLaunchItem}.
     *
     * @param label
     *        the label string to use when displaying this item to the user
     *        instead of the default label.
     */
    public void setLabel(String label);
}
