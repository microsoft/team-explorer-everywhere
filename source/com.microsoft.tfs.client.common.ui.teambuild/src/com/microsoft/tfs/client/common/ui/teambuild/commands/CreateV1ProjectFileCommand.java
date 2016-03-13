// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teambuild.commands;

import java.io.IOException;
import java.net.URL;
import java.text.MessageFormat;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import com.microsoft.tfs.client.common.ui.teambuild.Messages;
import com.microsoft.tfs.client.common.ui.teambuild.TFSTeamBuildPlugin;
import com.microsoft.tfs.client.common.ui.teambuild.VersionControlHelper;
import com.microsoft.tfs.core.clients.build.BuildConstants;
import com.microsoft.tfs.core.clients.build.IBuildDefinition;
import com.microsoft.tfs.core.clients.build.IWorkspaceMapping;
import com.microsoft.tfs.core.clients.build.IWorkspaceTemplate;
import com.microsoft.tfs.core.clients.build.internal.TeamBuildCache;
import com.microsoft.tfs.core.clients.build.soapextensions.WorkspaceMappingType;
import com.microsoft.tfs.core.clients.versioncontrol.path.LocalPath;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.IOUtils;
import com.microsoft.tfs.util.LocaleUtil;
import com.microsoft.tfs.util.StringUtil;
import com.microsoft.tfs.util.temp.TempStorageService;

public class CreateV1ProjectFileCommand extends CreateProjectFileCommand {
    private final String teamProject;
    private final String name;
    private final String description;
    private final IWorkspaceTemplate workspaceTemplate;
    private final String antBuildFile;
    private final String buildAgent;
    private final String buildDirectory;
    private final String dropLocation;

    public CreateV1ProjectFileCommand(
        final IBuildDefinition buildDefinition,
        final String teamProject,
        final String name,
        final String description,
        final IWorkspaceTemplate workspaceTemplate,
        final String antBuildFile,
        final String buildAgent,
        final String buildDirectory,
        final String dropLocation) {
        super(buildDefinition);

        Check.notNull(buildDefinition, "buildDefinition"); //$NON-NLS-1$
        Check.notNullOrEmpty(teamProject, "teamProject"); //$NON-NLS-1$
        Check.notNullOrEmpty(name, "name"); //$NON-NLS-1$
        Check.notNull(description, "description"); //$NON-NLS-1$
        Check.notNull(workspaceTemplate, "workspaceTemplate"); //$NON-NLS-1$
        Check.notNull(antBuildFile, "antBuildFile"); //$NON-NLS-1$
        Check.notNull(buildAgent, "buildAgent"); //$NON-NLS-1$
        Check.notNull(buildDirectory, "buildDirectory"); //$NON-NLS-1$
        Check.notNull(dropLocation, "dropLocation"); //$NON-NLS-1$

        this.teamProject = teamProject;
        this.name = name;
        this.description = description;
        this.workspaceTemplate = workspaceTemplate;
        this.antBuildFile = antBuildFile;
        this.buildAgent = buildAgent;

        // Remove any trailing /'s from build path.
        this.buildDirectory = VersionControlHelper.normalizeLocalPath(buildDirectory);

        this.dropLocation = dropLocation;
    }

    @Override
    public String getName() {
        final String messageFormat = Messages.getString("CreateV1ProjectFileCommand.CommandMessageFormat"); //$NON-NLS-1$
        return MessageFormat.format(messageFormat, name);
    }

    @Override
    public String getErrorDescription() {
        return Messages.getString("CreateV1ProjectFileCommand.CommandErrorMessage");//$NON-NLS-1$
    }

    @Override
    public String getLoggingDescription() {
        final String messageFormat =
            Messages.getString("CreateV1ProjectFileCommand.CommandMessageFormat", LocaleUtil.ROOT); //$NON-NLS-1$
        return MessageFormat.format(messageFormat, name);
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.microsoft.tfs.client.common.shared.command.Command#doRun(org.eclipse
     * .core.runtime. IProgressMonitor)
     */
    @Override
    protected IStatus doRun(final IProgressMonitor progressMonitor) throws Exception {
        Check.notNullOrEmpty(teamProject, "teamProject"); //$NON-NLS-1$
        Check.notNullOrEmpty(name, "name"); //$NON-NLS-1$
        Check.notNullOrEmpty(antBuildFile, "antBuildFile"); //$NON-NLS-1$

        // Check that config folder path is set
        final String serverFolder = VersionControlHelper.calculateDefaultBuildFileLocation(teamProject, name);
        Check.notNullOrEmpty(serverFolder, "serverFolder"); //$NON-NLS-1$

        final String messageFormat = Messages.getString("CreateV1ProjectFileCommand.ProgressMessageFormat"); //$NON-NLS-1$
        final String message = MessageFormat.format(messageFormat, name);
        progressMonitor.beginTask(message, IProgressMonitor.UNKNOWN);

        final String tempFolder = TempStorageService.getInstance().createTempDirectory().getCanonicalPath();

        writeProjectFile(tempFolder);
        writeWorkspaceFile(tempFolder);
        writeResponseFile(tempFolder);

        VersionControlHelper.checkinTemporaryBuildConfigFolder(
            getBuildDefinition().getBuildServer().getConnection(),
            tempFolder,
            serverFolder,
            true);

        // Force refresh of build agents and build definitions in the cache.
        // Do all this work while displaying a progress monitor so that the
        // users gets a better experience.
        TeamBuildCache.getInstance(
            getBuildDefinition().getBuildServer(),
            teamProject).reloadBuildDefinitionsAndControllers();

        progressMonitor.done();
        return Status.OK_STATUS;
    }

