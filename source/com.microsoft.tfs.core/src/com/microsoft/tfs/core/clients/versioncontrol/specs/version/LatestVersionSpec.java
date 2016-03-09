// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.specs.version;

import ms.tfs.versioncontrol.clientservices._03._LatestVersionSpec;

/**
 * Describes the latest version of an object. This class is a singleton: use the
 * {@link #INSTANCE} field.
 *
 * @threadsafety immutable
 * @since TEE-SDK-10.1
 */
public final class LatestVersionSpec extends VersionSpec {
    /**
     * The single character identifier for the type of VersionSpec implemented
     * by this class.
     *
     * T for "tip"!
     */
    protected static final char IDENTIFIER = 'T';

    /**
     * The single instance of this class.
     */
    public static final LatestVersionSpec INSTANCE = new LatestVersionSpec();

    private LatestVersionSpec() {
        super(new _LatestVersionSpec());
    }

    /*
     * (non-Javadoc)
     *
     * @see com.microsoft.tfs.core.soapextensions.IVersionSpec#toString()
     */
    public String versionString() {
        // Latest version only needs an identifier, no further
        // qualification is neccessary.
        return "" + IDENTIFIER; //$NON-NLS-1$
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        // Returns something like "T"
        return "" + IDENTIFIER; //$NON-NLS-1$
    }

}
