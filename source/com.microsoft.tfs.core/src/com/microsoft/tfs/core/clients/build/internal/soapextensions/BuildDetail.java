// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.build.internal.soapextensions;

import java.text.MessageFormat;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.microsoft.tfs.core.Messages;
import com.microsoft.tfs.core.clients.build.IBuildController;
import com.microsoft.tfs.core.clients.build.IBuildDefinition;
import com.microsoft.tfs.core.clients.build.IBuildDeletionResult;
import com.microsoft.tfs.core.clients.build.IBuildDetail;
import com.microsoft.tfs.core.clients.build.IBuildInformation;
import com.microsoft.tfs.core.clients.build.IBuildInformationNode;
import com.microsoft.tfs.core.clients.build.IBuildServer;
import com.microsoft.tfs.core.clients.build.IQueuedBuild;
import com.microsoft.tfs.core.clients.build.exceptions.InvalidFinalStatusException;
import com.microsoft.tfs.core.clients.build.flags.BuildReason;
import com.microsoft.tfs.core.clients.build.flags.BuildStatus;
import com.microsoft.tfs.core.clients.build.flags.BuildUpdate;
import com.microsoft.tfs.core.clients.build.flags.DeleteOptions;
import com.microsoft.tfs.core.clients.build.flags.InformationFields;
import com.microsoft.tfs.core.clients.build.flags.InformationTypes;
import com.microsoft.tfs.core.clients.build.flags.QueryDeletedOption;
import com.microsoft.tfs.core.clients.build.flags.QueryOptions;
import com.microsoft.tfs.core.exceptions.NotSupportedException;
import com.microsoft.tfs.core.internal.wrappers.WebServiceObjectWrapper;
import com.microsoft.tfs.core.internal.wrappers.WrapperUtils;
import com.microsoft.tfs.util.GUID;
import com.microsoft.tfs.util.StringUtil;
import com.microsoft.tfs.util.datetime.DotNETDate;

import ms.tfs.build.buildservice._04._BuildDetail;
import ms.tfs.build.buildservice._04._BuildInformationNode;

public class BuildDetail extends WebServiceObjectWrapper implements IBuildDetail {
    private String requestedBy;
    private String requestedFor;
    private String shelvesetName;
    private IBuildController buildController;
    private BuildUpdateOptions lastSnapshot;
    private BuildServer buildServer;
    private BuildDefinition buildDefinition;
    private IBuildInformation informationNodes;

    private int[] requestIds = new int[0];
    private IQueuedBuild[] requests = new IQueuedBuild[0];

    // This object will be used to synchronize Save methods for build
    // information nodes of this build detail.
    public Object syncSave = new Object();

    public BuildDetail(final IBuildServer buildServer, final _BuildDetail webServiceObject) {
        super(webServiceObject);
        this.buildServer = (BuildServer) buildServer;

        afterDeserialize();
    }

    public _BuildDetail getWebServiceObject() {
        return (_BuildDetail) this.webServiceObject;
    }

