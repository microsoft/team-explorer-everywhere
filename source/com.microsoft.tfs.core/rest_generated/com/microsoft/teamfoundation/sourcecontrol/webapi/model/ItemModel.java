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
public class ItemModel {

    private ReferenceLinks _links;
    private FileContentMetadata contentMetadata;
    private boolean isFolder;
    private boolean isSymLink;
    private String path;
    private String url;

    @JsonProperty("_links")
    public ReferenceLinks getLinks() {
        return _links;
    }

    @JsonProperty("_links")
    public void setLinks(final ReferenceLinks _links) {
        this._links = _links;
    }

    public FileContentMetadata getContentMetadata() {
        return contentMetadata;
    }

    public void setContentMetadata(final FileContentMetadata contentMetadata) {
        this.contentMetadata = contentMetadata;
    }

    public boolean getIsFolder() {
        return isFolder;
    }

    public void setIsFolder(final boolean isFolder) {
        this.isFolder = isFolder;
    }

    public boolean getIsSymLink() {
        return isSymLink;
    }

    public void setIsSymLink(final boolean isSymLink) {
        this.isSymLink = isSymLink;
    }

    public String getPath() {
        return path;
    }

    public void setPath(final String path) {
        this.path = path;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(final String url) {
        this.url = url;
    }
}
