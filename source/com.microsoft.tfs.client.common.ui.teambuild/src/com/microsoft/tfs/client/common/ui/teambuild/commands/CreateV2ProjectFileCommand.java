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
import com.microsoft.tfs.core.clients.versioncontrol.path.LocalPath;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.IOUtils;
import com.microsoft.tfs.util.LocaleUtil;
import com.microsoft.tfs.util.StringUtil;
import com.microsoft.tfs.util.temp.TempStorageService;

/**
 *         This class replace the property tag in the template files to
 *         generates the TFSBuild.proj and TFSBuild.rsp
 */
public class CreateV2ProjectFileCommand extends CreateProjectFileCommand {

    private final String buildFileServerPath;
    private final String templateLocationRoot;

    private final String javaHome;
    private final String buildToolHome;

    private final String javaZipPath;
    private final String buildToolZipPath;

    private static final String MAVEN_MARKER = "#MAVENSECTION#"; //$NON-NLS-1$
    private static final String ANT_MARKER = "#ANTSECTION#"; //$NON-NLS-1$
    public static final String NEWLINE = System.getProperty("line.separator"); //$NON-NLS-1$

    public CreateV2ProjectFileCommand(
        final IBuildDefinition buildDefinition,
        final String buildFileServerPath,
        final String templateLocationRoot) {
        this(buildDefinition, buildFileServerPath, templateLocationRoot, null, null, null, null);
    }

    public CreateV2ProjectFileCommand(
        final IBuildDefinition buildDefinition,
        final String buildFileServerPath,
        final String templateLocationRoot,
        final String javaHome,
        final String javaZipPath,
        final String buildToolHome,
        final String buildToolZipPath) {
        super(buildDefinition);
        this.buildFileServerPath = buildFileServerPath;
        this.templateLocationRoot = templateLocationRoot;
        this.javaHome = javaHome;
        this.javaZipPath = javaZipPath;
        this.buildToolHome = buildToolHome;
        this.buildToolZipPath = buildToolZipPath;
    }

    @Override
    public String getName() {
        final String messageFormat = Messages.getString("CreateV2ProjectFileCommand.CommandMessageFormat"); //$NON-NLS-1$
        return MessageFormat.format(messageFormat, getBuildDefinition().getName());
    }

    @Override
    public String getErrorDescription() {
        return Messages.getString("CreateV2ProjectFileCommand.CommandErrorMessage");//$NON-NLS-1$
    }

    @Override
    public String getLoggingDescription() {
        final String messageFormat =
            Messages.getString("CreateV2ProjectFileCommand.CommandMessageFormat", LocaleUtil.ROOT); //$NON-NLS-1$
        return MessageFormat.format(messageFormat, getBuildDefinition().getName());
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
        // Check that config folder path is set
        final String serverFolder = getBuildDefinition().getConfigurationFolderPath();
        Check.notNullOrEmpty(serverFolder, "serverFolder"); //$NON-NLS-1$

        final String messageFormat = Messages.getString("CreateV2ProjectFileCommand.ProgressMonitorTextFormat"); //$NON-NLS-1$
        final String message = MessageFormat.format(messageFormat, getBuildDefinition().getName());
        progressMonitor.beginTask(message, IProgressMonitor.UNKNOWN);

        final String tempFolder = TempStorageService.getInstance().createTempDirectory().getCanonicalPath();

        writeProjectFile(tempFolder);
        writeResponseFile(tempFolder);

        VersionControlHelper.checkinTemporaryBuildConfigFolder(
            getBuildDefinition().getBuildServer().getConnection(),
            tempFolder,
            serverFolder,
            true);

        progressMonitor.done();
        return Status.OK_STATUS;
    }

    protected String writeResponseFile(final String tempFolder) throws IOException {
        final String path = LocalPath.combine(tempFolder, BuildConstants.MSBUILD_RESPONSE_FILE);

        final URL url = TFSTeamBuildPlugin.getDefault().getBundle().getEntry(templateLocationRoot + "/TFSBuild.rsp"); //$NON-NLS-1$

        final String responseFile = IOUtils.toString(url.openStream(), BUILD_FILE_ENCODING);

        writeToFile(responseFile, path, BUILD_FILE_ENCODING);

        return path;
    }

