// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.webservices.internal;

import com.microsoft.tfs.util.TypesafeEnum;

/**
 * Describes identity management features supported by a client or server.
 *
 * @threadsafety immutable
 */
public class TeamFoundationSupportedFeatures extends TypesafeEnum {
    /**
     * None of the features in this enum are supported.
     */
    public static final TeamFoundationSupportedFeatures NONE = new TeamFoundationSupportedFeatures(0);

    /**
     * The client supports strongly-typed identity properties.
     */
    public static final TeamFoundationSupportedFeatures IDENTITY_PROPERTIES = new TeamFoundationSupportedFeatures(1);

    /**
     * This is a combination of all the features which are supported. Subject to
     * change across releases. You can send this value (from client object model
     * to server, or from server to client object model) and mask with it, but
     * you should not test for equality against it.
     */
    public static final TeamFoundationSupportedFeatures ALL =
        new TeamFoundationSupportedFeatures(IDENTITY_PROPERTIES.getValue());

    protected TeamFoundationSupportedFeatures(final int value) {
        super(value);
    }
}