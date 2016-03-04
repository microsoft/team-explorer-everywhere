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

import java.util.Date;
import java.util.List;

/** 
 */
public class WorkspaceTemplate {

    /**
    * Uri of the associated definition
    */
    private String definitionUri;
    /**
    * The identity that last modified this template
    */
    private String lastModifiedBy;
    /**
    * The last time this template was modified
    */
    private Date lastModifiedDate;
    /**
    * List of workspace mappings
    */
    private List<WorkspaceMapping> mappings;
    /**
    * Id of the workspace for this template
    */
    private int workspaceId;

    /**
    * Uri of the associated definition
    */
    public String getDefinitionUri() {
        return definitionUri;
    }

    /**
    * Uri of the associated definition
    */
    public void setDefinitionUri(final String definitionUri) {
        this.definitionUri = definitionUri;
    }

    /**
    * The identity that last modified this template
    */
    public String getLastModifiedBy() {
        return lastModifiedBy;
    }

    /**
    * The identity that last modified this template
    */
    public void setLastModifiedBy(final String lastModifiedBy) {
        this.lastModifiedBy = lastModifiedBy;
    }

    /**
    * The last time this template was modified
    */
    public Date getLastModifiedDate() {
        return lastModifiedDate;
    }

    /**
    * The last time this template was modified
    */
    public void setLastModifiedDate(final Date lastModifiedDate) {
        this.lastModifiedDate = lastModifiedDate;
    }

    /**
    * List of workspace mappings
    */
    public List<WorkspaceMapping> getMappings() {
        return mappings;
    }

    /**
    * List of workspace mappings
    */
    public void setMappings(final List<WorkspaceMapping> mappings) {
        this.mappings = mappings;
    }

    /**
    * Id of the workspace for this template
    */
    public int getWorkspaceId() {
        return workspaceId;
    }

    /**
    * Id of the workspace for this template
    */
    public void setWorkspaceId(final int workspaceId) {
        this.workspaceId = workspaceId;
    }
}
