// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teambuild.wizards;

public class CreateGitBuildConfigurationWizard extends CreateBuildConfigurationWizard {

    public CreateGitBuildConfigurationWizard() {
        super();
    }

    @Override
    protected boolean shouldAdd(final CreateBuildConfigurationWizardNode descriptor) {
        return descriptor.isGitNode();
    }
}
