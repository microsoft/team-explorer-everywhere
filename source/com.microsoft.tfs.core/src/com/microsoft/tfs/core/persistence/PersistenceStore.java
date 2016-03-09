// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.persistence;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.microsoft.tfs.util.locking.AdvisoryFileLock;

/**
 * <p>
 * <h1>Purpose</h1>
 * </p>
 * <p>
 * Provides long-term storage services for objects (identified by "item names").
 * Storage and retrieval can be done with {@link ObjectSerializer}s, or as with
 * {@link InputStream} and {@link OutputStream}s. Provides locking for single
 * items in the store to ensure exclusive read/update, and locking over the
 * entire store for exclusive access and atomic read/update of multiple items.
 * Merge handlers may be provided during serialization for cases where the
 * persisted data has changed since the previous store operation. Also provides
 * simple migration (copy) of items from other {@link PersistenceStore}s.
 * </p>
 * <p>
 * {@link PersistenceStore}s can be used in isolation (one
 * {@link PersistenceStore} maps to one one storage resource with one item
 * namespace), or hierarchichally (you can get {@link PersistenceStore}s for
 * named children of the first one). Think of {@link File} and
 * {@link File#listFiles()}. The implementation class of a child
 * {@link PersistenceStore} is always the same as its parent.
 * </p>
 * <p>
 * {@link PersistenceStore}s are lazily initialized. Construction is not
 * guaranteed to create all underlying storage resources, but use of other
 * public methods is guaranteed to create resources needed for those methods.
 * See {@link #initialize()}.
 * </p>
 * <p>
 * Implementations are encouraged to take some kind of implementation-specific
 * namespace identifier during construction and isolate storage of items within
 * this namespace. For example, a filesystem implementation may take a
 * filesystem directory; a database implementation may take an integer used as a
 * key into a storage table. This tactic provides for separation between
 * namespaces of instances, so item names don't conflict across them.
 * </p>
 * <p>
 * <h1>Item Names</h1>
 * </p>
 * <p>
 * Items in a {@link PersistenceStore} are identified by a string name (case
 * sensitivity and maximum length is undefined). Names cannot be the empty
 * string.
 * </p>
 *
 * @since TEE-SDK-10.1
 * @threadsafety thread-safe
 */
public interface PersistenceStore {
    /**
     * <p>
     * Creates any storage resources required by the implementation. For
     * example, creates filesystem directories, inserts a key row into a
     * database table, etc.
     * </p>
     * <p>
     * Calling this method externally is usually not required. This method is
     * public because some implementations of {@link PersistenceStore} allow
     * users to interact directly with their storage (for example, files in a
     * filesystem), and this method ensures resource creation (intermediate
     * dirctories).
     * </p>
     * <p>
     * Implementations of this method must be able to be called multiple times
     * without error (subsequent calls must not fail). Other methods in this
     * class must call {@link #initialize()} before they do their work, if their
     * work requires initialization.
     * </p>
     *
     * @throws IOException
     *         if an exception happened creating the storage location
     */
    public void initialize() throws IOException;

    /**
     * Gets a {@link PersistenceStore} for the given child name.
     *
     * @param childName
     *        the child name (must not be <code>null</code> or empty)
     * @return the new persistence store for the child
     */
    public PersistenceStore getChildStore(final String childName);

    /**
     * <p>
     * Gets a thread- and process-exclusive lock for the entire
     * {@link PersistenceStore}. This is simply a different synchronization
     * point than {@link #getItemLock(String, boolean)}; a lock on the entire
     * {@link PersistenceStore} does <b>not</b> interfere (or even interact)
     * with locks on items.
     * </p>
     * <p>
     * The lock should be released by its {@link AdvisoryFileLock#release()}
     * method. If you forget (you should not forget), the lock will be released
     * during its finalization (possibly as late as when the JVM exits).
     * </p>
     *
     * @param block
     *        if true, this method does not return until the lock is obtained
     *        (or the controlling thread is interrupted). If false, the method
     *        returns immediately; the value is null if the lock was not
     *        immediately available or a {@link AdvisoryFileLock} if it was.
     * @return a {@link AdvisoryFileLock}, initially owned. Returns null if and
     *         only if block was set to false and the lock was not immediately
     *         available.
     * @throws IOException
     *         if an exception occured creating or acquiring the underlying lock
     * @throws InterruptedException
     *         if this thread was interrupted while waiting to acquire the lock.
     */
    public AdvisoryFileLock getStoreLock(final boolean block) throws IOException, InterruptedException;

