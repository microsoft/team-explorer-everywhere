// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.build.flags;

import java.text.MessageFormat;

/**
 * The version of the build server.
 *
 * @since TEE-SDK-10.1
 */
public class BuildServerVersion implements Comparable {
    /**
     * TFS 2005 (Codenamed Whidbey)
     */
    public static final BuildServerVersion V1 = new BuildServerVersion(1);

    /**
     * TFS 2008 (Codenamed Orcas)
     */
    public static final BuildServerVersion V2 = new BuildServerVersion(2);

    /**
     * TFS 2010 (Codenamed Rosario/Dev10)
     */
    public static final BuildServerVersion V3 = new BuildServerVersion(3);

    /**
     * TFS 2012 (Codenamed Dev11)
     */
    public static final BuildServerVersion V4 = new BuildServerVersion(4);

    private final int version;

    private BuildServerVersion(final int version) {
        this.version = version;
    }

    @Override
    public int compareTo(final Object compareTo) {
        if (compareTo == null || !(compareTo instanceof BuildServerVersion)) {
            throw new ClassCastException("Instance of BuildServerVersion expected."); //$NON-NLS-1$
        }
        final int otherVersion = ((BuildServerVersion) compareTo).getVersion();
        return getVersion() - otherVersion;
    }

    /**
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return version;
    }

    @Override
    public boolean equals(final Object compareTo) {
        if (compareTo == null || !(compareTo instanceof BuildServerVersion)) {
            return false;
        }

        return getVersion() == ((BuildServerVersion) compareTo).getVersion();
    }

    public int getVersion() {
        return version;
    }

    public boolean isV1() {
        return version <= 1;
    }

    public boolean isV2() {
        return version == 2;
    }

    public boolean isV3() {
        return version == 3;
    }

    public boolean isV4() {
        return version == 4;
    }

    public boolean isLessThanV2() {
        return version < 2;
    }

    public boolean isLessThanV3() {
        return version < 3;
    }

    public boolean isLessThanV4() {
        return version < 4;
    }

    public boolean isV3OrGreater() {
        return version >= 3;
    }

    public boolean isV4OrGreater() {
        return version >= 4;
    }

    @Override
    public String toString() {
        return MessageFormat.format("Build Server Version: {0}", Integer.toString(version)); //$NON-NLS-1$
    }

}
