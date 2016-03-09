// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.internal.localworkspace;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URI;
import java.text.MessageFormat;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Map;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.xml.stream.XMLStreamException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.microsoft.tfs.core.Messages;
import com.microsoft.tfs.core.clients.versioncontrol.exceptions.VersionControlException;
import com.microsoft.tfs.core.clients.versioncontrol.localworkspace.LocalItemExclusionsUpdatedListener;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.LocalItemExclusionSet;
import com.microsoft.tfs.core.clients.versioncontrol.workspacecache.internal.InternalServerInfo;
import com.microsoft.tfs.core.util.URIUtils;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.GUID;
import com.microsoft.tfs.util.listeners.SingleListenerFacade;
import com.microsoft.tfs.util.xml.DOMCreateUtils;
import com.microsoft.tfs.util.xml.DOMSerializeUtils;
import com.microsoft.tfs.util.xml.DOMUtils;
import com.microsoft.tfs.util.xml.XMLException;

/**
 * An in-memory version of the local item exclusion cache from disk.
 *
 * @threadsafety thread-safe
 */
public class LocalItemExclusionCache {
    public static final String FILE_NAME = "LocalItemExclusions.config"; //$NON-NLS-1$

    // XML tokens
    private static final String EXCLUSION_ROOT = "LocalItemExclusions"; //$NON-NLS-1$
    private static final String TEAM_PROJECT_COLLECTION = "TeamProjectCollection"; //$NON-NLS-1$
    private static final String COLLECTION_ID = "id"; //$NON-NLS-1$
    private static final String COLLECTION_URL = "uri"; //$NON-NLS-1$

    private static final Log log = LogFactory.getLog(LocalItemExclusionSet.class);

    /**
     * Provides locking semantics equivalent to a synchronized block while using
     * read or write locks.
     */
    private final ReentrantReadWriteLock dataManagementLock = new ReentrantReadWriteLock();
    private final File cacheFile;
    private final boolean cacheEnabled;
    private volatile boolean cacheLoaded = false;

    /**
     * Listeners for changes to the cache.
     *
     * Thread-safe.
     */
    private final SingleListenerFacade listeners = new SingleListenerFacade(LocalItemExclusionsUpdatedListener.class);

    /**
     * The cache data.
     *
     * Synchronized on {@link #dataManagementLock}.
     */
    private final Map<InternalServerInfo, ExclusionSet> exclusions =
        new TreeMap<InternalServerInfo, ExclusionSet>(new InternalServerInfoGUIDComparator());

    // Unlike VS we pass in the file; it's easier this way
    public LocalItemExclusionCache(final File cacheFile, final boolean cacheEnabled) {
        this.cacheFile = cacheFile;
        this.cacheEnabled = cacheEnabled;
    }

    public void addExclusionsUpdatedListener(final LocalItemExclusionsUpdatedListener listener) {
        listeners.addListener(listener);
    }

    public void removeExclusionUpdatedListener(final LocalItemExclusionsUpdatedListener listener) {
        listeners.removeListener(listener);
    }

    /**
     * Removes the provided exclusion from both the default and user exclusion
     * lists.
     *
     * @param serverInfo
     * @param exclusion
     *        The exclusion to remove.
     */
    public void removeExclusion(final InternalServerInfo serverInfo, final String exclusion) {
        Check.notNull(serverInfo, "serverInfo"); //$NON-NLS-1$
        Check.notNullOrEmpty(exclusion, "exclusion"); //$NON-NLS-1$

        ensureDiskCacheLoaded();

        boolean somethingRemoved = false;

        try {
            dataManagementLock.writeLock().lock();

            final ExclusionSet exclusionSet = exclusions.get(serverInfo);
            if (exclusionSet != null) {
                somethingRemoved = exclusionSet.removeExclusion(exclusion);

                if (somethingRemoved) {
                    writeCacheToDisk();
                }
            }
        } finally {
            if (dataManagementLock.isWriteLockedByCurrentThread()) {
                dataManagementLock.writeLock().unlock();
            }
        }

        if (somethingRemoved) {
            fireExclusionChange(serverInfo.getServerGUID());
        }
    }

    /**
     * Overwrites the existing default exclusions with the provided default
     * exclusion list.
     *
     * @param serverInfo
     *        the server these exclusions are from (must not be
     *        <code>null</code>)
     * @param exclusions
     *        the default exclusions (must not be <code>null</code>)
     */
    public void setDefaultExclusions(final InternalServerInfo serverInfo, final String[] exclusions) {
        final LocalItemExclusionSet exclusionSet = new LocalItemExclusionSet();
        exclusionSet.setExclusions(exclusions);

        setDefaultExclusions(serverInfo, exclusionSet);
    }

