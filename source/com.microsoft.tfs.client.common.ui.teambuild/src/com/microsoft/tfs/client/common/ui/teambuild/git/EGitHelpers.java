// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teambuild.git;

import org.eclipse.swt.widgets.Shell;

import com.microsoft.tfs.client.common.git.utils.GitHelpers;
import com.microsoft.tfs.core.clients.build.IBuildDefinition;

public abstract class EGitHelpers extends GitHelpers {
    private static final String TFS_CLIENT_COMMON_UI_TEAMBUILD_BUNDLE_ID =
        "com.microsoft.tfs.client.common.ui.teambuild.egit"; //$NON-NLS-1$
    private static final String GIT_BUILD_DEFINITION_DIALOG_CLASS =
        "com.microsoft.tfs.client.common.ui.teambuild.egit.dialogs.GitBuildDefinitionDialog"; //$NON-NLS-1$

    public static Object getGitBuildDefinitionDialog(final Shell shell, final IBuildDefinition buildDefinition) {
        if (!isEGitInstalled(true)) {
            return null;
        }

        final Class<?>[] constructorParameterTypes = new Class<?>[] {
            Shell.class,
            IBuildDefinition.class
        };

        final Object[] constructorParameterValues = new Object[] {
            shell,
            buildDefinition
        };

        final Object dialog = getInstance(
            TFS_CLIENT_COMMON_UI_TEAMBUILD_BUNDLE_ID,
            GIT_BUILD_DEFINITION_DIALOG_CLASS,
            constructorParameterTypes,
            constructorParameterValues);

        return dialog;
    }
}
