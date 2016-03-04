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

import com.microsoft.visualstudio.services.webapi.model.IdentityRef;

/** 
 */
public class IdentityRefWithVote
    extends IdentityRef {

    private boolean isRequired;
    private String reviewerUrl;
    private short vote;
    private IdentityRefWithVote[] votedFor;

    public boolean getIsRequired() {
        return isRequired;
    }

    public void setIsRequired(final boolean isRequired) {
        this.isRequired = isRequired;
    }

    public String getReviewerUrl() {
        return reviewerUrl;
    }

    public void setReviewerUrl(final String reviewerUrl) {
        this.reviewerUrl = reviewerUrl;
    }

    public short getVote() {
        return vote;
    }

    public void setVote(final short vote) {
        this.vote = vote;
    }

    public IdentityRefWithVote[] getVotedFor() {
        return votedFor;
    }

    public void setVotedFor(final IdentityRefWithVote[] votedFor) {
        this.votedFor = votedFor;
    }
}
