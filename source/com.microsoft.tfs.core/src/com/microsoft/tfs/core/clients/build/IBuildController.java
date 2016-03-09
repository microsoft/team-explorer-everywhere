// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.build;

import java.util.Calendar;
import java.util.Map;

import com.microsoft.tfs.core.clients.build.soapextensions.ControllerStatus;

public interface IBuildController {
    /**
     * The service host in which this controller resides.
     *
     *
     * @return
     */
    public IBuildServiceHost getServiceHost();

    /**
     * The build agents owned by this controller.
     *
     *
     * @return
     */
    public IBuildAgent[] getAgents();

    /**
     * The version control path where custom assemblies are stored.
     *
     *
     * @return
     */
    public String getCustomAssemblyPath();

    public void setCustomAssemblyPath(String value);

    /**
     * The description of the build controller.
     *
     *
     * @return
     */
    public String getDescription();

    public void setDescription(String value);

    /**
     * The name of the build controller.
     *
     *
     * @return
     */
    public String getName();

    public void setName(String value);

    /**
     * The maximum number of builds that may run concurrently on this
     * controller.
     *
     *
     * @return
     */
    public int getMaxConcurrentBuilds();

    public void setMaxConcurrentBuilds(int value);

    /**
     * The current queue count (all queue statuses included) for the controller.
     *
     *
     * @return
     */
    public int getQueueCount();

    /**
     * The status of the build controller - when Offline it will not be reached
     * by AT, when Unavailable AT will attempt to fix, when Available it's
     * working.
     *
     *
     * @return
     */
    public ControllerStatus getStatus();

    public void setStatus(ControllerStatus value);

    /**
     * The Enabled/Disabled flag of the build controller - when not Enabled, the
     * build controller cannot queue or start any new builds.
     *
     *
     * @return
     */
    public boolean isEnabled();

    public void setEnabled(boolean value);

    /**
     * A displayable message from the server regarding the controller's status.
     * (May be empty)
     *
     *
     * @return
     */
    public String getStatusMessage();

    public void setStatusMessage(String value);

    /**
     * Attached properties
     *
     *
     * @return
     */
    public Map<String, Object> getAttachedProperties();

    /**
     * The union of the tags for all the controller's agents.
     *
     *
     * @return
     */
    public String[] getTags();

    /**
     * The Uri of the build controller.
     *
     *
     * @return
     */
    public String getURI();

    /**
     * The URL of the build controller.
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
     * The date and time at which this controller was created.
     *
     *
     * @return
     */
    public Calendar getDateCreated();

    /**
     * The date and time at which this controller was updated last.
     *
     *
     * @return
     */
    public Calendar getDateUpdated();

    /**
     * Adds a build agent to this controller.
     *
     *
     * @param agent
     *        The agent to be added.
     */
    public void addBuildAgent(IBuildAgent agent);

    /**
     * Deletes the build controller.
     *
     *
     */
    public void delete();

    /**
     * Refreshes the build controller by getting current property values from
     * the build server.
     *
     *
     * @param refreshAgentList
     *        If true, agents are requested from the server and the list of
     *        agents is replaced
     */
    public void refresh(boolean refreshAgentList);

    /**
     * Refreshes the build controller by getting current property values from
     * the build server.
     *
     *
     * @param propertyNameFilters
     *        The property names to get.
     * @param refreshAgentList
     *        If true, agents are requested from the server and the list of
     *        agents is replaced.
     */
    public void refresh(String[] propertyNameFilters, boolean refreshAgentList);

    /**
     * Removes a build agent from this controller.
     *
     *
     * @param agent
     *        The agent to be removed.
     */
    public void removeBuildAgent(IBuildAgent agent);

    /**
     * Saves any changes made to the build controller to the build server.
     *
     *
     */
    public void save();
}
