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
public class GitRefUpdateResult {

    /**
    * Custom message for the result object For instance, Reason for failing.
    */
    private String customMessage;
    /**
    * Ref name
    */
    private String name;
    /**
    * New object ID
    */
    private String newObjectId;
    /**
    * Old object ID
    */
    private String oldObjectId;
    /**
    * Name of the plugin that rejected the updated.
    */
    private String rejectedBy;
    /**
    * Repository ID
    */
    private UUID repositoryId;
    /**
    * True if the ref update succeeded, false otherwise
    */
    private boolean success;
    /**
    * Status of the update from the TFS server.
    */
    private GitRefUpdateStatus updateStatus;

    /**
    * Custom message for the result object For instance, Reason for failing.
    */
    public String getCustomMessage() {
        return customMessage;
    }

    /**
    * Custom message for the result object For instance, Reason for failing.
    */
    public void setCustomMessage(final String customMessage) {
        this.customMessage = customMessage;
    }

    /**
    * Ref name
    */
    public String getName() {
        return name;
    }

    /**
    * Ref name
    */
    public void setName(final String name) {
        this.name = name;
    }

    /**
    * New object ID
    */
    public String getNewObjectId() {
        return newObjectId;
    }

    /**
    * New object ID
    */
    public void setNewObjectId(final String newObjectId) {
        this.newObjectId = newObjectId;
    }

    /**
    * Old object ID
    */
    public String getOldObjectId() {
        return oldObjectId;
    }

    /**
    * Old object ID
    */
    public void setOldObjectId(final String oldObjectId) {
        this.oldObjectId = oldObjectId;
    }

    /**
    * Name of the plugin that rejected the updated.
    */
    public String getRejectedBy() {
        return rejectedBy;
    }

    /**
    * Name of the plugin that rejected the updated.
    */
    public void setRejectedBy(final String rejectedBy) {
        this.rejectedBy = rejectedBy;
    }

    /**
    * Repository ID
    */
    public UUID getRepositoryId() {
        return repositoryId;
    }

    /**
    * Repository ID
    */
    public void setRepositoryId(final UUID repositoryId) {
        this.repositoryId = repositoryId;
    }

    /**
    * True if the ref update succeeded, false otherwise
    */
    public boolean getSuccess() {
        return success;
    }

    /**
    * True if the ref update succeeded, false otherwise
    */
    public void setSuccess(final boolean success) {
        this.success = success;
    }

    /**
    * Status of the update from the TFS server.
    */
    public GitRefUpdateStatus getUpdateStatus() {
        return updateStatus;
    }

    /**
    * Status of the update from the TFS server.
    */
    public void setUpdateStatus(final GitRefUpdateStatus updateStatus) {
        this.updateStatus = updateStatus;
    }
}
