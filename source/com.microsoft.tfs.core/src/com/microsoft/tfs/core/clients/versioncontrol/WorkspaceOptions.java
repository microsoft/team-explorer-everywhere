// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol;

import com.microsoft.tfs.util.BitField;

/**
 * Options which control workspace options.
 *
 * @since TEE-SDK-11.0
 */
public class WorkspaceOptions extends BitField {
    private static final long serialVersionUID = 801310772713923037L;

    /**
     * @since TFS 2012
     */
    public static final WorkspaceOptions NONE = new WorkspaceOptions(0, "None"); //$NON-NLS-1$

    /**
     * Sets the last modified time for newly downloaded files to checkin time.
     *
     * @since TFS 2012
     */
    public static final WorkspaceOptions SET_FILE_TO_CHECKIN = new WorkspaceOptions(1, "SetFileToCheckin"); //$NON-NLS-1$

    private WorkspaceOptions(final int flags, final String name) {
        super(flags);
        registerStringValue(getClass(), flags, name);
    }

    private WorkspaceOptions(final int flags) {
        super(flags);
    }

    public static WorkspaceOptions fromFlags(final int flags) {
        return new WorkspaceOptions(flags);
    }

    public boolean containsAll(final WorkspaceOptions other) {
        return containsAllInternal(other);
    }

    public boolean contains(final WorkspaceOptions other) {
        return containsInternal(other);
    }

    public boolean containsAny(final WorkspaceOptions other) {
        return containsAnyInternal(other);
    }

    public WorkspaceOptions remove(final WorkspaceOptions other) {
        return new WorkspaceOptions(removeInternal(other));
    }

    public WorkspaceOptions retain(final WorkspaceOptions other) {
        return new WorkspaceOptions(retainInternal(other));
    }

    public WorkspaceOptions combine(final WorkspaceOptions other) {
        return new WorkspaceOptions(combineInternal(other));
    }
}
