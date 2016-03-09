// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.specs.version;

import com.microsoft.tfs.core.clients.versioncontrol.specs.LabelSpec;

import ms.tfs.versioncontrol.clientservices._03._LabelVersionSpec;

/**
 * Describes a label version.
 *
 * @threadsafety immutable
 * @since TEE-SDK-10.1
 */
public final class LabelVersionSpec extends VersionSpec {
    /**
     * The single character identifier for the type of VersionSpec implemented
     * by this class.
     */
    protected static final char IDENTIFIER = 'L';

    public LabelVersionSpec(final _LabelVersionSpec spec) {
        super(spec);
    }

    public LabelVersionSpec(final LabelSpec spec) {
        super(new _LabelVersionSpec(spec.getLabel(), spec.getScope()));
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        // Returns something like "Labc@def".
        return IDENTIFIER
            + new LabelSpec(
                ((_LabelVersionSpec) getWebServiceObject()).getLabel(),
                ((_LabelVersionSpec) getWebServiceObject()).getScope()).toString();
    }

    /**
     * @return the label name.
     */
    public String getLabel() {
        return ((_LabelVersionSpec) getWebServiceObject()).getLabel();
    }

    /**
     * @return the scope for this label spec, null for the default scope.
     */
    public String getScope() {
        return ((_LabelVersionSpec) getWebServiceObject()).getScope();
    }
}
