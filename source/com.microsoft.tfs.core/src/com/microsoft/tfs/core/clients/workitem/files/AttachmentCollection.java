// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.files;

import java.util.Iterator;

/**
 * Stores the collection of attachments that is associated with a work item.
 *
 * @since TEE-SDK-10.1
 */
public interface AttachmentCollection extends Iterable<Attachment> {
    /**
     * @return an iterator for this collection
     */
    @Override
    public Iterator<Attachment> iterator();

    /**
     * @return the number of {@link Attachment}s in the collection.
     */
    public int size();

    /**
     * Adds the specified attachment to this collection.
     *
     * @param attachment
     *        the attachment to add (must not be <code>null</code>)
     * @return <code>true</code> if the attachment was not already in this
     *         collection and was added, <code>false</code> if this collection
     *         already contained the attachment.
     */
    public boolean add(Attachment attachment);

    /**
     * Removes the specified attachment from this collection.
     *
     * @param attachment
     *        the attachment to remove (must not be <code>null</code>)
     */
    public void remove(Attachment attachment);

    /**
     * Gets the attachment by file ID.
     *
     * @param fileId
     *        the file ID to find in this collection
     * @return the {@link Attachment} with the specified ID or <code>null</code>
     *         if no matching attachment was found
     */
    public Attachment getAttachmentByFileID(int fileId);
}
