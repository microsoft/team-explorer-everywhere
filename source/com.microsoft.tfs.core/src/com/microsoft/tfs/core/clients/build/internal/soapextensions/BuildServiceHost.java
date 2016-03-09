// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.build.internal.soapextensions;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import com.microsoft.tfs.core.clients.build.IBuildAgent;
import com.microsoft.tfs.core.clients.build.IBuildController;
import com.microsoft.tfs.core.clients.build.IBuildServer;
import com.microsoft.tfs.core.clients.build.IBuildServiceHost;
import com.microsoft.tfs.core.clients.build.flags.BuildServiceHostUpdate;
import com.microsoft.tfs.core.clients.build.soapextensions.AgentStatus;
import com.microsoft.tfs.core.clients.build.soapextensions.ControllerStatus;
import com.microsoft.tfs.core.exceptions.NotSupportedException;
import com.microsoft.tfs.core.internal.wrappers.WebServiceObjectWrapper;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.datetime.DotNETDate;

import ms.tfs.build.buildservice._04._BuildServiceHost;

public class BuildServiceHost extends WebServiceObjectWrapper implements IBuildServiceHost {
    private final IBuildServer buildServer;
    private IBuildController controller;
    private BuildServiceHostUpdateOptions lastSnapshot;
    private final List<IBuildAgent> agents = new ArrayList<IBuildAgent>();

    private BuildServiceHost(final IBuildServer buildServer) {
        super(new _BuildServiceHost());

        Check.notNull(buildServer, "buildServer"); //$NON-NLS-1$
        this.buildServer = buildServer;

        getWebServiceObject().setStatus(ServiceHostStatus.OFFLINE.getWebServiceObject());
        getWebServiceObject().setStatusChangedOn(DotNETDate.MIN_CALENDAR);
    }

    public BuildServiceHost(final IBuildServer buildServer, final _BuildServiceHost webServiceObject) {
        super(webServiceObject);

        Check.notNull(buildServer, "buildServer"); //$NON-NLS-1$
        this.buildServer = buildServer;

        lastSnapshot = getSnapshot();
    }

    public BuildServiceHost(final IBuildServer buildServer, final BuildAgent2008 agent2008) {
        this(buildServer);

        setName(agent2008.getMachineName());
        setRequireClientCertificates(false);
        setURI(agent2008.getURI());

        lastSnapshot = getSnapshot();
    }

    public BuildServiceHost(
        final IBuildServer buildServer,
        final String uri,
        final IBuildController controller,
        final String name) {
        this(buildServer, new _BuildServiceHost());

        setURI(uri);
        setName(name);

        this.controller = controller;
    }

    public BuildServiceHost(final IBuildServer buildServer, final String name, final String baseUrl) {
        this(buildServer);

        setBaseURL(baseUrl);
        setName(name);

        this.lastSnapshot = getSnapshot();
    }

    public BuildServiceHost(final IBuildServer buildServer, final BuildServiceHost2010 serviceHost2010) {
        this(buildServer);

        setBaseURL(serviceHost2010.getBaseURL());
        setName(serviceHost2010.getName());
        setRequireClientCertificates(serviceHost2010.isRequireClientCertificates());
        setURI(serviceHost2010.getURI());

        this.lastSnapshot = getSnapshot();
    }

    public _BuildServiceHost getWebServiceObject() {
        return (_BuildServiceHost) this.webServiceObject;
    }

    /**
     * Gets or sets the base URL. {@inheritDoc}
     */
    @Override
    public String getBaseURL() {
        return getWebServiceObject().getBaseUrl();
    }

    @Override
    public void setBaseURL(final String value) {
        getWebServiceObject().setBaseUrl(value);
    }

