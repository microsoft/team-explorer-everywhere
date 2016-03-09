// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.soapextensions;

import java.util.Comparator;

/**
 * {@link Comparator} for {@link Shelveset}s. Compares by name, then owner.
 *
 * @threadsafety immutable
 * @since TEE-SDK-10.1
 */
public final class ShelvesetComparator implements Comparator {
    public static final ShelvesetComparator INSTANCE = new ShelvesetComparator();

    private ShelvesetComparator() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int compare(final Object firstObject, final Object secondObject) {
        final Shelveset first = (Shelveset) firstObject;
        final Shelveset second = (Shelveset) secondObject;

        final int ret = first.getName().compareToIgnoreCase(second.getName());
        if (ret != 0) {
            return ret;
        }

        return first.getOwnerName().compareToIgnoreCase(second.getOwnerName());
    }
}
