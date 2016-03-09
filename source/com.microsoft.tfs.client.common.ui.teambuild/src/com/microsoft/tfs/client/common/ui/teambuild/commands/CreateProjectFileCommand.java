// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teambuild.commands;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

import com.microsoft.tfs.client.common.commands.TFSCommand;
import com.microsoft.tfs.core.clients.build.IBuildDefinition;

public abstract class CreateProjectFileCommand extends TFSCommand {

    protected static final String BUILD_FILE_ENCODING = "UTF8"; //$NON-NLS-1$
    protected static final String UNKNOWN = "UNKNOWN"; //$NON-NLS-1$
    private final IBuildDefinition buildDefinition;

    public CreateProjectFileCommand(final IBuildDefinition buildDefinition) {
        this.buildDefinition = buildDefinition;
    }

    public IBuildDefinition getBuildDefinition() {
        return buildDefinition;
    }

    protected void writeToFile(final String stringToWrite, final String filePath, final String encoding)
        throws IOException {
        final OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(filePath), encoding);
        writer.write(stringToWrite);
        writer.close();
    }

    protected String getConfigurationString() {
        // TODO: Get the user to supply these.
        final String flavor = "Release"; //$NON-NLS-1$
        final String platform = "AnyCPU"; //$NON-NLS-1$

        return "    <ConfigurationToBuild Include=\"" //$NON-NLS-1$
            + flavor
            + "|" //$NON-NLS-1$
            + platform
            + "\">\r\n" //$NON-NLS-1$
            + "        <FlavorToBuild>" //$NON-NLS-1$
            + flavor
            + "</FlavorToBuild>\r\n" //$NON-NLS-1$
            + "        <PlatformToBuild>" //$NON-NLS-1$
            + platform
            + "</PlatformToBuild>\r\n" //$NON-NLS-1$
            + "    </ConfigurationToBuild>"; //$NON-NLS-1$
    }

}