    /**
     * Overwrites the existing default exclusions with the provided default
     * exclusion list.
     *
     * @param serverInfo
     *        the server these exclusions are from (must not be
     *        <code>null</code>)
     * @param localItemExclusionSet
     *        The list of default exclusions (must not be <code>null</code>)
     */
    public void setDefaultExclusions(
        final InternalServerInfo serverInfo,
        final LocalItemExclusionSet localItemExclusionSet) {
        Check.notNull(serverInfo, "serverInfo"); //$NON-NLS-1$
        Check.notNull(localItemExclusionSet, "localItemExclusionSet"); //$NON-NLS-1$

        ensureDiskCacheLoaded();

        try {
            dataManagementLock.writeLock().lock();

            ExclusionSet exclusionSet = exclusions.get(serverInfo);
            if (exclusionSet != null) {
                exclusionSet.setDefaultExclusions(localItemExclusionSet);
            } else {
                exclusionSet = new ExclusionSet();
                exclusionSet.setDefaultExclusions(localItemExclusionSet);
                exclusions.put(serverInfo, exclusionSet);
            }

            writeCacheToDisk();
        } finally {
            if (dataManagementLock.isWriteLockedByCurrentThread()) {
                dataManagementLock.writeLock().unlock();
            }
        }

        fireExclusionChange(serverInfo.getServerGUID());
    }

    /**
     * Returns the set of all default and user exclusions.
     *
     * @param serverInfo
     * @return The set of all default and user exclusions.
     */
    public String[] getExclusions(final InternalServerInfo serverInfo) {
        Check.notNull(serverInfo, "serverInfo"); //$NON-NLS-1$

        ensureDiskCacheLoaded();
        boolean readLockHeld = false;

        try {
            dataManagementLock.readLock().lock();
            readLockHeld = true;

            ExclusionSet exclusionSet = exclusions.get(serverInfo);
            if (exclusionSet == null) {
                // They are requesting exclusions and we don't have an exclusion
                // set for this collection. Generate one so that we can return
                // the defaults.

                // Get a writer lock and make sure someone else hasn't already
                // done our work.
                dataManagementLock.readLock().unlock();
                readLockHeld = false;
                dataManagementLock.writeLock().lock();

                exclusionSet = exclusions.get(serverInfo);
                if (exclusionSet == null) {
                    exclusionSet = new ExclusionSet();
                    exclusions.put(serverInfo, exclusionSet);
                }
            }

            return exclusionSet.getExclusions();
        } finally {
            if (readLockHeld) {
                dataManagementLock.readLock().unlock();
            }

            if (dataManagementLock.isWriteLockedByCurrentThread()) {
                dataManagementLock.writeLock().unlock();
            }
        }
    }

    /**
     * Returns the last time we called the server to determine the default
     * exclusions.
     *
     *
     * @param serverInfo
     * @return
     */
    public Calendar getLastDefaultExclusionUpdate(final InternalServerInfo serverInfo) {
        Check.notNull(serverInfo, "serverInfo"); //$NON-NLS-1$

        ensureDiskCacheLoaded();

        try {
            dataManagementLock.readLock().lock();

            final ExclusionSet exclusionSet = exclusions.get(serverInfo);
            if (exclusionSet != null) {
                return exclusionSet.getLastDefaultExclusionUpdate();
            }

            // Nothing found, return that we have never done this.
            final Calendar ret = Calendar.getInstance(TimeZone.getTimeZone("UTC")); //$NON-NLS-1$
            ret.set(1, Calendar.JANUARY, 1, 0, 0, 0);
            ret.set(Calendar.MILLISECOND, 0);
            return ret;
        } finally {
            dataManagementLock.readLock().unlock();
        }
    }

    /**
     * Make sure our caches have been loaded.
     */
    private void ensureDiskCacheLoaded() {
        // If the cache directory is inaccessible or we are already loaded,
        // we're done.
        if (!cacheEnabled || cacheLoaded) {
            return;
        }

        reload();
    }

    /**
     * Reloads the local item exclusion cache from disk.
     */
    private void reload() {
        try {
            dataManagementLock.writeLock().lock();

            if (cacheLoaded) {
                return;
            }

            try {
                if (cacheFile.exists()) {
                    try {
                        final Document document = DOMCreateUtils.parseFile(cacheFile, DOMCreateUtils.ENCODING_UTF8);
                        if (document != null) {
                            load(document);
                        }
                    } catch (final XMLException xmlException) {
                        // Swallowing this exception is compatible with VS.
                        final String format = Messages.getString("LocalItemExclusionCache.InvalidCacheFileFormat"); //$NON-NLS-1$
                        final String message = MessageFormat.format(format, cacheFile);
                        log.warn(message, xmlException);
                    }
                }

                // Mark our cache as loaded.
                cacheLoaded = true;
            } catch (final Exception exception) {
                final String format = Messages.getString("LocalItemExclusionCache.InvalidCacheFileFormat"); //$NON-NLS-1$
                throw new VersionControlException(MessageFormat.format(format, cacheFile), exception);
            }
        } finally {
            if (dataManagementLock.isWriteLockedByCurrentThread()) {
                dataManagementLock.writeLock().unlock();
            }
        }
    }

