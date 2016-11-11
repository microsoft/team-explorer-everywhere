// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.util.temp;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.GUID;
import com.microsoft.tfs.util.IOUtils;
import com.microsoft.tfs.util.Messages;
import com.microsoft.tfs.util.shutdown.ShutdownEventListener;
import com.microsoft.tfs.util.shutdown.ShutdownManager;
import com.microsoft.tfs.util.shutdown.ShutdownManager.Priority;

/**
 * <p>
 * Creates temporary files and directories with more flexibility than the
 * {@link File} class. Temp items can be deleted immediately with
 * {@link #cleanUpItem(File)}. By default items the service creates will be
 * deleted when the JVM shuts down, but this behavior can be changed per-item
 * with {@link #forgetItem(File)}.
 * </p>
 * <p>
 * This class is a singleton.
 * </p>
 *
 * @since TEE-SDK-10.1
 * @threadsafety thread-safe
 */
public final class TempStorageService {
    private final static Log log = LogFactory.getLog(TempStorageService.class);

    private final static int MAX_RENAME_ATTEMPTS = 5;
    private final static int RENAME_ATTEMPTS_DELAY = 500; // in milliseconds

    private static boolean nioClassesLoadable = true;
    private static boolean nioClassesLoaded = false;
    private static Class<?> filesClass;
    private static Class<?> pathInterface;
    private static Class<?> pathsClass;
    private static Class<?> copyOptionInterfaceArray;
    private static Class<?> copyOptionInterface;
    private static Method getMethod;
    private static Method moveMethod;
    private static Object copyOptions;

    /**
     * The singleton.
     */
    private static TempStorageService instance;

    /**
     * The default extension for files we create.
     */
    public final static String DEFAULT_EXTENSION = ".tmp"; //$NON-NLS-1$

    /**
     * A counter used tag {@link CleanUpItem}s in the order they are created.
     * When we process clean up items up we sort the collection by serial number
     * so we delete earlier items first.
     */
    private final AtomicLong currentSerialNumber = new AtomicLong(0);

    /**
     * When the user does not specify a directory where temp files should be
     * created, they go here. Lazily initialized with what Java thinks is the
     * system's temp dir. Synchronized on this.
     */
    private File systemTempDir;

    /**
     * Maps a {@link File} to a {@link CleanUpItem} that we can delete in the
     * future (unless its deletion is canceled by the user). Synchronized on
     * this.
     */
    private final Map<File, CleanUpItem> cleanUpItems = new HashMap<File, CleanUpItem>();

    /**
     * Describes a temporary file or directory that was allocated by
     * {@link TempStorageService} and needs to be deleted by it in the future.
     *
     * @threadsafety immutable
     */
    private static class CleanUpItem implements Comparable<CleanUpItem> {
        /**
         * The file that gets cleaned up for this item. May be the actual temp
         * item created but could be a parent if the parent was created by
         * {@link TempStorageService}.
         */
        private final File cleanUpFile;

        /**
         * Used to order {@link CleanUpItem}s during a sort. A
         * {@link TempStorageService} instance increments a counter each time it
         * creates a new {@link CleanUpItem} so it can sort them for correct
         * deletion.
         */
        private final long serialNumber;

        /**
         * Creates a {@link CleanUpItem} that is managed by
         * {@link TempStorageService}.
         *
         * @param cleanUpFile
         *        the local file or directory that should be cleaned up
         *        (recursive) for this item (must not be <code>null</code>)
         * @param serialNumber
         *        a number that represents the order in which this
         *        {@link CleanUpItem} was created relative to others created by
         *        the same {@link TempStorageService}
         */
        public CleanUpItem(final File cleanUpFile, final long serialNumber) {
            Check.notNull(cleanUpFile, "cleanUpFile"); //$NON-NLS-1$

            this.cleanUpFile = cleanUpFile;
            this.serialNumber = serialNumber;
        }

        /**
         * @return the local item to delete
         */
        public File getCleanUpFile() {
            return this.cleanUpFile;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int compareTo(final CleanUpItem other) {
            if (serialNumber < other.serialNumber) {
                return -1;
            } else if (serialNumber > other.serialNumber) {
                return 1;
            } else {
                return 0;
            }
        }
    }

