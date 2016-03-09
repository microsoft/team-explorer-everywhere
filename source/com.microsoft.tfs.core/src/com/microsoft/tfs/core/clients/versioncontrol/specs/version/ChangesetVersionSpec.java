// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.specs.version;

import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Changeset;

import ms.tfs.versioncontrol.clientservices._03._ChangesetVersionSpec;

/**
 * Describes a {@link Changeset} version.
 *
 * @threadsafety immutable
 * @since TEE-SDK-10.1
 */
public final class ChangesetVersionSpec extends VersionSpec {
    /**
     * The single character identifier for the type of VersionSpec implemented
     * by this class.
     */
    protected static final char IDENTIFIER = 'C';

    public ChangesetVersionSpec(final int changeSetNumber) {
        super(new _ChangesetVersionSpec(changeSetNumber));
    }

    public ChangesetVersionSpec(final _ChangesetVersionSpec spec) {
        super(spec);
    }

    /*
     * (non-Javadoc)
     *
     * @see com.microsoft.tfs.core.soapextensions.IVersionSpec#toString()
     */
    @Override
    public String toString() {
        // Returns something like "C2984".
        return IDENTIFIER + new Integer(getChangeset()).toString();
    }

    public int getChangeset() {
        return ((_ChangesetVersionSpec) getWebServiceObject()).getCs();
    }
}
