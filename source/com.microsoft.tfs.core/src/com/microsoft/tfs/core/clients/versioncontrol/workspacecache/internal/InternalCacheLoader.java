// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.workspacecache.internal;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.MessageFormat;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.microsoft.tfs.core.Messages;
import com.microsoft.tfs.core.clients.versioncontrol.Workstation;
import com.microsoft.tfs.core.clients.versioncontrol.exceptions.VersionControlException;
import com.microsoft.tfs.core.clients.versioncontrol.workspacecache.InternalWorkspaceConflictInfo;
import com.microsoft.tfs.jni.filelock.TFSFileLock;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.xml.DOMCreateUtils;
import com.microsoft.tfs.util.xml.DOMSerializeUtils;
import com.microsoft.tfs.util.xml.DOMUtils;
import com.microsoft.tfs.util.xml.XMLException;

/**
 * Loads and saves {@link InternalCache} objects from/to files.
 *
 * @threadsafety thread-safe
 */
public class InternalCacheLoader {
    private final static Log log = LogFactory.getLog(InternalCacheLoader.class);

    /**
     * Wait this long to acquire the lock to read or write the cache file.
     */
    private static final int LOCK_ACQUIRE_TIMEOUT_MILLISECONDS = 10 * 1000;

    /**
     * The cache file name without path.
     */
    public static final String FILE_NAME = "VersionControl.config"; //$NON-NLS-1$

    /**
     * The config file root node element name.
     */
    private static final String XML_VERSION_CONTROL_SERVER = "VersionControlServer"; //$NON-NLS-1$

    private static final String XML_SERVERS = "Servers"; //$NON-NLS-1$

    /**
     * Load the config data. This can be called more than once. When it is
     * called a second time, it means to discard all current config data and
     * reload.
     *
     * @param currentCache
     *        the current cache or <code>null</code>
     * @param dataDirectoryExists
     *        true if the data directory exists, false if it does not exist
     * @param conflictingWorkspaces
     *        list of workspaces that were removed from the cache due to
     *        conflicts (must not be <code>null</code>)
     * @param workstationMutex
     *        the synchronization object for the new instance to use (see
     *        {@link #InternalCache(Object)}) (must not be <code>null</code>)
     * @param file
     *        the file to read data from (must not be <code>null</code>)
     * @return the {@link Workstation}'s mutex object (must not be
     *         <code>null</code>), which is passed to the newly created
     *         {@link InternalCache}
     */
    public static InternalCache loadConfig(
        InternalCache currentCache,
        final boolean dataDirectoryExists,
        final AtomicReference<InternalWorkspaceConflictInfo[]> conflictingWorkspaces,
        final Object workstationMutex,
        final File file) {
        Check.notNull(conflictingWorkspaces, "conflictingWorkspaces"); //$NON-NLS-1$
        Check.notNull(workstationMutex, "workstationMutex"); //$NON-NLS-1$
        Check.notNull(file, "file"); //$NON-NLS-1$

        conflictingWorkspaces.set(InternalWorkspaceConflictInfo.EMPTY_ARRAY);

        // If the cache directory is inaccessible, we're done.
        if (!dataDirectoryExists) {
            // There is no cache file, so create an empty cache.
            return new InternalCache(workstationMutex);
        }

        final TFSFileLock lock = acquireLockOrThrow(file);

        Document config = null;
        try {
            config = readCacheAsDocument(file);

            if (config == null) {
                // There is no cache file, so create an empty cache.
                currentCache = new InternalCache(workstationMutex);
            } else {
                if (currentCache == null) {
                    // No cache has been loaded, so load what's on disk.
                    currentCache = new InternalCache(workstationMutex);
                    currentCache.load(DOMUtils.getFirstChildElement(config.getDocumentElement(), XML_SERVERS));
                } else {
                    // We have already loaded a cache, so we need to merge
                    // with what's on disk.
                    currentCache.merge(
                        DOMUtils.getFirstChildElement(config.getDocumentElement(), XML_SERVERS),
                        conflictingWorkspaces);
                }
            }
        } catch (final XMLException e) {
            log.warn(MessageFormat.format(
                Messages.getString("InternalCacheLoader.InvalidCacheFileFormat"), //$NON-NLS-1$
                file), e);

            throw new VersionControlException(
                MessageFormat.format(Messages.getString("InternalCacheLoader.InvalidCacheFileFormat"), file), //$NON-NLS-1$
                e);
        } finally {
            lock.release();
            lock.close();
        }

        return currentCache;
    }

