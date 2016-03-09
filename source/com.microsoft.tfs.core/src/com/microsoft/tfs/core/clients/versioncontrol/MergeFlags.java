// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol;

import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.internal.MergeOptions;
import com.microsoft.tfs.util.BitField;

/**
 * {@link MergeFlags} is used to control the options of a merge operation. These
 * flags were introduced in 2010, superseding {@link MergeOptions} in this API.
 *
 * @since TEE-SDK-11.0
 */
public class MergeFlags extends BitField {
    public static final MergeFlags NONE = new MergeFlags(0, "None"); //$NON-NLS-1$

    /**
     * Instructs the server to re-merge source versions which have been
     * previously merged
     */
    public static final MergeFlags FORCE_MERGE = new MergeFlags(1, "ForceMerge"); //$NON-NLS-1$

    /**
     * Instructs the server to perform a baseless merge between items
     */
    public static final MergeFlags BASELESS = new MergeFlags(2, "Baseless"); //$NON-NLS-1$

    // No item for value 4

    // No item for value 8

    /**
     * Instructs the server to not actually pend the merge, but just return a
     * preview of what needs to be merged
     */
    public static final MergeFlags NO_MERGE = new MergeFlags(16, "NoMerge"); //$NON-NLS-1$

    /**
     * Instructs the server to discard any changes from the source, keeping only
     * changes from the target.
     */
    public static final MergeFlags ALWAYS_ACCEPT_MINE = new MergeFlags(32, "AlwaysAcceptMine"); //$NON-NLS-1$

    /**
     * Instructs the server to supress any get operations and conflicts.
     *
     * @since TFS 2008
     */
    public static final MergeFlags SILENT = new MergeFlags(64, "Silent"); //$NON-NLS-1$

    /**
     * Only works prior on TFS 2008, instructs the server to not implicitly
     * baseless merge items with the same relative path from the source root and
     * target root. e.g. $/proj/trunk -> $/proj/branch If a new file is added at
     * $/proj/trunk/a.cs and $/proj/branch/a.cs Setting this option will tell
     * the server to not try and merge the 2 files
     *
     * @since TFS 2008
     */
    public static final MergeFlags NO_IMPLICIT_BASELESS = new MergeFlags(128, "NoImplicitBaseless"); //$NON-NLS-1$

    /**
     * Instructs the server to be conservative while declaring merges as
     * conflicts. Available from TFS 2010.
     */
    public static final MergeFlags CONSERVATIVE = new MergeFlags(256, "Conservative"); //$NON-NLS-1$

    /**
     * Used only on the client. Instructs the client not to try to auto resolve
     * conflicts.
     */
    public static final MergeFlags NO_AUTO_RESOLVE = new MergeFlags(512, "NoAutoResolve"); //$NON-NLS-1$

    public static MergeFlags combine(final MergeFlags[] values) {
        return new MergeFlags(BitField.combine(values));
    }

    private MergeFlags(final int flags, final String name) {
        super(flags);
        registerStringValue(getClass(), flags, name);
    }

    private MergeFlags(final int flags) {
        super(flags);
    }

    /**
     * Converts this {@link MergeFlags} to the legacy (TFS 2008 and previous)
     * {@link MergeOptions} type for use with the older web service.
     *
     * @return a {@link MergeOptions} with the options set which correspond to
     *         supported {@link MergeFlags} set in this object
     */
    public MergeOptions toMergeOptions() {
        final MergeOptions options = new MergeOptions();

        if (contains(MergeFlags.ALWAYS_ACCEPT_MINE)) {
            options.add(MergeOptions.ALWAYS_ACCEPT_MINE);
        }

        if (contains(MergeFlags.BASELESS)) {
            options.add(MergeOptions.BASELESS);
        }

        if (contains(MergeFlags.FORCE_MERGE)) {
            options.add(MergeOptions.FORCE_MERGE);
        }

        if (contains(MergeFlags.NO_MERGE)) {
            options.add(MergeOptions.NO_MERGE);
        }

        if (options.size() == 0) {
            return new MergeOptions(MergeOptions.NONE);
        }

        return options;
    }

    public boolean containsAll(final MergeFlags other) {
        return containsAllInternal(other);
    }

    public boolean contains(final MergeFlags other) {
        return containsInternal(other);
    }

    public boolean containsAny(final MergeFlags other) {
        return containsAnyInternal(other);
    }

    public MergeFlags remove(final MergeFlags other) {
        return new MergeFlags(removeInternal(other));
    }

    public MergeFlags retain(final MergeFlags other) {
        return new MergeFlags(retainInternal(other));
    }

    public MergeFlags combine(final MergeFlags other) {
        return new MergeFlags(combineInternal(other));
    }

}
