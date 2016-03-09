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
public class TfvcVersionDescriptor {

    private String version;
    private TfvcVersionOption versionOption;
    private TfvcVersionType versionType;

    public String getVersion() {
        return version;
    }

    public void setVersion(final String version) {
        this.version = version;
    }

    public TfvcVersionOption getVersionOption() {
        return versionOption;
    }

    public void setVersionOption(final TfvcVersionOption versionOption) {
        this.versionOption = versionOption;
    }

    public TfvcVersionType getVersionType() {
        return versionType;
    }

    public void setVersionType(final TfvcVersionType versionType) {
        this.versionType = versionType;
    }
}
