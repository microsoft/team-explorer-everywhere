// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.build;

import com.microsoft.tfs.core.clients.build.flags.QueryOptions;
import com.microsoft.tfs.core.clients.build.flags.QueueStatus;

public interface IQueuedBuildsView {
    /**
     * The build controller whose queued builds are included in the QueuedBuilds
     * property.
     *
     *
     * @return
     */
    public IBuildControllerSpec getControllerFilter();

    /**
     * The time span for which completed builds remain in the queue during
     * polling.
     *
     *
     * @return
     */
    public int getCompletedWindow();

    public void setCompletedWindow(int value);

    /**
     * The specification of the build definition(s) whose queued builds are
     * included in the QueuedBuilds property.
     *
     *
     * @return
     */
    public IBuildDefinitionSpec getDefinitionFilter();

    /**
     * The query options to use when querying the server.
     *
     *
     * @return
     */
    public QueryOptions getQueryOptions();

    public void setQueryOptions(QueryOptions value);

    /**
     * The requestor for which queued builds are included in the QueuedBuilds
     * property.
     *
     *
     * @return
     */
    public String getRequestedForFilter();

    public void setRequestedForFilter(String value);

    /**
     * The queue status(es) for which queued builds are included in the
     * QueuedBuilds property.
     *
     *
     * @return
     */
    public QueueStatus getStatusFilter();

    public void setStatusFilter(QueueStatus value);

    /**
     * The team project whose queued builds are included in the QueuedBuilds
     * property.
     *
     *
     * @return
     */
    public String getTeamProjectFilter();

    /**
     * The queued builds in the team project which match the specified
     * AgentFilter, DefinitionFilter, and StatusFilter. Both the array and the
     * individual IQueuedBuild items are updated in place during polling and
     * during manual refreshes.
     *
     *
     * @return
     */
    public IQueuedBuild[] getQueuedBuilds();
}
