// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.build.flags;

/**
 * Encapsulates the built-in information node type names.
 *
 * @threadsafety immutable
 * @since TEE-SDK-10.1
 */
public class InformationTypes {
    public static final String ACTIVITY_PROPERTIES = "ActivityProperties"; //$NON-NLS-1$
    public static final String ACTIVITY_TRACKING = "ActivityTracking"; //$NON-NLS-1$
    public static final String AGENT_SCOPE_ACTIVITY_TRACKING = "AgentScopeActivityTracking"; //$NON-NLS-1$
    public static final String ASSOCIATED_CHANGESET = "AssociatedChangeset"; //$NON-NLS-1$
    public static final String ASSOCIATED_WORK_ITEM = "AssociatedWorkItem"; //$NON-NLS-1$
    public static final String BUILD_ERROR = "BuildError"; //$NON-NLS-1$
    public static final String BUILD_MESSAGE = "BuildMessage"; //$NON-NLS-1$
    public static final String BUILD_PROJECT = "BuildProject"; //$NON-NLS-1$
    public static final String BUILD_STEP = "BuildStep"; //$NON-NLS-1$
    public static final String BUILD_WARNING = "BuildWarning"; //$NON-NLS-1$
    public static final String CHECK_IN_OUTCOME = "CheckInOutcome"; //$NON-NLS-1$
    public static final String COMPILATION_SUMMARY = "CompilationSummary"; //$NON-NLS-1$
    public static final String CONFIGURATION_SUMMARY = "ConfigurationSummary"; //$NON-NLS-1$
    public static final String EXTERNAL_LINK = "ExternalLink"; //$NON-NLS-1$
    public static final String OPENED_WORK_ITEM = "OpenedWorkItem"; //$NON-NLS-1$
    public static final String DEPLOYMENT_INFORMATION = "DeploymentInformation"; //$NON-NLS-1$
    protected static final String SYM_STORE_TRANSACTION = "SymStoreTransaction"; //$NON-NLS-1$
}