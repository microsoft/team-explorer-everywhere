// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.build.internal.soapextensions;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import com.microsoft.tfs.core.Messages;
import com.microsoft.tfs.core.clients.build.IBuildController;
import com.microsoft.tfs.core.clients.build.IBuildDefinition;
import com.microsoft.tfs.core.clients.build.IBuildDetail;
import com.microsoft.tfs.core.clients.build.IBuildServer;
import com.microsoft.tfs.core.clients.build.IQueuedBuild;
import com.microsoft.tfs.core.clients.build.exceptions.QueuedBuildDoesNotExistException;
import com.microsoft.tfs.core.clients.build.flags.BuildReason;
import com.microsoft.tfs.core.clients.build.flags.BuildStatus;
import com.microsoft.tfs.core.clients.build.flags.QueryOptions;
import com.microsoft.tfs.core.clients.build.flags.QueueStatus;
import com.microsoft.tfs.core.clients.build.flags.QueuedBuildRetryOption;
import com.microsoft.tfs.core.clients.build.flags.QueuedBuildUpdate;
import com.microsoft.tfs.core.clients.build.soapextensions.GetOption;
import com.microsoft.tfs.core.clients.build.soapextensions.QueuePriority;
import com.microsoft.tfs.core.exceptions.NotSupportedException;
import com.microsoft.tfs.core.internal.wrappers.WebServiceObjectWrapper;
import com.microsoft.tfs.util.GUID;
import com.microsoft.tfs.util.StringUtil;
import com.microsoft.tfs.util.GUID.GUIDStringFormat;
import com.microsoft.tfs.util.datetime.DotNETDate;

import ms.tfs.build.buildservice._04._QueuedBuild;

public class QueuedBuild extends WebServiceObjectWrapper implements IQueuedBuild {
    private IBuildController buildController;
    private IBuildDefinition buildDefinition;
    private BuildServer buildServer;
    private QueuedBuildUpdateOptions lastSnapshot;
    private boolean postponed;
    private boolean retry;
    private QueuedBuildRetryOption retryOption = QueuedBuildRetryOption.NONE;

    private BuildDetail build;
    private List<IBuildDetail> allBuilds = new ArrayList<IBuildDetail>();

    private QueuedBuild(final IBuildServer buildServer) {
        super(new _QueuedBuild());
        this.buildServer = (BuildServer) buildServer;

        final _QueuedBuild _o = getWebServiceObject();
        _o.setBuildUris(new String[0]);
        _o.setGetOption(GetOption.LATEST_ON_BUILD.getWebServiceObject());
        _o.setPriority(QueuePriority.NORMAL.getWebServiceObject());
        _o.setQueueTime(DotNETDate.MIN_CALENDAR);
        _o.setReason(BuildReason.MANUAL.getWebServiceObject());
        _o.setStatus(QueueStatus.NONE.getWebServiceObject());
    }

    public QueuedBuild(final IBuildServer buildServer, final _QueuedBuild webSerivceObject) {
        super(webSerivceObject);
        this.buildServer = (BuildServer) buildServer;

        afterDeserialize();
    }

    public QueuedBuild(final IBuildServer buildServer, final QueuedBuild2010 build2010) {
        this(buildServer);

        final _QueuedBuild qb = getWebServiceObject();
        qb.setBuildControllerUri(build2010.getBuildControllerUri());
        qb.setBuildDefinitionUri(build2010.getBuildDefinitionUri());

        if (build2010.getBuild() != null) {
            final BuildDetail currentBuild = TFS2010Helper.convert(buildServer, build2010.getBuild());

            this.build = currentBuild;
            this.allBuilds.add(currentBuild);
            qb.setBuildUris(new String[] {
                currentBuild.getURI()
            });
        }

        qb.setCustomGetVersion(build2010.getCustomGetVersion());
        qb.setDropLocation(build2010.getDropLocation());
        qb.setGetOption(TFS2010Helper.convert(build2010.getGetOption()).getWebServiceObject());
        qb.setId(build2010.getID());
        qb.setPriority(TFS2010Helper.convert(build2010.getPriority()).getWebServiceObject());
        qb.setProcessParameters(build2010.getProcessParameters());
        qb.setQueuePosition(build2010.getQueuePosition());
        qb.setQueueTime(build2010.getQueueTime());
        qb.setReason(TFS2010Helper.convert(build2010.getReason()).getWebServiceObject());
        qb.setRequestedBy(build2010.getRequestedBy());
        qb.setRequestedFor(build2010.getRequestedFor());
        qb.setShelvesetName(build2010.getShelvesetName());
        qb.setStatus(TFS2010Helper.convert(build2010.getStatus()).getWebServiceObject());
        qb.setTeamProject(build2010.getTeamProject());

        afterDeserialize();
    }

