// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.build;

import java.util.Calendar;

import com.microsoft.tfs.core.clients.build.flags.WorkspaceMappingDepth;
import com.microsoft.tfs.core.clients.build.soapextensions.WorkspaceMappingType;

public interface IWorkspaceTemplate {
    /**
     * The workspace mappings for the workspace.
     *
     *
     * @return
     */
    public IWorkspaceMapping[] getMappings();

    /**
     * The date and time at which the workspace was last modified.
     *
     *
     * @return
     */
    public Calendar getLastModifiedDate();

    /**
     * The user who last modified the workspace.
     *
     *
     * @return
     */
    public String getLastModifiedBy();

    /**
     * Adds a workspace mapping to the workspace.
     *
     *
     * @return The new workspace mapping.
     */
    public IWorkspaceMapping addMapping();

    /**
     * Adds a cloak entry to the workspace for the specified server item.
     *
     *
     * @param serverItem
     *        The server item which should be cloaked.
     * @return A new workspace mapping.
     */
    public IWorkspaceMapping cloak(String serverItem);

    /**
     * Adds a workspace mapping with the provided server item and local item
     * with WorkspaceMappingType.Map.
     *
     *
     * @param serverItem
     *        The server item to map.
     * @param localItem
     *        The local item to map.
     * @return A new workspace mapping.
     */
    public IWorkspaceMapping map(String serverItem, String localItem);

    /**
     * Adds a workspace mapping with the provided server item, local item, and
     * mapping type with a depth of 120. If WorkspaceMappingType.Cloak is
     * specified then the localItem is forced to null.
     *
     *
     * @param serverItem
     *        The server item for the mapping.
     * @param localItem
     *        The local item path for the mapping.
     * @param type
     *        The mapping type to create.
     * @return A new workspace mapping.
     */
    public IWorkspaceMapping addMapping(String serverItem, String localItem, WorkspaceMappingType type);

    /**
     * Adds a workspace mapping with the provided server item, local item,
     * mapping type, and depth. If WorkspaceMappingType.Cloak is specified then
     * the localItem parameter is forced to null.
     *
     *
     * @param serverItem
     *        The server item for the mapping
     * @param localItem
     *        The local item path for the mapping
     * @param type
     *        The mapping type to create
     * @param depth
     *        The depth of the mapping
     * @return A new workspace mapping
     */
    public IWorkspaceMapping addMapping(
        String serverItem,
        String localItem,
        WorkspaceMappingType type,
        WorkspaceMappingDepth depth);

    /**
     * Removes the workspace mapping from the workspace template.
     *
     *
     * @param mapping
     *        The mapping to be removed.
     * @return True if the mapping was removed, false otherwise.
     */
    public boolean removeMapping(IWorkspaceMapping mapping);

    /**
     * Removes the workspace mapping for the given server item from the
     * workspace template.
     *
     *
     * @param serverItem
     *        The server item of the mapping to be removed.
     * @return True if the mapping was removed, false otherwise.
     */
    public boolean removeMapping(String serverItem);

    /**
     * Removes all workspace mappings from the workspace template.
     */
    public void clearMappings();

    /**
     * Copies properties from a source workspace template to this one
     *
     *
     * @param source
     *        Template to copy from
     */
    public void copyFrom(IWorkspaceTemplate source);
}