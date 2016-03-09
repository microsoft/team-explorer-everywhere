// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.build;

import java.util.Calendar;

import com.microsoft.tfs.core.clients.build.flags.BuildQueryOrder;
import com.microsoft.tfs.core.clients.build.flags.BuildReason;
import com.microsoft.tfs.core.clients.build.flags.BuildStatus;
import com.microsoft.tfs.core.clients.build.flags.QueryDeletedOption;
import com.microsoft.tfs.core.clients.build.flags.QueryOptions;

public interface IBuildDetailSpec {
    /**
     * The number of the desired build(s). Wildcards supported.
     *
     *
     * @return
     */
    public String getBuildNumber();

    public void setBuildNumber(String value);

    /**
     * The specification of the definition of the desired build(s).
     *
     *
     * @return
     */
    public IBuildDefinitionSpec getDefinitionSpec();

    /**
     * The specification of the definition of the desired build(s).
     *
     *
     * @return
     */
    public String[] getDefinitionURIs();

    /**
     * The additional data to be returned from the query/queries.
     *
     *
     * @return
     */
    public QueryOptions getQueryOptions();

    public void setQueryOptions(QueryOptions value);

    /**
     * The ordering which should be used when selecting a max number of builds.
     *
     *
     * @return
     */
    public BuildQueryOrder getQueryOrder();

    public void setQueryOrder(BuildQueryOrder value);

    /**
     * The user for whom the build was requested.
     *
     *
     * @return
     */
    public String getRequestedFor();

    public void setRequestedFor(String value);

    /**
     * The maximum number of builds which should be returned per definition.
     *
     *
     * @return
     */
    public int getMaxBuildsPerDefinition();

    public void setMaxBuildsPerDefinition(int value);

    /**
     * The reason(s) of the desired build(s).
     *
     *
     * @return
     */
    public BuildReason getReason();

    public void setReason(BuildReason value);

    /**
     * The status(es) of the desired build(s).
     *
     *
     * @return
     */
    public BuildStatus getStatus();

    public void setStatus(BuildStatus value);

    /**
     * The quality of the desired build(s).
     *
     *
     * @return
     */
    public String getQuality();

    public void setQuality(String value);

    /**
     * The information types to be returned from the query/queries. A "*" will
     * retrieve all information types, an empty array will retrieve none, and
     * any other array will match types verbatim. Valid types include the
     * members of Microsoft.TeamFoundation.Build.Common.InformationTypes.
     *
     *
     * @return
     */
    public String[] getInformationTypes();

    public void setInformationTypes(String[] value);

    /**
     * The beginning of the finish time range of the desired build(s).
     *
     *
     * @return
     */
    public Calendar getMinFinishTime();

    public void setMinFinishTime(Calendar value);

    /**
     * The end of the finish time range of the desired build(s).
     *
     *
     * @return
     */
    public Calendar getMaxFinishTime();

    public void setMaxFinishTime(Calendar value);

    /**
     * The minimum last changed on value of the desired build(s).
     *
     *
     * @return
     */
    public Calendar getMinChangedTime();

    public void setMinChangedTime(Calendar value);

    /**
     * Specifies whether to query deleted builds or not.
     *
     *
     * @return
     */
    public QueryDeletedOption getQueryDeletedOption();

    public void setQueryDeletedOption(QueryDeletedOption value);
}
