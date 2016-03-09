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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.microsoft.visualstudio.services.webapi.model.IdentityRef;
import com.microsoft.visualstudio.services.webapi.model.ReferenceLinks;

/** 
 */
public class GitRef {

    private ReferenceLinks _links;
    private IdentityRef isLockedBy;
    private String name;
    private String objectId;
    private String url;

    @JsonProperty("_links")
    public ReferenceLinks getLinks() {
        return _links;
    }

    @JsonProperty("_links")
    public void setLinks(final ReferenceLinks _links) {
        this._links = _links;
    }

    public IdentityRef getIsLockedBy() {
        return isLockedBy;
    }

    public void setIsLockedBy(final IdentityRef isLockedBy) {
        this.isLockedBy = isLockedBy;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public String getObjectId() {
        return objectId;
    }

    public void setObjectId(final String objectId) {
        this.objectId = objectId;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(final String url) {
        this.url = url;
    }
}
