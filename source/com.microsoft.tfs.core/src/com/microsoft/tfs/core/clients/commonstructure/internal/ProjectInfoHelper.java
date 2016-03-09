// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.commonstructure.internal;

import java.util.Arrays;

import com.microsoft.tfs.core.clients.commonstructure.ProjectInfo;
import com.microsoft.tfs.util.Check;

public class ProjectInfoHelper {
    public static String[] getProjectNames(final ProjectInfo[] projects) {
        Check.notNull(projects, "projects"); //$NON-NLS-1$

        final String[] projectNames = new String[projects.length];
        for (int i = 0; i < projectNames.length; i++) {
            projectNames[i] = projects[i].getName();
        }

        Arrays.sort(projectNames);

        return projectNames;
    }
}
