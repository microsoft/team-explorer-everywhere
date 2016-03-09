// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.framework.catalog;

/**
 * Constants used by the TFS {@link CatalogService}. Use these constants with
 * certain methods when calling the {@link CatalogService} API.
 *
 * @threadsafety thread-safe
 * @since TEE-SDK-10.1
 */
public class CatalogConstants {
    public static final int MANDATORY_NODE_PATH_LENGTH = 24;
    public static final int MAXIMUM_PATH_LENGTH = 888; // 24 * 37 -- As deep as
                                                       // we can go because of
                                                       // 900 byte limit for
                                                       // primary keys

    public static final String FULL_RECURSE_STARS = "**"; //$NON-NLS-1$
    public static final String FULL_RECURSE_DOTS = "..."; //$NON-NLS-1$
    public static final String SINGLE_RECURSE_STAR = "*"; //$NON-NLS-1$
    public static final char[] PATTERN_MATCHING_CHARACTERS = new char[] {
        '*',
        '.'
    };
}
