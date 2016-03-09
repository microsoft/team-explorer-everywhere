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
import com.fasterxml.jackson.annotation.JsonProperty;
import com.microsoft.visualstudio.services.webapi.model.IdentityRef;
import com.microsoft.visualstudio.services.webapi.model.ReferenceLinks;

/** 
 */
public class TfvcChangesetRef {

    private ReferenceLinks _links;
    private IdentityRef author;
    private int changesetId;
    private IdentityRef checkedInBy;
    private String comment;
    private boolean commentTruncated;
    private Date createdDate;
    private String url;

    @JsonProperty("_links")
    public ReferenceLinks getLinks() {
        return _links;
    }

    @JsonProperty("_links")
    public void setLinks(final ReferenceLinks _links) {
        this._links = _links;
    }

    public IdentityRef getAuthor() {
        return author;
    }

    public void setAuthor(final IdentityRef author) {
        this.author = author;
    }

    public int getChangesetId() {
        return changesetId;
    }

    public void setChangesetId(final int changesetId) {
        this.changesetId = changesetId;
    }

    public IdentityRef getCheckedInBy() {
        return checkedInBy;
    }

    public void setCheckedInBy(final IdentityRef checkedInBy) {
        this.checkedInBy = checkedInBy;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(final String comment) {
        this.comment = comment;
    }

    public boolean getCommentTruncated() {
        return commentTruncated;
    }

    public void setCommentTruncated(final boolean commentTruncated) {
        this.commentTruncated = commentTruncated;
    }

    public Date getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(final Date createdDate) {
        this.createdDate = createdDate;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(final String url) {
        this.url = url;
    }
}
