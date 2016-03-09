// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol;

import com.microsoft.tfs.util.BitField;

public class SourceControlCapabilityFlags extends BitField {
    /**
     *
     */
    private static final long serialVersionUID = -8630576041295138762L;

    public static final SourceControlCapabilityFlags NONE = new SourceControlCapabilityFlags(0);

    public static final SourceControlCapabilityFlags TFS = new SourceControlCapabilityFlags(1);
    public static final SourceControlCapabilityFlags GIT = new SourceControlCapabilityFlags(2);
    public static final SourceControlCapabilityFlags GIT_TFS = GIT.combine(TFS);

    private SourceControlCapabilityFlags(final int flags) {
        super(flags);
    }

    public static SourceControlCapabilityFlags parse(final String flags) {
        try {
            return new SourceControlCapabilityFlags(Integer.parseInt(flags));
        } catch (final Exception e) {
            // pre-Git version of TFS
            return TFS;
        }
    }

    public boolean contains(final SourceControlCapabilityFlags other) {
        return containsInternal(other);
    }

    public boolean containsAny(final SourceControlCapabilityFlags other) {
        return containsAnyInternal(other);
    }

    public SourceControlCapabilityFlags combine(final SourceControlCapabilityFlags other) {
        return new SourceControlCapabilityFlags(combineInternal(other));
    }

    public SourceControlCapabilityFlags remove(final SourceControlCapabilityFlags other) {
        return new SourceControlCapabilityFlags(removeInternal(other));
    }

    public SourceControlCapabilityFlags retain(final SourceControlCapabilityFlags other) {
        return new SourceControlCapabilityFlags(retainInternal(other));
    }
}