    private TempStorageService() {
        /*
         * Register for late invocation because deleting temp files doesn't
         * depend on other resources but they may depend on this.
         */
        ShutdownManager.getInstance().addShutdownEventListener(new ShutdownEventListener() {
            @Override
            public void onShutdown() {
                /*
                 * Normally a class which implements ShutdownEventListener would
                 * remove itself from the ShutdownManager's listeners.
                 * Singletons don't really fit this pattern, and there's no harm
                 * in calling cleanUpAllItems() multiple times, so we don't
                 * bother.
                 */
                cleanUpAllItems();
            }
        }, Priority.LATE);
    }

    /**
     * @return the single instance of {@link TempStorageService}.
     */
    public static synchronized TempStorageService getInstance() {
        if (instance == null) {
            instance = new TempStorageService();
        }

        return instance;
    }

    /**
     * <p>
     * Creates a new empty file in a new directory inside the system's default
     * temporary directory. See {@link #createTempFile(File, String)} for how to
     * delete the file.
     * </p>
     *
     * @return the empty temp file that was created
     * @throws IOException
     *         if a filesystem error occurred creating the temporary file
     */
    public File createTempFile() throws IOException {
        return createTempFile(null);
    }

    /**
     * <p>
     * Creates a new empty file with the specified extension in a new directory
     * inside the system's default temporary directory. See
     * {@link #createTempFile(File, String)} for how to delete the file.
     * </p>
     *
     * @return the empty temp file that was created
     * @param extension
     *        the extension (including the '.') to use for the temporary file's
     *        name (pass <code>null</code> or empty to use the default,
     *        {@link #DEFAULT_EXTENSION})
     * @throws IOException
     *         if a filesystem error occurred creating the temporary file
     */
    public File createTempFile(final String extension) throws IOException {
        return createTempFile(null, extension);
    }

    /**
     * <p>
     * Creates a new empty file with the specified extension in the specified
     * directory.
     * </p>
     * <p>
     * When done using the file, do one of:
     * <ul>
     * <li>Call {@link #cleanUpItem(File)} with the file;
     * {@link TempStorageService} deletes it immediately</li>
     * <li>Do nothing; {@link TempStorageService} will delete it on JVM shutdown
     * </li>
     * <li>Call {@link #forgetItem(File)} with the file to prevent deletion on
     * JVM shutdown; delete it yourself if desired</li>
     * <li>Call {@link #cleanUpAllItems()} to delete all items created by
     * {@link TempStorageService}</li>
     * </ul>
     * </p>
     *
     * @return the temp file that was created
     * @param userDirectory
     *        the directory in which to create this file (which must already
     *        exist). If <code>null</code> or empty, a new directory is created
     *        inside the system's temporary location and the file is created
     *        there and that directory is automatically cleaned up when the file
     *        is cleaned up
     * @param extension
     *        the extension (including the '.') to use for the temporary file
     *        (pass <code>null</code> or empty to use the default).
     * @throws IOException
     *         if a filesystem error occurred creating the temporary file.
     */
    public File createTempFile(final File userDirectory, String extension) throws IOException {
        if (extension == null || extension.length() == 0) {
            extension = DEFAULT_EXTENSION;
        }

        final boolean createDirectory = userDirectory == null || userDirectory.length() == 0;

        /*
         * Create a temp directory, not remembered for clean up.
         */
        final File dir = (createDirectory) ? createTempDirectoryInternal() : userDirectory;

        // Create a file.
        final File file = File.createTempFile("tfs", extension, dir); //$NON-NLS-1$

        /*
         * Remember a clean up item for the temp directory we created if we
         * created one, otherwise for the file itself (the user specified a
         * directory).
         */
        cleanUpItems.put(file, new CleanUpItem((createDirectory) ? dir : file, currentSerialNumber.getAndIncrement()));

        if (createDirectory) {
            log.debug(MessageFormat.format("remembered directory ''{0}'' for clean up (parent of ''{1}'')", dir, file)); //$NON-NLS-1$
        } else {
            log.debug(MessageFormat.format("remembered file ''{0}'' for clean up", file)); //$NON-NLS-1$
        }

        return file;
    }

