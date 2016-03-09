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

package com.microsoft.teamfoundation.build.webapi.model;


/** 
 */
public class ArtifactResource {

    /**
    * The type-specific resource data. For example, "#/10002/5/drop", "$/drops/5", "\\myshare\myfolder\mydrops\5"
    */
    private String data;
    /**
    * Link to the resource. This might include things like query parameters to download as a zip file
    */
    private String downloadUrl;
    /**
    * The type of the resource: File container, version control folder, UNC path, etc.
    */
    private String type;
    /**
    * Link to the resource
    */
    private String url;

    /**
    * The type-specific resource data. For example, &quot;#/10002/5/drop&quot;, &quot;$/drops/5&quot;, &quot;\\myshare\myfolder\mydrops\5&quot;
    */
    public String getData() {
        return data;
    }

    /**
    * The type-specific resource data. For example, &quot;#/10002/5/drop&quot;, &quot;$/drops/5&quot;, &quot;\\myshare\myfolder\mydrops\5&quot;
    */
    public void setData(final String data) {
        this.data = data;
    }

    /**
    * Link to the resource. This might include things like query parameters to download as a zip file
    */
    public String getDownloadUrl() {
        return downloadUrl;
    }

    /**
    * Link to the resource. This might include things like query parameters to download as a zip file
    */
    public void setDownloadUrl(final String downloadUrl) {
        this.downloadUrl = downloadUrl;
    }

    /**
    * The type of the resource: File container, version control folder, UNC path, etc.
    */
    public String getType() {
        return type;
    }

    /**
    * The type of the resource: File container, version control folder, UNC path, etc.
    */
    public void setType(final String type) {
        this.type = type;
    }

    /**
    * Link to the resource
    */
    public String getUrl() {
        return url;
    }

    /**
    * Link to the resource
    */
    public void setUrl(final String url) {
        this.url = url;
    }
}