    /**
     * This constructor is for V2 compatibility and should not be used
     * otherwise.
     *
     *
     * @param buildServer
     * @param build2008
     */
    public QueuedBuild(final BuildServer buildServer, final QueuedBuild2008 build2008) {
        this(buildServer);

        setBuildControllerURI(build2008.getBuildAgentURI());
        setBuildDefinitionURI(build2008.getBuildDefinitionURI());
        getWebServiceObject().setId(build2008.getID());
        setPriority(TFS2010Helper.convert(build2008.getPriority()));
        setQueuePosition(build2008.getQueuePosition());
        getWebServiceObject().setQueueTime(build2008.getQueueTime());
        getWebServiceObject().setRequestedBy(build2008.getRequestedBy());
        getWebServiceObject().setRequestedFor(build2008.getRequestedFor());
        setStatus(TFS2010Helper.convert(build2008.getStatus()));

        final BuildDetail build = TFS2008Helper.convert(buildServer, build2008.getBuild());
        if (build != null) {
            this.build = build;
            this.allBuilds.add(this.build);
            getWebServiceObject().setBuildUris(new String[] {
                this.build.getURI()
            });
        }

        afterDeserialize();
    }

    /**
     * This constructor creates a "fake" request that will hang off the build.
     * This constructor is for V3 compatibility and should not be used
     * otherwise.
     *
     *
     * @param buildServer
     * @param build
     */
    public QueuedBuild(final IBuildServer buildServer, final BuildDetail build) {
        this(buildServer);

        final _QueuedBuild qb = getWebServiceObject();
        qb.setBuildControllerUri(build.getBuildControllerURI());
        this.buildServer = (BuildServer) build.getBuildServer();

        qb.setBuildDefinitionUri(build.getBuildDefinitionURI());
        qb.setDropLocation(build.getDropLocation());
        qb.setId(0);
        qb.setPriority(QueuePriority.NORMAL.getWebServiceObject());
        qb.setQueuePosition(1);
        qb.setQueueTime(build.getStartTime());
        qb.setReason(build.getReason().getWebServiceObject());
        qb.setRequestedBy(build.getRequestedBy());
        qb.setRequestedFor(build.getRequestedFor());
        qb.setShelvesetName(build.getShelvesetName());
        qb.setTeamProject(build.getTeamProject());

        if (build.getStatus().equals(BuildStatus.IN_PROGRESS)) {
            qb.setStatus(QueueStatus.IN_PROGRESS.getWebServiceObject());
        } else {
            qb.setStatus(QueueStatus.COMPLETED.getWebServiceObject());
        }

        this.allBuilds.add(build);
        qb.setBuildUris(new String[] {
            build.getURI()
        });

        afterDeserialize();
    }

    private void afterDeserialize() {
        this.postponed = (getStatus().equals(QueueStatus.POSTPONED));
        this.lastSnapshot = getSnapshot();

        if (StringUtil.isNullOrEmpty(getRequestedByDisplayName())) {
            getWebServiceObject().setRequestedByDisplayName(getRequestedBy());
        }

        if (StringUtil.isNullOrEmpty(getRequestedForDisplayName())) {
            getWebServiceObject().setRequestedForDisplayName(getRequestedFor());
        }

        if (getBuildURIs() == null) {
            getWebServiceObject().setBuildUris(new String[0]);
        }
    }

