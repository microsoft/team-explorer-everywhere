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
public class GitBaseVersionDescriptor
    extends GitVersionDescriptor {

    /**
    * Version string identifier (name of tag/branch, SHA1 of commit)
    */
    private String baseVersion;
    /**
    * Version options - Specify additional modifiers to version (e.g Previous)
    */
    private GitVersionOptions baseVersionOptions;
    /**
    * Version type (branch, tag, or commit). Determines how Id is interpreted
    */
    private GitVersionType baseVersionType;

    /**
    * Version string identifier (name of tag/branch, SHA1 of commit)
    */
    public String getBaseVersion() {
        return baseVersion;
    }

    /**
    * Version string identifier (name of tag/branch, SHA1 of commit)
    */
    public void setBaseVersion(final String baseVersion) {
        this.baseVersion = baseVersion;
    }

    /**
    * Version options - Specify additional modifiers to version (e.g Previous)
    */
    public GitVersionOptions getBaseVersionOptions() {
        return baseVersionOptions;
    }

    /**
    * Version options - Specify additional modifiers to version (e.g Previous)
    */
    public void setBaseVersionOptions(final GitVersionOptions baseVersionOptions) {
        this.baseVersionOptions = baseVersionOptions;
    }

    /**
    * Version type (branch, tag, or commit). Determines how Id is interpreted
    */
    public GitVersionType getBaseVersionType() {
        return baseVersionType;
    }

    /**
    * Version type (branch, tag, or commit). Determines how Id is interpreted
    */
    public void setBaseVersionType(final GitVersionType baseVersionType) {
        this.baseVersionType = baseVersionType;
    }
}
