// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.build;

import java.util.Calendar;

import com.microsoft.tfs.core.clients.build.flags.BuildReason;
import com.microsoft.tfs.core.clients.build.flags.BuildStatus;
import com.microsoft.tfs.core.clients.build.flags.DeleteOptions;
import com.microsoft.tfs.core.clients.build.flags.QueryOptions;
import com.microsoft.tfs.core.clients.build.internal.soapextensions.BuildPhaseStatus;
import com.microsoft.tfs.util.GUID;

public interface IBuildDetail {
    /**
     * The Number used to identify the build.
     *
     *
     * @return
     */
    public String getBuildNumber();

    public void setBuildNumber(String buildNumber);

    /**
     * The status of the compilation phase of the build.
     *
     *
     * @return
     */
    public BuildPhaseStatus getCompilationStatus();

    public void setCompilationStatus(BuildPhaseStatus compilationStatus);

    /**
     * The location of the output of the build - typically
     * DropLocationRoot\BuildDefinition.Name\BuildNumber.
     *
     *
     * @return
     */
    public String getDropLocation();

    public void setDropLocation(String dropLocation);

    /**
     * The root drop location of the build - either the DefaultDropLocation from
     * its IBuildDefinition, or the overridden DropLocation from its
     * IBuildRequest.
     *
     *
     * @return
     */
    public String getDropLocationRoot();

    /**
     * The name of the label created for the build.
     *
     *
     * @return
     */
    public String getLabelName();

    public void setLabelName(String labelName);

    /**
     * Specifies whether the build participates in its definition's retention
     * policy or should be kept forever.
     *
     *
     * @return
     */
    public boolean isKeepForever();

    public void setKeepForever(boolean keepForever);

    /**
     * The location of the log file for the build.
     *
     *
     * @return
     */
    public String getLogLocation();

    public void setLogLocation(String logLocation);

    /**
     * The quality of the build.
     *
     *
     * @return
     */
    public String getQuality();

    public void setQuality(String buildQuality);

    /**
     * The overall status of the build.
     *
     *
     * @return
     */
    public BuildStatus getStatus();

    public void setStatus(BuildStatus status);

    /**
     * The status of the test phase of the build.
     *
     *
     * @return
     */
    public BuildPhaseStatus getTestStatus();

    public void setTestStatus(BuildPhaseStatus testStatus);

    /**
     * The IBuildController used to perform the build. (May be null)
     *
     *
     * @return
     */
    public IBuildController getBuildController();

    /**
     * The Uri of the IBuildController used to perform the build.
     *
     *
     * @return
     */
    public String getBuildControllerURI();

    /**
     * The IBuildDefinition that owns the build. (May be null)
     *
     *
     * @return
     */
    public IBuildDefinition getBuildDefinition();

    /**
     * The Uri of the IBuildDefinition used to perform the build.
     *
     *
     * @return
     */
    public String getBuildDefinitionURI();

    /**
     * Indicates whether or not the build has finished.
     *
     *
     * @return
     */
    public boolean isBuildFinished();

    /**
     * The server that owns the build.
     *
     *
     * @return
     */
    public IBuildServer getBuildServer();

    /**
     * The collection of information nodes for the build.
     *
     *
     * @return
     */
    public IBuildInformation getInformation();

    /**
     * Gets the account name of the last user to change the build.
     *
     *
     * @return
     */
    public String getLastChangedBy();

    /**
     * Gets the display name of the last user to change the build.
     *
     *
     * @return
     */
    public String getLastChangedByDisplayName();

    /**
     * The date and time of the last change to the build.
     *
     *
     * @return
     */
    public Calendar getLastChangedOn();

    /**
     * The process parameters used for this build.
     *
     *
     * @return
     */
    public String getProcessParameters();

    /**
     * The reason the build exists.
     *
     *
     * @return
     */
    public BuildReason getReason();

    /**
     * The request Ids that started this build.
     *
     *
     * @return
     */
    public int[] getRequestIDs();

    /**
     * The requests that started this build.
     *
     *
     * @return
     */
    public IQueuedBuild[] getRequests();

    /**
     * The flag that indicates that the build has been deleted
     *
     *
     * @return
     */
    public boolean isIsDeleted();

    /**
     * The version specification for which the sources were retrieved for the
     * build.
     *
     *
     * @return
     */
    public String getSourceGetVersion();

    /**
     * The time that the build actually started
     *
     *
     * @return
     */
    public Calendar getStartTime();

    /**
     * The time that the build finished.
     *
     *
     * @return
     */
    public Calendar getFinishTime();

    /**
     * The URI of the build.
     *
     *
     * @return
     */
    public String getURI();

    /**
     * Gets the team project that owns the build.
     *
     *
     * @return
     */
    public String getTeamProject();

    /**
     * Deletes the build, and all associated data, from the server and drop
     * location.
     *
     *
     * @return
     */
    public IBuildDeletionResult delete();

    /**
     * Delete the build and only the associated information you specify.
     *
     *
     * @param options
     *        The parts of the build to delete.
     * @return
     */
    public IBuildDeletionResult delete(DeleteOptions options);

    /**
     * Retrieves the latest build data from the server with the given query
     * options and information types.
     *
     *
     * @param informationTypes
     *        The information types which should be retrieved. Valid types
     *        include "*", meaning all types, and the members of
     *        Microsoft.TeamFoundation.Build.Common.InformationTypes.
     * @param queryOptions
     *        The query options to use for the Refresh.
     */
    public void refresh(String[] informationTypes, QueryOptions queryOptions);

    /**
     * Saves any changes made to the build to the server.
     *
     *
     */
    public void save();

    /**
     * Stops the build.
     *
     *
     */
    public void stop();

    /**
     * Requests the intermediate diagnostics logs of an in-progress build.
     *
     *
     * @return The request identifier
     */
    public GUID requestIntermediateLogs();

    /**
     * The user that requested the build.
     *
     *
     * @return
     */
    public String getRequestedBy();

    /**
     * The user for whom the build was requested.
     *
     *
     * @return
     */
    public String getRequestedFor();

    /**
     * The shelveset that was built.
     *
     *
     * @return
     */
    public String getShelvesetName();

}
