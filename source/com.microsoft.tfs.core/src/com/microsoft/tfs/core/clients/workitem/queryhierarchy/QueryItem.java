// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.queryhierarchy;

import com.microsoft.tfs.core.clients.webservices.IdentityDescriptor;
import com.microsoft.tfs.core.clients.workitem.exceptions.WorkItemException;
import com.microsoft.tfs.core.clients.workitem.project.Project;
import com.microsoft.tfs.util.GUID;

/**
 * Base query item class, representing any hierarchical query node (a
 * {@link QueryFolder} or a {@link QueryDefinition}).
 *
 * @since TEE-SDK-10.1
 */
public interface QueryItem {
    /**
     * Returns the unique ID for this QueryItem.
     *
     * @return The id for this QueryItem.
     */
    public GUID getID();

    /**
     * Returns the {@link Project} that this {@link QueryItem} is associated
     * with. Will return <code>null</code> if this is a newly created
     * {@link QueryItem} with no project associated.
     *
     * @return The {@link Project} this {@link QueryItem} is associated with, or
     *         <code>null</code>.
     */
    public Project getProject();

    /**
     * Gets the name of this query item.
     *
     * @return The name of this query item.
     */
    public String getName();

    /**
     * Gets the original name of this query item, before any unsaved
     * modifications were made to the hierarchy.
     *
     * @return The original name of this query item.
     */
    public String getOriginalName();

    /**
     * Sets the name of this item.
     *
     * @param name
     *        The new name for this item (not <code>null</code>).
     */
    public void setName(final String name);

    /**
     * Gets the parent folder of this query item.
     *
     * @return The parent {@link QueryFolder}, or <code>null</code> if this item
     *         is the hierarchy root.
     */
    public QueryFolder getParent();

    /**
     * Gets the original parent folder of this query item, before any unsaved
     * modifications were made to the hierarchy.
     *
     * @return The parent {@link QueryFolder}, or <code>null</code> if this item
     *         is the hierarchy root.
     */
    public QueryFolder getOriginalParent();

    /**
     * Gets the owner's {@link IdentityDescriptor} for this query item.
     *
     * @return This query item's owner's {@link IdentityDescriptor}.
     */
    public IdentityDescriptor getOwnerDescriptor();

    /**
     * Gets the original owner's {@link IdentityDescriptor} for this query item,
     * before any unsaved modifications were made to the hierarchy.
     *
     * @return This query item's original owner's {@link IdentityDescriptor}.
     */
    public IdentityDescriptor getOriginalOwnerDescriptor();

    /**
     * Sets the owner of this query item to be the given
     * {@link IdentityDescriptor}.
     *
     * @param ownerDescriptor
     *        The new owner's {@link IdentityDescriptor}.
     * @throws WorkItemException
     *         if this server does not support permissions on query items.
     */
    public void setOwnerDescriptor(final IdentityDescriptor ownerDescriptor);

    /**
     * Deletes this query item from the server.
     *
     * @throws WorkItemException
     *         if this query item cannot be deleted because it has already been
     *         deleted or it is the query hierarchy root.
     */
    public void delete();

    /**
     * Queries the deletion state of this query item.
     *
     * @return <code>true</code> if this query item has been removed from the
     *         hierarchy, <code>false</code> otherwise.
     */
    public boolean isDeleted();

    /**
     * Determines whether this is a personal (private to the owner) query item.
     *
     * @return <code>true</code> if this query item is personal,
     *         <code>false</code> otherwise.
     */
    public boolean isPersonal();

    /**
     * Determines whether this query item is newly created and has not yet been
     * saved to the server.
     *
     * @return <code>true</code> if this query item is new, <code>false</code>
     *         otherwise.
     */
    public boolean isNew();

    /**
     * Determines whether this query item is dirty and has changes that have not
     * been saved to the server, including creation or deletion.
     *
     * @return <code>true</code> if this query item is dirty, <code>false</code>
     *         otherwise.
     */
    public boolean isDirty();

    /**
     * Gets the {@link QueryItemType} for this query item, determining whether
     * it is a Team Project (ie, the query hierarchy root), a query folder or a
     * query definition.
     *
     * @return The {@link QueryItemType} for this query item.
     */
    public QueryItemType getType();
}
