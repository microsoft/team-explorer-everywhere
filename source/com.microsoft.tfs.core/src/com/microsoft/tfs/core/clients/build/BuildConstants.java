// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.build;

/**
 * Common constants used in the build API.
 *
 * @since TEE-SDK-10.1
 */
public class BuildConstants {
    public static final String BUILD_CONTROLLER_SERVICE = "BuildControllerService"; //$NON-NLS-1$
    public static final String BUILD_LOG_FILE_NAME = "BuildLog.txt"; //$NON-NLS-1$
    public static final String BUILD_NUMBER_ARGUMENT = "BuildNumber"; //$NON-NLS-1$
    public static final String BUILD_SERVICE = "BuildService"; //$NON-NLS-1$
    public static final String BUILD_STORE_SERVICE = "BuildStoreService"; //$NON-NLS-1$
    public static final String BUILD_TYPE_FOLDER_NAME = "TeamBuildTypes"; //$NON-NLS-1$
    public static final String BUILD_URI_ARGUMENT = "BuildUri"; //$NON-NLS-1$
    public static final String CUSTOM_EVENTS_FILE_NAME = "CustomEvents.xml"; //$NON-NLS-1$
    public static final String DROP_LOCATION_ARGUMENT = "DropLocation"; //$NON-NLS-1$
    public static final String LAST_BUILD_STEP = "BuildCompleted"; //$NON-NLS-1$
    public static final String LOG_FILE_PER_PROJECT_ARGUMENT = "LogFilePerProject"; //$NON-NLS-1$
    public static final String PROJECT_FILE_NAME = "TFSBuild.proj"; //$NON-NLS-1$
    public static final String RESPONSE_FILE_NAME = "TFSBuild.rsp"; //$NON-NLS-1$
    public static final String SERVER_UI_CULTURE_ARGUMENT = "ServerUICulture"; //$NON-NLS-1$
    public static final String SERVER_APPLICATION_NAME = "TFSBuildService"; //$NON-NLS-1$
    public static final String SERVICE_FILE_NAME = "TFSBuildService.exe"; //$NON-NLS-1$
    public static final String SERVICE_NAME = "VSTFBUILD"; //$NON-NLS-1$
    public static final String TEAM_BUILD_DATABASE = "TeamBuild DB"; //$NON-NLS-1$
    public static final String TFS_FILE_PROJECT_ARGUMENT = "TFSProjectFile"; //$NON-NLS-1$
    public static final String TFS_URL_ARGUMENT = "TFSUrl"; //$NON-NLS-1$
    public static final String TOOL_ID = "Build"; //$NON-NLS-1$
    public static final int MAX_PATH_LENGTH = 259;
    public static final int MAX_PATH_NAME_LENGTH = 248;
    public static final int MAX_URI_LENGTH = 2048;
    public static final int DEFAULT_BUILD_AGENT_PORT = 9191;
    public static final String DEFAULT_WORKING_DIRECTORY = "$(Temp)\\$(BuildDefinitionPath)"; //$NON-NLS-1$
    public static final String SOURCE_DIR_ENVIRONMENT_VARIABLE = "$(SourceDir)"; //$NON-NLS-1$

    public static final String BUILD_AGENT_ID = "$(BuildAgentId)"; //$NON-NLS-1$
    public static final String BUILD_AGENT_NAME = "$(BuildAgentName)"; //$NON-NLS-1$
    public static final String BUILD_DEFINITION_ID = "$(BuildDefinitionId)"; //$NON-NLS-1$
    public static final String BUILD_DEFINITION_PATH = "$(BuildDefinitionPath)"; //$NON-NLS-1$
    public static final String BUILD_DIR_ENV = "$(BuildDir)"; //$NON-NLS-1$

    /**
     * The comment String which signals the continuous integration system to
     * ignore a check-in. Any check-in with this String at any point in the
     * comment will be ignored (i.e. will not trigger a new build).
     */
    public static final String NO_CI_CHECK_IN_COMMENT = "***NO_CI***"; //$NON-NLS-1$

    public static final String WORKSPACE_TEMPLATE_FILE_NAME = "WorkspaceMapping.xml"; //$NON-NLS-1$
    public static final String MSBUILD_RESPONSE_FILE = "TFSBuild.rsp"; //$NON-NLS-1$

    public static final String DEFAULT_SERVICE_HOST_URL_PATH = "Build/v3.0/Services"; //$NON-NLS-1$

    public static final String STAR = "*"; //$NON-NLS-1$
    public static final String[] ALL_INFORMATION_TYPES = new String[] {
        STAR
    };

    public static final String[] ALL_PROPERTY_NAMES = new String[] {
        STAR
    };

    public static final String[] NO_PROPERTY_NAMES = new String[] {};

    // Elements declared in Microsoft version of BuildContants that we probably
    // don't need.
    //
    // protected static int DEFAULT_DATE_TIME_YEAR_THRESHOLD = 1;
    // public static final String DefaultLocaleStr = "1033";
    // public static final String PLATFORM_FLAVOR_FORMAT_TEXT = "{0}/{1}";

    /**
     * Default constructure set to private - nothing should be initializing
     * this.
     */
    private BuildConstants() {
    }

}