    /**
     * <p>
     * Gets a thread- and process-exclusive lock for a single item in this
     * {@link PersistenceStore}. A lock on an item is not prevented by a lock on
     * the entire store. See {@link #getStoreLock(boolean)} for details.
     * </p>
     * <p>
     * The lock should be released by its {@link AdvisoryFileLock#release()}
     * method. If you forget (you should not forget), the lock will be released
     * during its finalization (possibly as late as when the JVM exits).
     * </p>
     *
     * @param itemName
     *        the item to get a lock for (must not be <code>null</code> or
     *        empty)
     * @param block
     *        if true, this method does not return until the lock is obtained
     *        (or the controlling thread is interrupted). If false, the method
     *        returns immediately; the value is null if the lock was not
     *        immediately available or a {@link AdvisoryFileLock} if it was.
     * @return a {@link AdvisoryFileLock}, initially owned. Returns null if and
     *         only if block was set to false and the lock was not immediately
     *         available.
     * @throws IOException
     *         if an exception occured creating or acquiring the underlying lock
     * @throws InterruptedException
     *         if this thread was interrupted while waiting to acquire the lock.
     */
    public AdvisoryFileLock getItemLock(final String itemName, final boolean block)
        throws IOException,
            InterruptedException;

    /**
     * Tests whether the item exists in this store. An item will exist if it was
     * previously written to (and not deleted).
     *
     * @param itemName
     *        the item to test for existence (must not be <code>null</code> or
     *        empty)
     * @return true if the item exists, false if it does not exist
     * @throws IOException
     *         if there was an error testing whether the item was contained in
     *         this store
     */
    public boolean containsItem(final String itemName) throws IOException;

    /**
     * Deletes the specified item. If the item does not exist, this method
     * should not throw an exception.
     *
     * @param itemName
     *        the item to delete (must not be <code>null</code> or empty)
     * @return true if the item existed and was deleted, false if it did not
     *         exist or did exist but could not be deleted
     * @throws IOException
     *         if an error occurred deleting the item, but not thrown if the
     *         item did not exist
     */
    public boolean deleteItem(final String itemName) throws IOException;

    /**
     * <p>
     * Copies the persisted item specified by oldItemName from the oldStore into
     * this store with the newItemName. If the newItemName already exists in
     * this store, it must not be overwritten. Data is only read from the old
     * store, and it is not translated during copying.
     * </p>
     * <p>
     * Implementations should not rethrow exceptions encountered during
     * migration to make the caller's work easier. They should simply return
     * false.
     * </p>
     *
     * @param oldStore
     *        the store to migrate the old item from (not
     *        {@link NullPointerException}, but may be this
     *        {@link PersistenceStore})
     * @param oldItemName
     *        the item to copy from the old store (must not be <code>null</code>
     *        or empty)
     * @param newItemName
     *        the item to copy into this store (must not be <code>null</code> or
     *        empty)
     * @return true if the item was migrated to the new store or already existed
     *         in the new store, false if no migration happened and the new item
     *         does not exist in the new store
     * @throws IOException
     *         if an error ocurred reading or writing persistence data from the
     *         old store or to the new store
     */
    public boolean migrateItem(PersistenceStore oldStore, String oldItemName, String newItemName);

    /**
     * @equivalence storeItem(itemName, object, lockMode, mergeHandler,
     *              serializer, PersistenceSecurity.PUBLIC)
     */
    public boolean storeItem(
        String itemName,
        Object object,
        LockMode lockMode,
        MergeHandler mergeHandler,
        ObjectSerializer serializer) throws IOException, InterruptedException;

