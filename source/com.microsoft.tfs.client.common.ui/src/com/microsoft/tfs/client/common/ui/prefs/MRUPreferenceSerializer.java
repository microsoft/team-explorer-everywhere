// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.prefs;

import java.util.Iterator;

import org.eclipse.jface.preference.IPreferenceStore;

import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.MRUSet;

/**
 * <p>
 * Reads/writes most-recently-used data sets from/to Eclipse preference stores.
 * </p>
 *
 * @threadsafety thread-compatible
 */
public class MRUPreferenceSerializer {
    private final IPreferenceStore preferenceStore;

    /**
     * Constructs an {@link MRUPreferenceSerializer} which reads from and writes
     * to the specified {@link IPreferenceStore}.
     *
     * @param preferenceStore
     *        the {@link IPreferenceStore} to read from and write to (must not
     *        be <code>null</code>)
     */
    public MRUPreferenceSerializer(final IPreferenceStore preferenceStore) {
        Check.notNull(preferenceStore, "preferenceStore"); //$NON-NLS-1$

        this.preferenceStore = preferenceStore;
    }

    /**
     * Writes the {@link MRUSet} to the preferences. Because
     * {@link IPreferenceStore}'s key namespace is flat, this method requires a
     * prefix which is appended to the element indexes in the {@link MRUSet} to
     * create the actual preference keys.
     *
     * @param set
     *        the set to write to the preferences (must not be <code>null</code>
     *        )
     * @param keyPrefix
     *        the string prepended to the indexes of the {@link MRUSet} to form
     *        the preference storage keys (must not be <code>null</code> or
     *        empty)
     */
    public void write(final MRUSet set, final String keyPrefix) {
        Check.notNull(set, "set"); //$NON-NLS-1$
        Check.notNullOrEmpty(keyPrefix, "keyPrefix"); //$NON-NLS-1$

        int i = 0;
        final Iterator<String> iterator = set.iterator();
        while (iterator.hasNext()) {
            preferenceStore.setValue(makeKey(keyPrefix, i++), iterator.next());
        }

    }

    /**
     * Reads preference values which start with the given key prefix into an
     * {@link MRUSet}. Use the same key prefix as passed to
     * {@link #write(MRUSet, String)} to retrive those saved values.
     *
     *
     * @param maxSize
     *        the maximum number of items to read (must be > 0)
     * @param keyPrefix
     *        the preference key prefix to read (must not be <code>null</code>
     *        or empty)
     * @return the set of the preferences read
     */
    public MRUSet read(final int maxSize, final String keyPrefix) {
        Check.isTrue(maxSize > 0, "maxSize > 0"); //$NON-NLS-1$
        Check.notNullOrEmpty(keyPrefix, "keyPrefix"); //$NON-NLS-1$

        final MRUSet set = new MRUSet(maxSize);

        for (int i = 0; i < maxSize; i++) {
            final String key = makeKey(keyPrefix, i);

            if (preferenceStore.contains(key)) {
                final String value = preferenceStore.getString(makeKey(keyPrefix, i));
                if (value != null) {
                    set.add(value);
                }
            }
        }

        return set;
    }

    /**
     * Resets preference values which start with the given key prefix into an
     *
     * @param maxSize
     *        the maximum number of items to reset (must be > 0)
     *
     * @param keyPrefix
     *        the preference key prefix to reset (must not be <code>null</code>
     *        or empty)
     */
    public void reset(final int maxSize, final String keyPrefix) {
        Check.notNullOrEmpty(keyPrefix, "keyPrefix"); //$NON-NLS-1$
        for (int i = 0; i < maxSize; i++) {
            final String key = makeKey(keyPrefix, i);
            if (preferenceStore.contains(key)) {
                preferenceStore.setToDefault(key);
            }
        }
    }

    private String makeKey(final String keyPrefix, final int index) {
        return keyPrefix + "." + index; //$NON-NLS-1$
    }
}
