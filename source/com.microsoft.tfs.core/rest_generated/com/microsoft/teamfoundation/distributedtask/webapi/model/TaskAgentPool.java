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

package com.microsoft.teamfoundation.distributedtask.webapi.model;

import java.util.Date;
import java.util.UUID;
import com.microsoft.visualstudio.services.webapi.model.IdentityRef;
import com.microsoft.visualstudio.services.webapi.model.PropertiesCollection;

/** 
 */
public class TaskAgentPool
    extends TaskAgentPoolReference {

    /**
    * Gets the administrators group for this agent pool.
    */
    private IdentityRef administratorsGroup;
    /**
    * Gets or sets a value indicating whether or not a queue should be automatically provisioned for each project collection or not.
    */
    private boolean autoProvision;
    /**
    * Gets the identity who created this pool. The creator of the pool is automatically added into the administrators group for the pool on creation.
    */
    private IdentityRef createdBy;
    /**
    * Gets the date/time of the pool creation.
    */
    private Date createdOn;
    /**
    * Gets the scope identifier for groups/roles which are owned by this pool.
    */
    private UUID groupScopeId;
    /**
    * Gets or sets a value indicating whether or not this pool is managed by the service.
    */
    private boolean isHosted;
    private PropertiesCollection properties;
    /**
    * Gets the service accounts group for this agent pool.
    */
    private IdentityRef serviceAccountsGroup;
    /**
    * Gets the current size of the pool.
    */
    private int size;

    /**
    * Gets the administrators group for this agent pool.
    */
    public IdentityRef getAdministratorsGroup() {
        return administratorsGroup;
    }

    /**
    * Gets the administrators group for this agent pool.
    */
    public void setAdministratorsGroup(final IdentityRef administratorsGroup) {
        this.administratorsGroup = administratorsGroup;
    }

    /**
    * Gets or sets a value indicating whether or not a queue should be automatically provisioned for each project collection or not.
    */
    public boolean getAutoProvision() {
        return autoProvision;
    }

    /**
    * Gets or sets a value indicating whether or not a queue should be automatically provisioned for each project collection or not.
    */
    public void setAutoProvision(final boolean autoProvision) {
        this.autoProvision = autoProvision;
    }

    /**
    * Gets the identity who created this pool. The creator of the pool is automatically added into the administrators group for the pool on creation.
    */
    public IdentityRef getCreatedBy() {
        return createdBy;
    }

    /**
    * Gets the identity who created this pool. The creator of the pool is automatically added into the administrators group for the pool on creation.
    */
    public void setCreatedBy(final IdentityRef createdBy) {
        this.createdBy = createdBy;
    }

    /**
    * Gets the date/time of the pool creation.
    */
    public Date getCreatedOn() {
        return createdOn;
    }

    /**
    * Gets the date/time of the pool creation.
    */
    public void setCreatedOn(final Date createdOn) {
        this.createdOn = createdOn;
    }

    /**
    * Gets the scope identifier for groups/roles which are owned by this pool.
    */
    public UUID getGroupScopeId() {
        return groupScopeId;
    }

    /**
    * Gets the scope identifier for groups/roles which are owned by this pool.
    */
    public void setGroupScopeId(final UUID groupScopeId) {
        this.groupScopeId = groupScopeId;
    }

    /**
    * Gets or sets a value indicating whether or not this pool is managed by the service.
    */
    public boolean getIsHosted() {
        return isHosted;
    }

    /**
    * Gets or sets a value indicating whether or not this pool is managed by the service.
    */
    public void setIsHosted(final boolean isHosted) {
        this.isHosted = isHosted;
    }

    public PropertiesCollection getProperties() {
        return properties;
    }

    public void setProperties(final PropertiesCollection properties) {
        this.properties = properties;
    }

    /**
    * Gets the service accounts group for this agent pool.
    */
    public IdentityRef getServiceAccountsGroup() {
        return serviceAccountsGroup;
    }

    /**
    * Gets the service accounts group for this agent pool.
    */
    public void setServiceAccountsGroup(final IdentityRef serviceAccountsGroup) {
        this.serviceAccountsGroup = serviceAccountsGroup;
    }

    /**
    * Gets the current size of the pool.
    */
    public int getSize() {
        return size;
    }

    /**
    * Gets the current size of the pool.
    */
    public void setSize(final int size) {
        this.size = size;
    }
}
