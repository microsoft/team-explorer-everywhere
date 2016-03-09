// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.build.internal.soapextensions;

import java.util.Calendar;
import java.util.Set;
import java.util.TreeSet;

import com.microsoft.tfs.core.clients.build.BuildConstants;
import com.microsoft.tfs.core.clients.build.IBuildAgent;
import com.microsoft.tfs.core.clients.build.IBuildController;
import com.microsoft.tfs.core.clients.build.IBuildDefinition;
import com.microsoft.tfs.core.clients.build.IBuildServer;
import com.microsoft.tfs.core.clients.build.IBuildServiceHost;
import com.microsoft.tfs.core.clients.build.flags.BuildAgentUpdate;
import com.microsoft.tfs.core.clients.build.soapextensions.AgentStatus;
import com.microsoft.tfs.core.clients.build.utils.BuildPath;
import com.microsoft.tfs.core.clients.versioncontrol.path.LocalPath;
import com.microsoft.tfs.core.exceptions.NotSupportedException;
import com.microsoft.tfs.core.internal.wrappers.WebServiceObjectWrapper;
import com.microsoft.tfs.core.internal.wrappers.WrapperUtils;
import com.microsoft.tfs.util.datetime.DotNETDate;

import ms.tfs.build.buildservice._04._BuildAgent;
import ms.tfs.build.buildservice._04._PropertyValue;

public class BuildAgent extends WebServiceObjectWrapper implements IBuildAgent {
    private IBuildController controller;
    private IBuildServiceHost serviceHost;
    private BuildAgentUpdateOptions lastSnapshot;
    private final Object lockProperties = new Object();
    private AttachedPropertyDictionary attachedProperties;

    private BuildAgent(final IBuildServiceHost serviceHost) {
        super(new _BuildAgent());

        this.serviceHost = serviceHost;

        final _BuildAgent _o = getWebServiceObject();
        _o.setDateCreated(DotNETDate.MIN_CALENDAR);
        _o.setDateUpdated(DotNETDate.MIN_CALENDAR);
        _o.setStatus(AgentStatus.UNAVAILABLE.getWebServiceObject());
        _o.setTags(new String[0]);
        _o.setProperties(new _PropertyValue[0]);
    }

    public BuildAgent(final IBuildServiceHost serviceHost, final _BuildAgent webServiceObject) {
        super(webServiceObject);

        this.serviceHost = serviceHost;
    }

    public BuildAgent(final IBuildServiceHost serviceHost, final String name, final String buildDirectory) {
        this(serviceHost);

        final _BuildAgent _o = getWebServiceObject();

        _o.setName(name);
        _o.setBuildDirectory(buildDirectory);

        // We need the snapshot to be a non-null value for locking purposes, so
        // go ahead and initialize it here. The snapshot will not be used for
        // anything until the agent is saved to the server for the first time
        // and has a URI, so this should not be a big deal.
        this.lastSnapshot = getSnapshot();

        _o.setStatus(AgentStatus.OFFLINE.getWebServiceObject());
        _o.setEnabled(true);
    }

    public BuildAgent(final IBuildServiceHost serviceHost, final BuildAgent2010 agent) {
        this(serviceHost);

        final _BuildAgent _o = getWebServiceObject();
        _o.setBuildDirectory(agent.getBuildDirectory());
        _o.setControllerUri(agent.getControllerURI());
        _o.setDateCreated(agent.getDateCreated());
        _o.setDateUpdated(agent.getDateUpdated());
        _o.setDescription(agent.getDescription());
        _o.setEnabled(agent.isEnabled());
        _o.setName(agent.getName());
        _o.setReservedForBuild(agent.getReservedForBuild());
        _o.setServiceHostUri(agent.getServiceHostURI());
        _o.setStatus(TFS2010Helper.convert(agent.getStatus()).getWebServiceObject());
        _o.setStatusMessage(agent.getStatusMessage());
        _o.setTags(agent.getTags());
        _o.setUri(agent.getURI());
        _o.setUrl(agent.getURL());
    }

    public _BuildAgent getWebServiceObject() {
        return (_BuildAgent) this.webServiceObject;
    }

    /**
     * Gets or sets the build directory. Environment variables of the form
     * $(variable) are allowed. {@inheritDoc}
     */
    @Override
    public String getBuildDirectory() {
        return getWebServiceObject().getBuildDirectory();
    }

    @Override
    public void setBuildDirectory(final String value) {
        getWebServiceObject().setBuildDirectory(value);
    }

    public String getControllerURI() {
        return getWebServiceObject().getControllerUri();
    }

    @Override
    public Calendar getDateCreated() {
        return getWebServiceObject().getDateCreated();
    }

