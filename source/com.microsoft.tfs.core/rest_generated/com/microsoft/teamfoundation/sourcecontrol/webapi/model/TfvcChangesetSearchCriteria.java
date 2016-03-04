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
public class TfvcChangesetSearchCriteria {

    /**
    * Alias or display name of user who made the changes
    */
    private String author;
    /**
    * Whether or not to follow renames for the given item being queried
    */
    private boolean followRenames;
    /**
    * If provided, only include changesets created after this date (string) Think of a better name for this.
    */
    private String fromDate;
    /**
    * If provided, only include changesets after this changesetID
    */
    private int fromId;
    /**
    * Whether to include the _links field on the shallow references
    */
    private boolean includeLinks;
    /**
    * Path of item to search under
    */
    private String path;
    /**
    * If provided, only include changesets created before this date (string) Think of a better name for this.
    */
    private String toDate;
    /**
    * If provided, a version descriptor for the latest change list to include
    */
    private int toId;

    /**
    * Alias or display name of user who made the changes
    */
    public String getAuthor() {
        return author;
    }

    /**
    * Alias or display name of user who made the changes
    */
    public void setAuthor(final String author) {
        this.author = author;
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
    * If provided, only include changesets created after this date (string) Think of a better name for this.
    */
    public String getFromDate() {
        return fromDate;
    }

    /**
    * If provided, only include changesets created after this date (string) Think of a better name for this.
    */
    public void setFromDate(final String fromDate) {
        this.fromDate = fromDate;
    }

    /**
    * If provided, only include changesets after this changesetID
    */
    public int getFromId() {
        return fromId;
    }

    /**
    * If provided, only include changesets after this changesetID
    */
    public void setFromId(final int fromId) {
        this.fromId = fromId;
    }

    /**
    * Whether to include the _links field on the shallow references
    */
    public boolean getIncludeLinks() {
        return includeLinks;
    }

    /**
    * Whether to include the _links field on the shallow references
    */
    public void setIncludeLinks(final boolean includeLinks) {
        this.includeLinks = includeLinks;
    }

    /**
    * Path of item to search under
    */
    public String getPath() {
        return path;
    }

    /**
    * Path of item to search under
    */
    public void setPath(final String path) {
        this.path = path;
    }

    /**
    * If provided, only include changesets created before this date (string) Think of a better name for this.
    */
    public String getToDate() {
        return toDate;
    }

    /**
    * If provided, only include changesets created before this date (string) Think of a better name for this.
    */
    public void setToDate(final String toDate) {
        this.toDate = toDate;
    }

    /**
    * If provided, a version descriptor for the latest change list to include
    */
    public int getToId() {
        return toId;
    }

    /**
    * If provided, a version descriptor for the latest change list to include
    */
    public void setToId(final int toId) {
        this.toId = toId;
    }
}
