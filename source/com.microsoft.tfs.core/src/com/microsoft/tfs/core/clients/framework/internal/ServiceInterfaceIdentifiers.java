// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.framework.internal;

import com.microsoft.tfs.util.GUID;

/**
 * @threadsafety immutable
 */
public final class ServiceInterfaceIdentifiers {
    // Find these in VS's FrameworkServiceIdentifiers class, or in the
    // CollectionServiceIdentifier property on a VS service implementation,
    // or in cached location XML data.

    // Framework and Core Services

    public static final GUID ADMINISTRATION = new GUID("C18D6E34-68E8-40D2-A619-E7477558976E"); //$NON-NLS-1$
    public static final GUID BUILD = new GUID("543cf133-319b-4c7b-800a-fafff734f291"); //$NON-NLS-1$
    public static final GUID BUILD_3 = new GUID("427febc8-f703-482b-9f79-bfe1bb4631bc"); //$NON-NLS-1$
    public static final GUID BUILD_4 = new GUID("aae1325c-e97f-4a15-b557-9d1620d5d5f4"); //$NON-NLS-1$
    public static final GUID BUILD_CONTROLLER = new GUID("36cffc58-f0d7-4b48-8e2d-6c79ab4447cb"); //$NON-NLS-1$
    public static final GUID BUILD_LINKING = new GUID("204687d1-5df8-493c-bcda-e43a19b935e9"); //$NON-NLS-1$
    public static final GUID BUILD_STORE = new GUID("c13c2a8e-4a9f-4fd4-8225-6e40cc733787"); //$NON-NLS-1$
    public static final GUID BUILD_QUEUE_SERVICE = new GUID("984f10cc-bc83-48fc-abe7-27053ca78f20"); //$NON-NLS-1$
    public static final GUID BUILD_QUEUE_SERVICE_4 = new GUID("3F6F3C6C-EDF5-429B-A1AE-F7D94306A4C8"); //$NON-NLS-1$
    public static final GUID BUILD_ADMINISTRATION_SERVICE = new GUID("d1e9471d-7e69-4210-ad4c-3c941b245e2f"); //$NON-NLS-1$
    public static final GUID BUILD_ADMINISTRATION_SERVICE_4 = new GUID("fb42b129-9e9b-4cf4-ba4f-f87859c2db1c"); //$NON-NLS-1$
    public static final GUID BUILD_AGENT_RESERVATION_SERVICE = new GUID("d64516b0-fc8d-41a5-9319-584474c267c4"); //$NON-NLS-1$
    public static final GUID CATALOG = new GUID("C2F9106F-127A-45B7-B0A3-E0AD8239A2A7"); //$NON-NLS-1$
    public static final GUID COLLECTION_IDENTITY_MANAGEMENT = new GUID("1e29861e-76b6-4b1e-bf41-5f868aea63fe"); //$NON-NLS-1$
    public static final GUID COLLECTION_IDENTITY_MANAGEMENT_2 = new GUID("a4ce4577-b38e-49c8-bdb4-b9c53615e0da"); //$NON-NLS-1$
    public static final GUID FILE_CONTAINER_SERVICE = new GUID("056BB484-A8B5-4961-A4EB-9118ED0C9560"); //$NON-NLS-1$
    public static final GUID LOCATION = new GUID("bf9cf1d0-24ac-4d35-aeca-6cd18c69c1fe"); //$NON-NLS-1$
    public static final GUID SECURITY = new GUID("af3178da-1ec3-4bd0-b245-9f5decdc572e"); //$NON-NLS-1$
    public static final GUID TEAM_CONFIGURATION = new GUID("56baa505-9d62-4e68-b64c-b88697dc5322"); //$NON-NLS-1$
    public static final GUID VERSION_CONTROL = new GUID("b2b178f5-bef9-460d-a5cf-35bcc0281cc4"); //$NON-NLS-1$
    public static final GUID VERSION_CONTROL_3 = new GUID("ec9b0153-ee54-450e-b6e0-664ecb033c99"); //$NON-NLS-1$
    public static final GUID VERSION_CONTROL_4 = new GUID("FA9FCC37-F9BD-496F-A1B8-CE351F6BFE8A"); //$NON-NLS-1$
    public static final GUID VERSION_CONTROL_4_DOT_1 = new GUID("C8926592-E3D0-4F4F-BBE5-9F52EDF5103E"); //$NON-NLS-1$
    public static final GUID VERSION_CONTROL_4_DOT_2 = new GUID("CEEE60A4-39FD-4013-8B33-5DC5BA4A6BD9"); //$NON-NLS-1$
    public static final GUID VERSION_CONTROL_4_DOT_3 = new GUID("71900729-7BE6-45CA-923D-3B00AA97DAE8"); //$NON-NLS-1$
    public static final GUID VERSION_CONTROL_5 = new GUID("A25D0656-DA63-4F51-9DA9-800FFF229D1A"); //$NON-NLS-1$
    public static final GUID VERSION_CONTROL_5_DOT_1 = new GUID("54EB89EB-36D1-46AD-85C1-84EB5E8C7DE7"); //$NON-NLS-1$
    public static final GUID VERSION_CONTROL_ADMIN = new GUID("0ade2b5a-efa4-419e-bf11-24f7cfe7c1a2"); //$NON-NLS-1$
    public static final GUID VERSION_CONTROL_DOWNLOAD = new GUID("29b91065-1314-41d5-ab70-0bfa9896a51d"); //$NON-NLS-1$
    public static final GUID VERSION_CONTROL_LINKING = new GUID("10a3ab2b-7140-4b4b-a72a-0feca94d5b6d"); //$NON-NLS-1$
    public static final GUID VERSION_CONTROL_UPLOAD = new GUID("1c04c122-7ad1-4f02-87ba-979b9d278bee"); //$NON-NLS-1$
    public static final GUID WORK_ITEM = new GUID("179b6a0b-a5be-43fc-879f-cfa2a43cd3d8"); //$NON-NLS-1$
    public static final GUID WORK_ITEM_2 = new GUID("7EDE8C17-7965-4AEE-874D-ED9B25276DEB"); //$NON-NLS-1$
    public static final GUID WORK_ITEM_3 = new GUID("CA87FA49-58C9-4089-8535-1299FA60EEBC"); //$NON-NLS-1$
    public static final GUID WORK_ITEM_5 = new GUID("4c5eb288-4c0a-4888-bb1b-742a4b5b706e"); //$NON-NLS-1$
    public static final GUID WORK_ITEM_6 = new GUID("a4ed4fbf-eb4a-467a-9de6-13599c3f81de"); //$NON-NLS-1$
    public static final GUID WORK_ITEM_7 = new GUID("bc9b27aa-eda2-4fc9-ac3b-644bb7999c19"); //$NON-NLS-1$
    public static final GUID WORK_ITEM_8 = new GUID("1cc519db-7813-49eb-8db5-04003dd776e8"); //$NON-NLS-1$
    public static final GUID WORK_ITEM_ATTACHMENT_HANDLER = new GUID("F04F5BFC-FF3D-4EA2-BFC4-6FA485AD594E"); //$NON-NLS-1$
    public static final GUID WORK_ITEM_CONFIG = new GUID("1e9d1b48-775a-49c8-af8f-d41a06e0cdb0"); //$NON-NLS-1$
    public static final GUID WORK_ITEM_LINKING = new GUID("40329fc1-f737-4ef8-807f-b91856676a56"); //$NON-NLS-1$