    /**
     * Gets a value indicating whether or not this service host is virtual.
     * {@inheritDoc}
     */
    @Override
    public boolean isVirtual() {
        return getWebServiceObject().isIsVirtual();
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

    @Override
    public boolean isRequireClientCertificates() {
        return getWebServiceObject().isRequireClientCertificates();
    }

    @Override
    public void setRequireClientCertificates(final boolean value) {
        getWebServiceObject().setRequireClientCertificates(value);
    }

    /**
     * Gets or sets the online status of the service host.
     *
     *
     * @return
     */
    public ServiceHostStatus getStatus() {
        return ServiceHostStatus.fromWebServiceObject(getWebServiceObject().getStatus());
    }

    /**
     * Gets the date and time of the last connection from the service host.
     *
     *
     * @return
     */
    public Calendar getStatusChangedOn() {
        return getWebServiceObject().getStatusChangedOn();
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

    @Override
    public IBuildServer getBuildServer() {
        return buildServer;
    }

    @Override
    public IBuildController getController() {
        return controller;
    }

    @Override
    public IBuildAgent[] getAgents() {
        return agents.toArray(new IBuildAgent[agents.size()]);
    }

    @Override
    public IBuildController createBuildController(final String name) {
        final IBuildController controller = new BuildController(buildServer, this, name);
        controller.setStatus(ControllerStatus.OFFLINE);
        controller.setEnabled(true);
        controller.setMaxConcurrentBuilds(0);

        this.controller = controller;
        return controller;
    }

    @Override
    public void setBuildAgentStatus(final IBuildAgent agent, final AgentStatus status, final String message) {
        agent.setStatus(status);
        agent.setStatusMessage(message);
        agent.save();
    }

    @Override
    public void setBuildControllerStatus(final ControllerStatus status, final String message) {
        if (controller != null) {
            controller.setStatus(status);
            controller.setStatusMessage(message);
            controller.save();
        }
    }

    @Override
    public void takeOwnership() {
        throw new NotSupportedException();
    }

    @Override
    public void releaseOwnership() {
        throw new NotSupportedException();
    }

    @Override
    public IBuildAgent createBuildAgent(final String name, final String buildDirectory) {
        return createBuildAgent(name, buildDirectory, controller);
    }

    @Override
    public IBuildAgent createBuildAgent(
        final String name,
        final String buildDirectory,
        final IBuildController buildController) {
        // Create the build agent and assign the build controller to the value
        // that was provided
        final BuildAgent buildAgent = new BuildAgent(this, name, buildDirectory);
        buildAgent.setController(buildController);
        buildAgent.setStatus(AgentStatus.OFFLINE);
        buildAgent.setEnabled(true);

        // Make sure the agent is added to our in-memory list of agents
        agents.add(buildAgent);

        return buildAgent;
    }

    /**
     * Provides a mechanism by which agents may be added to the list during
     * deserialization. {@inheritDoc}
     */
    @Override
    public void addBuildAgent(final IBuildAgent agent) {
        ((BuildAgent) agent).setServiceHost(this);
        agents.add(agent);
    }

    public BuildServiceHostUpdateOptions getSnapshot() {
        final BuildServiceHostUpdateOptions options = new BuildServiceHostUpdateOptions();
        options.setURI(this.getURI());
        options.setBaseURL(this.getBaseURL());
        options.setName(this.getName());
        options.setRequireClientCertificates(this.isRequireClientCertificates());
        return options;
    }

    /**
     * Gets a snapshot of the current updates pending on this service host.
     *
     *
     * @return
     */
    public BuildServiceHostUpdateOptions getUpdateOptions() {
        BuildServiceHostUpdateOptions currentSnapshot;

        synchronized (lastSnapshot) {
            currentSnapshot = getSnapshot();

            currentSnapshot.setFields(compareSnapshots(lastSnapshot, currentSnapshot));
        }

        return currentSnapshot;
    }

    /**
     * Updates the current snapshot to the provided values.
     *
     *
     * @param options
     */
    public void setUpdateOptions(final BuildServiceHostUpdateOptions options) {
        synchronized (lastSnapshot) {
            lastSnapshot = options;
        }
    }

    /**
     * Compares two snapshots and determines the fields that have changed.
     *
     *
     * @param originalValues
     *        The original values
     * @param modifiedValues
     *        The modified values
     * @return A enumeration value signifying the fields that are different
     *         between the two parameters
     */
    private BuildServiceHostUpdate compareSnapshots(
        final BuildServiceHostUpdateOptions originalValues,
        final BuildServiceHostUpdateOptions modifiedValues) {
        BuildServiceHostUpdate result = BuildServiceHostUpdate.NONE;

        if (!originalValues.getBaseURL().equals(modifiedValues.getBaseURL())) {
            result = result.combine(BuildServiceHostUpdate.BASE_URI);
        }

        if (!originalValues.getName().equalsIgnoreCase(modifiedValues.getName())) {
            result = result.combine(BuildServiceHostUpdate.NAME);
        }

        if (originalValues.isRequireClientCertificates() != modifiedValues.isRequireClientCertificates()) {
            result = result.combine(BuildServiceHostUpdate.REQUIRE_CLIENT_CERTIFICATE);
        }

        return result;
    }

    /**
     * Provides a mechanism by which a controller may be added to this service
     * host during deserialization. {@inheritDoc}
     */
    @Override
    public void setBuildController(final IBuildController controller) {
        if (controller != null) {
            ((BuildController) controller).setServiceHost(this);
        }

        this.controller = controller;
    }

    @Override
    public void delete() {
        if (getURI() != null) {
            buildServer.deleteBuildServiceHost(getURI());
        }
    }

    @Override
    public void save() {
        buildServer.saveBuildServiceHost(this);
    }

    @Override
    public void deleteBuildController() {
        controller.delete();
        controller = null;
    }

    @Override
    public IBuildAgent findBuildAgent(final String controller, final String name) {
        for (final IBuildAgent a : agents) {
            if (a.getName().equals(name) && a.getController().getName().equals(controller)) {
                return a;
            }
        }
        return null;
    }

    @Override
    public boolean deleteBuildAgent(final IBuildAgent agent) {
        for (final IBuildAgent a : agents) {
            if (a.getName().equals(agent.getName())
                && a.getController().getName().equals(agent.getController().getName())) {
                a.delete();
                agents.remove(agent);
                return true;
            }
        }

        return false;
    }
}
