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

import java.util.List;

/** 
 */
public class GitQueryCommitsCriteria {

    /**
    * Number of entries to skip
    */
    private int $skip;
    /**
    * Maximum number of entries to retrieve
    */
    private int $top;
    /**
    * Alias or display name of the author
    */
    private String author;
    /**
    * If provided, the earliest commit in the graph to search
    */
    private GitVersionDescriptor compareVersion;
    /**
    * If true, don't include delete history entries
    */
    private boolean excludeDeletes;
    /**
    * If provided, a lower bound for filtering commits alphabetically
    */
    private String fromCommitId;
    /**
    * If provided, only include history entries created after this date (string)
    */
    private String fromDate;
    /**
    * If provided, specifies the exact commit ids of the commits to fetch. May not be combined with other parameters.
    */
    private List<String> ids;
    /**
    * Whether to include the _links field on the shallow references
    */
    private boolean includeLinks;
    /**
    * Path of item to search under
    */
    private String itemPath;
    /**
    * If provided, identifies the commit or branch to search
    */
    private GitVersionDescriptor itemVersion;
    /**
    * If provided, an upper bound for filtering commits alphabetically
    */
    private String toCommitId;
    /**
    * If provided, only include history entries created before this date (string)
    */
    private String toDate;
    /**
    * Alias or display name of the committer
    */
    private String user;

    /**
    * Number of entries to skip
    */
    public int get$skip() {
        return $skip;
    }

    /**
    * Number of entries to skip
    */
    public void set$skip(final int $skip) {
        this.$skip = $skip;
    }

    /**
    * Maximum number of entries to retrieve
    */
    public int get$top() {
        return $top;
    }

    /**
    * Maximum number of entries to retrieve
    */
    public void set$top(final int $top) {
        this.$top = $top;
    }

    /**
    * Alias or display name of the author
    */
    public String getAuthor() {
        return author;
    }

    /**
    * Alias or display name of the author
    */
    public void setAuthor(final String author) {
        this.author = author;
    }

    /**
    * If provided, the earliest commit in the graph to search
    */
    public GitVersionDescriptor getCompareVersion() {
        return compareVersion;
    }

    /**
    * If provided, the earliest commit in the graph to search
    */
    public void setCompareVersion(final GitVersionDescriptor compareVersion) {
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
    * If provided, a lower bound for filtering commits alphabetically
    */
    public String getFromCommitId() {
        return fromCommitId;
    }

    /**
    * If provided, a lower bound for filtering commits alphabetically
    */
    public void setFromCommitId(final String fromCommitId) {
        this.fromCommitId = fromCommitId;
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
    * If provided, specifies the exact commit ids of the commits to fetch. May not be combined with other parameters.
    */
    public List<String> getIds() {
        return ids;
    }

    /**
    * If provided, specifies the exact commit ids of the commits to fetch. May not be combined with other parameters.
    */
    public void setIds(final List<String> ids) {
        this.ids = ids;
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
    * If provided, identifies the commit or branch to search
    */
    public GitVersionDescriptor getItemVersion() {
        return itemVersion;
    }

    /**
    * If provided, identifies the commit or branch to search
    */
    public void setItemVersion(final GitVersionDescriptor itemVersion) {
        this.itemVersion = itemVersion;
    }

    /**
    * If provided, an upper bound for filtering commits alphabetically
    */
    public String getToCommitId() {
        return toCommitId;
    }

    /**
    * If provided, an upper bound for filtering commits alphabetically
    */
    public void setToCommitId(final String toCommitId) {
        this.toCommitId = toCommitId;
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
    * Alias or display name of the committer
    */
    public String getUser() {
        return user;
    }

    /**
    * Alias or display name of the committer
    */
    public void setUser(final String user) {
        this.user = user;
    }
}