    // Integration Service

    public static final GUID AUTHORIZATION = new GUID("6373ee32-aad4-4bf9-9ec8-72201ab1c45c"); //$NON-NLS-1$
    public static final GUID AUTHORIZATION_3 = new GUID("DA728B84-3C54-46BB-A423-8A5FB526A722"); //$NON-NLS-1$
    public static final GUID COMMON_STRUCTURE = new GUID("d9c3f8ff-8938-4193-919b-7588e81cb730"); //$NON-NLS-1$
    public static final GUID COMMON_STRUCTURE_3 = new GUID("02ea5fcc-1e40-4d94-a8e5-ed62c15cb676"); //$NON-NLS-1$
    public static final GUID COMMON_STRUCTURE_4 = new GUID("edd317f7-a7c3-4c97-a039-ba933e895201"); //$NON-NLS-1$
    public static final GUID GROUP_SECURITY = new GUID("dbd733d9-8ca6-42db-b17b-aedb2decea6d"); //$NON-NLS-1$
    public static final GUID GROUP_SECURITY_2 = new GUID("6448b75a-5ab4-492b-ba8d-5bd55b4ff523"); //$NON-NLS-1$
    public static final GUID PROCESS_TEMPLATE = new GUID("75ab998e-7f09-479e-9559-b86b5b06f688"); //$NON-NLS-1$
    public static final GUID REGISTRATION = new GUID("b8f97328-80d2-412d-9810-67c5a3f4190f"); //$NON-NLS-1$
    public static final GUID SERVER_STATUS = new GUID("d395630a-d784-45b9-b8d1-f4b82042a8d0"); //$NON-NLS-1$

    // Web

    public static final GUID TSWA_IDENTITY_MANAGEMENT = new GUID("206B8759-9838-4D85-A5CD-5DA6024F893D"); //$NON-NLS-1$
    public static final GUID TSWA_SECURITY_MANAGEMENT = new GUID("6C41E618-1A04-45F3-BDD8-30BE12A890C3"); //$NON-NLS-1$
    public static final GUID TSWA_AREAS_MANAGEMENT = new GUID("4734349B-D85E-4354-BB93-87C6D1311A04"); //$NON-NLS-1$
    public static final GUID TSWA_ITERATIONS_MANAGEMENT = new GUID("6D4F90E9-6023-425B-9102-4F4B710CDA7E"); //$NON-NLS-1$
    public static final GUID TSWA_PROJECT_ALERTS = new GUID("AB86E286-8190-417C-AF08-FA5867B2D2BF"); //$NON-NLS-1$
}
