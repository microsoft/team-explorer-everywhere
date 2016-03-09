// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.link;

/**
 * Interface which exposes link type information from WIT metadata.
 *
 * @since TEE-SDK-10.1
 */
public interface WorkItemLinkTypeCollection {
    /**
     * Returns true if the specified link type identifier is for a forward link.
     * This will always return true for non directional links.
     *
     *
     * @param linkTypeId
     *        The link type identifier.
     *
     * @return Returns true if the link type is a forward link.
     */
    public boolean isForwardLink(int linkTypeId);

    /**
     * Returns true if the specified link type identifier is for a reverse link.
     * This will always return true for non directional links.
     *
     *
     * @param linkTypeId
     *        The link type identifier.
     *
     * @return Returns true if the link type is a reverse link.
     */
    public boolean isReverseLink(int linkTypeId);

    /**
     * Returns the reference name of the specified link type identifier or null
     * if there is no such link type.
     *
     *
     * @param linkTypeId
     *        The link type identifier.
     *
     * @return The String reference name for this link type or null if there is
     *         no such link type.
     */
    public String getReferenceName(int linkTypeId);

    /**
     * Returns the display name of the specified link type identifier or null if
     * there is no such link type.
     *
     *
     * @param linkTypeId
     *        The link type identifier.
     *
     * @return The String display name for this link type or null if there is no
     *         such link type.
     */
    public String getDisplayName(int linkTypeId);

    /**
     * Get the number of link types in the collection.
     */
    public int getCount();

    /**
     * Retrieves a work item link type based on the link type's reference name.
     */
    public WorkItemLinkType get(String referenceName);

    /**
     * Contains takes a string as the link type name
     *
     * @param linkTypeReferenceName
     *        Must be the reference name of the link type
     */
    public boolean contains(String linkTypeReferenceName);

    /**
     * Retrieves a collection of all the link type ends across all link types.
     * This is provided for convenience and faster lookup of link type ends by
     * Id, Name, and ImmutableName.
     */
    public WorkItemLinkTypeEndCollection getLinkTypeEnds();

}
