// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.webservices;

/**
 * Contains the security identifiers for well-known groups.
 *
 * @since TEE-SDK-10.1
 */
public class GroupWellKnownSIDConstants {
    public static final String EVERYONE_GROUP_SID_SUFFIX = "0-0-0-3"; //$NON-NLS-1$
    public static final String EVERYONE_GROUP_SID =
        (SIDIdentityHelper.WELL_KNOWN_SID_PREFIX + EVERYONE_GROUP_SID_SUFFIX);

    public static final String LICENSEES_GROUP_SID_SUFFIX = "0-0-0-4"; //$NON-NLS-1$
    public static final String LICENSEES_GROUP_SID =
        (SIDIdentityHelper.WELL_KNOWN_SID_PREFIX + LICENSEES_GROUP_SID_SUFFIX);

    public static final String NAMESPACE_ADMINISTRATORS_GROUP_SID_SUFFIX = "0-0-0-1"; //$NON-NLS-1$
    public static final String NAMESPACE_ADMINISTRATORS_GROUP_SID =
        (SIDIdentityHelper.WELL_KNOWN_SID_PREFIX + NAMESPACE_ADMINISTRATORS_GROUP_SID_SUFFIX);

    public static final String SERVICE_USERS_GROUP_SID_SUFFIX = "0-0-0-2"; //$NON-NLS-1$
    public static final String SERVICE_USERS_GROUP_SID =
        (SIDIdentityHelper.WELL_KNOWN_SID_PREFIX + SERVICE_USERS_GROUP_SID_SUFFIX);
}
