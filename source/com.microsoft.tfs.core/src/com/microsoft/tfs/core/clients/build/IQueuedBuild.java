// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.build;

import java.util.Calendar;
import java.util.List;

import com.microsoft.tfs.core.clients.build.flags.BuildReason;
import com.microsoft.tfs.core.clients.build.flags.QueryOptions;
import com.microsoft.tfs.core.clients.build.flags.QueueStatus;
import com.microsoft.tfs.core.clients.build.flags.QueuedBuildRetryOption;
import com.microsoft.tfs.core.clients.build.soapextensions.GetOption;
import com.microsoft.tfs.core.clients.build.soapextensions.QueuePriority;
import com.microsoft.tfs.util.GUID;

public interface IQueuedBuild extends Comparable<IQueuedBuild> {
    /**
     * The priority of the queued build.
     *
     *
     * @return
     */
    public QueuePriority getPriority();

    public void setPriority(QueuePriority value);

    /**
     * The batch ID of the queued build.
     *
     *
     * @return
     */
    public GUID getBatchID();

    /**
     * The unique Id of the queued build.
     *
     *
     * @return
     */
    public int getID();

    /**
     * Gets the team project that owns the queued build.
     *
     *
     * @return
     */
    public String getTeamProject();

    /**
     * The build controller on which the queued build will be built.
     *
     *
     * @return
     */
    public IBuildController getBuildController();

    /**
     * The URI of the build controller on which the queued build will be built.
     *
     *
     * @return
     */
    public String getBuildControllerURI();

    /**
     * The build definition for which the queued build will be built.
     *
     *
     * @return
     */
    public IBuildDefinition getBuildDefinition();

    /**
     * The URI of the build definition for which the queued build will be built.
     *
     *
     * @return
     */
    public String getBuildDefinitionURI();

    /**
     * Gets the collection of builds for this queue entry.
     *
     *
     * @return
     */
    public List<IBuildDetail> getBuilds();

    /**
     * Gets the currently active build.
     *
     *
     * @return
     */
    public IBuildDetail getBuild();

    /**
     * The server that owns this queued build.
     *
     *
     * @return
     */
    public IBuildServer getBuildServer();

    /**
     * If GetOption is CustomTime, the time for which sources should be
     * retrieved for the queued build.
     *
     *
     * @return
     */
    public String getCustomGetVersion();

    /**
     * The location at which to drop the outputs of the queued build.
     *
     *
     * @return
     */
    public String getDropLocation();

    /**
     * The time for which sources should be retrieved for the queued build -
     * either QueueTime, BuildTime, or CustomTime.
     *
     *
     * @return
     */
    public GetOption getGetOption();

    /**
     * The time at which the build was queued.
     *
     *
     * @return
     */
    public Calendar getQueueTime();

    /**
     * The process parameters used for this build.
     *
     *
     * @return
     */
    public String getProcessParameters();

    /**
     * The current position of the build in the queue.
     *
     *
     * @return
     */
    public int getQueuePosition();

    /**
     * The reason that the build was queued.
     *
     *
     * @return
     */
    public BuildReason getReason();

    /**
     * Gets the account name of the user who requested the build.
     *
     *
     * @return
     */
    public String getRequestedBy();

    /**
     * Gets the display name of the user who requested the build.
     *
     *
     * @return
     */
    public String getRequestedByDisplayName();

    /**
     * Gets the account name of the user for whom the build was requested.
     *
     *
     * @return
     */
    public String getRequestedFor();

    /**
     * Gets the display name of the user for whom the build was requested.
     *
     *
     * @return
     */
    public String getRequestedForDisplayName();

    /**
     * The shelveset that will be built.
     *
     *
     * @return
     */
    public String getShelvesetName();

    /**
     * The status of the queued build.
     *
     *
     * @return
     */
    public QueueStatus getStatus();

    /**
     * Removes the build from the queue.
     *
     *
     */
    public void cancel();

    /**
     * Copies the data from the queued build into the current instance. The
     * return value indicates whether or not anything in the queued build was
     * actually updated that would effect state.
     *
     *
     * @param build
     *        The source of the copy operation
     * @param options
     *        The options used to query the copy source
     * @return True if the build state changed, false otherwise
     */
    public boolean copy(IQueuedBuild build, QueryOptions options);

    /**
     * Postpones the queued build.
     *
     *
     */
    public void postpone();

    /**
     * Retrieves the latest property values from the server.
     *
     *
     * @param queryOptions
     */
    public void refresh(QueryOptions queryOptions);

    /**
     * Resumes the queued build.
     *
     *
     */
    public void resume();

    /**
     * Marks the build for retry without batching. If batching with other builds
     * is desired see IBuildServer.RetryQueuedBuilds.
     *
     *
     */
    public void retry();

    /**
     * Marks the build for retry and places it in the specified batch.
     *
     *
     * @param batchId
     *        The batch in which this build should be included
     */
    public void retry(GUID batchId);

    /**
     * Marks the build for retry and places it in the specified batch with the
     * specified retry option.
     *
     *
     * @param batchId
     *        The batch in which this build should be included
     * @param retryOption
     *        Option to retry a completed or an in progress build
     */
    public void retry(GUID batchId, QueuedBuildRetryOption retryOption);

    /**
     * Sends any changes made to the queued build to the server.
     *
     *
     */
    public void save();
}
