// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.build.internal.soapextensions;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.microsoft.tfs.core.clients.build.BuildConstants;
import com.microsoft.tfs.core.clients.build.IBuildAgent;
import com.microsoft.tfs.core.clients.build.IBuildController;
import com.microsoft.tfs.core.clients.build.IBuildServer;
import com.microsoft.tfs.core.clients.build.IBuildServiceHost;
import com.microsoft.tfs.core.clients.build.flags.BuildControllerUpdate;
import com.microsoft.tfs.core.clients.build.internal.utils.BuildValidation;
import com.microsoft.tfs.core.clients.build.soapextensions.Agent2008Status;
import com.microsoft.tfs.core.clients.build.soapextensions.ControllerStatus;
import com.microsoft.tfs.core.clients.versioncontrol.path.ServerPath;
import com.microsoft.tfs.core.internal.wrappers.WebServiceObjectWrapper;
import com.microsoft.tfs.core.internal.wrappers.WrapperUtils;
import com.microsoft.tfs.util.Check;

import ms.tfs.build.buildservice._04._BuildController;
import ms.tfs.build.buildservice._04._PropertyValue;

public class BuildController extends WebServiceObjectWrapper implements IBuildController {
    private BuildControllerUpdateOptions lastSnapshot;
    private IBuildServiceHost serviceHost;
    private final BuildServer buildServer;
    private List<IBuildAgent> agents = new ArrayList<IBuildAgent>();
    private AttachedPropertyDictionary attachedProperties = new AttachedPropertyDictionary();
    private final Object lockProperties = new Object();

    private BuildController(final IBuildServer buildServer) {
        super(new _BuildController());

        Check.notNull(buildServer, "buildServer"); //$NON-NLS-1$
        this.buildServer = (BuildServer) buildServer;

        getWebServiceObject().setTags(new String[0]);
        getWebServiceObject().setProperties(new _PropertyValue[0]);
    }

    public BuildController(final IBuildServer buildServer, final _BuildController webServiceObject) {
        super(webServiceObject);
        this.buildServer = (BuildServer) buildServer;
    }

    public BuildController(final IBuildServer buildServer, final BuildServiceHost serviceHost, final String name) {
        this(buildServer);

        final _BuildController _o = getWebServiceObject();

        _o.setName(name);
        _o.setMaxConcurrentBuilds(0);
        _o.setCustomAssemblyPath(null);
        _o.setTags(new String[0]);

        setServiceHost(serviceHost);

        // We need the snapshot to be a non-null value for locking purposes, so
        // go ahead and initialize it here. The snapshot will not be used for
        // anything until the agent is saved to the server for the first time
        // and has a URI, so this should not be a big deal.
        lastSnapshot = getSnapshot();

        _o.setStatus(ControllerStatus.OFFLINE.getWebServiceObject());
        _o.setEnabled(true);
    }

    public BuildController(final IBuildServer buildServer, final BuildController2010 controller2010) {
        this(buildServer);

        final _BuildController _o = getWebServiceObject();

        _o.setCustomAssemblyPath(controller2010.getCustomAssemblyPath());
        _o.setDateCreated(controller2010.getDateCreated());
        _o.setDateUpdated(controller2010.getDateUpdated());
        _o.setDescription(controller2010.getDescription());
        _o.setEnabled(controller2010.isEnabled());
        _o.setName(controller2010.getName());
        _o.setServiceHostUri(controller2010.getServiceHostURI());
        _o.setStatus(TFS2010Helper.convert(controller2010.getStatus()).getWebServiceObject());
        _o.setStatusMessage(controller2010.getStatusMessage());
        _o.setTags(controller2010.getTags());
        _o.setUri(controller2010.getURI());
        _o.setUrl(controller2010.getURL());

        afterDeserialize();
    }

    /**
     * This constructor is for V2 compatibility and should not be used
     * otherwise.
     *
     *
     * @param buildServer
     * @param agent2008
     */
    public BuildController(final IBuildServer buildServer, final BuildAgent2008 agent2008) {
        this(buildServer);

        final _BuildController _o = getWebServiceObject();

        _o.setDescription(agent2008.getDescription());
        // Using the full path of the agent as the controller name so the user
        // doesn't see duplicates.
        _o.setName(agent2008.getFullPath());
        _o.setQueueCount(agent2008.getQueueCount());
        _o.setStatus(TFS2008Helper.convert(agent2008.getStatus()).getWebServiceObject());
        _o.setEnabled(agent2008.getStatus() != Agent2008Status.DISABLED);
        _o.setServiceHostUri(agent2008.getURI());
        _o.setStatusMessage(agent2008.getStatusMessage());
        _o.setUri(agent2008.getURI());

        afterDeserialize();
    }

    public _BuildController getWebServiceObject() {
        return (_BuildController) this.webServiceObject;
    }

    /**
     * Gets or sets the version control folder containing custom assemblies.
     * {@inheritDoc}
     */
    @Override
    public String getCustomAssemblyPath() {
        return getWebServiceObject().getCustomAssemblyPath();
    }

    @Override
    public void setCustomAssemblyPath(final String value) {
        getWebServiceObject().setCustomAssemblyPath(value);
    }