    /**
     * Creates a temporary directory (named after a new GUID) inside the
     * system's default temporary directory and returns its full path. When
     * you're done with the object, you may call {@link #cleanUpItem(File)} to
     * have it deleted.
     * <p>
     * If you do not call {@link #cleanUpItem(File)} or
     * {@link #forgetItem(File)} in the future, any future call to
     * {@link #cleanUpAllItems()} will clean it up for you.
     *
     * @return the temp directory that was created
     * @throws IOException
     *         if a filesystem error occurred creating the temporary directory.
     */
    public File createTempDirectory() throws IOException {
        final File dir = createTempDirectoryInternal();

        cleanUpItems.put(dir, new CleanUpItem(dir, currentSerialNumber.getAndIncrement()));

        log.debug(MessageFormat.format("remembered directory ''{0}'' for clean up", dir)); //$NON-NLS-1$

        return dir;
    }

    /**
     * Prevents the {@link TempStorageService} from cleaning up (deleting) this
     * item automatically in the future.
     *
     * @param item
     *        the file or directory created by {@link TempStorageService} that
     *        should not be cleaned up (deleted) by {@link TempStorageService}
     *        in the future (must not be <code>null</code>)
     */
    public synchronized void forgetItem(final File item) {
        Check.notNull(item, "item"); //$NON-NLS-1$

        final CleanUpItem removed = cleanUpItems.remove(item);

        if (removed != null) {
            log.debug(MessageFormat.format(
                "forgot clean up item ''{0}'' for temp item ''{1}''", //$NON-NLS-1$
                removed.getCleanUpFile(),
                item));
        } else {
            log.debug(
                MessageFormat.format("could not forget clean up item for ''{0}'': not found (this is harmless)", item)); //$NON-NLS-1$
        }
    }

    /**
     * Sometime a file cannot be renamed because it's locked by a file system
     * scanning process like anti-viruses software, user virtualization client,
     * Google console, etc. This tries to rename with several retries. If rename
     * fails, the source file copies to the target destination and registers
     * with TempStorageService for future deletion.
     *
     * @param sourceItem
     *        the file or directory to rename (must not be <code>null</code>)
     * @param targetItem
     *        the new file or directory (must not be <code>null</code> and not
     *        exist in the file system)
     */
    public synchronized void renameItem(final File sourceItem, final File targetItem) {
        Check.notNull(sourceItem, "sourceItem"); //$NON-NLS-1$
        Check.notNull(targetItem, "targetItem"); //$NON-NLS-1$

        if (!sourceItem.exists()) {
            throw new RuntimeException(
                MessageFormat.format(
                    Messages.getString("TempStorageService.RenameErrorSourceDoesNotExistFormat"), //$NON-NLS-1$
                    sourceItem.getAbsolutePath(),
                    targetItem.getAbsolutePath()));
        }
        if (targetItem.exists()) {
            throw new RuntimeException(
                MessageFormat.format(
                    Messages.getString("TempStorageService.RenameErrorTargetExistsFormat"), //$NON-NLS-1$
                    sourceItem.getAbsolutePath(),
                    targetItem.getAbsolutePath()));
        }

        for (int k = 0; k < MAX_RENAME_ATTEMPTS; k++) {
            if (k > 0) {
                log.debug(
                    MessageFormat.format(
                        "delaying attempt {0} to rename ''{1}'' to ''{2}'' for {3} milliseconds.", //$NON-NLS-1$
                        k + 1,
                        sourceItem.getAbsolutePath(),
                        targetItem.getAbsolutePath(),
                        RENAME_ATTEMPTS_DELAY));

                try {
                    synchronized (Thread.currentThread()) {
                        Thread.currentThread().wait(RENAME_ATTEMPTS_DELAY);
                    }
                } catch (final Exception e) {
                    log.debug(MessageFormat.format("the renaming delay cancelled before attempt {0}.", k + 11)); //$NON-NLS-1$
                    return;
                }
            }

            if (renameInternal(sourceItem, targetItem)) {
                log.debug(MessageFormat.format(
                    "attempt {0} to rename ''{1}'' to ''{2}'' succeeded.", //$NON-NLS-1$
                    k + 1,
                    sourceItem.getAbsolutePath(),
                    targetItem.getAbsolutePath()));
                return;
            } else {
                log.debug(MessageFormat.format(
                    "attempt {0} to rename ''{1}'' to ''{2}'' failed.", //$NON-NLS-1$
                    k + 1,
                    sourceItem.getAbsolutePath(),
                    targetItem.getAbsolutePath()));
            }
        }

        if (IOUtils.copy(sourceItem, targetItem)) {
            log.debug(MessageFormat.format(
                "copy ''{1}'' to ''{2}'' succeeded.", //$NON-NLS-1$
                sourceItem.getAbsolutePath(),
                targetItem.getAbsolutePath()));

        } else {
            log.debug(MessageFormat.format(
                "copy ''{1}'' to ''{2}'' failed.", //$NON-NLS-1$
                sourceItem.getAbsolutePath(),
                targetItem.getAbsolutePath()));
        }

        deleteItem(sourceItem);
    }