    /**
     * Save the local configuration data if it is dirty and the
     * {@link Workstation}'s cache is enabled.
     *
     * @param internalCache
     *        the cache to save (must not be <code>null</code>)
     * @param conflictingWorkspaces
     *        list of workspaces that were removed from the cache due to
     *        conflicts (must not be <code>null</code>)
     * @param workstationMutex
     *        the {@link Workstation}'s mutex object (must not be
     *        <code>null</code>), which is held during the save
     * @param file
     *        the file to save to (must not be <code>null</code>)
     */
    public static void saveConfigIfDirty(
        final InternalCache internalCache,
        final AtomicReference<InternalWorkspaceConflictInfo[]> conflictingWorkspaces,
        final Object workstationMutex,
        final File file) {
        Check.notNull(conflictingWorkspaces, "conflictingWorkspaces"); //$NON-NLS-1$
        Check.notNull(workstationMutex, "workstationMutex"); //$NON-NLS-1$
        Check.notNull(file, "file"); //$NON-NLS-1$

        conflictingWorkspaces.set(InternalWorkspaceConflictInfo.EMPTY_ARRAY);

        // If the cached data hasn't changed in RAM or the cache directory is
        // inaccessible, we're done.
        if (!internalCache.isDirty()) {
            return;
        }

        // Create the document and the root node.
        final Document config = DOMCreateUtils.newDocument(XML_VERSION_CONTROL_SERVER);
        final Element rootNode = config.getDocumentElement();

        /*
         * We need to lock the cache file, if it exists, during the entire save
         * process. To prevent a deadlock where one thread grabs the file lock
         * and the other grabs the workstation lock, grab the workstation lock
         * first and hold it until the cache is marked clean (MarkClean()).
         */

        synchronized (workstationMutex) {
            final TFSFileLock lock = acquireLockOrThrow(file);

            try {
                // Read in the existing cache file, if any.
                final Document oldConfig = readCacheAsDocument(file);

                Element oldCacheNode = null;
                if (oldConfig != null) {
                    oldCacheNode = DOMUtils.getFirstChildElement(oldConfig.getDocumentElement(), XML_SERVERS);
                }

                // Save the cached workspace data.
                final Element cacheNode = DOMUtils.appendChild(rootNode, XML_SERVERS);

                internalCache.save(oldCacheNode, cacheNode, conflictingWorkspaces);

                // Save the file.

                // Ensure the directories exist
                if (!file.exists() && file.getParentFile().exists() == false) {
                    file.getParentFile().mkdirs();
                }

                OutputStream stream;
                try {
                    stream = new FileOutputStream(file);

                    DOMSerializeUtils.serializeToStream(
                        config,
                        stream,
                        DOMSerializeUtils.ENCODING_UTF8,
                        DOMSerializeUtils.INDENT);
                } catch (final FileNotFoundException e) {
                    // from FileOutputStream

                    // We tried to create the directories above, so this may be
                    // a permissions problem. Ignore the error and mark the
                    // cache clean (below) during a normal exit.
                }
            } finally {
                lock.release();
                lock.close();
            }

            /*
             * The file has been saved and is no longer dirty. NOTE: This must
             * be inside the (synchronized) lock to prevent the race condition
             * where the cache gets marked clean right after another thread
             * modifies it (lock must be taken here and where the cache is
             * modified).
             */
            internalCache.markClean();
        }
    }

    /**
     * Reads the cache file into a {@link Document}. Does no locking; the caller
     * must ensure exclusive access to the file.
     *
     * @param file
     *        the file to read (must not be <code>null</code>)
     * @return the {@link Document} read from the file, <code>null</code> if the
     *         file was not found
     * @throws XMLException
     *         if the file existed but could not be parsed as an XML document
     */
    private static Document readCacheAsDocument(final File file) {
        Check.notNull(file, "file"); //$NON-NLS-1$

        try {
            // Open the file here to detect FileNotFoundException and other
            // problems
            final InputStream stream = new FileInputStream(file);

            // parseStream always closes the stream
            return DOMCreateUtils.parseStream(stream, DOMSerializeUtils.ENCODING_UTF8, DOMCreateUtils.NONE);
        } catch (final FileNotFoundException e) {
            // from FileInputStream

            return null;
        } catch (final XMLException e) {
            // from DOMCreatUtils

            log.warn(MessageFormat.format(
                Messages.getString("InternalCacheLoader.InvalidCacheFileFormat"), //$NON-NLS-1$
                file), e);

            throw new VersionControlException(
                MessageFormat.format(Messages.getString("InternalCacheLoader.InvalidCacheFileFormat"), file), //$NON-NLS-1$
                e);
        }
    }

    /**
     * Creates a {@link TFSFileLock} for the given {@link File} and tries to
     * acquire it. If the lock can't be acquired after
     * {@value #LOCK_ACQUIRE_TIMEOUT_MILLISECONDS} ms, closes the lock and
     * throws a {@link VersionControlException}.
     * <p>
     * If this method throws {@link VersionControlException} the caller does not
     * have to do any cleanup.
     * <p>
     * If this method returns a {@link TFSFileLock} the caller must call
     * {@link TFSFileLock#release()} and {@link TFSFileLock#close()}.
     *
     * @param file
     *        the file to get the lock on (must not be <code>null</code>)
     * @return a {@link TFSFileLock} object, acquired
     * @throws VersionControlException
     *         if the file lock could not be allocated or acquired
     */
    private static TFSFileLock acquireLockOrThrow(final File file) throws VersionControlException {
        Check.notNull(file, "file"); //$NON-NLS-1$

        final TFSFileLock lock = new TFSFileLock(file.getAbsolutePath());

        if (!lock.acquire(LOCK_ACQUIRE_TIMEOUT_MILLISECONDS)) {
            lock.close();

            throw new VersionControlException(Messages.getString("InternalCacheLoader.CouldNotLockCacheFile")); //$NON-NLS-1$
        }

        return lock;
    }
}