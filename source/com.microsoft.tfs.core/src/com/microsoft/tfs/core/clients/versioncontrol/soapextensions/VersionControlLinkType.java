// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.soapextensions;

import com.microsoft.tfs.util.TypesafeEnum;

/**
 * Represents a type of {@link VersionControlLink}.
 *
 * @since TEE-SDK-10.1
 */
public class VersionControlLinkType extends TypesafeEnum {
    private VersionControlLinkType(final int value) {
        super(value);
    }

    /**
     * An invalid link.
     */
    public static final VersionControlLinkType INVALID = new VersionControlLinkType(0);

    /**
     * A link to a work item that was resolved.
     */
    public static final VersionControlLinkType RESOLVE = new VersionControlLinkType(1025);

    /**
     * A link that only associates the change with a work item.
     */
    public static final VersionControlLinkType ASSOCIATE = new VersionControlLinkType(1026);
}
