// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.persistence;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.MessageFormat;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.microsoft.tfs.jni.FileSystemAttributes;
import com.microsoft.tfs.jni.FileSystemUtils;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.FileHelpers;
import com.microsoft.tfs.util.locking.AdvisoryFileLock;

/**
 * <p>
 * Implements {@link PersistenceStore} by persisting objects to files in
 * directories on the filesystem. Each instance can be configured to a specific
 * directory, and item names map to file names in that directory. Children
 * created via {@link #getChildStore(String)} are always a subdirectory of the
 * parent. Because of the way filesystems work, a
 * {@link FilesystemPersistenceStore} cannot have a child item with the same
 * name as a child store.
 * </p>
 * <p>
 * Child store and item names are case-sensitive if the underlying filesystem
 * is.
 * </p>
 *
 * @since TEE-SDK-10.1
 * @threadsafety thread-safe
 */
public class FilesystemPersistenceStore implements PersistenceStore {
    private final static Log log = LogFactory.getLog(FilesystemPersistenceStore.class);

    /**
     * The name of the file we'll use for locking entire store.
     */
    private static final String DIRECTORY_LOCK_FILE = ".lock"; //$NON-NLS-1$

    /**
     * The prefix we'll apply when locking individual items.
     */
    private static final String FILE_LOCK_PREFIX = ".lock-"; //$NON-NLS-1$

    /**
     * A {@link File} that represents a directory (which does not have to exist)
     * where this store saves things.
     */
    private final File directory;

