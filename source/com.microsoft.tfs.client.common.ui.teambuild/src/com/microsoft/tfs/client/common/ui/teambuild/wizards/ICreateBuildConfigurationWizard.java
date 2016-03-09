// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teambuild.wizards;

import org.eclipse.jface.wizard.IWizard;

import com.microsoft.tfs.core.clients.build.IBuildDefinition;

/**
 * Interface implemented by all Create Build Configuration Wizards.
 */
public interface ICreateBuildConfigurationWizard extends IWizard {

    void init(IBuildDefinition buildDefinition);

}
