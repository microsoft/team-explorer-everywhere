// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.build;

import com.microsoft.tfs.core.clients.build.soapextensions.AgentStatus;
import com.microsoft.tfs.core.clients.build.soapextensions.ControllerStatus;

public interface IBuildServiceHost {
    /**
     * The build server for this service host.
     *
     *
     * @return
     */
    public IBuildServer getBuildServer();

    /**
     * Gets a value indicating whether or not this service host is virtual. A
     * virtual service host is dynamically allocated to machines on demand
     * rather than statically at install time.
     *
     *
     * @return
     */
    public boolean isVirtual();

    /**
     * Gets the URI for this service host
     *
     *
     * @return
     */
    public String getURI();

    /**
     * Gets or sets the name for this service host
     *
     *
     * @return
     */
    public String getName();

    public void setName(String value);

    /**
     * Gets or sets the base URL for this service host
     *
     *
     * @return
     */
    public String getBaseURL();

    public void setBaseURL(String value);

    /**
     * Gets the message queue address. This field is for system use only.
     *
     *
     * @return
     */
    public String getMessageQueueURL();

    /**
     * Gets or sets a value indicating if the service host requires client
     * certificates for incoming calls.
     *
     *
     * @return
     */
    public boolean isRequireClientCertificates();

    public void setRequireClientCertificates(boolean value);

    /**
     * Gets the build controller associated with this service host
     *
     *
     * @return
     */
    public IBuildController getController();

    /**
     * Gets the list of agents associated with this service host
     *
     *
     * @return
     */
    public IBuildAgent[] getAgents();

    /**
     * Creates a build controller associated with the current service host. Only
     * one build controller may be associated with a given service host.
     *
     *
     * @param name
     *        The name by which the controller should be referenced
     * @return An IBuildController implementation
     */
    public IBuildController createBuildController(String name);

    /**
     * Creates a build agent associated with the current service host and adds
     * it to the list of agents.
     *
     *
     * @param name
     *        The name by which the agent should be referenced
     * @param buildDirectory
     * @return An IBuildAgent implementation
     */
    public IBuildAgent createBuildAgent(String name, String buildDirectory);

    /**
     * Creates a build agent associated with the current service host and adds
     * it to the list of agents.
     *
     *
     * @param name
     *        The name by which the agent should be referenced
     * @param buildDirectory
     *        The build directory to use when running builds on the agent
     * @param buildController
     *        The build controller to associate the agent with
     * @return An IBuildAgent implementation
     */
    public IBuildAgent createBuildAgent(String name, String buildDirectory, IBuildController buildController);

    /**
     * Deletes the service host along with all associated controllers and
     * agents.
     *
     *
     */
    public void delete();

    /**
     * Saves any changes made since the last time the save method was called.
     *
     *
     */
    public void save();

    /**
     * Delete BuildController
     *
     *
     */
    public void deleteBuildController();

    /**
     * Delete BuildAgent
     *
     *
     * @param agent
     * @return
     */
    public boolean deleteBuildAgent(IBuildAgent agent);

    /**
     * Find BuildAgent
     *
     *
     * @param controller
     * @param name
     * @return
     */
    public IBuildAgent findBuildAgent(String controller, String name);

    /**
     * Takes ownership of the service host.
     *
     *
     */
    public void takeOwnership();

    /**
     * Releases ownership of the service host.
     *
     *
     */
    public void releaseOwnership();

    /**
     * Set build controller status
     *
     *
     * @param status
     * @param message
     */
    public void setBuildControllerStatus(ControllerStatus status, String message);

    /**
     * Set build agent status
     *
     *
     * @param agent
     * @param status
     * @param message
     */
    public void setBuildAgentStatus(IBuildAgent agent, AgentStatus status, String message);

    /**
     * Provides a mechanism by which agents may be added to the list during
     * deserialization.
     *
     *
     * @param agent
     */
    public void addBuildAgent(IBuildAgent agent);

    /**
     * Provides a mechanism by which a controller may be added to this service
     * host during deserialization.
     *
     *
     * @param controller
     */
    public void setBuildController(IBuildController controller);
}
