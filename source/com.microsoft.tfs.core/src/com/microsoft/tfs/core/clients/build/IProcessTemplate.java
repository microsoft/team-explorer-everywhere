// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.build;

import com.microsoft.tfs.core.clients.build.flags.BuildReason;
import com.microsoft.tfs.core.clients.build.soapextensions.ProcessTemplateType;

public interface IProcessTemplate {
    /**
     * Gets the identifier for this process template.
     *
     *
     * @return
     */
    public int getID();

    /**
     * The Team Project for this process template.
     *
     *
     * @return
     */
    public String getTeamProject();

    /**
     * The version control path of the build process file.
     *
     *
     * @return
     */
    public String getServerPath();

    /**
     * The type of the build process template. This property is for system use
     * and should be set to ProcessTemplateType.Custom for all user defined
     * templates.
     *
     *
     * @return
     */
    public ProcessTemplateType getTemplateType();

    public void setTemplateType(ProcessTemplateType value);

    /**
     * The description of the build process template.
     *
     *
     * @return
     */
    public String getDescription();

    public void setDescription(String value);

    /**
     * The valid build reasons that are supported by this build process
     * template.
     *
     *
     * @return
     */
    public BuildReason getSupportedReasons();

    public void setSupportedReasons(BuildReason value);

    /**
     * The process templates parameters. This property is the root
     * DynamicActivity of the build process along with all top-level arguments
     * and properties serialized as XAML.
     *
     *
     * @return
     */
    public String getParameters();

    /**
     * Gets the version of this process template.
     *
     *
     * @return
     */
    public String getVersion();

    /**
     * Downloads the latest version of the build process template. The build
     * process is downloaded to a temporary file and deleted after reading the
     * file contents.
     *
     *
     * @return The entire build process template as a string.
     */
    public String download();

    /**
     * Downloads the build process template at the version specified by version
     * control version specifier. The build process is downloaded to a temporary
     * file and deleted after reading the file contents.
     *
     *
     * @param versionSpec
     *        Any valid version control version specifier in string form.
     * @return The entire build process template as a string.
     */
    public String download(String versionSpec);

    /**
     * Saves the build process template to the server.
     *
     *
     */
    public void save();

    /**
     * Deletes the build process template from the server.
     *
     *
     */
    public void delete();

    /**
     * Copies properties from a source process template to this instance.
     *
     *
     * @param source
     *        Template to copy from
     */
    public void copyFrom(IProcessTemplate source);
}
