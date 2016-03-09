// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.build;

import com.microsoft.tfs.core.clients.build.flags.QueryOptions;
import com.microsoft.tfs.core.clients.build.soapextensions.ContinuousIntegrationType;
import com.microsoft.tfs.core.clients.build.soapextensions.DefinitionTriggerType;

public interface IBuildDefinitionSpec {
    /**
     * The full path to the desired build definition(s), of the form
     * \TeamProject\Name.
     *
     *
     * @return
     */
    public String getFullPath();

    /**
     * The name of the desired build definition(s). Wildcards supported.
     *
     *
     * @return
     */
    public String getName();

    public void setName(String value);

    /**
     * The team project of the desired build definition(s).
     *
     *
     * @return
     */
    public String getTeamProject();

    /**
     * Query options used to determine whether or not supporting objects are
     * returned from the query.
     *
     *
     * @return
     */
    public QueryOptions getOptions();

    public void setOptions(QueryOptions value);

    /**
     * An optional filter to control the type of build definitions returned from
     * the query.
     *
     *
     * @return
     */
    public DefinitionTriggerType getTriggerType();

    public void setTriggerType(DefinitionTriggerType value);

    /**
     * An optional filter to control the type of build definitions returned from
     * the query.
     *
     *
     * @return
     */
    public ContinuousIntegrationType getContinuousIntegrationType();

    public void setContinuousIntegrationType(ContinuousIntegrationType value);

    /**
     * The property names to be returned from the query/queries. A "*" will
     * retrieve all property names, an empty array will retrieve none, and any
     * other array will match types verbatim.
     *
     *
     * @return
     */
    public String[] getPropertyNameFilters();

    public void setPropertyNameFilters(String[] value);
}
