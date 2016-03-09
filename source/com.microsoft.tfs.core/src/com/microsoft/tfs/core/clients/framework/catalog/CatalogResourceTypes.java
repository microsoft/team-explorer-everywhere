// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.framework.catalog;

import com.microsoft.tfs.util.GUID;

/**
 * Constants used when querying the catalog service.
 *
 * @threadsafety threadsafe
 * @since TEE-SDK-10.1
 */
public class CatalogResourceTypes {

    public static final GUID ORGANIZATIONAL_ROOT = new GUID("69A51C5E-C093-447e-A177-A09E47A60974"); //$NON-NLS-1$
    public static final GUID INFRASTRUCTURE_ROOT = new GUID("14F04669-6779-42d5-8975-184B93650C83"); //$NON-NLS-1$
    public static final GUID TEAM_FOUNDATION_SERVER_INSTANCE = new GUID("B36F1BDA-DF2D-482b-993A-F194A31A1FA2"); //$NON-NLS-1$
    public static final GUID PROJECT_COLLECTION = new GUID("26338D9E-D437-44aa-91F2-55880A328B54"); //$NON-NLS-1$
    public static final GUID TEAM_PROJECT = new GUID("48577A4A-801E-412c-B8AE-CF7EF3529616"); //$NON-NLS-1$
    public static final GUID RESOURCE_FOLDER = new GUID("55F97194-EC42-4dfc-B596-7DECC43CDE1E"); //$NON-NLS-1$
    public static final GUID GENERIC_LINK = new GUID("53D857F7-0197-45fa-BB58-EDF76AD7BFB2"); //$NON-NLS-1$
    public static final GUID MACHINE = new GUID("0584A4A2-475B-460e-A7AC-10C28951518F"); //$NON-NLS-1$
    public static final GUID TEAM_FOUNDATION_WEB_APPLICATION = new GUID("FFAF34BB-ADED-4507-9E52-FCA85E91BA63"); //$NON-NLS-1$
    public static final GUID SQL_DATABASE_INSTANCE = new GUID("EB1E0B3B-FAA1-49d2-931A-FDC373682BA5"); //$NON-NLS-1$
    public static final GUID SQL_ANALYSIS_INSTANCE = new GUID("D22D57DA-355D-4a3c-82DE-62B3E157D0B3"); //$NON-NLS-1$
    public static final GUID SQL_REPORTING_INSTANCE = new GUID("065977D6-00EA-4a77-81EC-1CD011644AAC"); //$NON-NLS-1$
    public static final GUID APPLICATION_DATABASE = new GUID("526301DE-F821-48c8-ABBD-3430DC7946D3"); //$NON-NLS-1$
    public static final GUID PROJECT_COLLECTION_DATABASE = new GUID("1B6B5931-69F6-4c53-90A0-220B177353B7"); //$NON-NLS-1$
    public static final GUID SHARE_POINT_WEB_APPLICATION = new GUID("3DADD190-40E6-4fc1-A306-4906713C87CE"); //$NON-NLS-1$
    public static final GUID SHARE_POINT_SITE_CREATION_LOCATION = new GUID("9FB288AE-9D94-40cb-B5E7-0EFC3FE3599F"); //$NON-NLS-1$
    public static final GUID PROJECT_PORTAL = new GUID("450901B6-B528-4863-9876-5BD3927DF467"); //$NON-NLS-1$
    public static final GUID PROCESS_GUIDANCE_SITE = new GUID("15DA1594-45F5-47d4-AE52-78F16E67EB1E"); //$NON-NLS-1$
    public static final GUID WAREHOUSE_DATABASE = new GUID("CE318CD9-F797-45dc-ACC7-792C3428E39D"); //$NON-NLS-1$
    public static final GUID ANALYSIS_DATABASE = new GUID("64C0C64F-7199-4c0a-A1F7-6D979292E86E"); //$NON-NLS-1$
    public static final GUID REPORTING_CONFIGURATION = new GUID("143B22C5-D1B9-494f-B124-68D098ABA598"); //$NON-NLS-1$
    public static final GUID REPORTING_SERVER = new GUID("F756975E-3593-448b-A6B8-E34010908621"); //$NON-NLS-1$
    public static final GUID REPORTING_FOLDER = new GUID("41C8B6DB-39EC-49db-9DB8-0760E836BFBE"); //$NON-NLS-1$
    public static final GUID TEAM_SYSTEM_WEB_ACCESS = new GUID("47FA57A4-8157-4fb5-9A64-A7A4954BD284"); //$NON-NLS-1$
    public static final GUID TEST_CONTROLLER = new GUID("3C856555-8737-48b6-8B61-4B24DB7FEB15"); //$NON-NLS-1$
    public static final GUID TEST_ENVIRONMENT = new GUID("D457AA94-F00E-4342-92E8-FFE81535E74B"); //$NON-NLS-1$

    /**
     * Static values only, do not construct.
     */
    private CatalogResourceTypes() {
    }
}
