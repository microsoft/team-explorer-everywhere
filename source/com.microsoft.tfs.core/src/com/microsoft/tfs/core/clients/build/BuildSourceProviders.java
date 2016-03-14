// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.build;

import com.microsoft.tfs.core.clients.build.internal.soapextensions.BuildDefinitionSourceProvider;
import com.microsoft.tfs.core.clients.build.soapextensions.DefinitionTriggerType;
import com.microsoft.tfs.util.StringUtil;

public class BuildSourceProviders {
    public static final String TfVersionControl = "TFVC"; //$NON-NLS-1$
    public static final String TfGit = "TFGIT"; //$NON-NLS-1$
    public static final String Git = "GIT"; //$NON-NLS-1$

    public static boolean isTfGit(final String sourceProvider) {
        return TfGit.equalsIgnoreCase(sourceProvider);
    }

    public static boolean isGit(final String sourceProvider) {
        // Git includes TfGit as well
        return Git.equalsIgnoreCase(sourceProvider) || isTfGit(sourceProvider);
    }

    public static boolean isTfVersionControl(final String sourceProvider) {
        // Tfs is the default provider so empty/null means Tfs
        return StringUtil.isNullOrEmpty(sourceProvider) || sourceProvider.equalsIgnoreCase(TfVersionControl);
    }

    public static boolean isTfGit(final IBuildDefinitionSourceProvider sourceProvider) {
        if (sourceProvider == null || sourceProvider.getName() == null) {
            return false;
        }
        return isTfGit(sourceProvider.getName());
    }

    public static boolean isTfVersionControl(final IBuildDefinitionSourceProvider sourceProvider) {
        if (sourceProvider == null || sourceProvider.getName() == null) {
            return true;
        }
        return isTfVersionControl(sourceProvider.getName());
    }

    public static BuildDefinitionSourceProvider createGitSourceProvider() {
        return new BuildDefinitionSourceProvider(
            TfGit,
            DefinitionTriggerType.ALL.remove(DefinitionTriggerType.GATED_CHECKIN));
    }

    public static BuildDefinitionSourceProvider createTfVcSourceProvider() {
        return new BuildDefinitionSourceProvider(TfVersionControl, DefinitionTriggerType.ALL);
    }
}