    protected String writeProjectFile(final String tempFolder) throws IOException {
        final URL url = TFSTeamBuildPlugin.getDefault().getBundle().getEntry(templateLocationRoot + "/TFSBuild.proj"); //$NON-NLS-1$

        String projectFile = IOUtils.toString(url.openStream(), BUILD_FILE_ENCODING);

        String buildDirectory;
        String buildMachine;
        if (getBuildDefinition().getBuildController() != null) {
            // TODO .getExpandedBuildDirectory(buildDefinition);
            // buildDirectory =
            // buildDefinition.getDefaultBuildAgent().getExpandedBuildDirectory
            // (buildDefinition);
            buildDirectory = UNKNOWN;

            buildMachine = getBuildDefinition().getBuildController().getServiceHost().getName();
        } else {
            buildDirectory = UNKNOWN;
            buildMachine = UNKNOWN;
        }
        String dropLocation;
        if (!StringUtil.isNullOrEmpty(getBuildDefinition().getDefaultDropLocation())) {
            dropLocation = getBuildDefinition().getDefaultDropLocation();
        } else {
            dropLocation = "\\\\" + UNKNOWN + "\\drops"; //$NON-NLS-1$ //$NON-NLS-2$
        }

        String apiVersion = "9.0"; //$NON-NLS-1$
        final String version = "2"; //$NON-NLS-1$

        if (getBuildDefinition().getBuildServer().getBuildServerVersion().isV3OrGreater()) {
            apiVersion = "10.0"; //$NON-NLS-1$
        }

        projectFile = StringUtil.replace(projectFile, "#TFSAPIVERSION#", apiVersion); //$NON-NLS-1$
        projectFile = StringUtil.replace(projectFile, "#VERSION#", version); //$NON-NLS-1$
        projectFile = StringUtil.replace(projectFile, "#BUILDDIRECTORY#", buildDirectory); //$NON-NLS-1$
        projectFile = StringUtil.replace(projectFile, "#BUILDMACHINE#", buildMachine); //$NON-NLS-1$
        projectFile = StringUtil.replace(projectFile, "#CONFIGURATIONTOBUILD#", getConfigurationString()); //$NON-NLS-1$
        projectFile = StringUtil.replace(projectFile, "#DESCRIPTION#", getBuildDefinition().getDescription()); //$NON-NLS-1$
        projectFile = StringUtil.replace(projectFile, "#DROPLOCATION#", dropLocation); //$NON-NLS-1$
        projectFile = StringUtil.replace(projectFile, "#TEAMPROJECT#", getBuildDefinition().getTeamProject()); //$NON-NLS-1$
        projectFile = StringUtil.replace(projectFile, "#COMPILEFILE#", getBuildFilePath()); //$NON-NLS-1$
        projectFile = StringUtil.replace(projectFile, "#JAVASECTION#", getJavaSettings()); //$NON-NLS-1$
        projectFile = replaceBuildToolSection(projectFile);

        final String path = LocalPath.combine(tempFolder, BuildConstants.PROJECT_FILE_NAME);

        writeToFile(projectFile, path, BUILD_FILE_ENCODING);

        return path;
    }

    private String replaceBuildToolSection(final String projectFile) {
        if (projectFile.indexOf(MAVEN_MARKER) != -1) {
            return StringUtil.replace(projectFile, MAVEN_MARKER, getMavenSettings());
        } else if (projectFile.indexOf(ANT_MARKER) != -1) {
            return StringUtil.replace(projectFile, ANT_MARKER, getAntSettings());
        } else {
            return projectFile;
        }
    }

    private String getBuildFilePath() {
        return buildFileServerPath;
    }

