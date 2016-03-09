// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.reporting;

/**
 * Constants defining known node types for reports service.
 *
 * @since TEE-SDK-10.1
 * @threadsafety immutable
 */
public interface ReportNodeType {
    public static final String UNKNOWN = "Unknown"; //$NON-NLS-1$
    public static final String FOLDER = "Folder"; //$NON-NLS-1$
    public static final String REPORT = "Report"; //$NON-NLS-1$
}
