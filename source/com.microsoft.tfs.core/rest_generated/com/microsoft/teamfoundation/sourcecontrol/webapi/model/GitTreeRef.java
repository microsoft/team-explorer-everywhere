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
import com.fasterxml.jackson.annotation.JsonProperty;
import com.microsoft.visualstudio.services.webapi.model.ReferenceLinks;

/** 
 */
public class GitTreeRef {

    private ReferenceLinks _links;
    /**
    * SHA1 hash of git object
    */
    private String objectId;
    /**
    * Sum of sizes of all children
    */
    private long size;
    /**
    * Blobs and trees under this tree
    */
    private List<GitTreeEntryRef> treeEntries;
    /**
    * Url to tree
    */
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
    * Sum of sizes of all children
    */
    public long getSize() {
        return size;
    }

    /**
    * Sum of sizes of all children
    */
    public void setSize(final long size) {
        this.size = size;
    }

    /**
    * Blobs and trees under this tree
    */
    public List<GitTreeEntryRef> getTreeEntries() {
        return treeEntries;
    }

    /**
    * Blobs and trees under this tree
    */
    public void setTreeEntries(final List<GitTreeEntryRef> treeEntries) {
        this.treeEntries = treeEntries;
    }

    /**
    * Url to tree
    */
    public String getUrl() {
        return url;
    }

    /**
    * Url to tree
    */
    public void setUrl(final String url) {
        this.url = url;
    }
}
