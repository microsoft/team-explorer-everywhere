// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

 /*
* ---------------------------------------------------------
* Generated file, DO NOT EDIT
* ---------------------------------------------------------
*
* See following wiki page for instructions on how to regenerate:
*   https://vsowiki.com/index.php?title=Rest_Client_Generation
*/

package com.microsoft.teamfoundation.sourcecontrol.webapi.model;


/** 
 * Criteria used in a search for change lists
 * 
 */
public class ChangeListSearchCriteria {

    /**
    * If provided, a version descriptor to compare against base
    */
    private String compareVersion;
    /**
    * If true, don't include delete history entries
    */
    private boolean excludeDeletes;
    /**
    * Whether or not to follow renames for the given item being queried
    */
    private boolean followRenames;
    /**
    * If provided, only include history entries created after this date (string)
    */
    private String fromDate;
    /**
    * If provided, a version descriptor for the earliest change list to include
    */
    private String fromVersion;
    /**
    * Path of item to search under
    */
    private String itemPath;
    /**
    * Version of the items to search
    */
    private String itemVersion;
    /**
    * Number of results to skip (used when clicking more...)
    */
    private int skip;
    /**
    * If provided, only include history entries created before this date (string)
    */
    private String toDate;
    /**
    * If provided, the maximum number of history entries to return
    */
    private int top;
    /**
    * If provided, a version descriptor for the latest change list to include
    */
    private String toVersion;
    /**
    * Alias or display name of user who made the changes
    */
    private String user;

    /**
    * If provided, a version descriptor to compare against base
    */
    public String getCompareVersion() {
        return compareVersion;
    }

    /**
    * If provided, a version descriptor to compare against base
    */
    public void setCompareVersion(final String compareVersion) {
        this.compareVersion = compareVersion;
    }

    /**
    * If true, don't include delete history entries
    */
    public boolean getExcludeDeletes() {
        return excludeDeletes;
    }

    /**
    * If true, don't include delete history entries
    */
    public void setExcludeDeletes(final boolean excludeDeletes) {
        this.excludeDeletes = excludeDeletes;
    }

    /**
    * Whether or not to follow renames for the given item being queried
    */
    public boolean getFollowRenames() {
        return followRenames;
    }

    /**
    * Whether or not to follow renames for the given item being queried
    */
    public void setFollowRenames(final boolean followRenames) {
        this.followRenames = followRenames;
    }

    /**
    * If provided, only include history entries created after this date (string)
    */
    public String getFromDate() {
        return fromDate;
    }

    /**
    * If provided, only include history entries created after this date (string)
    */
    public void setFromDate(final String fromDate) {
        this.fromDate = fromDate;
    }

    /**
    * If provided, a version descriptor for the earliest change list to include
    */
    public String getFromVersion() {
        return fromVersion;
    }

    /**
    * If provided, a version descriptor for the earliest change list to include
    */
    public void setFromVersion(final String fromVersion) {
        this.fromVersion = fromVersion;
    }

    /**
    * Path of item to search under
    */
    public String getItemPath() {
        return itemPath;
    }

    /**
    * Path of item to search under
    */
    public void setItemPath(final String itemPath) {
        this.itemPath = itemPath;
    }

    /**
    * Version of the items to search
    */
    public String getItemVersion() {
        return itemVersion;
    }

    /**
    * Version of the items to search
    */
    public void setItemVersion(final String itemVersion) {
        this.itemVersion = itemVersion;
    }

    /**
    * Number of results to skip (used when clicking more...)
    */
    public int getSkip() {
        return skip;
    }

    /**
    * Number of results to skip (used when clicking more...)
    */
    public void setSkip(final int skip) {
        this.skip = skip;
    }

    /**
    * If provided, only include history entries created before this date (string)
    */
    public String getToDate() {
        return toDate;
    }

    /**
    * If provided, only include history entries created before this date (string)
    */
    public void setToDate(final String toDate) {
        this.toDate = toDate;
    }

    /**
    * If provided, the maximum number of history entries to return
    */
    public int getTop() {
        return top;
    }

    /**
    * If provided, the maximum number of history entries to return
    */
    public void setTop(final int top) {
        this.top = top;
    }

    /**
    * If provided, a version descriptor for the latest change list to include
    */
    public String getToVersion() {
        return toVersion;
    }

    /**
    * If provided, a version descriptor for the latest change list to include
    */
    public void setToVersion(final String toVersion) {
        this.toVersion = toVersion;
    }

    /**
    * Alias or display name of user who made the changes
    */
    public String getUser() {
        return user;
    }

    /**
    * Alias or display name of user who made the changes
    */
    public void setUser(final String user) {
        this.user = user;
    }
}
