// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teambuild;

import com.microsoft.tfs.core.clients.build.IBuildServer;

/**
 * Class used to represent a team project in the build UI.
 */
public class TeamProject {

    private final IBuildServer buildServer;
    private final String name;

    public TeamProject(final IBuildServer buildServer, final String name) {
        super();
        this.buildServer = buildServer;
        this.name = name;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @return the buildServer
     */
    public IBuildServer getBuildServer() {
        return buildServer;
    }

}