    /**
     * Gets the date this build agent was created. This field is read-only.
     * {@inheritDoc}
     */
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
     * Gets or sets the number of concurrent builds allowed to run in parallel.
     * {@inheritDoc}
     */
    @Override
    public int getMaxConcurrentBuilds() {
        return getWebServiceObject().getMaxConcurrentBuilds();
    }

    @Override
    public void setMaxConcurrentBuilds(final int value) {
        getWebServiceObject().setMaxConcurrentBuilds(value);
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
     * Gets the current queue depth. This field is read-only. {@inheritDoc}
     */
    @Override
    public int getQueueCount() {
        return getWebServiceObject().getQueueCount();
    }

    public void setQueueCount(final int value) {
        getWebServiceObject().setQueueCount(value);
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
    public ControllerStatus getStatus() {
        return ControllerStatus.fromWebServiceObject(getWebServiceObject().getStatus());
    }

    @Override
    public void setStatus(final ControllerStatus value) {
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

    @Override
    public IBuildServiceHost getServiceHost() {
        if (serviceHost == null && getWebServiceObject().getServiceHostUri() != null) {
            // Lazily load service host object
            serviceHost = buildServer.getBuildServiceHostByURI(getWebServiceObject().getServiceHostUri());
        }
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
     * The build agents owned by this controller. {@inheritDoc}
     */
    @Override
    public IBuildAgent[] getAgents() {
        return agents.toArray(new IBuildAgent[agents.size()]);
    }

    public void setAgents(final List<IBuildAgent> value) {
        agents = value;
    }

    /**
     * The union of the tags for all the controller's agents. {@inheritDoc}
     */
    @Override
    public String[] getTags() {
        if (agents != null) {
            // TODO: The server returns these when queried, but the OM does not
            // provide the necessary hooks to update this list on-demand. For
            // this reason we currently build the list of tags each time they
            // are requested. This *IS* a bug and needs to be fixed when an
            // appropriate solution has been reached.
            final List<String> tags = new ArrayList<String>();
            for (final IBuildAgent agent : agents) {
                for (final String tag : agent.getTags()) {
                    if (!tags.contains(tag)) {
                        tags.add(tag);
                    }
                }
            }
            return tags.toArray(new String[tags.size()]);
        } else {
            return getWebServiceObject().getTags();
        }
    }

    public void setTags(final String[] value) {
        getWebServiceObject().setTags(value);
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
     * Adds a build agent to this controller. {@inheritDoc}
     */
    @Override
    public void addBuildAgent(final IBuildAgent agent) {
        if (!agents.contains(agent)) {
            agents.add(agent);
            agent.setController(this);
        }
    }

    /**
     * Deletes the build controller. {@inheritDoc}
     */
    @Override
    public void delete() {
        if (getURI() != null) {
            serviceHost.getBuildServer().deleteBuildControllers(new String[] {
                getURI()
            });
        }
    }

    /**
     * Refreshes the build controller by getting current property values from
     * the build server. {@inheritDoc}
     */
    @Override
    public void refresh(final boolean refreshAgentList) {
        if (getURI() != null) {
            copy(serviceHost.getBuildServer().getBuildController(
                getURI(),
                BuildConstants.NO_PROPERTY_NAMES,
                refreshAgentList), refreshAgentList);
        }
    }

    /**
     * Refreshes the build controller by getting current property values from
     * the build server. {@inheritDoc}
     */
    @Override
    public void refresh(final String[] propertyNameFilters, final boolean refreshAgentList) {
        if (getURI() != null) {
            copy(
                serviceHost.getBuildServer().getBuildController(getURI(), propertyNameFilters, refreshAgentList),
                refreshAgentList);
        }
    }

    // / <summary>
    // / Copy all properties from the argument into this controller.
    // / </summary>
    // / <param name="controller">The controller whose values are
    // copied.</param>
    /**
     * Copy all properties from the argument into this controller.
     *
     *
     * @param controller
     *        The controller whose values are copied.
     * @param copyAgentList
     */
    public void copy(final IBuildController controller, final boolean copyAgentList) {
        if (controller != null) {
            if (copyAgentList) {
                // Get a dictionary of all the new agents
                final Map<String, BuildAgent> agentsToCopy = new HashMap<String, BuildAgent>();
                for (final IBuildAgent agent : controller.getAgents()) {
                    agentsToCopy.put(agent.getURI(), (BuildAgent) agent);
                }

                // Scan all the existing agents
                final List<IBuildAgent> agentsToDelete = new ArrayList<IBuildAgent>();
                for (final IBuildAgent agent : agents) {
                    if (agent.getURI() == null) {
                        // The agent hasn't been saved yet and we need to leave
                        // it alone.
                    } else if (agentsToCopy.containsKey(agent.getURI())) {
                        // Copy new agent values and remove agentToCopy from
                        // dictionary.
                        final BuildAgent agentToCopy = agentsToCopy.get(agent.getURI());
                        ((BuildAgent) agent).copy(agentToCopy);
                        agentsToCopy.remove(agentToCopy.getURI());
                    } else {
                        // Agent no longer exists or no longer belongs to this
                        // controller.
                        agentsToDelete.add(agent);
                    }
                }

                // Add new ones
                for (final IBuildAgent agent : agentsToCopy.values()) {
                    agent.setController(this);
                }

                // Delete old ones
                for (final IBuildAgent agent : agentsToDelete) {
                    this.agents.remove(agent);
                }
            }

            setCustomAssemblyPath(controller.getCustomAssemblyPath());
            setDescription(controller.getDescription());
            setMaxConcurrentBuilds(controller.getMaxConcurrentBuilds());
            setName(controller.getName());
            setQueueCount(controller.getQueueCount());
            serviceHost = controller.getServiceHost();
            setStatus(controller.getStatus());
            setEnabled(controller.isEnabled());
            setStatusMessage(controller.getStatusMessage());
            setTags(controller.getTags());
            setURL(controller.getURL());
            setAttachedProperties(((BuildController) controller).getAttachedProperties());

            // Set the last snapshot by initializing it from the values
            lastSnapshot = getSnapshot();
        }
    }

    /**
     * Removes a build agent from this controller. {@inheritDoc}
     */
    @Override
    public void removeBuildAgent(final IBuildAgent agent) {
        if (agents.remove(agent)) {
            agent.setController(null);
        }
    }

    public void afterDeserialize() {
        // Get the properties
        attachedProperties = new AttachedPropertyDictionary(getInternalProperties());

        lastSnapshot = getSnapshot();
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
    private BuildControllerUpdate compareSnapshots(
        final BuildControllerUpdateOptions originalValues,
        final BuildControllerUpdateOptions modifiedValues) {
        BuildControllerUpdate result = BuildControllerUpdate.NONE;

        if (!ServerPath.equals(originalValues.getCustomAssemblyPath(), modifiedValues.getCustomAssemblyPath())) {
            // We still need to use custom assembly URI so an older server
            // doesn't blow up
            result = result.combine(BuildControllerUpdate.CUSTOM_ASSEMBLY_PATH);
        }
        if (!originalValues.getDescription().equals(modifiedValues.getDescription())) {
            result = result.combine(BuildControllerUpdate.DESCRIPTION);
        }
        if (originalValues.getMaxConcurrentBuilds() != modifiedValues.getMaxConcurrentBuilds()) {
            result = result.combine(BuildControllerUpdate.MAX_CONCURRENT_BUILDS);
        }
        if (!originalValues.getName().equals(modifiedValues.getName())) {
            result = result.combine(BuildControllerUpdate.NAME);
        }
        if (!originalValues.getStatus().equals(modifiedValues.getStatus())) {
            result = result.combine(BuildControllerUpdate.STATUS);
        }
        if (originalValues.isEnabled() != modifiedValues.isEnabled()) {
            result = result.combine(BuildControllerUpdate.ENABLED);
        }
        if (!originalValues.getStatusMessage().equals(modifiedValues.getStatusMessage())) {
            result = result.combine(BuildControllerUpdate.STATUS_MESSAGE);
        }

        if (modifiedValues.getAttachedProperties().length > 0) {
            result = result.combine(BuildControllerUpdate.ATTACHED_PROPERTIES);
        }

        return result;
    }

    /**
     * Saves a snapshot of the object's current properties.
     *
     *
     * @return
     */
    private BuildControllerUpdateOptions getSnapshot() {
        final BuildControllerUpdateOptions result = new BuildControllerUpdateOptions();

        result.setCustomAssemblyPath(getCustomAssemblyPath());
        result.setDescription(getDescription());
        result.setMaxConcurrentBuilds(getMaxConcurrentBuilds());
        result.setName(getName());
        result.setStatus(getStatus());
        result.setEnabled(isEnabled());
        result.setStatusMessage(getStatusMessage());
        result.setURI(getURI());
        result.setAttachedProperties(attachedProperties.getChangedProperties());

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
    public BuildControllerUpdateOptions getUpdateOptions() {
        BuildControllerUpdateOptions currentSnapshot;

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
    public void setUpdateOptions(final BuildControllerUpdateOptions snapshot) {
        synchronized (lastSnapshot) {
            lastSnapshot = snapshot;
        }

        // Since we only call this method after Save we clear the delta to start
        // collecting it again
        attachedProperties.clearChangedProperties();
    }

    /**
     * Saves any changes made to the build controller to the build server.
     * {@inheritDoc}
     */
    @Override
    public void save() {
        serviceHost.getBuildServer().saveBuildControllers(new IBuildController[] {
            this
        });
    }

    public void prepareToSave() {
        // Validate Name here, using CheckValidControllerName to prevent bad
        // error messages.
        BuildValidation.checkValidControllerName(getName(), false /* allowWildcards */);

        setInternalProperties(attachedProperties.getChangedProperties());
    }

    /**
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        if (getURI() == null) {
            return 0;
        }
        return getURI().hashCode();
    }

    /**
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof BuildController)) {
            return false;
        }

        final BuildController other = (BuildController) obj;

        if (getURI() == null) {
            return false;
        }

        return getURI().equals(other.getURI());
    }
}
