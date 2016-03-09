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
public class GitTargetVersionDescriptor
    extends GitVersionDescriptor {

    /**
    * Version string identifier (name of tag/branch, SHA1 of commit)
    */
    private String targetVersion;
    /**
    * Version options - Specify additional modifiers to version (e.g Previous)
    */
    private GitVersionOptions targetVersionOptions;
    /**
    * Version type (branch, tag, or commit). Determines how Id is interpreted
    */
    private GitVersionType targetVersionType;

    /**
    * Version string identifier (name of tag/branch, SHA1 of commit)
    */
    public String getTargetVersion() {
        return targetVersion;
    }

    /**
    * Version string identifier (name of tag/branch, SHA1 of commit)
    */
    public void setTargetVersion(final String targetVersion) {
        this.targetVersion = targetVersion;
    }

    /**
    * Version options - Specify additional modifiers to version (e.g Previous)
    */
    public GitVersionOptions getTargetVersionOptions() {
        return targetVersionOptions;
    }

    /**
    * Version options - Specify additional modifiers to version (e.g Previous)
    */
    public void setTargetVersionOptions(final GitVersionOptions targetVersionOptions) {
        this.targetVersionOptions = targetVersionOptions;
    }

    /**
    * Version type (branch, tag, or commit). Determines how Id is interpreted
    */
    public GitVersionType getTargetVersionType() {
        return targetVersionType;
    }

    /**
    * Version type (branch, tag, or commit). Determines how Id is interpreted
    */
    public void setTargetVersionType(final GitVersionType targetVersionType) {
        this.targetVersionType = targetVersionType;
    }
}