    /**
     * Load all of the exclusion given the root exclusion node.
     *
     *
     * @param document
     *        The root exclusion node in the XmlDocument.
     */
    private void load(final Document document) {
        final Element rootElement = DOMUtils.getFirstChildElement(document, EXCLUSION_ROOT);
        Check.notNull(rootElement, "rootElement"); //$NON-NLS-1$

        final Element[] tpcElements = DOMUtils.getChildElements(rootElement, TEAM_PROJECT_COLLECTION);
        for (final Element tpcElement : tpcElements) {
            final String id = tpcElement.getAttribute(COLLECTION_ID);
            if (id != null) {
                final GUID collectionID = new GUID(id);
                final String url = tpcElement.getAttribute(COLLECTION_URL);
                final URI collectionURI = (url != null) ? URIUtils.newURI(url) : null;

                final InternalServerInfo info = new InternalServerInfo(collectionURI, collectionID);
                final ExclusionSet exclusionSet = new ExclusionSet(tpcElement);

                exclusions.put(info, exclusionSet);
            }
        }
    }

    private void writeCacheToDisk() {
        if (!cacheEnabled) {
            // Nothing to do.
            return;
        }

        try {
            // Load the existing cache on disk.
            LocalItemExclusionCache cacheOnDisk = null;

            if (cacheFile.exists() && cacheFile.isFile()) {
                try {
                    final Document document = DOMCreateUtils.parseFile(cacheFile, DOMCreateUtils.ENCODING_UTF8);
                    cacheOnDisk = new LocalItemExclusionCache(cacheFile, true);
                    cacheOnDisk.load(document);
                } catch (final Exception e) {
                    log.warn(
                        MessageFormat.format("Error reading exclusion file XML from {0}, overwriting", cacheFile), //$NON-NLS-1$
                        e);
                    cacheOnDisk = null;
                }
            }

            final Document document = DOMCreateUtils.newDocument(EXCLUSION_ROOT);

            // Save the cached exclusions data.
            save(cacheOnDisk, DOMUtils.getFirstChildElement(document, EXCLUSION_ROOT));

            DOMSerializeUtils.serializeToStream(
                document,
                new FileOutputStream(cacheFile),
                DOMSerializeUtils.ENCODING_UTF8,
                DOMSerializeUtils.INDENT);

        } catch (final Exception e) {
            log.warn(MessageFormat.format("Error writing exclusion file XML at {0}", cacheFile), e); //$NON-NLS-1$
        }

        // The file has been saved and is no longer dirty.
        // NOTE: This must be inside the lock to prevent the race condition
        // where the cache
        // gets marked clean right after another thread modifies it (lock must
        // be
        // taken here and where the cache is modified).
        markClean();
    }

    /**
     * Save the exclusion data under the specified node in the specified
     * document. We don't clear the dirty flag here because the caller could
     * fail to write the data to disk. The caller should clear the dirty flag
     * once the save has been successful. See Load for some sample XML.
     */
    private void save(final LocalItemExclusionCache cacheOnDisk, final Element rootElement) {
        // If there is an existing cache file, merge the current cache and the
        // cache file.
        if (cacheOnDisk != null) {
            merge(cacheOnDisk);
        }

        for (final InternalServerInfo key : exclusions.keySet()) {
            final ExclusionSet value = exclusions.get(key);

            final Element tpcElement = DOMUtils.appendChild(rootElement, TEAM_PROJECT_COLLECTION);
            tpcElement.setAttribute(COLLECTION_ID, key.getServerGUID().getGUIDString());
            tpcElement.setAttribute(COLLECTION_URL, key.getURI().toString());

            value.save(tpcElement);
        }
    }

    /**
     * Intelligently merges an existing cache file's contents (input) with the
     * current in-memory.
     *
     *
     * @param cacheOnDisk
     *        the contents of the cache file on disk
     * @throws XMLStreamException
     */
    private void merge(final LocalItemExclusionCache cacheOnDisk) {
        // Merge the two caches.
        for (final InternalServerInfo key : cacheOnDisk.exclusions.keySet()) {
            final ExclusionSet value = cacheOnDisk.exclusions.get(key);
            final ExclusionSet cachedExclusionSet = exclusions.get(key);

            if (cachedExclusionSet != null) {
                cachedExclusionSet.merge(value);
            } else {
                exclusions.put(key, value);
            }
        }
    }

    private void markClean() {
        for (final ExclusionSet exclusionSet : exclusions.values()) {
            exclusionSet.markClean();
        }
    }

    private void fireExclusionChange(final GUID collectionID) {
        // Fire the event
        ((LocalItemExclusionsUpdatedListener) listeners.getListener()).exclusionsUpdated(collectionID);
    }

    /**
     * A comparator for InternalServerInfo that only compares the GUIDs
     * contained within the server info.
     *
     * @threadsafety unknown
     */
    private class InternalServerInfoGUIDComparator implements Comparator<InternalServerInfo> {
        @Override
        public int compare(final InternalServerInfo o1, final InternalServerInfo o2) {
            return o1.getServerGUID().compareTo(o2.getServerGUID());
        }
    }
}