    public _QueuedBuild getWebServiceObject() {
        return (_QueuedBuild) this.webServiceObject;
    }

    public String[] getBuildURIs() {
        return getWebServiceObject().getBuildUris();
    }

    /**
     * Gets the batch ID for this build. This field is read-only. {@inheritDoc}
     */
    @Override
    public GUID getBatchID() {
        return new GUID(getWebServiceObject().getBatchId());
    }

    /**
     * Gets the URI of the build controller. This field is read-only.
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
     * Gets the URI of the build definition. This field is read-only.
     * {@inheritDoc}
     */
    @Override
    public String getBuildDefinitionURI() {
        return getWebServiceObject().getBuildDefinitionUri();
    }

    public void setBuildDefinitionURI(final String value) {
        getWebServiceObject().setBuildDefinitionUri(value);
    }

    /**
     * Gets the version of sources to download for the build if GetOption.Custom
     * is specified. This field is read-only. {@inheritDoc}
     */
    @Override
    public String getCustomGetVersion() {
        return getWebServiceObject().getCustomGetVersion();
    }

    /**
     * Gets the drop location to use for the build. A null value indicates the
     * default drop location of the build definition. This field is read-only.
     * {@inheritDoc}
     */
    @Override
    public String getDropLocation() {
        return getWebServiceObject().getDropLocation();
    }

    /**
     * Gets the desired option for determing the version of sources to download
     * for the build. This field is read-only. {@inheritDoc}
     */
    @Override
    public GetOption getGetOption() {
        return GetOption.fromWebServiceObject(getWebServiceObject().getGetOption());
    }

    /**
     * Gets the ID. This field is read-only. {@inheritDoc}
     */
    @Override
    public int getID() {
        return getWebServiceObject().getId();
    }

    /**
     * Gets the priority in the queue. This field is read-only. {@inheritDoc}
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
     * Gets the process parameters to use for the build. Parameters which are
     * not explicitly overridden by the build will inherit values from the build
     * definition. This field is read-only. {@inheritDoc}
     */
    @Override
    public String getProcessParameters() {
        return getWebServiceObject().getProcessParameters();
    }

    /**
     * Gets the current queue depth. This field is read-only. {@inheritDoc}
     */
    @Override
    public int getQueuePosition() {
        return getWebServiceObject().getQueuePosition();
    }

    public void setQueuePosition(final int value) {
        getWebServiceObject().setQueuePosition(value);
    }

    /**
     * Gets the date and time this entry was created. This field is read-only.
     * {@inheritDoc}
     */
    @Override
    public Calendar getQueueTime() {
        return getWebServiceObject().getQueueTime();
    }

    /**
     * Gets the reason this build was queued. This field is read-only.
     * {@inheritDoc}
     */
    @Override
    public BuildReason getReason() {
        return BuildReason.fromWebServiceObject(getWebServiceObject().getReason());
    }

    public void setReason(final BuildReason value) {
        getWebServiceObject().setReason(value.getWebServiceObject());
    }

    /**
     * Gets the domain user name of the user that requested the build. This
     * field is read-only. {@inheritDoc}
     */
    @Override
    public String getRequestedBy() {
        return getWebServiceObject().getRequestedBy();
    }

    /**
     * Gets the display name of the user that requested the build. This field is
     * read-only. {@inheritDoc}
     */
    @Override
    public String getRequestedByDisplayName() {
        return getWebServiceObject().getRequestedByDisplayName();
    }

    /**
     * Gets the domain user name of the user this build was requested for. This
     * field is read-only. {@inheritDoc}
     */
    @Override
    public String getRequestedFor() {
        return getWebServiceObject().getRequestedFor();
    }

