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
public class GitVersionDescriptor {

    /**
    * Version string identifier (name of tag/branch/index, SHA1 of commit)
    */
    private String version;
    /**
    * Version options - Specify additional modifiers to version (e.g Previous)
    */
    private GitVersionOptions versionOptions;
    /**
    * Version type (branch, tag, commit, or index). Determines how Id is interpreted
    */
    private GitVersionType versionType;

    /**
    * Version string identifier (name of tag/branch/index, SHA1 of commit)
    */
    public String getVersion() {
        return version;
    }

    /**
    * Version string identifier (name of tag/branch/index, SHA1 of commit)
    */
    public void setVersion(final String version) {
        this.version = version;
    }

    /**
    * Version options - Specify additional modifiers to version (e.g Previous)
    */
    public GitVersionOptions getVersionOptions() {
        return versionOptions;
    }

    /**
    * Version options - Specify additional modifiers to version (e.g Previous)
    */
    public void setVersionOptions(final GitVersionOptions versionOptions) {
        this.versionOptions = versionOptions;
    }

    /**
    * Version type (branch, tag, commit, or index). Determines how Id is interpreted
    */
    public GitVersionType getVersionType() {
        return versionType;
    }

    /**
    * Version type (branch, tag, commit, or index). Determines how Id is interpreted
    */
    public void setVersionType(final GitVersionType versionType) {
        this.versionType = versionType;
    }
}
