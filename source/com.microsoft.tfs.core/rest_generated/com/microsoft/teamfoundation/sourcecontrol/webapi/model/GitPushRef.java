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
import java.util.UUID;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.microsoft.visualstudio.services.webapi.model.IdentityRef;
import com.microsoft.visualstudio.services.webapi.model.ReferenceLinks;

/** 
 */
public class GitPushRef {

    private ReferenceLinks _links;
    private Date date;
    private UUID pushCorrelationId;
    private IdentityRef pushedBy;
    private int pushId;
    private String url;

    @JsonProperty("_links")
    public ReferenceLinks getLinks() {
        return _links;
    }

    @JsonProperty("_links")
    public void setLinks(final ReferenceLinks _links) {
        this._links = _links;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(final Date date) {
        this.date = date;
    }

    public UUID getPushCorrelationId() {
        return pushCorrelationId;
    }

    public void setPushCorrelationId(final UUID pushCorrelationId) {
        this.pushCorrelationId = pushCorrelationId;
    }

    public IdentityRef getPushedBy() {
        return pushedBy;
    }

    public void setPushedBy(final IdentityRef pushedBy) {
        this.pushedBy = pushedBy;
    }

    public int getPushId() {
        return pushId;
    }

    public void setPushId(final int pushId) {
        this.pushId = pushId;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(final String url) {
        this.url = url;
    }
}