    public BuildDetail(IBuildServer buildServer, final BuildDetail2010 build2010) {
        // Make sure that we copy the agent Uri field over to the controller Uri
        // field. This works because the Agent Uri field will return the
        // controller uri (or agent uri if 2008)
        super(new _BuildDetail());
        this.buildServer = (BuildServer) buildServer;

        final _BuildDetail proxy = getWebServiceObject();

        proxy.setBuildControllerUri(build2010.getBuildAgentUri());
        proxy.setBuildDefinitionUri(build2010.getBuildDefinitionUri());
        proxy.setBuildNumber(build2010.getBuildNumber());

        buildServer = build2010.getBuildServer();
        proxy.setCompilationStatus(TFS2010Helper.convert(build2010.getCompilationStatus()).getWebServiceObject());
        proxy.setDropLocation(build2010.getDropLocation());
        proxy.setDropLocationRoot(build2010.getDropLocationRoot());
        proxy.setFinishTime(build2010.getFinishTime());
        proxy.setInformation(
            (_BuildInformationNode[]) WrapperUtils.unwrap(
                _BuildInformationNode.class,
                TFS2010Helper.convert(build2010.getInternalInformation())));
        proxy.setIsDeleted(build2010.isIsDeleted());
        proxy.setKeepForever(build2010.isKeepForever());
        proxy.setLabelName(build2010.getLabelName());
        proxy.setLastChangedBy(build2010.getLastChangedBy());
        proxy.setLastChangedOn(build2010.getLastChangedOn());
        proxy.setLogLocation(build2010.getLogLocation());
        proxy.setProcessParameters(build2010.getProcessParameters());
        proxy.setQuality(build2010.getQuality());
        proxy.setReason(TFS2010Helper.convert(build2010.getReason()).getWebServiceObject());

        requestedBy = build2010.getRequestedBy();
        requestedFor = build2010.getRequestedFor();
        shelvesetName = build2010.getShelvesetName();

        proxy.setSourceGetVersion(build2010.getSourceGetVersion());
        proxy.setStartTime(build2010.getStartTime());
        proxy.setStatus(TFS2010Helper.convert(build2010.getStatus()).getWebServiceObject());
        proxy.setTeamProject(build2010.getTeamProject());
        proxy.setTestStatus(TFS2010Helper.convert(build2010.getTestStatus()).getWebServiceObject());
        proxy.setUri(build2010.getURI());

        // Create a fake request for this build
        proxy.setQueueIds(new int[] {
            0
        });

        final IQueuedBuild request = new QueuedBuild(buildServer, this);
        requests = new IQueuedBuild[1];
        requests[0] = request;

        afterDeserialize();
    }

