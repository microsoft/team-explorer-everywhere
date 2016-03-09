// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.build;

import java.util.Calendar;
import java.util.Map;

import com.microsoft.tfs.core.clients.build.soapextensions.AgentStatus;

public interface IBuildAgent extends IBuildGroupItem {
    /**
     * The service host in which this build agent resides.
     *
     *
     * @return
     */
    public IBuildServiceHost getServiceHost();

    /**
     * The build controller that owns this build agent.
     *
     *
     * @return
     */
    public IBuildController getController();

    public void setController(IBuildController value);

    /**
     * The working directory for the build agent.
     *
     *
     * @return
     */
    public String getBuildDirectory();

    public void setBuildDirectory(String value);

    /**
     * The description of the build agent.
     *
     *
     * @return
     */
    public String getDescription();

    public void setDescription(String value);

    /**
     * The status of the build agent - when Offline it will not be reached by
     * AT, when Unavailable AT will attempt to fix, when Available it's working.
     *
     *
     * @return
     */
    public AgentStatus getStatus();

    public void setStatus(AgentStatus value);

    /**
     * The Enabled/Disabled flag of the build agent - when not Enabled, the
     * build agent cannot queue or start any new builds.
     *
     *
     * @return
     */
    public boolean isEnabled();

    public void setEnabled(boolean value);

    /**
     * A displayable message from the server regarding the agent's status. (May
     * be empty)
     *
     *
     * @return
     */
    public String getStatusMessage();

    public void setStatusMessage(String value);

    /**
     * The URL which may be used to communicate with the build agent.
     *
     *
     * @return
     */
    public String getURL();

    /**
     * Gets the message queue address. This field is for system use only.
     *
     *
     * @return
     */
    public String getMessageQueueURL();

    /**
     * The tags defined for this build agent.
     *
     *
     * @return
     */
    public String[] getTags();

    public void setTags(String[] value);

    /**
     * The date and time at which this agent was created.
     *
     *
     * @return
     */
    public Calendar getDateCreated();

    /**
     * The date and time at which this agent was updated last.
     *
     *
     * @return
     */
    public Calendar getDateUpdated();

    /**
     * Gets a value indicating whether or not this agent is currently in use by
     * a build.
     *
     *
     * @return
     */
    public boolean isReserved();

    /**
     * Gets the identifier of the build which is currently using this agent or
     * null if not currently in use.
     *
     *
     * @return
     */
    public String getReservedForBuild();

    /**
     * Attached properties
     *
     *
     * @return
     */
    public Map<String, Object> getAttachedProperties();

    /**
     * Deletes the build agent from the build server.
     *
     *
     */
    public void delete();

    /**
     * Returns the BuildDirectory for the build agent with all variable
     * expansions evaluated according to the given build definition.
     *
     *
     * @param buildDefinition
     * @return
     */
    public String getExpandedBuildDirectory(IBuildDefinition buildDefinition);

    /**
     * Saves any changes made to the build agent to the build server.
     *
     *
     */
    public void save();

    /**
     * Refresh this build agent by getting updated property values from the
     * server.
     *
     *
     * @param propertyNameFilters
     *        The property names to get.
     */
    public void refresh(String[] propertyNameFilters);
}
