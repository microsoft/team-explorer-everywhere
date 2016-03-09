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
 * Mapping for a workspace
 * 
 */
public class WorkspaceMapping {

    /**
    * Uri of the associated definition
    */
    private String definitionUri;
    /**
    * Depth of this mapping
    */
    private int depth;
    /**
    * local location of the definition
    */
    private String localItem;
    /**
    * type of workspace mapping
    */
    private WorkspaceMappingType mappingType;
    /**
    * Server location of the definition
    */
    private String serverItem;
    /**
    * Id of the workspace
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
    * Depth of this mapping
    */
    public int getDepth() {
        return depth;
    }

    /**
    * Depth of this mapping
    */
    public void setDepth(final int depth) {
        this.depth = depth;
    }

    /**
    * local location of the definition
    */
    public String getLocalItem() {
        return localItem;
    }

    /**
    * local location of the definition
    */
    public void setLocalItem(final String localItem) {
        this.localItem = localItem;
    }

    /**
    * type of workspace mapping
    */
    public WorkspaceMappingType getMappingType() {
        return mappingType;
    }

    /**
    * type of workspace mapping
    */
    public void setMappingType(final WorkspaceMappingType mappingType) {
        this.mappingType = mappingType;
    }

    /**
    * Server location of the definition
    */
    public String getServerItem() {
        return serverItem;
    }

    /**
    * Server location of the definition
    */
    public void setServerItem(final String serverItem) {
        this.serverItem = serverItem;
    }

    /**
    * Id of the workspace
    */
    public int getWorkspaceId() {
        return workspaceId;
    }

    /**
    * Id of the workspace
    */
    public void setWorkspaceId(final int workspaceId) {
        this.workspaceId = workspaceId;
    }
}
