// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.build;

public interface IBuildAgentSpec {
    /**
     * The service host name of the desired build agent(s). Wildcards supported.
     *
     *
     * @return
     */
    public String getServiceHostName();

    public void setServiceHostName(String value);

    /**
     * The name of the desired build agent(s). Wildcards supported.
     *
     *
     * @return
     */
    public String getName();

    public void setName(String value);

    /**
     * The tags defined for the desired build agent(s).
     *
     *
     * @return
     */
    public String[] getTags();

    public void setTags(String[] value);

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