    /**
     * Gets the display name of the user this build was requested for. This
     * field is read-only. {@inheritDoc}
     */
    @Override
    public String getRequestedForDisplayName() {
        return getWebServiceObject().getRequestedForDisplayName();
    }

    /**
     * Gets the name of the shelveset to incorporate in this build. This field
     * is read-only. {@inheritDoc}
     */
    @Override
    public String getShelvesetName() {
        return getWebServiceObject().getShelvesetName();
    }

    /**
     * Gets the current status. This field is read-only. {@inheritDoc}
     */
    @Override
    public QueueStatus getStatus() {
        return QueueStatus.fromWebServiceObject(getWebServiceObject().getStatus());
    }

    public void setStatus(final QueueStatus value) {
        getWebServiceObject().setStatus(value.getWebServiceObject());
    }

    /**
     * Gets the team project. This field is read-only. {@inheritDoc}
     */
    @Override
    public String getTeamProject() {
        return getWebServiceObject().getTeamProject();
    }

    public List<IBuildDetail> getAllBuilds() {
        return allBuilds;
    }

    @Override
    public List<IBuildDetail> getBuilds() {
        return allBuilds;
    }

    @Override
    public IBuildDetail getBuild() {
        if (allBuilds.size() > 0) {
            return allBuilds.get(allBuilds.size() - 1);
        }
        return null;
    }

    /**
     * The build controller on which the queued build will be built.
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
     * The build definition from which this queued build was generated.
     * {@inheritDoc}
     */
    @Override
    public IBuildDefinition getBuildDefinition() {
        return buildDefinition;
    }

    public void setBuildDefinition(final IBuildDefinition value) {
        buildDefinition = value;

        // Compat: Orcas servers don't return the TeamProject for the Queued
        // Build
        if (buildDefinition != null) {
            if (StringUtil.isNullOrEmpty(getTeamProject())) {
                // Set Team Project to match the definition
                getWebServiceObject().setTeamProject(buildDefinition.getTeamProject());
            }
        }
    }

    @Override
    public IBuildServer getBuildServer() {
        return buildServer;
    }

    public void setBuildServer(final IBuildServer value) {
        buildServer = (BuildServer) value;
    }

    /**
     * Removes the build from the queue. {@inheritDoc}
     */
    @Override
    public void cancel() {
        getBuildServer().cancelBuilds(new int[] {
            this.getID()
        });
    }

    /**
     * Postpones the queued build. {@inheritDoc}
     */
    @Override
    public void postpone() {
        postponed = true;
    }

    /**
     * Retrieves the latest property values from the server. Throws an
     * InvalidQueueIdException if the queued build is no longer in the queue.
     * {@inheritDoc}
     */
    @Override
    public void refresh(final QueryOptions queryOptions) {
        BuildQueueQueryResult result;
        if (buildServer.getBuildServerVersion().isV2()) {
            result = buildServer.getBuild2008Helper().queryQueuedBuildsById(new int[] {
                this.getID()
            }, queryOptions);
        } else if (buildServer.getBuildServerVersion().isV3()) {
            result = buildServer.getBuild2010Helper().queryQueuedBuildsById(new int[] {
                this.getID()
            }, queryOptions);
        } else {
            result = buildServer.getBuildQueueService().queryBuildsById(new int[] {
                this.getID()
            }, null, queryOptions);
        }

        final IQueuedBuild[] queuedBuilds = result.getQueuedBuilds();
        if (queuedBuilds.length == 0 || queuedBuilds[0] == null) {
            final String format = Messages.getString("QueuedBuild2012.DoesNotExistFormat"); //$NON-NLS-1$
            final String message = MessageFormat.format(format, getID());
            throw new QueuedBuildDoesNotExistException(message);
        }

        copy(result.getQueuedBuilds()[0], queryOptions);
    }

    /**
     * Resumes the queued build. {@inheritDoc}
     */
    @Override
    public void resume() {
        postponed = false;
    }

    @Override
    public void retry() {
        retry(GUID.newGUID());
    }