    private boolean renameInternal(final File sourceItem, final File targetItem) {
        if (nioClassesLoadable && nioClassesLoaded) {
            /*
             * We have already tried reflection and it worked out. Let's
             * continue the same way.
             */
            return renameInternalUsingReflection(sourceItem, targetItem);
        }

        /*
         * We never tried reflection or JDK 1.7 is not available. Let's try
         * first to rename using JDK 1.0
         */
        if (sourceItem.renameTo(targetItem)) {
            return true;
        } else if (nioClassesLoadable) {
            /*
             * Let's try to load JDK 1.7 classes and use reflection
             */
            tryLoadNioClasses();
            return renameInternalUsingReflection(sourceItem, targetItem);
        } else {
            /*
             * We did our best, but were not able neither to rename files nor to
             * provide extended error diagnostic.
             */
            return false;
        }
    }

    private boolean renameInternalUsingReflection(final File sourceItem, final File targetItem) {
        if (nioClassesLoaded) {
            try {
                final Object sourcePath = getMethod.invoke(null, sourceItem.getAbsolutePath(), new String[0]);
                final Object targetPath = getMethod.invoke(null, targetItem.getAbsolutePath(), new String[0]);
                moveMethod.invoke(null, sourcePath, targetPath, copyOptions);

                return true;
            } catch (final Exception e) {
                log.warn(e.getMessage());
                return false;
            }
        } else {
            return false;
        }
    }

    private void tryLoadNioClasses() {
        if (nioClassesLoadable && !nioClassesLoaded) {
            try {
                filesClass = Class.forName("java.nio.file.Files"); //$NON-NLS-1$
                pathInterface = Class.forName("java.nio.file.Path"); //$NON-NLS-1$
                pathsClass = Class.forName("java.nio.file.Paths"); //$NON-NLS-1$
                copyOptionInterfaceArray = Class.forName("[Ljava.nio.file.CopyOption;"); //$NON-NLS-1$
                copyOptionInterface = Class.forName("java.nio.file.CopyOption"); //$NON-NLS-1$

                moveMethod = filesClass.getMethod("move", new Class<?>[] { //$NON-NLS-1$
                    pathInterface,
                    pathInterface,
                    copyOptionInterfaceArray
                });
                getMethod = pathsClass.getMethod("get", new Class<?>[] { //$NON-NLS-1$
                    String.class,
                    String[].class
                });

                copyOptions = Array.newInstance(copyOptionInterface, 0);

                nioClassesLoaded = true;
            } catch (final Exception e) {
                log.warn("Cannot load java.nio.file classes: " + e.getMessage()); //$NON-NLS-1$
                nioClassesLoadable = false;
            }
        }
    }

    public synchronized void deleteItem(final File item) {
        log.debug(MessageFormat.format("Trying to delete item ''{1}''.", item.getAbsolutePath())); //$NON-NLS-1$
        if (!item.delete()) {
            log.debug(MessageFormat.format("Remember a clean up item later for ''{1}''.", item.getAbsolutePath())); //$NON-NLS-1$
            cleanUpItems.put(item, new CleanUpItem(item, currentSerialNumber.getAndIncrement()));
        }
    }