    private String writeProjectFile(final String tempFolder) throws IOException {
        final String path = LocalPath.combine(tempFolder, BuildConstants.PROJECT_FILE_NAME);

        final URL url = TFSTeamBuildPlugin.getDefault().getBundle().getEntry("/templates/ant/v1/TFSBuild.proj"); //$NON-NLS-1$

        String projectFile = IOUtils.toString(url.openStream(), BUILD_FILE_ENCODING);

        projectFile = StringUtil.replace(projectFile, "#VERSION#", "1"); //$NON-NLS-1$ //$NON-NLS-2$
        projectFile = StringUtil.replace(projectFile, "#BUILDDIRECTORY#", buildDirectory); //$NON-NLS-1$
        projectFile = StringUtil.replace(projectFile, "#BUILDMACHINE#", buildAgent); //$NON-NLS-1$
        projectFile = StringUtil.replace(projectFile, "#CONFIGURATIONTOBUILD#", getConfigurationString()); //$NON-NLS-1$
        projectFile = StringUtil.replace(projectFile, "#DESCRIPTION#", description); //$NON-NLS-1$
        projectFile = StringUtil.replace(projectFile, "#DROPLOCATION#", dropLocation); //$NON-NLS-1$
        projectFile = StringUtil.replace(projectFile, "#TEAMPROJECT#", teamProject); //$NON-NLS-1$
        projectFile = StringUtil.replace(projectFile, "#COMPILEFILE#", antBuildFile); //$NON-NLS-1$

        writeToFile(projectFile, path, BUILD_FILE_ENCODING);

        return path;
    }

    private String writeWorkspaceFile(final String tempFolder) throws IOException {
        final String path = LocalPath.combine(tempFolder, BuildConstants.WORKSPACE_TEMPLATE_FILE_NAME);

        final URL url = TFSTeamBuildPlugin.getDefault().getBundle().getEntry("/templates/ant/v1/WorkspaceMapping.xml"); //$NON-NLS-1$

        String workspaceFile = IOUtils.toString(url.openStream(), BUILD_FILE_ENCODING);

        workspaceFile = StringUtil.replace(workspaceFile, "#INTERNALMAPPINGS#", getInternalMappingsString()); //$NON-NLS-1$

        writeToFile(workspaceFile, path, BUILD_FILE_ENCODING);

        return path;
    }

    private String writeResponseFile(final String tempFolder) throws IOException {
        final String path = LocalPath.combine(tempFolder, BuildConstants.MSBUILD_RESPONSE_FILE);

        final URL url = TFSTeamBuildPlugin.getDefault().getBundle().getEntry("/templates/ant/v1/TFSBuild.rsp"); //$NON-NLS-1$

        final String responseFile = IOUtils.toString(url.openStream(), BUILD_FILE_ENCODING);

        // TODO: ADD LOG VERBOSITY?

        writeToFile(responseFile, path, BUILD_FILE_ENCODING);

        return path;
    }

    private String getInternalMappingsString() {
        // Return string in format
        // $/teamproject/folder1/folder2 to c:\temp\folder
        final StringBuffer internalMappings = new StringBuffer();

        for (final IWorkspaceMapping mapping : workspaceTemplate.getMappings()) {
            internalMappings.append("    <InternalMapping ServerItem=\""); //$NON-NLS-1$
            internalMappings.append(mapping.getServerItem());
            if (mapping.getMappingType() != WorkspaceMappingType.CLOAK) {
                internalMappings.append("\" LocalItem=\""); //$NON-NLS-1$
                internalMappings.append(
                    StringUtil.replace(
                        mapping.getLocalItem(),
                        BuildConstants.SOURCE_DIR_ENVIRONMENT_VARIABLE,
                        buildDirectory));
            }
            internalMappings.append("\" Type=\""); //$NON-NLS-1$
            internalMappings.append((mapping.getMappingType() == WorkspaceMappingType.CLOAK) ? "Cloak" : "Map"); //$NON-NLS-1$ //$NON-NLS-2$
            internalMappings.append("\" />\r\n"); //$NON-NLS-1$
        }
        // remove the last \r\n
        internalMappings.setLength(internalMappings.length() - 2);
        return internalMappings.toString();
    }
}