    @Override
    public void retry(final GUID batchId) {
        // Retry only works for queued builds that are in progress
        retry(batchId, QueuedBuildRetryOption.IN_PROGRESS_BUILD);

        // This flag is here to keep client server compatibility for Pioneer
        retry = true;
    }

    @Override
    public void retry(final GUID batchId, final QueuedBuildRetryOption retryOption) {
        if (buildServer.getBuildServerVersion().isLessThanV4()) {
            final String format = Messages.getString("QueuedBuild2012.MethodNotSupportedFormat"); //$NON-NLS-1$
            final String message = MessageFormat.format(format, "Retry", "IQueuedBuild"); //$NON-NLS-1$ //$NON-NLS-2$
            throw new NotSupportedException(message);
        }

        if (retryOption == QueuedBuildRetryOption.NONE) {
            final String format = Messages.getString("QueuedBuild2012.InvalidArgumentFormat"); //$NON-NLS-1$
            final String message = MessageFormat.format(format, QueuedBuildRetryOption.NONE.toString());
            throw new IllegalArgumentException(message);
        }

        this.retryOption = retryOption;
        getWebServiceObject().setBatchId(batchId.getGUIDString(GUIDStringFormat.NONE));
    }

    /**
     * Sends any changes made to the queued build to the server. {@inheritDoc}
     */
    @Override
    public void save() {
        synchronized (lastSnapshot) {
            final QueuedBuildUpdateOptions currentSnapshot = getSnapshot();

            currentSnapshot.setFields(compareSnapshots(lastSnapshot, currentSnapshot));

            IQueuedBuild[] results;
            if (currentSnapshot.getFields() != QueuedBuildUpdate.NONE) {
                if (buildServer.getBuildServerVersion().isV2()) {
                    results = buildServer.getBuild2008Helper().updateQueuedBuilds(new QueuedBuildUpdateOptions[] {
                        currentSnapshot
                    });
                } else if (buildServer.getBuildServerVersion().isV3()) {
                    results = buildServer.getBuild2010Helper().updateQueuedBuilds(new QueuedBuildUpdateOptions[] {
                        currentSnapshot
                    });
                } else {
                    results = buildServer.getBuildQueueService().updateBuilds(new QueuedBuildUpdateOptions[] {
                        currentSnapshot
                    }).getQueuedBuilds();
                }

                copy(results[0], QueryOptions.NONE);
            }

            lastSnapshot = currentSnapshot;
        }
    }

    /**
     * Get the current update options for the QueuedBuild for use in bulk
     * updates. NOTE: This is not a threadsafe operation, unlike the instance
     * Save method.
     *
     *
     * @return
     */
    public QueuedBuildUpdateOptions getUpdateOptions() {
        QueuedBuildUpdateOptions currentSnapshot;

        synchronized (lastSnapshot) {
            currentSnapshot = getSnapshot();

            currentSnapshot.setFields(compareSnapshots(lastSnapshot, currentSnapshot));
        }

        return currentSnapshot;
    }

    /**
     * Sets the last update options for the QueuedBuild after a successful bulk
     * update. NOTE: This is not a threadsafe operation, unlike the instance
     * Save method.
     *
     *
     * @param snapshot
     */
    public void setUpdateOptions(final QueuedBuildUpdateOptions snapshot) {
        synchronized (lastSnapshot) {
            lastSnapshot = snapshot;
        }
    }

