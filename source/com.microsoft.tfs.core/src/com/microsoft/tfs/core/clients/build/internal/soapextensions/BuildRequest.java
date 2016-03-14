// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.build.internal.soapextensions;

import com.microsoft.tfs.core.clients.build.IBuildController;
import com.microsoft.tfs.core.clients.build.IBuildDefinition;
import com.microsoft.tfs.core.clients.build.IBuildRequest;
import com.microsoft.tfs.core.clients.build.IBuildServer;
import com.microsoft.tfs.core.clients.build.flags.BuildReason;
import com.microsoft.tfs.core.clients.build.soapextensions.GetOption;
import com.microsoft.tfs.core.clients.build.soapextensions.QueuePriority;
import com.microsoft.tfs.core.clients.versioncontrol.specs.WorkspaceSpec;
import com.microsoft.tfs.core.internal.wrappers.WebServiceObjectWrapper;
import com.microsoft.tfs.util.GUID;
import com.microsoft.tfs.util.StringUtil;
import com.microsoft.tfs.util.GUID.GUIDStringFormat;

import ms.tfs.build.buildservice._04._BuildRequest;

public class BuildRequest extends WebServiceObjectWrapper implements IBuildRequest {
    private IBuildController buildController;
    private IBuildDefinition buildDefinition;
    private IBuildServer buildServer;

    private BuildRequest() {
        super(new _BuildRequest());
    }

    public BuildRequest(final BuildDefinition definition) {
        this();
        // Set default values from definition.
        setBuildDefinition(definition);
        setBuildServer(definition.getBuildServer());
        setBuildController(definition.getBuildController());
        getWebServiceObject().setDropLocation(definition.getDefaultDropLocation());

        // Process Parameters are inherited from the definition if left empty
        getWebServiceObject().setProcessParameters(null);
    }

    public BuildRequest(
        final IBuildServer buildServer,
        final String buildDefinitionUri,
        final String buildControllerUri) {
        this();
        setBuildServer(buildServer);
        getWebServiceObject().setBuildDefinitionUri(buildDefinitionUri);
        getWebServiceObject().setBuildControllerUri(buildControllerUri);
    }

    public _BuildRequest getWebServiceObject() {
        return (_BuildRequest) this.webServiceObject;
    }

    @Override
    public IBuildController getBuildController() {
        return buildController;
    }

    @Override
    public void setBuildController(final IBuildController value) {
        buildController = value;

        if (buildController == null) {
            getWebServiceObject().setBuildControllerUri(null);
        } else {
            getWebServiceObject().setBuildControllerUri(buildController.getURI());
        }
    }

    @Override
    public IBuildDefinition getBuildDefinition() {
        return buildDefinition;
    }

    public void setBuildDefinition(final IBuildDefinition value) {
        buildDefinition = value;

        if (buildDefinition == null) {
            getWebServiceObject().setBuildDefinitionUri(null);
        } else {
            getWebServiceObject().setBuildDefinitionUri(value.getURI());
        }
    }

    @Override
    public IBuildServer getBuildServer() {
        return buildServer;
    }

    private void setBuildServer(final IBuildServer value) {
        buildServer = value;
    }

    /**
     * Gets or sets the batch ID for this request. Requests with matching batch
     * IDs are started together in a single build up to a maximum batch size
     * configured on the Build Definition. {@inheritDoc}
     */
    @Override
    public GUID getBatchID() {
        return new GUID(getWebServiceObject().getBatchId());
    }

    @Override
    public void setBatchID(final GUID value) {
        getWebServiceObject().setBatchId(value.getGUIDString(GUIDStringFormat.NONE));
    }

    /**
     * Gets or sets the build controller to use for the build. A null value
     * indicates the default build controller of the build definition.
     * {@inheritDoc}
     */
    @Override
    public String getBuildControllerURI() {
        return getWebServiceObject().getBuildControllerUri();
    }

    public void setBuildControllerURI(final String value) {
        getWebServiceObject().setBuildControllerUri(value);
    }

    /**
     * Gets or sets the target build definition. {@inheritDoc}
     */
    @Override
    public String getBuildDefinitionURI() {
        return getWebServiceObject().getBuildDefinitionUri();
    }

    @Override
    public void setBuildDefinitionURI(final String value) {
        getWebServiceObject().setBuildDefinitionUri(value);
    }

