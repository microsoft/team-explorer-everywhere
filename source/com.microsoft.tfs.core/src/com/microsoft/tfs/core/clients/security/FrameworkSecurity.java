// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.security;

import com.microsoft.tfs.util.GUID;

public abstract class FrameworkSecurity {
    /**
     * This is the set of namespaces that exist in the framework.
     */
    public static final GUID FRAMEWORK_NAMESPACE_ID = new GUID("1f4179b3-6bac-4d01-b421-71ea09171400"); //$NON-NLS-1$
    public static final GUID EVENT_SUBSCRIPTION_NAMESPACE_ID = new GUID("58B176E7-3411-457a-89D0-C6D0CCB3C52B"); //$NON-NLS-1$
    public static final GUID JOB_NAMESPACE_ID = new GUID("2a887f97-db68-4b7c-9ae3-5cebd7add999"); //$NON-NLS-1$
    public static final GUID REGISTRY_NAMESPACE_ID = new GUID("4ae0db5d-8437-4ee8-a18b-1f6fb38bd34c"); //$NON-NLS-1$
    public static final GUID COLLECTION_MANAGEMENT_NAMESPACE_ID = new GUID("f66fc5d6-60e1-443e-9d16-851364ce3b99"); //$NON-NLS-1$
    public static final GUID CATALOG_NAMESPACE_ID = new GUID("6BACCF73-1500-476f-8B2B-94F4489A59AA"); //$NON-NLS-1$
    public static final GUID IDENTITIES_NAMESPACE_ID = new GUID("5A27515B-CCD7-42c9-84F1-54C998F03866"); //$NON-NLS-1$
    public static final GUID HOSTING_ACCOUNTS_NAMESPACE_ID = new GUID("4b541417-7b8d-4c4c-b855-2a657e4fc215"); //$NON-NLS-1$
    public static final GUID STRONG_BOX_NAMESPACE_ID = new GUID("4A9E8381-289A-4DFD-8460-69028EAA93B3"); //$NON-NLS-1$

    /**
     * This is currently pointing to the Integration "Namespace" security
     * namespace
     */
    public static final GUID PROCESS_TEMPLATES_NAMESPACE_ID = new GUID("3E65F728-F8BC-4ecd-8764-7E378B19BFA7"); //$NON-NLS-1$

    /**
     * The namespace identifier for message queue security.
     */
    public static final GUID MESSAGE_QUEUE_NAMESPACE_ID = new GUID("F3E9DDE6-32CD-48BB-B62D-1D73BCAF42F1"); //$NON-NLS-1$

    /**
     * The root token for message queue security.
     */
    public static final String MESSAGE_QUEUE_NAMESPACE_ROOT_TOKEN = "Tfsmq"; //$NON-NLS-1$

    /**
     * The path separator character for message queue security.
     */
    public static final char MESSAGE_QUEUE_PATH_SEPARATOR = '/';

    /**
     * This is the set of tokens used for the framework security namespaces
     */
    public static final String FRAMEWORK_NAMESPACE_TOKEN = "FrameworkGlobalSecurity"; //$NON-NLS-1$

    /**
     * Token information for the job security tokens.
     */
    public static final String JOB_NAMESPACE_TOKEN = "AllJobs"; //$NON-NLS-1$

    /**
     * This will serve as the collection management namespace.
     */
    public static final String COLLECTION_MANAGEMENT_NAMESPACE_TOKEN = "AllCollections"; //$NON-NLS-1$
    public static final char COLLECTION_MANAGEMENT_PATH_SEPARATOR = '/';

    /**
     * Token information for the registry security tokens
     */
    public static final char REGISTRY_PATH_SEPARATOR = '/';
    public static final String REGISTRY_NAMESPACE_ROOT_TOKEN = "" + REGISTRY_PATH_SEPARATOR; //$NON-NLS-1$

    /**
     * This will serve as the hosting account namespace.
     */
    public static final String HOSTING_ACCOUNT_NAMESPACE_TOKEN = "AllAccounts"; //$NON-NLS-1$
    public static final char HOSTING_ACCOUNT_PATH_SEPARATOR = '/';

    public static final char IDENTITY_SECURITY_PATH_SEPARATOR = '\\';

    public static final char STRONG_BOX_SECURITY_PATH_SEPARATOR = '/';
    public static final String STRONG_BOX_SECURITY_NAMESPACE_ROOT_TOKEN = "StrongBox"; //$NON-NLS-1$

    /**
     * Token information for the process template namespace.
     */
    public static final String PROCESS_TEMPLATE_NAMESPACE_TOKEN = "NAMESPACE"; //$NON-NLS-1$
}