    private String getJavaSettings() {
        final StringBuilder javaSettings = new StringBuilder();
        if (javaHome != null) {
            javaSettings.append("<!--  JAVA_HOME");//$NON-NLS-1$
            javaSettings.append(NEWLINE);
            javaSettings.append("     Set this flag to enable/disable updating JAVA_HOME on a successful build.");//$NON-NLS-1$
            javaSettings.append(NEWLINE);
            javaSettings.append("     -->"); //$NON-NLS-1$
            javaSettings.append(NEWLINE);
            javaSettings.append("    <JAVA_HOME>" + javaHome + "</JAVA_HOME>");//$NON-NLS-1$ //$NON-NLS-2$
            javaSettings.append(NEWLINE);
        } else if (javaZipPath != null) {
            javaSettings.append("<!--  JavaServerPath");//$NON-NLS-1$
            javaSettings.append(NEWLINE);
            javaSettings.append("     Set this property to the server path of the Java JDK archive.");//$NON-NLS-1$
            javaSettings.append(NEWLINE);
            javaSettings.append("     -->"); //$NON-NLS-1$
            javaSettings.append(NEWLINE);
            javaSettings.append("    <JavaServerPath>" + javaZipPath + "</JavaServerPath>"); //$NON-NLS-1$ //$NON-NLS-2$
            javaSettings.append(NEWLINE);
        }
        return javaSettings.toString();
    }

    private String getAntSettings() {
        final StringBuilder antSettings = new StringBuilder();
        if (!StringUtil.isNullOrEmpty(buildToolHome)) {
            antSettings.append("<!--  Ant_Home");//$NON-NLS-1$
            antSettings.append(NEWLINE);
            antSettings.append("     Set this flag to enable/disable updating ANT_HOME on a successful build.");//$NON-NLS-1$
            antSettings.append(NEWLINE);
            antSettings.append("     -->"); //$NON-NLS-1$
            antSettings.append(NEWLINE);
            antSettings.append("    <Ant_Home>" + buildToolHome + "</Ant_Home>");//$NON-NLS-1$ //$NON-NLS-2$
            antSettings.append(NEWLINE);
        } else if (!StringUtil.isNullOrEmpty(buildToolZipPath)) {
            antSettings.append("<!--  ANTServerPath");//$NON-NLS-1$
            antSettings.append(NEWLINE);
            antSettings.append("     Set this property to the server path of the Ant archive.");//$NON-NLS-1$
            antSettings.append(NEWLINE);
            antSettings.append("     -->"); //$NON-NLS-1$
            antSettings.append(NEWLINE);
            antSettings.append("    <ANTServerPath>" + buildToolZipPath + "</ANTServerPath>"); //$NON-NLS-1$ //$NON-NLS-2$
            antSettings.append(NEWLINE);
        }
        return antSettings.toString();
    }

    private String getMavenSettings() {
        final StringBuilder mavenSettings = new StringBuilder();
        if (!StringUtil.isNullOrEmpty(buildToolHome)) {
            mavenSettings.append("<!--  M2_Home");//$NON-NLS-1$
            mavenSettings.append(NEWLINE);
            mavenSettings.append("     Set this flag to enable/disable updating M2_HOME on a successful build.");//$NON-NLS-1$
            mavenSettings.append(NEWLINE);
            mavenSettings.append("     -->"); //$NON-NLS-1$
            mavenSettings.append(NEWLINE);
            mavenSettings.append("    <M2_Home>" + buildToolHome + "</M2_Home>");//$NON-NLS-1$ //$NON-NLS-2$
            mavenSettings.append(NEWLINE);
        } else if (!StringUtil.isNullOrEmpty(buildToolZipPath)) {
            mavenSettings.append("<!--  MavenServerPath");//$NON-NLS-1$
            mavenSettings.append(NEWLINE);
            mavenSettings.append("     Set this property to the server path of the Maven archive.");//$NON-NLS-1$
            mavenSettings.append(NEWLINE);
            mavenSettings.append("     -->"); //$NON-NLS-1$
            mavenSettings.append(NEWLINE);
            mavenSettings.append("    <MavenServerPath>" + buildToolZipPath + "</MavenServerPath>"); //$NON-NLS-1$ //$NON-NLS-2$
            mavenSettings.append(NEWLINE);
        }
        return mavenSettings.toString();
    }
}