    /**
     * Gets or sets the version of sources to download for the build.
     * GetOption.Custom must be specified. {@inheritDoc}
     */
    @Override
    public String getCustomGetVersion() {
        return getWebServiceObject().getCustomGetVersion();
    }

    @Override
    public void setCustomGetVersion(final String value) {
        getWebServiceObject().setCustomGetVersion(value);
    }

    /**
     * Gets or sets the drop location to use for the build. A null value
     * indicates the default drop location of the build definition.
     * {@inheritDoc}
     */
    @Override
    public String getDropLocation() {
        return getWebServiceObject().getDropLocation();
    }

    @Override
    public void setDropLocation(final String value) {
        getWebServiceObject().setDropLocation(value);
    }

    /**
     * Gets or sets the check-in ticket generated by the server to use for this
     * build. {@inheritDoc}
     */
    @Override
    public String getGatedCheckInTicket() {
        return getWebServiceObject().getCheckInTicket();
    }

    @Override
    public void setGatedCheckInTicket(final String value) {
        getWebServiceObject().setCheckInTicket(value);
    }

    /**
     * Gets or sets the desired option for determing the version of sources to
     * download for the build. {@inheritDoc}
     */
    @Override
    public GetOption getGetOption() {
        return GetOption.fromWebServiceObject(getWebServiceObject().getGetOption());
    }

    @Override
    public void setGetOption(final GetOption value) {
        getWebServiceObject().setGetOption(value.getWebServiceObject());
    }

    /**
     * Gets or sets a value indicating the maximum queue depth allowed for this
     * build. {@inheritDoc}
     */
    @Override
    public int getMaxQueuePosition() {
        return getWebServiceObject().getMaxQueuePosition();
    }

    @Override
    public void setMaxQueuePosition(final int value) {
        getWebServiceObject().setMaxQueuePosition(value);
    }

    /**
     * Gets or sets a value indicating whether or not the queued build is active
     * immediately. {@inheritDoc}
     */
    @Override
    public boolean isPostponed() {
        return getWebServiceObject().isPostponed();
    }

    @Override
    public void setPostponed(final boolean value) {
        getWebServiceObject().setPostponed(value);
    }

    /**
     * Gets or sets the priority of the build. {@inheritDoc}
     */
    @Override
    public QueuePriority getPriority() {
        return QueuePriority.fromWebServiceObject(getWebServiceObject().getPriority());
    }

    @Override
    public void setPriority(final QueuePriority value) {
        getWebServiceObject().setPriority(value.getWebServiceObject());
    }

    /**
     * Gets or sets the process parameters to use for the build. Parameters
     * which are not explicitly overridden by the build will inherit values from
     * the build definition. {@inheritDoc}
     */
    @Override
    public String getProcessParameters() {
        return getWebServiceObject().getProcessParameters();
    }

    @Override
    public void setProcessParameters(final String value) {
        getWebServiceObject().setProcessParameters(value);
    }

    /**
     * Gets or sets the reason this build was queued. {@inheritDoc}
     */
    @Override
    public BuildReason getReason() {
        return BuildReason.fromWebServiceObject(getWebServiceObject().getReason());
    }

    @Override
    public void setReason(final BuildReason value) {
        getWebServiceObject().setReason(value.getWebServiceObject());
    }

    /**
     * Gets or sets the name of the user this build was requested for.
     * {@inheritDoc}
     */
    @Override
    public String getRequestedFor() {
        return getWebServiceObject().getRequestedFor();
    }

    @Override
    public void setRequestedFor(final String value) {
        getWebServiceObject().setRequestedFor(value);
    }

    /**
     * Gets or sets the name of the shelveset to incorporate in this build.
     * {@inheritDoc}
     */
    @Override
    public String getShelvesetName() {
        return getWebServiceObject().getShelvesetName();
    }

    @Override
    public void setShelvesetName(final String value) {
        getWebServiceObject().setShelvesetName(value);
    }

    public void beforeSerialize() {
        // If shelveset name is empty, leave it empty.
        if (!StringUtil.isNullOrEmpty(getShelvesetName())) {
            // Ensure that the shelveset name includes the full owner name.
            final WorkspaceSpec spec = WorkspaceSpec.parse(
                getShelvesetName(),
                buildServer.getConnection().getAuthorizedIdentity().getUniqueName());
            setShelvesetName(spec.toString());
        }
    }
}
