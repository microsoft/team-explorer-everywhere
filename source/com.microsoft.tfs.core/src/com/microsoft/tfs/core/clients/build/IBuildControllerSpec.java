// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.build;

public interface IBuildControllerSpec {
    /**
     * The service host name of the desired build controller(s). Wildcards
     * supported.
     *
     *
     * @return
     */
    public String getServiceHostName();

    public void setServiceHostName(String value);

    /**
     * Whether or not to include agents in the result set.
     *
     *
     * @return
     */
    public boolean isIncludeAgents();

    public void setIncludeAgents(boolean value);

    /**
     * The name of the desired build controller(s). Wildcards supported.
     *
     *
     * @return
     */
    public String getName();

    public void setName(String value);

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
