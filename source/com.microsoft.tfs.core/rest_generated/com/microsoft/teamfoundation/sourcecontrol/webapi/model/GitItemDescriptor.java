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
public class GitItemDescriptor {

    /**
    * Path to item
    */
    private String path;
    /**
    * Specifies whether to include children (OneLevel), all descendants (Full), or None
    */
    private VersionControlRecursionType recursionLevel;
    /**
    * Version string (interpretation based on VersionType defined in subclass
    */
    private String version;
    /**
    * Version modifiers (e.g. previous)
    */
    private GitVersionOptions versionOptions;
    /**
    * How to interpret version (branch,tag,commit)
    */
    private GitVersionType versionType;

    /**
    * Path to item
    */
    public String getPath() {
        return path;
    }

    /**
    * Path to item
    */
    public void setPath(final String path) {
        this.path = path;
    }

    /**
    * Specifies whether to include children (OneLevel), all descendants (Full), or None
    */
    public VersionControlRecursionType getRecursionLevel() {
        return recursionLevel;
    }

    /**
    * Specifies whether to include children (OneLevel), all descendants (Full), or None
    */
    public void setRecursionLevel(final VersionControlRecursionType recursionLevel) {
        this.recursionLevel = recursionLevel;
    }

    /**
    * Version string (interpretation based on VersionType defined in subclass
    */
    public String getVersion() {
        return version;
    }

    /**
    * Version string (interpretation based on VersionType defined in subclass
    */
    public void setVersion(final String version) {
        this.version = version;
    }

    /**
    * Version modifiers (e.g. previous)
    */
    public GitVersionOptions getVersionOptions() {
        return versionOptions;
    }

    /**
    * Version modifiers (e.g. previous)
    */
    public void setVersionOptions(final GitVersionOptions versionOptions) {
        this.versionOptions = versionOptions;
    }

    /**
    * How to interpret version (branch,tag,commit)
    */
    public GitVersionType getVersionType() {
        return versionType;
    }

    /**
    * How to interpret version (branch,tag,commit)
    */
    public void setVersionType(final GitVersionType versionType) {
        this.versionType = versionType;
    }
}
