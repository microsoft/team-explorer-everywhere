// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem;

import com.microsoft.tfs.core.clients.workitem.exceptions.UnableToSaveException;
import com.microsoft.tfs.core.clients.workitem.fields.FieldCollection;
import com.microsoft.tfs.core.clients.workitem.files.AttachmentCollection;
import com.microsoft.tfs.core.clients.workitem.link.LinkCollection;
import com.microsoft.tfs.core.clients.workitem.project.Project;
import com.microsoft.tfs.core.clients.workitem.revision.RevisionCollection;
import com.microsoft.tfs.core.clients.workitem.wittype.WorkItemType;

/**
 * Represents a work item on Team Foundation Server. Implementations must
 * implement equals() and hashCode().
 *
 * @since TEE-SDK-10.1
 * @threadsafety thread-compatible
 */
public interface WorkItem {
    /**
     * Opens this work item for modification.
     */
    public void open();

    /**
     * Saves any pending changes on this work item.
     *
     * @throws UnableToSaveException
     *         if there was an error saving the work item
     */
    public void save() throws UnableToSaveException;

    /**
     * Reverts all changes that were made since the last save.
     */
    public void reset();

    /**
     * Synchronizes the work item to the latest revision.
     */
    public void syncToLatest();

    /**
     * Gets the next state of this work item based on the action of a user.
     *
     * @param action
     *        the action string (must not be <code>null</code>)
     * @return the next state
     */
    public String getNextState(String action);

    /**
     * @return a {@link WorkItemType} object that represents the type of this
     *         work item.
     */
    public WorkItemType getType();

    /**
     * @return a {@link Project} object that contains this work item.
     */
    public Project getProject();

    /**
     * @return the FieldCollection object that contains the Fields of this work
     *         item.
     */
    public FieldCollection getFields();

    /**
     * @return a {@link RevisionCollection} object that represents a collection
     *         of valid revision numbers for this work item.
     */
    public RevisionCollection getRevisions();

    /**
     * @return the {@link AttachmentCollection} object that represents the
     *         attachments that belong to this work item.
     */
    public AttachmentCollection getAttachments();

    /**
     * @return the collection of the links in this work item.
     */
    public LinkCollection getLinks();

    /**
     * @return the ID of this work item.
     */
    public int getID();

    /**
     * @return the temporary ID of this work item.
     */
    public int getTemporaryID();

    /**
     * @return Gets the uniform resource identifier (URI) of this work item.
     */
    public String getURI();

    /**
     * @return Gets the title of this work item.
     */
    public String getTitle();

    /**
     * Sets the title of this work item.
     *
     * @param title
     *        the the title of this work item
     */
    public void setTitle(String title);

    /**
     * Adds a listener for the state changed event.
     *
     * @param listener
     *        the listener to add (must not be <code>null</code>)
     */
    public void addWorkItemStateListener(WorkItemStateListener listener);

    /**
     * Removes a listener for the state changed event.
     *
     * @param listener
     *        the listener to remove (must not be <code>null</code>)
     */
    public void removeWorkItemStateListener(WorkItemStateListener listener);

    /**
     * @return <code>true</code> this work item is currently open for edit,
     *         <code>false</code> otherwise
     */
    public boolean isOpen();

    /**
     * @return <code>true</code> this work item has been changed since its last
     *         save, <code>false</code> otherwise
     */
    public boolean isDirty();

    /**
     * Validates the fields of this work item.
     *
     * @return <code>true</code> if the work item is valid, <code>false</code>
     *         if it is not valid
     */
    public boolean isValid();

    /**
     * @return the {@link WorkItemClient} for this work item
     */
    public WorkItemClient getClient();

    /**
     * @return a copy of this {@link WorkItem} instance.
     */
    public WorkItem copy();

    /**
     * @return a copy of this {@link WorkItem} instance that is of the specified
     *         {@link WorkItemType}.
     */
    public WorkItem copy(WorkItemType targetType);
}
