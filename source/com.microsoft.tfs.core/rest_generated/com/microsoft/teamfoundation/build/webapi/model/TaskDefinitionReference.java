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

import java.util.UUID;

/** 
 */
public class TaskDefinitionReference {

    private UUID id;
    private String versionSpec;

    public UUID getId() {
        return id;
    }

    public void setId(final UUID id) {
        this.id = id;
    }

    public String getVersionSpec() {
        return versionSpec;
    }

    public void setVersionSpec(final String versionSpec) {
        this.versionSpec = versionSpec;
    }
}
