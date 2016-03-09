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

import java.util.UUID;

/** 
 */
public class GitPullRequestSearchCriteria {

    private UUID creatorId;
    /**
    * Whether to include the _links field on the shallow references
    */
    private boolean includeLinks;
    private UUID repositoryId;
    private UUID reviewerId;
    private String sourceRefName;
    private PullRequestStatus status;
    private String targetRefName;

    public UUID getCreatorId() {
        return creatorId;
    }

    public void setCreatorId(final UUID creatorId) {
        this.creatorId = creatorId;
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

    public UUID getRepositoryId() {
        return repositoryId;
    }

    public void setRepositoryId(final UUID repositoryId) {
        this.repositoryId = repositoryId;
    }

    public UUID getReviewerId() {
        return reviewerId;
    }

    public void setReviewerId(final UUID reviewerId) {
        this.reviewerId = reviewerId;
    }

    public String getSourceRefName() {
        return sourceRefName;
    }

    public void setSourceRefName(final String sourceRefName) {
        this.sourceRefName = sourceRefName;
    }

    public PullRequestStatus getStatus() {
        return status;
    }

    public void setStatus(final PullRequestStatus status) {
        this.status = status;
    }

    public String getTargetRefName() {
        return targetRefName;
    }

    public void setTargetRefName(final String targetRefName) {
        this.targetRefName = targetRefName;
    }
}
