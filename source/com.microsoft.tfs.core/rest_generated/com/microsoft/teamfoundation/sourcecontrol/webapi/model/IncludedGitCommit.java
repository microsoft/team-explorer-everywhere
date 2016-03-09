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

import java.util.Date;
import java.util.List;
import java.util.UUID;

/** 
 */
public class IncludedGitCommit {

    private String commitId;
    private Date commitTime;
    private List<String> parentCommitIds;
    private UUID repositoryId;

    public String getCommitId() {
        return commitId;
    }

    public void setCommitId(final String commitId) {
        this.commitId = commitId;
    }

    public Date getCommitTime() {
        return commitTime;
    }

    public void setCommitTime(final Date commitTime) {
        this.commitTime = commitTime;
    }

    public List<String> getParentCommitIds() {
        return parentCommitIds;
    }

    public void setParentCommitIds(final List<String> parentCommitIds) {
        this.parentCommitIds = parentCommitIds;
    }

    public UUID getRepositoryId() {
        return repositoryId;
    }

    public void setRepositoryId(final UUID repositoryId) {
        this.repositoryId = repositoryId;
    }
}
