// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.soapextensions;

import com.microsoft.tfs.util.TypesafeEnum;

/**
 * An enumeration of types of working folder comparisons available in
 * {@link WorkingFolderComparator}.
 *
 * @threadsafety immutable
 * @since TEE-SDK-10.1
 */
public class WorkingFolderComparatorType extends TypesafeEnum {
    private WorkingFolderComparatorType(final int value) {
        super(value);
    }

    /**
     * Compares working folders by server path, in the normal way ("$/ABC" sorts
     * before "$/DEF").
     */
    public final static WorkingFolderComparatorType SERVER_PATH = new WorkingFolderComparatorType(0);

    /**
     * Compares working folders by server path, in reverse ("$/DEF" sorts before
     * "$/ABC").
     */
    public final static WorkingFolderComparatorType SERVER_PATH_REVERSE = new WorkingFolderComparatorType(1);

    /**
     * Compares working folders by local path, in the normal way. Working
     * folders with a null local path (a cloaked folder) will sort before those
     * with a non-null local path.
     */
    public final static WorkingFolderComparatorType LOCAL_PATH = new WorkingFolderComparatorType(2);
}