    /**
     * Gets the date this build agent was last updated. This field is read-only.
     * {@inheritDoc}
     */
    @Override
    public Calendar getDateUpdated() {
        return getWebServiceObject().getDateUpdated();
    }

    /**
     * Gets or sets the description. {@inheritDoc}
     */
    @Override
    public String getDescription() {
        return getWebServiceObject().getDescription();
    }

    @Override
    public void setDescription(final String value) {
        getWebServiceObject().setDescription(value);
    }

    /**
     * Gets or sets a value indicating the enabled state. {@inheritDoc}
     */
    @Override
    public boolean isEnabled() {
        return getWebServiceObject().isEnabled();
    }

    @Override
    public void setEnabled(final boolean value) {
        getWebServiceObject().setEnabled(value);
    }

    /**
     * Gets the attached property values.
     *
     *
     * @return
     */
    public PropertyValue[] getInternalProperties() {
        return (PropertyValue[]) WrapperUtils.wrap(PropertyValue.class, getWebServiceObject().getProperties());
    }

    public void setInternalProperties(final PropertyValue[] value) {
        getWebServiceObject().setProperties((_PropertyValue[]) WrapperUtils.unwrap(_PropertyValue.class, value));
    }

    /**
     * Gets the message queue address. This field is read-only. {@inheritDoc}
     */
    @Override
    public String getMessageQueueURL() {
        return getWebServiceObject().getMessageQueueUrl();
    }

    /**
     * Gets or sets the display name. {@inheritDoc}
     */
    @Override
    public String getName() {
        return getWebServiceObject().getName();
    }

    @Override
    public void setName(final String value) {
        getWebServiceObject().setName(value);
    }

    /**
     * Gets the URI of the build for which this build agent is currently
     * reserved. This field is read-only. {@inheritDoc}
     */
    @Override
    public String getReservedForBuild() {
        return getWebServiceObject().getReservedForBuild();
    }

    public void setReservedForBuild(final String value) {
        getWebServiceObject().setReservedForBuild(value);
    }

    /**
     * Gets or sets the service host URI.
     *
     *
     * @return
     */
    public String getServiceHostURI() {
        return getWebServiceObject().getServiceHostUri();
    }

    /**
     * Gets or sets the status. This field is reserved for system use only.
     * {@inheritDoc}
     */
    @Override
    public AgentStatus getStatus() {
        return AgentStatus.fromWebServiceObject(getWebServiceObject().getStatus());
    }

    @Override
    public void setStatus(final AgentStatus value) {
        getWebServiceObject().setStatus(value.getWebServiceObject());
    }

    /**
     * Gets or sets the status message. This field is reserved for system use
     * only. {@inheritDoc}
     */
    @Override
    public String getStatusMessage() {
        return getWebServiceObject().getStatusMessage();
    }

    @Override
    public void setStatusMessage(final String value) {
        getWebServiceObject().setStatusMessage(value);
    }

    /**
     * Gets the URI. This field is read-only. {@inheritDoc}
     */
    @Override
    public String getURI() {
        return getWebServiceObject().getUri();
    }

    public void setURI(final String value) {
        getWebServiceObject().setUri(value);
    }

    /**
     * Gets the physical address for the remote service. This field is
     * read-only. {@inheritDoc}
     */
    @Override
    public String getURL() {
        return getWebServiceObject().getUrl();
    }

    public void setURL(final String value) {
        getWebServiceObject().setUrl(value);
    }

    /**
     * The service host to which this build agent belongs. {@inheritDoc}
     */
    @Override
    public IBuildServiceHost getServiceHost() {
        return serviceHost;
    }

    public void setServiceHost(final IBuildServiceHost value) {
        serviceHost = value;

        if (serviceHost != null) {
            getWebServiceObject().setServiceHostUri(serviceHost.getURI());
        } else {
            getWebServiceObject().setServiceHostUri(null);
        }
    }

    /**
     * The build controller that owns this build agent. {@inheritDoc}
     */
    @Override
    public IBuildController getController() {
        return controller;
    }

    @Override
    public void setController(final IBuildController value) {
        // If the URI is null then there is no good way to tell whether or not
        // it is the same controller. We go ahead and remove/add the agent from
        // the controller due to this fact.
        if (controller != value) {
            final IBuildController oldController = controller;

            // We need to assign this variable here to remove the possibility of
            // an infinite loop when calling the RemoveBuildAgent on the
            // previous controller below.
            controller = value;

            if (oldController != null) {
                oldController.removeBuildAgent(this);
            }

            if (value == null) {
                getWebServiceObject().setControllerUri(null);
            } else {
                // It is very important that we assign the controller variable
                // before calling AddBuildAgent to ensure that our first
                // ReferenceEquals check above removes the possibility of an
                // infinite loop. This is critical since the implementation of
                // AddBuildAgent will call back into this setter method.
                // Although it may appear odd, we also need to assign this
                // variable again to ensure it has the correct value.
                controller = value;
                controller.addBuildAgent(this);
                getWebServiceObject().setControllerUri(controller.getURI());
            }
        }
    }

