// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol;

import com.microsoft.tfs.util.BitField;

/**
 * Specifies which conflicts will be resolved as automerge.
 *
 * @threadsafety unknown
 */
public class AutoResolveOptions extends BitField {
    private static final long serialVersionUID = -8903968241030510630L;

    /**
     * Do nothing.
     */
    public final static AutoResolveOptions NONE = new AutoResolveOptions(0);

    /**
     * Only conflicts with ChangeSummary.TotalConflicting = 0 and TotalLatest =
     * 0 will be resolved.
     */
    public final static AutoResolveOptions ONLY_LOCAL_TARGET = new AutoResolveOptions(1);

    /**
     * Only conflicts with ChangeSummary.TotalConflicting = 0 and TotalModified
     * = 0 will be resolved.
     */
    public final static AutoResolveOptions ONLY_SERVER_SOURCE = new AutoResolveOptions(2);

    /**
     * All conflicts with ChangeSummary.TotalConflict = 0 will be resolved.
     */
    public final static AutoResolveOptions BOTH_LOCAL_TARGET_AND_SERVER_SOURCE = new AutoResolveOptions(4);

    /*
     * Flags below are not content related.
     */

    /**
     * Conflicts that include source/server rename, but not local/target rename
     * will be resolved.
     */
    public final static AutoResolveOptions INCOMING_RENAME = new AutoResolveOptions(32);

    /**
     * Behavior intended for Gated Checkin:
     *
     * - Only version Get/Checkin
     *
     * - If Your and Their change is identical, content, encoding and path are
     * identical resolves conflicts as TakeTheirs.
     *
     * Content is compared for Edit and Branch changes.
     */
    public final static AutoResolveOptions REDUNDANT = new AutoResolveOptions(64);

    /*
     * Flags below affect reporting.
     */

    /**
     * Conflicts that require name or encoding parameters are not reported to
     * the user.
     */
    public final static AutoResolveOptions SILENT = new AutoResolveOptions(1024);

    /*
     * Combined types.
     */

    /**
     * All conflicts will be resolved and conflict that require name or encoding
     * will be reported to the user.
     */
    public final static AutoResolveOptions ALL_CONTENT = new AutoResolveOptions(combine(new BitField[] {
        ONLY_LOCAL_TARGET,
        ONLY_SERVER_SOURCE,
        BOTH_LOCAL_TARGET_AND_SERVER_SOURCE
    }));

    /**
     * All content and redundant conflicts will be resolved.
     */
    public final static AutoResolveOptions ALL = new AutoResolveOptions(combine(new BitField[] {
        ONLY_LOCAL_TARGET,
        ONLY_SERVER_SOURCE,
        BOTH_LOCAL_TARGET_AND_SERVER_SOURCE,
        REDUNDANT
    }));

    /**
     * All content and redundant conflicts will be resolved. Conflicts that
     * require name or encoding will be ignored.
     */
    public final static AutoResolveOptions ALL_SILENT = new AutoResolveOptions(combine(new BitField[] {
        ALL,
        SILENT
    }));

    private AutoResolveOptions(final int value) {
        super(value);
    }

    public boolean contains(final AutoResolveOptions other) {
        return containsInternal(other);
    }
}
