// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.telemetry;

public class TfsTelemetryConstants {
    public static final String CONTEXT_PROPERTY_USER_ID = "Context.Default.TEE.Core.user.id"; //$NON-NLS-1$

    public static final String CONTEXT_PROPERTY_BUILD_NUMBER = "Context.Default.TEE.Core.BuildNumber"; //$NON-NLS-1$
    public static final String CONTEXT_PROPERTY_MAJOR_VERSION = "Context.Default.TEE.Core.MajorVersion"; //$NON-NLS-1$
    public static final String CONTEXT_PROPERTY_MINOR_VERSION = "Context.Default.TEE.Core.MinorVersion"; //$NON-NLS-1$
    public static final String CONTEXT_PROPERTY_SERVICEPACK = "Context.Default.TEE.Core.ServicePack"; //$NON-NLS-1$
    public static final String CONTEXT_PROPERTY_EXE_NAME = "Context.Default.TEE.Core.ExeName"; //$NON-NLS-1$

    public static final String CONTEXT_PROPERTY_PROCESSOR_ARCHITECTURE =
        "Context.Default.TEE.Core.Machine.Processor.Architecture"; //$NON-NLS-1$
    public static final String CONTEXT_PROPERTY_LOCALE_NAME = "Context.Default.TEE.Core.Locale.SystemLocaleName"; //$NON-NLS-1$
    public static final String CONTEXT_PROPERTY_OS_MAJOR_VERSION = "Context.Default.TEE.Core.OS.MajorVersion"; //$NON-NLS-1$
    public static final String CONTEXT_PROPERTY_OS_MINOR_VERSION = "Context.Default.TEE.Core.OS.MinorVersion"; //$NON-NLS-1$
    public static final String CONTEXT_PROPERTY_OS_NAME = "Context.Default.TEE.Core.OS.Name"; //$NON-NLS-1$
    public static final String CONTEXT_PROPERTY_OS_SHORT_NAME = "Context.Default.TEE.Core.OS.ShortName"; //$NON-NLS-1$
    public static final String CONTEXT_PROPERTY_OS_FULL_NAME = "Context.Default.TEE.Core.OS.FullName"; //$NON-NLS-1$

    public static final String CONTEXT_PROPERTY_JAVA_RUNTIME_NAME = "Context.Default.TEE.Core.Java.Name"; //$NON-NLS-1$
    public static final String CONTEXT_PROPERTY_JAVA_RUNTIME_VERSION = "Context.Default.TEE.Core.Java.Version"; //$NON-NLS-1$

    public static final String CONTEXT_PROPERTY_FRAMEWORK_NAME = "Context.Default.TEE.Core.Framework.Name"; //$NON-NLS-1$
    public static final String CONTEXT_PROPERTY_FRAMEWORK_VERSION = "Context.Default.TEE.Core.Framework.Version"; //$NON-NLS-1$

    public static final String SHARED_PROPERTY_IS_HOSTED = "TEE.TeamFoundationServer.IsHostedServer"; //$NON-NLS-1$
    public static final String SHARED_PROPERTY_SERVER_ID = "TEE.TeamFoundationServer.ServerId"; //$NON-NLS-1$
    public static final String SHARED_PROPERTY_COLLECTION_ID = "TEE.TeamFoundationServer.CollectionId"; //$NON-NLS-1$

    public static final String CLC_EVENT_PROPERTY_IS_SUCCESS = "TEE.CommandLineClient.Command.Success"; //$NON-NLS-1$
    public static final String CLC_EVENT_PROPERTY_COMMAND_NAME = "TEE.CommandLineClient.Command.Name"; //$NON-NLS-1$

    public static final String PLUGIN_EVENT_PROPERTY_IS_SUCCESS = "TEE.Plugin.Command.Success"; //$NON-NLS-1$
    public static final String PLUGIN_EVENT_PROPERTY_COMMAND_NAME = "TEE.Plugin.Command.Name"; //$NON-NLS-1$

    public static final String PLUGIN_PAGE_VIEW_PROPERTY_UNDOCKED = "TEE.Plugin.PageView.Undocked"; //$NON-NLS-1$
    public static final String PLUGIN_COMMAND_EVENT_PROPERTY_VERSION_CONTROL = "TEE.Plugin.Command.VersionControl"; //$NON-NLS-1$

    public static final String WIZARD_PAGE_VIEW_NAME_FORMAT = "TEE/Plugin/Wizard/{0}"; //$NON-NLS-1$
    public static final String DIALOG_PAGE_VIEW_NAME_FORMAT = "TEE/Plugin/Dialog/{0}"; //$NON-NLS-1$
    public static final String EXPLORER_PAGE_VIEW_NAME_FORMAT = "TEE/Plugin/Explorer/{0}"; //$NON-NLS-1$

    public static final String CLC_COMMAND_EVENT_NAME_FORMAT = "TEE/TeamFoundationServer/CLC/Command/{0}"; //$NON-NLS-1$
    public static final String PLUGIN_COMMAND_EVENT_NAME_FORMAT = "TEE/TeamFoundationServer/Plugin/Command/{0}"; //$NON-NLS-1$
    public static final String PLUGIN_ACTION_EVENT_NAME_FORMAT = "TEE/TeamFoundationServer/Plugin/Action/{0}"; //$NON-NLS-1$

    public static final String FEEDBACK_PROPERTY_COMMENT = "TEE.Feedback.Comment"; //$NON-NLS-1$
    public static final String FEEDBACK_PROPERTY_EMAIL = "TEE.Feedback.Email"; //$NON-NLS-1$
    public static final String FEEDBACK_PROPERTY_CONTEXT = "TEE.Feedback.Context"; //$NON-NLS-1$

}
