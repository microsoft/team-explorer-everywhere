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


/** 
 */
public class GitTreeEntryRef {

    /**
    * Blob or tree
    */
    private GitObjectType gitObjectType;
    /**
    * Mode represented as octal string
    */
    private String mode;
    /**
    * SHA1 hash of git object
    */
    private String objectId;
    /**
    * Path relative to parent tree object
    */
    private String relativePath;
    /**
    * Size of content
    */
    private long size;
    /**
    * url to retrieve tree or blob
    */
    private String url;

    /**
    * Blob or tree
    */
    public GitObjectType getGitObjectType() {
        return gitObjectType;
    }

    /**
    * Blob or tree
    */
    public void setGitObjectType(final GitObjectType gitObjectType) {
        this.gitObjectType = gitObjectType;
    }

    /**
    * Mode represented as octal string
    */
    public String getMode() {
        return mode;
    }

    /**
    * Mode represented as octal string
    */
    public void setMode(final String mode) {
        this.mode = mode;
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
    * Path relative to parent tree object
    */
    public String getRelativePath() {
        return relativePath;
    }

    /**
    * Path relative to parent tree object
    */
    public void setRelativePath(final String relativePath) {
        this.relativePath = relativePath;
    }

    /**
    * Size of content
    */
    public long getSize() {
        return size;
    }

    /**
    * Size of content
    */
    public void setSize(final long size) {
        this.size = size;
    }

    /**
    * url to retrieve tree or blob
    */
    public String getUrl() {
        return url;
    }

    /**
    * url to retrieve tree or blob
    */
    public void setUrl(final String url) {
        this.url = url;
    }
}
