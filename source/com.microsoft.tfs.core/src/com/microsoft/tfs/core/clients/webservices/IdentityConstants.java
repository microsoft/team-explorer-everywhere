// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.webservices;

/**
 * Contains identity constants.
 *
 * @since TEE-SDK-10.1
 */
public class IdentityConstants {
    public static final String WINDOWS_TYPE = "System.Security.Principal.WindowsIdentity"; //$NON-NLS-1$
    public static final String TEAM_FOUNDATION_TYPE = "Microsoft.TeamFoundation.Identity"; //$NON-NLS-1$

    public static final int MAX_ID_LENGTH = 256;
    public static final int MAX_TYPE_LENGTH = 64;

    public static final int ACTIVE_UNIQUE_ID = 0;

    public static final String SCHEMA_CLASS_GROUP = "Group"; //$NON-NLS-1$
    public static final String SCHEMA_CLASS_USER = "User"; //$NON-NLS-1$
}
