// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.persistence;

/**
 * <p>
 * Monitors serialization and deserialization by {@link PersistenceStore} for
 * update conflict events, and handles the conflicts when they are detected.
 * </p>
 * <p>
 * A {@link MergeHandler} is given modification stamps (long integers) that
 * increment by at least 1 every time an object is modified (modification times
 * on filesystems may be given). Filesystem implementations could use the
 * modification time (in milliseconds).
 * </p>
 *
 * @see PersistenceStore
 *
 * @since TEE-SDK-10.1
 * @threadsafety thread-compatible
 */
public interface MergeHandler {
    /**
     * Updates the handler's modification stamp for the item immediately after
     * it was saved to storage.
     *
     * @param itemName
     *        the name of the item just written (must not be <code>null</code>
     *        or empty)
     * @param modificationStamp
     *        the modification number of the object after it was just written
     */
    public void updateModificationStampAfterStore(String itemName, long modificationStamp);

    /**
     * Updates the handler's modification stamp for the item immediately after
     * the it was read from storage.
     *
     * @param itemName
     *        the name of the item just read (must not be <code>null</code> or
     *        empty)
     * @param modificationStamp
     *        the modification number of the object after it was just read
     */
    public void updateModificationStampAfterRetrieve(String itemName, long modificationStamp);

    /**
     * Tests whether the given item needs merged with the item in storage that
     * has the given modificationStamp.
     *
     * @param itemName
     *        the name of the item being tested (must not be <code>null</code>
     *        or empty)
     * @param modificationStamp
     *        the modification number of the object after it exists in storage
     *        now (before it is read)
     * @return true if the file needs merged, false if it does not.
     */
    public boolean needsMerge(String itemName, long modificationStamp);

    /**
     * Invoked by {@link PersistenceStore} when a write conflict is detected to
     * merge the two objects into a new object that will be stored. An
     * implementation is free to return one of the given objects, or to return a
     * new object.
     *
     * @param storedObject
     *        the object currently in storage (must not be <code>null</code>)
     * @param latestObject
     *        the latest object not yet stored (must not be <code>null</code>)
     * @return the object that will be saved.
     */
    public Object merge(Object storedObject, Object latestObject);
}
