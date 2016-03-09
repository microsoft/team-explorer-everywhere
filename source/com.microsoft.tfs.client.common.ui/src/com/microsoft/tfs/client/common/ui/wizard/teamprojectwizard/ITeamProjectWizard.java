// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.wizard.teamprojectwizard;

import java.net.URI;

import org.eclipse.jface.wizard.IWizard;

import com.microsoft.tfs.client.common.server.TFSServer;
import com.microsoft.tfs.core.clients.commonstructure.ProjectInfo;

public interface ITeamProjectWizard extends IWizard {
    public void setServerURI(URI serverURI);

    public TFSServer getServer();

    public ProjectInfo[] getSelectedProjects();
}
