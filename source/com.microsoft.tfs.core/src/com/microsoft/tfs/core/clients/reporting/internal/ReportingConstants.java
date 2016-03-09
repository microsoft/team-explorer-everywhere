// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.reporting.internal;

public class ReportingConstants {
    public static final String FOLDER_SEPARATOR_CHAR = "/"; //$NON-NLS-1$
    public static final String ROOT_PATH = "/"; //$NON-NLS-1$
    public static final String DEFAULT_ROOT_ITEM_PATH = "/TfsReports"; //$NON-NLS-1$
    public static final String WEB_SERVICE = "ReportService2005.asmx"; //$NON-NLS-1$
    public static final String OBSOLETE_WEB_SERVICE = "ReportService.asmx"; //$NON-NLS-1$

    public static final String REGISTRY_WAREHOUSE_CONNECTION_STRING =
        "/Configuration/Database/Warehouse/ConnectionString"; //$NON-NLS-1$

    public static final String REGISTRY_ANALYSIS_CONNECTION_STRING = "/Configuration/Analysis/Cube/ConnectionString"; //$NON-NLS-1$

    public static final String[] KNOWN_WEB_SERVICE_PATHS = new String[] {
        WEB_SERVICE,
        OBSOLETE_WEB_SERVICE,
    };
}