    /**
     * Creates a new temp directory in the system's temp directory using a GUID
     * name. Does not remember the directory for clean up.
     *
     * @return the new temp directory (not remembered for clean up)
     */
    private File createTempDirectoryInternal() {
        final File dir = new File(getSystemTempFile(), GUID.newGUIDString());

        dir.mkdirs();

        return dir;
    }

    /**
     * <p>
     * Cleans up (deletes) a file or directory which was allocated by this
     * service. If the item is a file in a directory which was also created by
     * the service, that directory is cleaned up (recursively).
     * </p>
     * <p>
     * If the path was not allocated by this {@link TempStorageService} or was
     * already cleaned up, no exception is thrown and the item is ignored.
     * </p>
     *
     * @param item
     *        an item that was created by {@link #createTempFile()} or
     *        {@link #createTempDirectory()} (must not be <code>null</code>)
     */
    public synchronized void cleanUpItem(final File item) {
        Check.notNull(item, "item"); //$NON-NLS-1$

        final CleanUpItem tempItem = cleanUpItems.get(item);

        if (tempItem == null) {
            log.debug(MessageFormat.format("could not clean up for item ''{0}'': not found (this is harmless)", item)); //$NON-NLS-1$
            return;
        }

        cleanUpItemInternal(tempItem);

        if (!item.exists()) {
            cleanUpItems.remove(item);
        }
    }

    /**
     * Cleans up (deletes) files and directories allocated by this service. Can
     * be called multiple times on a single {@link TempStorageService} instance.
     */
    public synchronized void cleanUpAllItems() {
        final Collection<CleanUpItem> values = cleanUpItems.values();

        if (values.size() == 0) {
            return;
        }

        // Sort them so we delete them in the order created.
        final CleanUpItem[] items = values.toArray(new CleanUpItem[values.size()]);
        Arrays.sort(items);

        for (int i = 0; i < items.length; i++) {
            cleanUpItemInternal(items[i]);
        }

        cleanUpItems.clear();
    }

    /**
     * Cleans up one {@link CleanUpItem}.
     *
     * @param tempItem
     *        the item that should be deleted from disk (must not be
     *        <code>null</code>)
     */
    private void cleanUpItemInternal(final CleanUpItem tempItem) {
        Check.notNull(tempItem, "tempItem"); //$NON-NLS-1$

        final File cleanUpFile = tempItem.getCleanUpFile();

        log.debug(MessageFormat.format("deleting ''{0}'' (recursively)", cleanUpFile)); //$NON-NLS-1$

        deleteRecursive(cleanUpFile);
    }

    /**
     * Deletes a {@link File} (whether actually directory or file), recursing
     * down any directories it finds.
     *
     * @param fileOrDirectory
     *        the local disk file or directory to delete (if <code>null</code>,
     *        returns immediately)
     */
    private void deleteRecursive(final File fileOrDirectory) {
        if (fileOrDirectory == null) {
            return;
        }

        final File[] subFiles = fileOrDirectory.listFiles();

        if (subFiles == null) {
            // Object is a file, not a directory.
            if (fileOrDirectory.delete() == false) {
                log.warn(MessageFormat.format(
                    "could not delete ''{0}'' (no futher information available)", //$NON-NLS-1$
                    fileOrDirectory));
            }
        } else {
            // Recurse into each child directory.
            for (int i = 0; i < subFiles.length; i++) {
                deleteRecursive(subFiles[i]);
            }

            if (fileOrDirectory.delete() == false) {
                log.warn(MessageFormat.format(
                    "could not delete directory ''{0}'' (no futher information available)", //$NON-NLS-1$
                    fileOrDirectory.getAbsolutePath()));
            }
        }
    }

    /**
     * @return the system's temporary directory (precisely, java.io.tmpdir
     *         property).
     */
    private synchronized File getSystemTempFile() {
        if (systemTempDir == null) {
            systemTempDir = new File(System.getProperty("java.io.tmpdir")); //$NON-NLS-1$
        }

        return systemTempDir;
    }
}
