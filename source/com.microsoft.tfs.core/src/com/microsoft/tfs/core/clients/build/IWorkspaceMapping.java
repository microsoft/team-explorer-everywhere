// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.build;

import com.microsoft.tfs.core.clients.build.flags.WorkspaceMappingDepth;
import com.microsoft.tfs.core.clients.build.soapextensions.WorkspaceMappingType;

public interface IWorkspaceMapping {
    /**
     * The type of the mapping - Map or Cloak.
     *
     *
     * @return
     */
    public WorkspaceMappingType getMappingType();

    public void setMappingType(WorkspaceMappingType value);

    /**
     * The local item of the mapping.
     *
     *
     * @return
     */
    public String getLocalItem();

    public void setLocalItem(String value);

    /**
     * The server item of the mapping.
     *
     *
     * @return
     */
    public String getServerItem();

    public void setServerItem(String value);

    /**
     * Gets or sets the depth of the mapping.
     *
     *
     * @return
     */
    public WorkspaceMappingDepth getDepth();

    public void setDepth(WorkspaceMappingDepth value);
}
