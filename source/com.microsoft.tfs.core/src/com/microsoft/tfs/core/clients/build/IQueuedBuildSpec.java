// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.build;

import com.microsoft.tfs.core.clients.build.flags.QueryOptions;
import com.microsoft.tfs.core.clients.build.flags.QueueStatus;

public interface IQueuedBuildSpec {
    /**
     * Gets the controller spec for this build queue specification.
     *
     *
     * @return
     */
    public IBuildControllerSpec getControllerSpec();

    /**
     * Gets the definition spec for this build queue specification.
     *
     *
     * @return
     */
    public IBuildDefinitionSpec getDefinitionSpec();

    /**
     * Gets the definitioin URIs for this build queue specification.
     *
     *
     * @return
     */
    String[] getDefinitionURIs();

    /**
     * Gets or sets the query options used for this queue query.
     *
     *
     * @return
     */
    public QueryOptions getQueryOptions();

    public void setQueryOptions(QueryOptions value);

    /**
     * The user for whom the build was requested.
     *
     *
     * @return
     */
    public String getRequestedFor();

    public void setRequestedFor(String value);

    /**
     * Gets or sets the status filter used for this queue query.
     *
     *
     * @return
     */
    public QueueStatus getStatus();

    public void setStatus(QueueStatus value);

    /**
     * Gets or sets the window used to query for completed builds. Queued builds
     * that have been completed for a greater duration than the window specified
     * will not be included in the query results.
     *
     *
     * @return
     */
    public int getCompletedAge();

    public void setCompletedAge(int value);
}