    /**
     * Attached properties {@inheritDoc}
     */
    @Override
    public AttachedPropertyDictionary getAttachedProperties() {
        synchronized (lockProperties) {
            if (attachedProperties == null) {
                attachedProperties = new AttachedPropertyDictionary();
            }

            return attachedProperties;
        }
    }

    public void setAttachedProperties(final AttachedPropertyDictionary value) {
        synchronized (lockProperties) {
            attachedProperties = value;
        }
    }

    /**
     * The tags defined for this build agent. {@inheritDoc}
     */
    @Override
    public String[] getTags() {
        return getWebServiceObject().getTags();
    }

    @Override
    public void setTags(final String[] value) {
        getWebServiceObject().setTags(value);
    }

    /**
     * Gets a value indicating whether or not this agent is currently in use by
     * a build. {@inheritDoc}
     */
    @Override
    public boolean isReserved() {
        return getWebServiceObject().getReservedForBuild() != null;
    }

    /**
     * Compares two snapshots of an agent and determines the fields that are
     * different between the two.
     *
     *
     * @param originalValues
     *        The original values for comparison
     * @param modifiedValues
     *        The new modified values for comparison
     * @return A BuildAgentUpdate enumeration with all required field updates
     */
    private BuildAgentUpdate compareSnapshots(
        final BuildAgentUpdateOptions originalValues,
        final BuildAgentUpdateOptions modifiedValues) {
        BuildAgentUpdate result = BuildAgentUpdate.NONE;

        if (!LocalPath.equals(originalValues.getBuildDirectory(), modifiedValues.getBuildDirectory())) {
            result = result.combine(BuildAgentUpdate.BUILD_DIRECTORY);
        }
        if (!originalValues.getControllerURI().equals(modifiedValues.getControllerURI())) {
            result = result.combine(BuildAgentUpdate.CONTROLLER_URI);
        }
        if (!originalValues.getDescription().equals(modifiedValues.getDescription())) {
            result = result.combine(BuildAgentUpdate.DESCRIPTION);
        }
        if (!originalValues.getName().equals(modifiedValues.getName())) {
            result = result.combine(BuildAgentUpdate.NAME);
        }
        if (!originalValues.getStatus().equals(modifiedValues.getStatus())) {
            result = result.combine(BuildAgentUpdate.STATUS);
        }
        if (originalValues.isEnabled() != modifiedValues.isEnabled()) {
            result = result.combine(BuildAgentUpdate.ENABLED);
        }
        if (!originalValues.getStatusMessage().equals(modifiedValues.getStatusMessage())) {
            result = result.combine(BuildAgentUpdate.STATUS_MESSAGE);
        }

        if (modifiedValues.getAttachedProperties().length > 0) {
            result = result.combine(BuildAgentUpdate.ATTACHED_PROPERTIES);
        }

        // The easy way out of determining the tags is to see if the list
        // lengths has changed. If the length has not changed then we need to do
        // the more expensive comparison by intersecting the two lists and
        // determining if there are any values that are missing on either side
        // of the intersection.
        if (tagsDiffer(originalValues.getTags(), modifiedValues.getTags())) {
            result = result.combine(BuildAgentUpdate.TAGS);
        }
        return result;
    }

    /**
     * Saves a snapshot of the object's current properties.
     *
     *
     * @return
     */
    private BuildAgentUpdateOptions getSnapshot() {
        final BuildAgentUpdateOptions result = new BuildAgentUpdateOptions();

        result.setBuildDirectory(getBuildDirectory());
        result.setControllerURI(getControllerURI());
        result.setDescription(getDescription());
        result.setName(getName());
        result.setStatus(getStatus());
        result.setEnabled(isEnabled());
        result.setStatusMessage(getStatusMessage());
        result.setTags(getTags());
        result.setURI(getURI());
        result.setAttachedProperties(getAttachedProperties().getChangedProperties());

        return result;
    }

    /**
     * Get the current update options for the BuildAgent for use in bulk
     * updates. NOTE: This is not a threadsafe operation, unlike the instance
     * Save method.
     *
     *
     * @return
     */
    public BuildAgentUpdateOptions getUpdateOptions() {
        BuildAgentUpdateOptions currentSnapshot;

        synchronized (lastSnapshot) {
            // Take a snapshot of the properties
            currentSnapshot = getSnapshot();

            // Update the fields of the current snapshot
            currentSnapshot.setFields(compareSnapshots(lastSnapshot, currentSnapshot));
        }

        return currentSnapshot;
    }

