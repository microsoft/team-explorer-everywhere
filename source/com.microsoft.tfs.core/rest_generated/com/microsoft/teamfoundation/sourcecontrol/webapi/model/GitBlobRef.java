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
import com.microsoft.visualstudio.services.webapi.model.ReferenceLinks;

/** 
 */
public class GitBlobRef {

    private ReferenceLinks _links;
    /**
    * SHA1 hash of git object
    */
    private String objectId;
    /**
    * Size of blob content (in bytes)
    */
    private long size;
    private String url;

    @JsonProperty("_links")
    public ReferenceLinks getLinks() {
        return _links;
    }

    @JsonProperty("_links")
    public void setLinks(final ReferenceLinks _links) {
        this._links = _links;
    }

    /**
    * SHA1 hash of git object
    */
    public String getObjectId() {
        return objectId;
    }

    /**
    * SHA1 hash of git object
    */
    public void setObjectId(final String objectId) {
        this.objectId = objectId;
    }

    /**
    * Size of blob content (in bytes)
    */
    public long getSize() {
        return size;
    }

    /**
    * Size of blob content (in bytes)
    */
    public void setSize(final long size) {
        this.size = size;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(final String url) {
        this.url = url;
    }
}