    /**
     * Take a baseline snapshot just after deserialization so we know what to
     * update in Save calls, etc.
     *
     *
     */
    public void afterDeserialize() {
        final _BuildDetail proxy = getWebServiceObject();

        lastSnapshot = getSnapshot();

        final BuildInformationNode[] info =
            (BuildInformationNode[]) WrapperUtils.wrap(BuildInformationNode.class, proxy.getInformation());
        informationNodes = new BuildInformation(this, info);

        requestIds = proxy.getQueueIds().clone();

        if (StringUtil.isNullOrEmpty(proxy.getLastChangedByDisplayName())) {
            proxy.setLastChangedByDisplayName(proxy.getLastChangedBy());
        }

        // TODO: patcarna: We should obsolete the CompilationSummary type by
        // converting these nodes for the Dev11 upgrade to BuildProject nodes.
        // Once this work is done this logic should be removed. Create fake
        // BuildProject nodes in place of any CompilationSummary nodes that may
        // exist
        final IBuildInformationNode[] compilationSummaryNodes =
            informationNodes.getNodesByType(InformationTypes.COMPILATION_SUMMARY, true);

        for (final IBuildInformationNode compilationSummaryNode : compilationSummaryNodes) {
            if (compilationSummaryNode.getParent() != null) {
                // Create a build project node (not saved to the database)
                final IBuildInformationNode node = compilationSummaryNode.getParent().getChildren().createNode();
                final Map<String, String> fields = compilationSummaryNode.getFields();

                node.setType(InformationTypes.BUILD_PROJECT);
                node.getFields().put(
                    InformationFields.COMPILATION_ERRORS,
                    CommonInformationHelper.getString(fields, InformationFields.COMPILATION_ERRORS));
                node.getFields().put(
                    InformationFields.COMPILATION_WARNINGS,
                    CommonInformationHelper.getString(fields, InformationFields.COMPILATION_WARNINGS));
                node.getFields().put(
                    InformationFields.FINISH_TIME,
                    CommonInformationHelper.getString(fields, InformationFields.FINISH_TIME));
                node.getFields().put(
                    InformationFields.LOCAL_PATH,
                    CommonInformationHelper.getString(fields, InformationFields.PROJECT_FILE));
                node.getFields().put(
                    InformationFields.SERVER_PATH,
                    CommonInformationHelper.getString(fields, InformationFields.PROJECT_FILE));
                node.getFields().put(
                    InformationFields.START_TIME,
                    CommonInformationHelper.getString(fields, InformationFields.START_TIME));
                node.getFields().put(
                    InformationFields.STATIC_ANALYSIS_ERRORS,
                    CommonInformationHelper.getString(fields, InformationFields.STATIC_ANALYSIS_ERRORS));
                node.getFields().put(
                    InformationFields.STATIC_ANALYSIS_WARNINGS,
                    CommonInformationHelper.getString(fields, InformationFields.STATIC_ANALYSIS_WARNINGS));
                node.getFields().put(InformationFields.TARGET_NAMES, StringUtil.EMPTY);

                // Get platform and flavor from the parent node (which should be
                // a configuration summary)
                node.getFields().put(
                    InformationFields.FLAVOR,
                    CommonInformationHelper.getString(
                        compilationSummaryNode.getParent().getFields(),
                        InformationFields.FLAVOR));
                node.getFields().put(
                    InformationFields.PLATFORM,
                    CommonInformationHelper.getString(
                        compilationSummaryNode.getParent().getFields(),
                        InformationFields.PLATFORM));

                // Add the external link to the build's log file
                if (getLogLocation() != null) {
                    final IBuildInformationNode logFileNode = node.getChildren().createNode();
                    logFileNode.setType(InformationTypes.EXTERNAL_LINK);
                    logFileNode.getFields().put(
                        InformationFields.DISPLAY_TEXT,
                        Messages.getString("BuildDetail2012.BuildLogFileLink")); //$NON-NLS-1$
                    logFileNode.getFields().put(InformationFields.URL, getLogLocation());
                }
            }
        }
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public String getBuildControllerURI() {
        return getWebServiceObject().getBuildControllerUri();
    }

    public void setBuildContrllerURI(final String value) {
        getWebServiceObject().setBuildControllerUri(value);
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public String getBuildDefinitionURI() {
        return getWebServiceObject().getBuildDefinitionUri();
    }

    public void setBuildDefinitionUri(final String value) {
        getWebServiceObject().setBuildDefinitionUri(value);
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public String getBuildNumber() {
        return getWebServiceObject().getBuildNumber();
    }

    @Override
    public void setBuildNumber(final String value) {
        getWebServiceObject().setBuildNumber(value);
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public BuildPhaseStatus getCompilationStatus() {
        return BuildPhaseStatus.fromWebServiceObject(getWebServiceObject().getCompilationStatus());
    }

    @Override
    public void setCompilationStatus(final BuildPhaseStatus value) {
        getWebServiceObject().setCompilationStatus(value.getWebServiceObject());
    }

    /**
     *
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
     *
     * {@inheritDoc}
     */
    @Override
    public String getDropLocationRoot() {
        return getWebServiceObject().getDropLocationRoot();
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public Calendar getFinishTime() {
        return getWebServiceObject().getFinishTime();
    }

    /**
     * Gets the build information node hierarchy.
     *
     *
     * @return
     */
    public BuildInformationNode[] getInternalInformation() {
        return (BuildInformationNode[]) WrapperUtils.wrap(
            BuildInformationNode.class,
            getWebServiceObject().getInformation());
    }

    public void setInternalInformation(final BuildInformationNode[] value) {
        getWebServiceObject().setInformation(
            (_BuildInformationNode[]) WrapperUtils.unwrap(_BuildInformationNode.class, value));
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public boolean isIsDeleted() {
        return getWebServiceObject().isIsDeleted();
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public boolean isKeepForever() {
        return getWebServiceObject().isKeepForever();
    }

    @Override
    public void setKeepForever(final boolean value) {
        getWebServiceObject().setKeepForever(value);
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public String getLabelName() {
        return getWebServiceObject().getLabelName();
    }

    @Override
    public void setLabelName(final String value) {
        getWebServiceObject().setLabelName(value);
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public String getLastChangedBy() {
        return getWebServiceObject().getLastChangedBy();
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public String getLastChangedByDisplayName() {
        return getWebServiceObject().getLastChangedByDisplayName();
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public Calendar getLastChangedOn() {
        return getWebServiceObject().getLastChangedOn();
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public String getLogLocation() {
        return getWebServiceObject().getLogLocation();
    }

    @Override
    public void setLogLocation(final String value) {
        getWebServiceObject().setLogLocation(value);
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public String getProcessParameters() {
        return getWebServiceObject().getProcessParameters();
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public String getQuality() {
        return getWebServiceObject().getQuality();
    }

    @Override
    public void setQuality(final String value) {
        getWebServiceObject().setQuality(value);
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public BuildReason getReason() {
        return BuildReason.fromWebServiceObject(getWebServiceObject().getReason());
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public String getSourceGetVersion() {
        return getWebServiceObject().getSourceGetVersion();
    }

    public void setSourceGetVersion(final String value) {
        getWebServiceObject().setSourceGetVersion(value);
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public Calendar getStartTime() {
        return getWebServiceObject().getStartTime();
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public BuildStatus getStatus() {
        return BuildStatus.fromWebServiceObject(getWebServiceObject().getStatus());
    }

    @Override
    public void setStatus(final BuildStatus value) {
        getWebServiceObject().setStatus(value.getWebServiceObject());
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public String getTeamProject() {
        return getWebServiceObject().getTeamProject();
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public BuildPhaseStatus getTestStatus() {
        return BuildPhaseStatus.fromWebServiceObject(getWebServiceObject().getTestStatus());
    }

    @Override
    public void setTestStatus(final BuildPhaseStatus value) {
        getWebServiceObject().setTestStatus(value.getWebServiceObject());
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public String getURI() {
        return getWebServiceObject().getUri();
    }

    public BuildServer getInternalBuildServer() {
        return buildServer;
    }

    @Override
    public IBuildServer getBuildServer() {
        return buildServer;
    }

    public void setBuildServer(final IBuildServer value) {
        buildServer = (BuildServer) value;
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public IBuildController getBuildController() {
        return buildController;
    }

    public void setBuildController(final IBuildController value) {
        buildController = value;
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public IBuildDefinition getBuildDefinition() {
        return buildDefinition;
    }

    public void setBuildDefinition(final IBuildDefinition value) {
        buildDefinition = (BuildDefinition) value;

        // Update the BuildDefinitionUri property, since this is all that gets
        // sent to the AT.
        if (buildDefinition == null) {
            getWebServiceObject().setBuildDefinitionUri(null);
        } else {
            getWebServiceObject().setBuildDefinitionUri(buildDefinition.getURI());

            // Compat: Orcas servers don't return the TeamProject for the Build
            // Detail
            if (StringUtil.isNullOrEmpty(getWebServiceObject().getTeamProject())) {
                // Set Team Project to match the definition
                getWebServiceObject().setTeamProject(buildDefinition.getTeamProject());
            }
        }
    }

    /**
     * A convenience property - true if the build is completed, false otherwise.
     * {@inheritDoc}
     */
    @Override
    public boolean isBuildFinished() {
        return getFinishTime().after(DotNETDate.MIN_CALENDAR);
    }

    /**
     * Gets the collection of information nodes for this build. {@inheritDoc}
     */
    @Override
    public IBuildInformation getInformation() {
        return informationNodes;
    }

    public void setInformation(final IBuildInformation value) {
        informationNodes = value;
    }

    @Override
    public int[] getRequestIDs() {
        return requestIds;
    }

    @Override
    public IQueuedBuild[] getRequests() {
        return requests;
    }

    public void addRequest(final IQueuedBuild newRequest) {
        if (requestIds.length == 1 && requestIds[0] == 0) {
            // Remove the fake request id.
            requestIds = new int[0];
            requests = new IQueuedBuild[0];
        }

        final IQueuedBuild[] newRequests = new IQueuedBuild[requests.length + 1];
        for (int i = 0; i < requests.length; i++) {
            newRequests[i] = requests[i];
        }

        newRequests[requests.length] = newRequest;
        requests = newRequests;
    }

    @Override
    public String getRequestedBy() {
        if (requests.length == 1) {
            return requests[0].getRequestedByDisplayName();
        } else if (requests.length > 1) {
            final String[] names = getDistinctRequestedByDisplayNames();
            return names.length > 1 ? Messages.getString("BuildDetail2012.MultiplePlaceHolder") : names[0]; //$NON-NLS-1$
        } else if (buildServer.getBuildServerVersion().isLessThanV4()) {
            return requestedBy;
        }
        return null;
    }

    @Override
    public String getRequestedFor() {
        if (requests.length == 1) {
            return requests[0].getRequestedForDisplayName();
        } else if (requests.length > 1) {
            final String[] names = getDistinctRequestedForDisplayNames();
            return names.length > 1 ? Messages.getString("BuildDetail2012.MultiplePlaceHolder") : names[0]; //$NON-NLS-1$
        } else if (buildServer.getBuildServerVersion().isLessThanV4()) {
            return requestedFor;
        }
        return null;
    }

    @Override
    public String getShelvesetName() {
        if (requests.length == 1) {
            return requests[0].getShelvesetName();
        } else if (requests.length > 1) {
            // It's not possible to have the same shelvesets for few requests
            return Messages.getString("BuildDetail2012.MultiplePlaceHolder"); //$NON-NLS-1$
        } else if (buildServer.getBuildServerVersion().isLessThanV4()) {
            return shelvesetName;
        }
        return null;
    }

    public void setShelvesetName(final String value) {
        shelvesetName = value;
    }

    /**
     * Delete the build and all associated data from the server and drop
     * location. {@inheritDoc}
     */
    @Override
    public IBuildDeletionResult delete() {
        return delete(DeleteOptions.ALL);
    }

    /**
     * Delete the build and only the associated information you specify.
     * {@inheritDoc}
     */
    @Override
    public IBuildDeletionResult delete(final DeleteOptions options) {
        if (buildServer.getBuildServerVersion().isLessThanV3() && !options.equals(DeleteOptions.ALL)) {
            // Older versions only support deleting everything, so error out.
            final String format = Messages.getString("BuildDetail2012.DeleteOptionsNotSupportedFormat"); //$NON-NLS-1$
            final String message = MessageFormat.format(format, options.toString());
            throw new NotSupportedException(message);
        }

        return buildServer.deleteBuilds(new String[] {
            getURI()
        }, options)[0];
    }

    /**
     * Notifies the server that the build is complete.
     *
     *
     */
    public void finalizeStatus() {
        if (!getStatus().equals(BuildStatus.FAILED)
            && !getStatus().equals(BuildStatus.PARTIALLY_SUCCEEDED)
            && !getStatus().equals(BuildStatus.STOPPED)
            && !getStatus().equals(BuildStatus.SUCCEEDED)) {
            throw new InvalidFinalStatusException(getBuildNumber(), getStatus(), getBuildServer());
        }

        // Save the current state of the build
        save();

        if (buildServer.getBuildServerVersion().isV3OrGreater()) {
            BuildDetail build;
            if (buildServer.getBuildServerVersion().isV3()) {
                build = buildServer.getBuild2010Helper().notifyBuildCompleted(getURI());
            } else {
                build = buildServer.getBuildService().notifyBuildCompleted(getURI());
            }

            // Copy the result into our current object so the finish time is
            // up-to-date
            copy(build, null, QueryOptions.NONE);
        }
    }

    /**
     * Refresh this build detail by getting updated property values from the
     * server. {@inheritDoc}
     */
    @Override
    public void refresh(final String[] informationTypes, final QueryOptions queryOptions) {
        if (getURI() != null) {
            // Make sure to include deleted builds, in case this build has been
            // deleted
            copy(
                buildServer.getBuild(getURI(), informationTypes, queryOptions, QueryDeletedOption.INCLUDE_DELETED),
                informationTypes,
                queryOptions);
        }
    }

    /**
     * Requests the intermediate diagnostics logs of an in-progress build.
     * {@inheritDoc}
     */
    @Override
    public GUID requestIntermediateLogs() {
        if (buildServer.getBuildServerVersion().isLessThanV4()) {
            final String format = Messages.getString("BuildDetail2012.MethodNotSupportedFormat"); //$NON-NLS-1$
            final String message = MessageFormat.format(format, "RequestIntermediateLogs", "IBuildDetail"); //$NON-NLS-1$//$NON-NLS-2$
            throw new NotSupportedException(message);
        }

        return buildServer.getBuildService().requestIntermediateLogs(getURI());
    }

    /**
     * Copy all property values from the argument into this build.
     *
     *
     * @param build
     *        The build whose values are copied.
     * @param informationTypes
     * @param queryOptions
     */
    public void copy(final IBuildDetail build, final String[] informationTypes, final QueryOptions queryOptions) {
        synchronized (lastSnapshot) {
            setBuildNumber(build.getBuildNumber());
            setCompilationStatus(build.getCompilationStatus());
            setDropLocation(build.getDropLocation());
            setLabelName(build.getLabelName());
            setLogLocation(build.getLogLocation());
            setKeepForever(build.isKeepForever());
            setQuality(build.getQuality());
            setSourceGetVersion(build.getSourceGetVersion());
            setStatus(build.getStatus());
            setTestStatus(build.getTestStatus());
            getWebServiceObject().setLastChangedBy(build.getLastChangedBy());
            getWebServiceObject().setLastChangedOn(build.getLastChangedOn());
            getWebServiceObject().setStartTime(build.getStartTime());
            getWebServiceObject().setFinishTime(build.getFinishTime());

            if (queryOptions.contains(QueryOptions.BATCHED_REQUESTS)) {
                requestIds = ((BuildDetail) build).getRequestIDs();
                requests = build.getRequests();
            }

            if (queryOptions.contains(QueryOptions.DEFINITIONS)) {
                setBuildDefinition(build.getBuildDefinition());
            }

            if (informationTypes != null) {
                // TODO_VNEXT: This should probably be synchronized with the
                // information node Save calls, etc.
                setInformation(build.getInformation());
                setBuildInformation(getInformation());
            }

            lastSnapshot = getSnapshot();
        }
    }

    private void setBuildInformation(final IBuildInformation information) {
        for (final IBuildInformationNode node : information.getNodes()) {
            ((BuildInformationNode) node).setBuild(this);
            setBuildInformation(node.getChildren());
        }
    }

    /**
     * Save any changes to this build to the server. {@inheritDoc}
     */
    @Override
    public void save() {
        // Lock on the last snapshot - don't want Save getting called by two
        // threads at the same time or our snapshots will get out of sync.
        synchronized (lastSnapshot) {
            // Get the current modifiable values for comparison with the last
            // snapshot.
            final BuildUpdateOptions currentSnapshot = getSnapshot();

            // Update the Fields of the current snapshot.
            currentSnapshot.setFields(compareSnapshots(lastSnapshot, currentSnapshot));

            // For a V2/V3 server, just update the entire build if anything has
            // changed.
            if (!currentSnapshot.getFields().equals(BuildUpdate.NONE)) {
                BuildDetail build = null;
                if (buildServer.getBuildServerVersion().isV2()) {
                    build = buildServer.getBuild2008Helper().updateBuilds(new BuildUpdateOptions[] {
                        currentSnapshot
                    })[0];
                } else if (buildServer.getBuildServerVersion().isV3()) {
                    build = buildServer.getBuild2010Helper().updateBuilds(new BuildUpdateOptions[] {
                        currentSnapshot
                    })[0];
                } else {
                    build = buildServer.getBuildService().updateBuilds(new BuildUpdateOptions[] {
                        currentSnapshot
                    })[0];
                }

                // Updating the status has the potential to start the
                // start/finish time (and perhaps other properties in the
                // future) so update our local copy if the status was changed.
                if (build != null && isDirty(currentSnapshot, BuildUpdate.STATUS)) {
                    copy(build, null, QueryOptions.NONE);
                }
            }

            lastSnapshot = currentSnapshot;
        }
    }

    /**
     * Attempt to stop this build. {@inheritDoc}
     */
    @Override
    public void stop() {
        buildServer.stopBuilds(new String[] {
            getURI()
        });
    }

    /**
     * Get the current update options for the BuildDetail for use in bulk
     * updates. NOTE: This is not a threadsafe operation, unlike the instance
     * Save method.
     *
     *
     * @return
     */
    public BuildUpdateOptions getUpdateOptions() {
        BuildUpdateOptions currentSnapshot;

        synchronized (lastSnapshot) {
            // Get the current modifiable values for comparison with the last
            // snapshot.
            currentSnapshot = getSnapshot();

            // Update the Fields of the current snapshot.
            currentSnapshot.setFields(compareSnapshots(lastSnapshot, currentSnapshot));
        }

        return currentSnapshot;
    }

    /**
     * Sets the last update options for the BuildDetail after a successful bulk
     * update. NOTE: This is not a threadsafe operation, unlike the instance
     * Save method.
     *
     *
     * @param snapshot
     */
    public void setUpdateOptions(final BuildUpdateOptions snapshot) {
        synchronized (lastSnapshot) {
            lastSnapshot = snapshot;
        }
    }

    /**
     * Returns the current values of all of the modifiable properties in a
     * BuildUpdateOptions object.
     *
     *
     * @return A BuildUpdateOptions object representing the current state of
     *         this BuildDetail.
     */
    private BuildUpdateOptions getSnapshot() {
        final BuildUpdateOptions result = new BuildUpdateOptions();

        result.setBuildNumber(getBuildNumber());
        result.setCompilationStatus(getCompilationStatus());
        // we save off the Uri to the drop in the snapshot, so DropLocation...
        result.setDropLocation(getDropLocation());
        result.setKeepForever(isKeepForever());
        result.setLabelName(getLabelName());
        result.setLogLocation(getLogLocation());
        result.setQuality(getQuality());
        result.setSourceGetVersion(getSourceGetVersion());
        result.setStatus(getStatus());
        result.setTestStatus(getTestStatus());
        result.setURI(getURI());

        return result;
    }

    /**
     * Update the Fields property of modifiedValues based on a comparison with
     * originalValues.
     *
     *
     * @param originalValues
     *        The original values.
     * @param modifiedValues
     *        The potentially modified values.
     * @return
     */
    private BuildUpdate compareSnapshots(
        final BuildUpdateOptions originalValues,
        final BuildUpdateOptions modifiedValues) {
        final BuildUpdate result = new BuildUpdate();

        if (!equalsIgnoreCase(originalValues.getBuildNumber(), modifiedValues.getBuildNumber())) {
            result.add(BuildUpdate.BUILD_NUMBER);
        }
        if (!originalValues.getCompilationStatus().equals(modifiedValues.getCompilationStatus())) {
            result.add(BuildUpdate.COMPILATION_STATUS);
        }
        if (!equalsIgnoreCase(originalValues.getDropLocation(), modifiedValues.getDropLocation())) {
            result.add(BuildUpdate.DROP_LOCATION);
        }
        if (originalValues.isKeepForever() != modifiedValues.isKeepForever()) {
            result.add(BuildUpdate.KEEP_FOREVER);
        }
        if (!equalsIgnoreCase(originalValues.getLabelName(), modifiedValues.getLabelName())) {
            result.add(BuildUpdate.LABEL_NAME);
        }
        if (!equalsIgnoreCase(originalValues.getLogLocation(), modifiedValues.getLogLocation())) {
            result.add(BuildUpdate.LOG_LOCATION);
        }
        if (!equalsIgnoreCase(originalValues.getQuality(), modifiedValues.getQuality())) {
            result.add(BuildUpdate.QUALITY);
        }
        if (!equalsIgnoreCase(originalValues.getSourceGetVersion(), modifiedValues.getSourceGetVersion())) {
            result.add(BuildUpdate.SOURCE_GET_VERSION);
        }
        if (!originalValues.getStatus().equals(modifiedValues.getStatus())) {
            result.add(BuildUpdate.STATUS);
        }
        if (!originalValues.getTestStatus().equals(modifiedValues.getTestStatus())) {
            result.add(BuildUpdate.TEST_STATUS);
        }

        return result;
    }

    private static boolean equalsIgnoreCase(final String s1, final String s2) {
        if (s1 == null) {
            return s2 == null;
        }
        return s1.equalsIgnoreCase(s2);
    }

    private boolean isDirty(final BuildUpdateOptions snapshot, final BuildUpdate.Field dirtyFlag) {
        return (snapshot.getFields().contains(dirtyFlag));
    }

    private String[] getDistinctRequestedByDisplayNames() {
        final Set<String> set = new HashSet<String>();

        for (final IQueuedBuild request : requests) {
            if (!set.contains(request.getRequestedByDisplayName())) {
                set.add(request.getRequestedForDisplayName());
            }
        }

        return set.toArray(new String[set.size()]);
    }

    private String[] getDistinctRequestedForDisplayNames() {
        final Set<String> set = new HashSet<String>();

        for (final IQueuedBuild request : requests) {
            if (!set.contains(request.getRequestedForDisplayName())) {
                set.add(request.getRequestedForDisplayName());
            }
        }

        return set.toArray(new String[set.size()]);
    }
}
