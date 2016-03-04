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
import java.util.HashMap;
import com.microsoft.visualstudio.services.webapi.model.PropertiesCollection;

/** 
 */
public class TaskAgent
    extends TaskAgentReference {

    /**
    * Gets the date on which this agent was created.
    */
    private Date createdOn;
    /**
    * Gets or sets a value indicating whether or not this agent should be enabled for job execution.
    */
    private boolean enabled;
    /**
    * Gets or sets the maximum job parallelism allowed on this host.
    */
    private int maxParallelism;
    private PropertiesCollection properties;
    /**
    * Gets the current connectivity status of the agent.
    */
    private TaskAgentStatus status;
    /**
    * Gets the date on which the last connectivity status change occurred.
    */
    private Date statusChangedOn;
    private HashMap<String,String> systemCapabilities;
    private HashMap<String,String> userCapabilities;

    /**
    * Gets the date on which this agent was created.
    */
    public Date getCreatedOn() {
        return createdOn;
    }

    /**
    * Gets the date on which this agent was created.
    */
    public void setCreatedOn(final Date createdOn) {
        this.createdOn = createdOn;
    }

    /**
    * Gets or sets a value indicating whether or not this agent should be enabled for job execution.
    */
    public boolean getEnabled() {
        return enabled;
    }

    /**
    * Gets or sets a value indicating whether or not this agent should be enabled for job execution.
    */
    public void setEnabled(final boolean enabled) {
        this.enabled = enabled;
    }

    /**
    * Gets or sets the maximum job parallelism allowed on this host.
    */
    public int getMaxParallelism() {
        return maxParallelism;
    }

    /**
    * Gets or sets the maximum job parallelism allowed on this host.
    */
    public void setMaxParallelism(final int maxParallelism) {
        this.maxParallelism = maxParallelism;
    }

    public PropertiesCollection getProperties() {
        return properties;
    }

    public void setProperties(final PropertiesCollection properties) {
        this.properties = properties;
    }

    /**
    * Gets the current connectivity status of the agent.
    */
    public TaskAgentStatus getStatus() {
        return status;
    }

    /**
    * Gets the current connectivity status of the agent.
    */
    public void setStatus(final TaskAgentStatus status) {
        this.status = status;
    }

    /**
    * Gets the date on which the last connectivity status change occurred.
    */
    public Date getStatusChangedOn() {
        return statusChangedOn;
    }

    /**
    * Gets the date on which the last connectivity status change occurred.
    */
    public void setStatusChangedOn(final Date statusChangedOn) {
        this.statusChangedOn = statusChangedOn;
    }

    public HashMap<String,String> getSystemCapabilities() {
        return systemCapabilities;
    }

    public void setSystemCapabilities(final HashMap<String,String> systemCapabilities) {
        this.systemCapabilities = systemCapabilities;
    }

    public HashMap<String,String> getUserCapabilities() {
        return userCapabilities;
    }

    public void setUserCapabilities(final HashMap<String,String> userCapabilities) {
        this.userCapabilities = userCapabilities;
    }
}