    /**
     * Sets the last update options for the BuildAgent after a successful bulk
     * update. NOTE: This is not a threadsafe operation, unlike the instance
     * Save method.
     *
     *
     * @param snapshot
     */
    public void setUpdateOptions(final BuildAgentUpdateOptions snapshot) {
        synchronized (lastSnapshot) {
            lastSnapshot = snapshot;
        }

        // Since we only call this method after Save we clear the delta to start
        // collecting it again
        attachedProperties.clearChangedProperties();
    }

    @Override
    public String getExpandedBuildDirectory(final IBuildDefinition definition) {
        throw new NotSupportedException();
    }

    /**
     * Delete this build agent. {@inheritDoc}
     */
    @Override
    public void delete() {
        if (getWebServiceObject().getUri() != null) {
            serviceHost.getBuildServer().deleteBuildAgents(new String[] {
                getURI()
            });
            if (controller != null) {
                // Remove the agent from the contoller's agents list
                controller.removeBuildAgent(this);
            }
        }
    }

    /**
     * Refresh this build agent by getting updated property values from the
     * server. {@inheritDoc}
     */
    @Override
    public void refresh() {
        if (getURI() != null) {
            copy(getServiceHost().getBuildServer().getBuildAgent(getURI()));
        }
    }

    /**
     * Refresh this build agent by getting updated property values from the
     * server. {@inheritDoc}
     */
    @Override
    public void refresh(final String[] propertyNameFilters) {
        if (getURI() != null) {
            copy(serviceHost.getBuildServer().getBuildAgent(getURI(), propertyNameFilters));
        }
    }

    /**
     * Copy all property values from the argument into this agent.
     *
     *
     * @param agent
     *        The agent whose values are copied.
     */
    public void copy(final IBuildAgent agent) {
        synchronized (lastSnapshot) {
            if (agent != null) {
                if (controller != null && controller.getURI().equals(agent.getController().getURI())) {
                    // Copy all the agent controller properties leaving the
                    // reference the same
                    ((BuildController) controller).copy(agent.getController(), false);
                } else {
                    // Controller has been changed; we cannot possibly keep this
                    // reference...
                    controller = agent.getController();
                }

                setBuildDirectory(agent.getBuildDirectory());
                setDescription(agent.getDescription());
                setName(agent.getName());
                setReservedForBuild(agent.getReservedForBuild());
                setStatus(agent.getStatus());
                setEnabled(agent.isEnabled());
                setStatusMessage(agent.getStatusMessage());
                setTags(agent.getTags());
                setAttachedProperties(((BuildAgent) agent).getAttachedProperties());

                // Set the last snapshot by initializing it from the values
                lastSnapshot = getSnapshot();
            }
        }
    }

    public void prepareToSave() {
        // TODO: NYI
    }

    /**
     * Save any changes to this build agent to the TFS Build server.
     * {@inheritDoc}
     */
    @Override
    public void save() {
        // NOTE: Sharing the build server code here results in a potential race
        // condition, since there will be some minimal amount of time between
        // the snapshot being taken and the save happening. We can probably live
        // with this, since it is unlikely that there will be two threads making
        // simultaneous changes to a single BuildAgent object.
        getServiceHost().getBuildServer().saveBuildAgents(new IBuildAgent[] {
            this
        });
    }

    /**
     * Back compat only - always returns "*". {@inheritDoc}
     */
    @Override
    public String getTeamProject() {
        return BuildConstants.STAR;
    }

    /**
     * The full path of the build agent. Back compat only - the team project
     * portion will always be "*". {@inheritDoc}
     */
    @Override
    public String getFullPath() {
        final StringBuilder sb = new StringBuilder();
        sb.append(BuildPath.PATH_SEPERATOR);
        sb.append(getTeamProject());
        sb.append(BuildPath.PATH_SEPERATOR);
        sb.append(getName());
        return sb.toString();
    }

    /**
     * The machine this agent will use to process builds.
     *
     *
     * @return
     */
    public String getMachineName() {
        return serviceHost.getName();
    }

    public void setMachineName(final String value) {
        throw new NotSupportedException();
    }

    /**
     * The build server that owns this build agent.
     *
     *
     * @return
     */
    public IBuildServer getBuildServer() {
        return null;
    }

    private boolean tagsDiffer(final String[] t1, final String[] t2) {
        if (t1 == null && t2 == null) {
            return false;
        }

        if (t1 == null || t2 == null) {
            return true;
        }

        if (t1.length != t2.length) {
            return true;
        }

        final Set<String> set = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
        for (final String s : t1) {
            set.add(s);
        }

        for (final String s : t2) {
            if (!set.contains(s)) {
                return true;
            }
        }

        return false;
    }
}
