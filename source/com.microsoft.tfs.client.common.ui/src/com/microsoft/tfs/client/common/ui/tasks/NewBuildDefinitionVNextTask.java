// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.tasks;

import java.net.URI;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.swt.widgets.Shell;

import com.microsoft.alm.teamfoundation.build.webapi.BuildDefinitionTemplate;
import com.microsoft.tfs.core.TFSTeamProjectCollection;

public class NewBuildDefinitionVNextTask extends BuildDefinitionVNextTask {
    private final BuildDefinitionTemplate template;

    public NewBuildDefinitionVNextTask(
        final Shell shell,
        final TFSTeamProjectCollection connection,
        final String projectName,
        final BuildDefinitionTemplate template) {
        super(shell, connection, projectName);
        this.template = template;
    }

    @Override
    public IStatus run() {
        final URI uri = getActionUri(NEW_DEFINITION_ACTION, template);
        openBrowser(uri);
        return Status.OK_STATUS;
    }
}