    /**
     * Creates a {@link FilesystemPersistenceStore} for the given directory.
     *
     * @param directory
     *        the directory, which does not have to exist (must not be
     *        <code>null</code>)
     */
    public FilesystemPersistenceStore(final File directory) {
        Check.notNull(directory, "directory"); //$NON-NLS-1$
        this.directory = directory;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PersistenceStore getChildStore(final String childName) {
        Check.notNullOrEmpty(childName, "childName"); //$NON-NLS-1$

        return new FilesystemPersistenceStore(new File(directory, childName));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void initialize() throws IOException {
        if (directory.exists() == false && directory.mkdirs() == false) {
            throw new RuntimeException(
                MessageFormat.format(
                    "Error creating directories up to {0}.  Possible permissions problem.", //$NON-NLS-1$
                    directory.getAbsolutePath()));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean containsItem(final String itemName) {
        Check.notNullOrEmpty(itemName, "itemName"); //$NON-NLS-1$

        return getItemFile(itemName).exists();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean deleteItem(final String itemName) {
        Check.notNullOrEmpty(itemName, "itemName"); //$NON-NLS-1$

        return getItemFile(itemName).delete();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean storeItem(
        final String itemName,
        final Object object,
        final LockMode lockMode,
        final MergeHandler mergeHandler,
        final ObjectSerializer serializer) throws IOException, InterruptedException {
        return storeItem(itemName, object, lockMode, mergeHandler, serializer, PersistenceSecurity.PUBLIC);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean storeItem(
        final String itemName,
        Object object,
        final LockMode lockMode,
        final MergeHandler mergeHandler,
        final ObjectSerializer serializer,
        final PersistenceSecurity security) throws IOException, InterruptedException {
        Check.notNullOrEmpty(itemName, "itemName"); //$NON-NLS-1$
        Check.notNull(object, "object"); //$NON-NLS-1$
        Check.notNull(lockMode, "lockMode"); //$NON-NLS-1$
        Check.notNull(serializer, "serializer"); //$NON-NLS-1$
        Check.notNull(security, "security"); //$NON-NLS-1$

        log.debug("storeObject called for " + itemName); //$NON-NLS-1$

        AdvisoryFileLock lock = null;
        try {
            /*
             * Get the correct kind of lock, if we need one.
             */
            if (lockMode != LockMode.NONE) {
                lock = getItemLock(itemName, lockMode == LockMode.WAIT_FOREVER);

                // A null lock meant the call didn't block and there wasn't one
                // available.
                if (lock == null) {
                    log.debug(MessageFormat.format("No lock available for {0}, returning", itemName)); //$NON-NLS-1$
                    return false;
                }
            }

            final File file = getItemFile(itemName);

            // See if we need to merge based on the last modified time.
            if (mergeHandler != null && file.exists() && mergeHandler.needsMerge(itemName, file.lastModified())) {
                log.debug(MessageFormat.format("Object file {0} needs merged, performing merge", file)); //$NON-NLS-1$

                // Load the old version.
                final Object diskVersion = retrieveItem(itemName, LockMode.NONE, null, serializer);

                object = mergeHandler.merge(diskVersion, object);
            }

            // Create any missing directories.
            initialize();

            /*
             * In order to be more robust during interruption during save
             * (process killed, out of disk space, etc.), write the component to
             * a temporary file, then rename it over the new file.
             */
            OutputStream outputStream = null;

            File tempFile;
            if (PersistenceSecurity.PRIVATE.equals(security)) {
                tempFile = FileSystemUtils.getInstance().createTempFileSecure("vvfps", ".tmp", file.getParentFile()); //$NON-NLS-1$ //$NON-NLS-2$
            } else {
                tempFile = File.createTempFile("vvfps", ".tmp", file.getParentFile()); //$NON-NLS-1$ //$NON-NLS-2$
            }

            log.debug(MessageFormat.format("Writing {0} to {1}", itemName, tempFile)); //$NON-NLS-1$
            try {
                outputStream = new FileOutputStream(tempFile);
                outputStream = new BufferedOutputStream(outputStream);
                serializer.serialize(object, outputStream);
            } catch (final Exception e) {
                log.error("Error writing to temp file", e); //$NON-NLS-1$
                tempFile.delete();

                if (e instanceof IOException) {
                    throw (IOException) e;
                } else {
                    throw new IOException(MessageFormat.format("Error writing to temp file: {0}", e.getMessage())); //$NON-NLS-1$
                }
            } finally {
                if (outputStream != null) {
                    try {
                        outputStream.close();
                    } catch (final IOException e) {
                    }
                }
            }

            if (PersistenceSecurity.PRIVATE.equals(security)) {
                final FileSystemAttributes attributes =
                    FileSystemUtils.getInstance().getAttributes(tempFile.getAbsolutePath());
                attributes.setOwnerOnly(true);
                FileSystemUtils.getInstance().setAttributes(tempFile.getAbsolutePath(), attributes);
            }

            /*
             * Rename the temp file to the real component file, overwriting an
             * existing one.
             */
            log.debug(MessageFormat.format("Renaming temp file {0} to final object file {1}", tempFile, file)); //$NON-NLS-1$

            try {
                FileHelpers.rename(tempFile, file);
            } catch (final Exception e) {
                /*
                 * Errors could be for permissions, file in use, etc.
                 */
                log.error("Error renaming temp file to object file", e); //$NON-NLS-1$
                tempFile.delete();

                if (e instanceof IOException) {
                    throw (IOException) e;
                } else {
                    throw new IOException(MessageFormat.format("Could not rename temp file: {0}", e.getMessage())); //$NON-NLS-1$
                }
            }

            log.debug("Done saving object file " + file); //$NON-NLS-1$

            if (mergeHandler != null) {
                mergeHandler.updateModificationStampAfterStore(itemName, file.lastModified());
            }
        } finally {
            if (lock != null) {
                lock.release();
            }
        }

        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object retrieveItem(
        final String itemName,
        final LockMode lockMode,
        final MergeHandler mergeHandler,
        final ObjectSerializer serializer) throws IOException, InterruptedException {
        Check.notNullOrEmpty(itemName, "itemName"); //$NON-NLS-1$
        Check.notNull(lockMode, "lockMode"); //$NON-NLS-1$
        Check.notNull(serializer, "serializer"); //$NON-NLS-1$

        log.debug(MessageFormat.format("retrieveObject called for {0}", itemName)); //$NON-NLS-1$

        AdvisoryFileLock lock = null;
        Object component = null;
        try {
            if (lockMode != LockMode.NONE) {
                lock = getItemLock(itemName, lockMode == LockMode.WAIT_FOREVER);

                // A null lock meant the call didn't block and there wasn't one
                // available.
                if (lock == null) {
                    log.debug(MessageFormat.format("No lock available for {0}, returning", itemName)); //$NON-NLS-1$
                    return null;
                }
            }

            final File componentFile = getItemFile(itemName);

            log.debug(MessageFormat.format("Reading object file {0}", componentFile)); //$NON-NLS-1$

            InputStream inputStream = null;
            try {
                inputStream = new FileInputStream(componentFile);
                inputStream = new BufferedInputStream(inputStream);
                component = serializer.deserialize(inputStream);
            } catch (final Exception e) {
                log.debug(MessageFormat.format("Could not read object file {0}", componentFile), e); //$NON-NLS-1$
            } finally {
                if (inputStream != null) {
                    try {
                        inputStream.close();
                    } catch (final IOException e) {
                    }
                }
            }

            log.debug(MessageFormat.format("Done reading object file {0}", componentFile)); //$NON-NLS-1$

            if (mergeHandler != null) {
                mergeHandler.updateModificationStampAfterRetrieve(itemName, componentFile.lastModified());
            }
        } catch (final Exception e) {
            log.warn(MessageFormat.format("Could not read {0}", itemName), e); //$NON-NLS-1$
        } finally {
            if (lock != null) {
                lock.release();
            }
        }

        return component;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean migrateItem(final PersistenceStore oldStore, final String oldItemName, final String newItemName) {
        Check.notNull(oldStore, "oldStore"); //$NON-NLS-1$
        Check.notNullOrEmpty(oldItemName, "oldItemName"); //$NON-NLS-1$
        Check.notNullOrEmpty(newItemName, "newItemName"); //$NON-NLS-1$

        /*
         * If the object already exists in this store, there is nothing to do.
         */
        if (containsItem(newItemName)) {
            return true;
        }

        boolean haveOldItem;

        try {
            haveOldItem = oldStore.containsItem(oldItemName);
        } catch (final IOException e) {
            log.warn(MessageFormat.format(
                "Error testing for existence of item {0} in old store {1}", //$NON-NLS-1$
                oldItemName,
                oldStore.toString()), e);
            return false;
        }

        /*
         * Does the old store hold a component that we can migrate to our
         * location?
         */
        if (haveOldItem) {
            AdvisoryFileLock oldComponentLock = null;
            AdvisoryFileLock newComponentLock = null;
            try {
                /*
                 * Take out a blocking lock on the old an new locations.
                 */
                oldComponentLock = oldStore.getItemLock(oldItemName, true);
                newComponentLock = getItemLock(newItemName, true);

                /*
                 * Now that we have the locks, do another check to make sure we
                 * still should do the migration. Doing this check here helps
                 * reduce the window for a double-migration race condition.
                 */
                if (oldStore.containsItem(oldItemName) && !containsItem(newItemName)) {
                    log.info(MessageFormat.format(
                        "attempting to migrate [{0}] from old store [{1}] to new store [{2}]", //$NON-NLS-1$
                        oldItemName,
                        oldStore.toString(),
                        toString()));

                    /*
                     * Call copyObject() to attempt the migration.
                     */
                    if (copyObjectStreams(
                        oldStore.getItemInputStream(oldItemName),
                        getItemOutputStream(newItemName)) == false) {
                        /*
                         * Delete the incomplete target.
                         */
                        deleteItem(newItemName);

                        return false;
                    }

                    return true;
                }
            } catch (final Exception ex) {
                log.warn(MessageFormat.format(
                    "problem during locking phase for migration of [{0}] from old store [{1}] to new store [{2}]", //$NON-NLS-1$
                    oldItemName,
                    oldStore.toString(),
                    toString()), ex);
                /*
                 * an exception happened trying to get a lock
                 */
                return false;
            } finally {
                /*
                 * make sure to release any locks we've managed to acquire (and
                 * release them in the same order they were acquired)
                 */

                if (oldComponentLock != null) {
                    try {
                        oldComponentLock.release();
                    } catch (final IOException e) {
                    }
                }
                if (newComponentLock != null) {
                    try {
                        newComponentLock.release();
                    } catch (final IOException e) {
                    }
                }
            }
        }

        log.info(
            MessageFormat.format("object [{0}] was not found in the store [{1}]", oldItemName, oldStore.toString())); //$NON-NLS-1$
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public InputStream getItemInputStream(final String itemName) throws IOException {
        Check.notNullOrEmpty(itemName, "itemName"); //$NON-NLS-1$

        return new FileInputStream(getItemFile(itemName));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public OutputStream getItemOutputStream(final String itemName) throws IOException {
        Check.notNullOrEmpty(itemName, "itemName"); //$NON-NLS-1$

        return new FileOutputStream(getItemFile(itemName));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return directory.toString();
    }

    /**
     * @return the {@link File} that is the directory where this
     *         {@link PersistenceStore} saves data.
     */
    public File getStoreFile() {
        return directory;
    }

    /**
     * Gets the {@link File} for the given item name.
     *
     * @param itemName
     *        the item name (must not be <code>null</code> or empty)
     * @return the {@link File} for the item
     */
    public File getItemFile(final String itemName) {
        Check.notNullOrEmpty(itemName, "itemName"); //$NON-NLS-1$

        return new File(directory, itemName);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AdvisoryFileLock getItemLock(final String itemName, final boolean block)
        throws IOException,
            InterruptedException {
        Check.notNullOrEmpty(itemName, "itemName"); //$NON-NLS-1$

        return getLockInternal(itemName, block);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AdvisoryFileLock getStoreLock(final boolean block) throws IOException, InterruptedException {
        return getLockInternal(null, block);
    }

    /**
     * Copies the source data to the target.
     *
     * @param source
     *        the source (must not be <code>null</code>)
     * @param target
     *        the target (must not be <code>null</code>)
     * @return true if the copy succeeded, false if it failed (and the target
     *         should probably be deleted)
     */
    private boolean copyObjectStreams(final InputStream source, final OutputStream target) {
        Check.notNull(source, "source"); //$NON-NLS-1$
        Check.notNull(target, "target"); //$NON-NLS-1$

        InputStream input = null;
        OutputStream output = null;
        boolean errorOccurred = false;

        try {
            input = new BufferedInputStream(source);
            output = new BufferedOutputStream(target);
            final byte[] buffer = new byte[2048];
            int len;

            while ((len = input.read(buffer)) != -1) {
                output.write(buffer, 0, len);
            }
        } catch (final IOException ex) {
            log.warn("unable to copy object streams for migration", ex); //$NON-NLS-1$
            errorOccurred = true;
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (final IOException e) {
                }
            }
            if (output != null) {
                try {
                    output.close();
                } catch (final IOException e) {
                }
            }
        }

        return errorOccurred == false;
    }

    /**
     * Pass null for objectID for a directory lock, non-null for a file lock.
     */
    private AdvisoryFileLock getLockInternal(final String itemName, final boolean block)
        throws IOException,
            InterruptedException {
        File lockFile;

        // Construct the lock file for the given component.
        if (itemName != null) {
            lockFile = new File(directory, FILE_LOCK_PREFIX + itemName);
        } else {
            lockFile = new File(directory, DIRECTORY_LOCK_FILE);
        }

        // Ensure the intermediate directories exist.
        initialize();

        /*
         * Create the lock file if it doesn't already exist. Harmless to do if
         * it does exist. We don't ever delete this file.
         */
        lockFile.createNewFile();

        /*
         * This method does most of the work. It will block if desired, and
         * returns null under only the same conditions as we do.
         */
        return AdvisoryFileLock.create(lockFile, block);
    }

    @Override
    public int hashCode() {
        return directory.hashCode();
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == this) {
            return true;
        }

        if (obj instanceof FilesystemPersistenceStore == false) {
            return false;
        }

        return directory.equals(((FilesystemPersistenceStore) obj).directory);
    }
}