    /**
     * Saves an object as the specified item name. The object can be retrieved
     * with
     * {@link #retrieveItem(String, LockMode, MergeHandler, ObjectSerializer)} ,
     * by passing the same item name.
     *
     * @param itemName
     *        the desired name for the object (must not be <code>null</code> or
     *        empty)
     * @param object
     *        the object to serialize. Access to the Java object is not
     *        synchronized by this method during serialization. If
     *        synchronization is required, it is the responsibility of the
     *        caller (must not be <code>null</code>)
     * @param lockMode
     *        if {@link LockMode#NONE}, no locking is performed. If
     *        {@link LockMode#WAIT_FOREVER}, a lock is obtained and held for the
     *        identifier during the operation (and unlocked before this method
     *        returns). If {@link LockMode#NO_WAIT}, locking is attempted, but
     *        if the lock was not immediately available this method must not
     *        write the serialized object and returns false (must not be
     *        <code>null</code>)
     * @param mergeHandler
     *        if not <code>null</code>, this {@link MergeHandler} is used to
     *        detect and resolve merge conflicts in the persistence store. If
     *        null, no merging is done: the given component instance will always
     *        overwrite an existing serialized component file on disk.
     * @param serializer
     *        specifies the serializer to use (must not be <code>null</code>)
     * @param security
     *        specifies the security with which this item will be saved.
     *        {@link PersistenceSecurity#PUBLIC} indicates that no effort will
     *        be taken by the persistence store to prevent other users from
     *        accessing this data. {@link PersistenceStore#PRIVATE} indicates
     *        that the persistence store will attempt to prevent other users
     *        from accessing this data using the underlying permission model of
     *        the persistence store. (must not be <code>null</code>)
     * @throws IOException
     *         if an error occurred writing the persistent data (implementation
     *         specific; perhaps insufficient permissions, out of disk space,
     *         etc.).
     * @throws InterruptedException
     *         if this thread was interrupted while waiting on a lock when the
     *         {@link LockMode} specifies waiting, or while waiting on some
     *         other resource
     * @return true if the object was successfully persisted, false if and only
     *         if {@link LockMode#NO_WAIT} was given and the lock could not be
     *         obtained
     */
    public boolean storeItem(
        String itemName,
        Object object,
        LockMode lockMode,
        MergeHandler mergeHandler,
        ObjectSerializer serializer,
        PersistenceSecurity security) throws IOException, InterruptedException;

    /**
     * Retrieves an object that was previuosly stored via
     * {@link #storeItem(String, Object, LockMode, MergeHandler, ObjectSerializer)}
     * .
     *
     * @param itemName
     *        the name of the object. (must not be <code>null</code> or empty)
     * @param lockMode
     *        if {@link LockMode#NONE}, no locking is performed. If
     *        {@link LockMode#WAIT_FOREVER}, a lock is obtained and held for the
     *        componentName in this store's settings location during the
     *        operation (and unlocked before this method returns). If
     *        {@link LockMode#NO_WAIT}, locking is attempted, but if the lock
     *        was not immediately available this method does not read the file
     *        and returns false. (must not be <code>null</code>)
     * @param mergeHandler
     *        if not <code>null</code>, this {@link MergeHandler} is given the
     *        last modification date of the persisted object when it is read, so
     *        the handler can handle future merges. If null, no such
     *        notification is performed and the resource is still read normally.
     * @param serializer
     *        specifies the serializer to use (must not be <code>null</code>)
     * @throws IOException
     *         if an error occurred reading the persistent data (implementation
     *         specific; perhaps insufficient permissions, out of disk space,
     *         etc.).
     * @throws InterruptedException
     *         if this thread was interrupted while waiting on a lock when the
     *         {@link LockMode} specifies waiting, or while waiting on some
     *         other resource
     * @return the retrieved object, <code>null</code> if and only if
     *         {@link LockMode#NO_WAIT} was given and the lock could not be
     *         obtained
     */
    public Object retrieveItem(
        String itemName,
        LockMode lockMode,
        MergeHandler mergeHandler,
        ObjectSerializer serializer) throws IOException, InterruptedException;

    /**
     * <p>
     * Gets an {@link InputStream} for reading the specified item's data. No
     * locking is done by this method.
     * </p>
     * <p>
     * The behavior of opening an {@link InputStream} for an item that does not
     * exist is unspecified.
     * </p>
     *
     * @param itemName
     *        the item to read (must not be <code>null</code> or empty)
     * @return an {@link InputStream} open for reading the item
     * @throws IOException
     *         if an error occurred opening the item for read
     */
    public InputStream getItemInputStream(String itemName) throws IOException;

    /**
     * <p>
     * Gets an {@link OutputStream} to write the specified item's data. No
     * locking is done by this method.
     * </p>
     * <p>
     * Getting an {@link OutputStream} for a valid item name that does not yet
     * exist in the store should succeed (it should be created).
     * </p>
     *
     * @param itemName
     *        the item to write (must not be <code>null</code> or empty)
     * @return an {@link OutputStream} open for writing the item
     * @throws IOException
     *         if an error occurred opening the item for write
     */
    public OutputStream getItemOutputStream(String itemName) throws IOException;
}