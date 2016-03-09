// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.webservices;

import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;

/**
 * Contains string constants for identity attributes.
 *
 * @threadsafety immutable
 * @since TEE-SDK-10.1
 */
public final class IdentityAttributeTags {
    public static final String DOMAIN = "Domain"; //$NON-NLS-1$
    public static final String ACCOUNT_NAME = "Account"; //$NON-NLS-1$
    public static final String DESCRIPTION = "Description"; //$NON-NLS-1$
    public static final String MAIL_ADDRESS = "Mail"; //$NON-NLS-1$
    public static final String DISTINGUISHED_NAME = "DN"; //$NON-NLS-1$
    public static final String SECURITY_GROUP = "SecurityGroup"; //$NON-NLS-1$
    public static final String SPECIAL_TYPE = "SpecialType"; //$NON-NLS-1$
    public static final String RESTRICTED_VISIBLE = "RestrictedVisible"; //$NON-NLS-1$
    public static final String DISAMBIGUATION = "Disambiguation"; //$NON-NLS-1$
    public static final String SCOPE_NAME = "ScopeName"; //$NON-NLS-1$
    public static final String GLOBAL_SCOPE = "GlobalScope"; //$NON-NLS-1$
    public static final String CROSS_PROJECT = "CrossProject"; //$NON-NLS-1$
    public static final String SCHEMA_CLASS_NAME = "SchemaClassName"; //$NON-NLS-1$

    public static final Set<String> READ_ONLY_PROPERTIES = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);

    static {
        READ_ONLY_PROPERTIES.addAll(Arrays.asList(new String[] {
            ACCOUNT_NAME,
            CROSS_PROJECT,
            DESCRIPTION,
            DISAMBIGUATION,
            DISTINGUISHED_NAME,
            DOMAIN,
            GLOBAL_SCOPE,
            MAIL_ADDRESS,
            RESTRICTED_VISIBLE,
            SCHEMA_CLASS_NAME,
            SCOPE_NAME,
            SECURITY_GROUP,
            SPECIAL_TYPE
        }));
    }
}
