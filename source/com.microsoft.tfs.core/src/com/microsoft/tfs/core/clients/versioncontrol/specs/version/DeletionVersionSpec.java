// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.specs.version;

import java.text.MessageFormat;

import com.microsoft.tfs.core.clients.versioncontrol.exceptions.VersionControlException;

import ms.tfs.versioncontrol.clientservices._03._VersionSpec;

/**
 * Represents a deletion specification. This class does not extend a web service
 * object, because the web service uses simple integers for deletion specs.
 *
 *
 * @threadsafety immutable
 * @since TEE-SDK-10.1
 */
public final class DeletionVersionSpec extends VersionSpec {
    /**
     * The single character identifier for the type of spec implemented by this
     * class.
     */
    protected static final char IDENTIFIER = 'X';

    private final int deletionID;

    public DeletionVersionSpec(final int deletionID) {
        super(new Object());
        this.deletionID = deletionID;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.microsoft.tfs.core.vc.specs.version.AVersionSpec#toString()
     */
    @Override
    public String toString() {
        // Returns something like "X3".
        return IDENTIFIER + new Integer(deletionID).toString();
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(final Object o) {
        if (o == this) {
            return true;
        }
        if (o == null) {
            return false;
        }
        if (o instanceof DeletionVersionSpec == false) {
            return false;
        }

        return ((DeletionVersionSpec) o).deletionID == deletionID;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return (23 * 37) + deletionID;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.microsoft.tfs.core.vc.specs.version.AVersionSpec#getWebServiceObject
     * ()
     */
    @Override
    public _VersionSpec getWebServiceObject() {
        throw new VersionControlException(
            MessageFormat.format(
                "{0} does not support returning a web service compatible VersionSpec, because the web service does not define one.", //$NON-NLS-1$
                DeletionVersionSpec.class.getName()));
    }

    /**
     * @return the deletion ID, 0 if none.
     */
    public int getDeletionID() {
        return deletionID;
    }
}
