// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.util;

import java.text.MessageFormat;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.microsoft.tfs.core.memento.Memento;
import com.microsoft.tfs.core.persistence.LockMode;
import com.microsoft.tfs.core.persistence.PersistenceStore;
import com.microsoft.tfs.core.util.internal.MementoRepositorySerializer;
import com.microsoft.tfs.util.Check;

/**
 * <p>
 * Provides persistent storage of {@link Memento}s, identified by string keys,
 * in a {@link PersistenceStore}.
 * </p>
 * <p>
 * This class is convenient for sharing settings between multiple users of
 * com.microsoft.tfs.core that may not otherwise share any standard storage (for
 * example, Eclipse plug-in and Explorer want to share the user's custom merge
 * tool assignments). To share settings, use the same {@link PersistenceStore},
 * and specify the same key from both clients. Client applications are not
 * required to use this class to store settings, they may use a
 * {@link PersistenceStore} directly, or store settings in another place
 * (Eclipse workspace, etc.).
 * </p>
 * <p>
 * Depending on the kind of {@link PersistenceStore} supplied, key names may be
 * restricted to prohibit some characters, or have a maximum length, or have
 * other restrictions. See {@link PersistenceStore} for details.
 * </p>
 * <p>
 * Locking is used during {@link #save(String, Memento)} and
 * {@link #load(String)} to prevent concurrent reads and writes from other
 * threads as well as other processes on the same computer.
 * </p>
 *
 * @since TEE-SDK-10.1
 * @threadsafety thread-safe
 */
public final class MementoRepository {
    private static final Log log = LogFactory.getLog(MementoRepository.class);

    private static final String CHILD_STORE_NAME = "TEE-Mementos"; //$NON-NLS-1$
    private static final String MEMENTO_VALUE_FILE_EXTENSION = ".xml"; //$NON-NLS-1$

    private final PersistenceStore store;

    /**
     * Creates a {@link MementoRepository} that stores its data inside a
     * {@value #CHILD_STORE_NAME} child store inside the given
     * {@link PersistenceStore}.
     *
     * @param store
     *        the {@link PersistenceStore} to use (must not be <code>null</code>
     *        )
     */
    public MementoRepository(final PersistenceStore store) {
        Check.notNull(store, "store"); //$NON-NLS-1$

        this.store = store.getChildStore(CHILD_STORE_NAME);
    }

    /**
     * Loads the settings {@link Memento} for the given key. If the settings
     * file for the given key is in use by another process, waits (potentially
     * forever) until the file is available.
     *
     * @param key
     *        the setting key, which must be a valid {@link PersistenceStore}
     *        item name (must not be <code>null</code> or empty)
     * @return the {@link Memento} for the given key, or null if no settings
     *         exist for the given key or an error happened reading the settings
     *         (check the logs for an error message)
     */
    public Memento load(final String key) {
        Check.notNullOrEmpty(key, "key"); //$NON-NLS-1$

        try {
            return (Memento) store.retrieveItem(
                key + MEMENTO_VALUE_FILE_EXTENSION,
                LockMode.WAIT_FOREVER,
                null,
                new MementoRepositorySerializer());
        } catch (final Exception e) {
            log.warn(MessageFormat.format("unable to load settings for key '{0}'", key), e); //$NON-NLS-1$
            return null;
        }
    }

    /**
     * Saves the given {@link Memento} for the given key. Overwrites any data
     * already associated with the key.
     *
     * @param key
     *        the setting key, which must be a valid {@link PersistenceStore}
     *        item name (must not be <code>null</code> or empty)
     * @param memento
     *        the {@link Memento} to save (must not be <code>null</code>)
     *
     * @return true if the save succeeded, false if it failed (check the logs
     *         for an error message)
     */
    public boolean save(final String key, final Memento memento) {
        Check.notNullOrEmpty(key, "key"); //$NON-NLS-1$
        Check.notNull(memento, "memento"); //$NON-NLS-1$

        try {
            store.storeItem(
                key + MEMENTO_VALUE_FILE_EXTENSION,
                memento,
                LockMode.WAIT_FOREVER,
                null,
                new MementoRepositorySerializer());
        } catch (final Exception e) {
            log.error(MessageFormat.format("unable to save settings for key '{0}'", key), e); //$NON-NLS-1$
            return false;
        }

        return true;
    }
}
