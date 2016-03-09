// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.build;

import com.microsoft.tfs.core.clients.build.flags.BuildReason;
import com.microsoft.tfs.core.clients.build.soapextensions.GetOption;
import com.microsoft.tfs.core.clients.build.soapextensions.QueuePriority;
import com.microsoft.tfs.util.GUID;

public interface IBuildRequest {
    /**
     * The batch ID for the request.
     *
     *
     * @return
     */
    public GUID getBatchID();

    public void setBatchID(GUID value);

    /**
     * The build controller on which the requested build should be started.
     *
     *
     * @return
     */
    public IBuildController getBuildController();

    public void setBuildController(IBuildController value);

    /**
     * The Uri of the build controller on which the requested build should be
     * started.
     *
     *
     * @return
     */
    public String getBuildControllerURI();

    /**
     * The build definition for which the requested build should be started.
     * Provides default values for BuildController and DropLocation.
     *
     *
     * @return
     */
    public IBuildDefinition getBuildDefinition();

    /**
     * The Uri of the build definition for which the requested build should be
     * started.
     *
     *
     * @return
     */
    public String getBuildDefinitionURI();

    public void setBuildDefinitionURI(String value);

    /**
     * If GetOption is Custom, the version spec for which sources should be
     * retrieved for the requested build.
     *
     *
     * @return
     */
    public String getCustomGetVersion();

    public void setCustomGetVersion(String value);

    /**
     * The location to drop the outputs of the requested build.
     *
     *
     * @return
     */
    public String getDropLocation();

    public void setDropLocation(String value);

    /**
     * Specifies how sources should be retrieved for the requested build -
     * either LatestOnQueue - get the latest sources as of the time the build
     * was queued, LatestOnBuild - get the latest sources as of the time the
     * build starts, or Custom - If you set this to custom, you must specify a
     * valid VersionSpec in the CustomGetVersion field.
     *
     *
     * @return
     */
    public GetOption getGetOption();

    public void setGetOption(GetOption value);

    /**
     * The maximum position in the queue for the requested build at queue time.
     * If the build request falls below this position in a call to QueueBuild an
     * exception will be thrown.
     *
     *
     * @return
     */
    public int getMaxQueuePosition();

    public void setMaxQueuePosition(int value);

    /**
     * Determines whether or not the request will be submitted with a postponed
     * status.
     *
     *
     * @return
     */
    public boolean isPostponed();

    public void setPostponed(boolean value);

    /**
     * The priority for the requested build.
     *
     *
     * @return
     */
    public QueuePriority getPriority();

    public void setPriority(QueuePriority value);

    /**
     * The process parameters used to initialize the build process.
     *
     *
     * @return
     */
    public String getProcessParameters();

    public void setProcessParameters(String value);

    /**
     * The user for whom the build is being requested.
     *
     *
     * @return
     */
    public String getRequestedFor();

    public void setRequestedFor(String value);

    /**
     * The reason to be used for the build request.
     *
     *
     * @return
     */
    public BuildReason getReason();

    public void setReason(BuildReason value);

    /**
     * Optional shelveset to be built.
     *
     *
     * @return
     */
    public String getShelvesetName();

    public void setShelvesetName(String value);

    /**
     * Optional ticket issued by the server for gated check-in submissions. When
     * a check-in is rejected due to Gated Check-in a <see cref=
     * "Microsoft.TeamFoundation.VersionControl.Client.GatedCheckinException" >
     * GatedCheckinException</see> is thrown. The gated check-in ticket is
     * available on the exception and should be used when submitting the build
     * request to the build system. Setting this property from the exception
     * allows a rejected check-in to be queued by a user who does not have the
     * appropriate permissions to perform the action under normal circumstances.
     *
     *
     * @return
     */
    public String getGatedCheckInTicket();

    public void setGatedCheckInTicket(String value);

    /**
     * Gets the build server from which this build request was created.
     *
     *
     * @return
     */
    public IBuildServer getBuildServer();
}
