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

import java.util.UUID;

/** 
 */
public class GitRefUpdate {

    private String name;
    private String newObjectId;
    private String oldObjectId;
    private UUID repositoryId;

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public String getNewObjectId() {
        return newObjectId;
    }

    public void setNewObjectId(final String newObjectId) {
        this.newObjectId = newObjectId;
    }

    public String getOldObjectId() {
        return oldObjectId;
    }

    public void setOldObjectId(final String oldObjectId) {
        this.oldObjectId = oldObjectId;
    }

    public UUID getRepositoryId() {
        return repositoryId;
    }

    public void setRepositoryId(final UUID repositoryId) {
        this.repositoryId = repositoryId;
    }
}