    /**
     * Internal copy method that may be used to determine if any of the fields
     * in the queued build actually {@inheritDoc}
     */
    @Override
    public boolean copy(final IQueuedBuild build, final QueryOptions queryOptions) {
        final boolean changed = !getPriority().equals(build.getPriority())
            || getQueuePosition() != build.getQueuePosition()
            || !getStatus().equals(build.getStatus());

        synchronized (lastSnapshot) {
            this.allBuilds = new ArrayList<IBuildDetail>(build.getBuilds());

            getWebServiceObject().setBatchId(build.getBatchID().getGUIDString(GUIDStringFormat.NONE));
            getWebServiceObject().setBuildUris(((QueuedBuild) build).getBuildURIs());
            getWebServiceObject().setPriority(build.getPriority().getWebServiceObject());
            getWebServiceObject().setQueuePosition(build.getQueuePosition());
            getWebServiceObject().setStatus(build.getStatus().getWebServiceObject());
            this.postponed = (this.getStatus().equals(QueueStatus.POSTPONED));
            this.retry = (this.getStatus().equals(QueueStatus.RETRY));
            this.retryOption = ((QueuedBuild) build).retryOption;
            getWebServiceObject().setQueueTime(build.getQueueTime());

            // If the Build Definition changed (e.g. rename), we want to pick up
            // that change.
            if (queryOptions.contains(QueryOptions.DEFINITIONS)) {
                setBuildDefinition(build.getBuildDefinition());
            }

            lastSnapshot = getSnapshot();
        }

        return changed;
    }

    private QueuedBuildUpdateOptions getSnapshot() {
        final QueuedBuildUpdateOptions result = new QueuedBuildUpdateOptions();

        result.setQueueID(getID());
        result.setPostponed(postponed);
        result.setPriority(getPriority());
        result.setBatchID(getBatchID());
        result.setRetry(retry);
        result.setRetryOption(retryOption);

        return result;
    }

    private QueuedBuildUpdate compareSnapshots(
        final QueuedBuildUpdateOptions originalValues,
        final QueuedBuildUpdateOptions modifiedValues) {
        QueuedBuildUpdate result = QueuedBuildUpdate.NONE;

        if (!originalValues.getBatchID().equals(modifiedValues.getBatchID())) {
            result = result.combine(QueuedBuildUpdate.BATCHID);
        }
        if (originalValues.isPostponed() != modifiedValues.isPostponed()) {
            result = result.combine(QueuedBuildUpdate.POSTPONED);
        }
        if (!originalValues.getPriority().equals(modifiedValues.getPriority())) {
            result = result.combine(QueuedBuildUpdate.PRIORITY);
        }
        if (originalValues.isRetry() != modifiedValues.isRetry()) {
            result = result.combine(QueuedBuildUpdate.RETRY);
        }
        if (!originalValues.getRetryOption().equals(modifiedValues.getRetryOption())) {
            result = result.combine(QueuedBuildUpdate.REQUEUE);
        }

        return result;
    }

    public int compareTo(final QueuedBuild build) {
        return compareTo((IQueuedBuild) build);
    }

    @Override
    public int compareTo(final IQueuedBuild build) {
        int result;

        if (getQueuePosition() > 0 && build.getQueuePosition() > 0) {
            // Sort items that are actually in a Queue. Queue position can't be
            // used because the Agents may be different and thus the queue
            // positions are not unique. So, look at the status first and then
            // priority
            result = 0;
            if (getStatus().toIntFlags() > build.getStatus().toIntFlags()) {
                result = 1;
            } else if (getStatus().toIntFlags() < build.getStatus().toIntFlags()) {
                result = -1;
            }

            // If the statuses are the same then compare the priority. The order
            // of the values of the QueuePriority enum are in the correct order
            // so, just do a simple comparison.
            if (result == 0) {
                result = getPriority().compareTo(build.getPriority());
            }
        } else {
            // Sort special QueuePositions: Canceled = -2, Complete = -1,
            // InProgress = 0 The position is determined on the server by
            // looking at Queue, status, priority
            result = new Integer(getQueuePosition()).compareTo(build.getQueuePosition());
        }

        // Re-sort completed builds by FinishTime.
        if (result == 0) {
            if (getBuild() != null && build.getBuild() != null) {
                result = getBuild().getFinishTime().compareTo(build.getBuild().getFinishTime());
            } else {
                result = getQueueTime().compareTo(build.getQueueTime());
            }

            // The last sort is by Id so we get a deterministic sort order
            if (result == 0) {
                result = new Integer(getID()).compareTo(build.getID());
            }
        }

        return result;
    }
}
