// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse.ui.wizard.sharewizard;

import org.eclipse.core.resources.IProject;
import org.eclipse.team.ui.IConfigurationWizardExtension;
import org.eclipse.ui.IWorkbench;

import com.microsoft.tfs.util.Check;

/**
 * Dummy class to implement the {@link IConfigurationWizardExtension} interface.
 *
 * All the logic to do this is actually in {@link ShareWizard}, but since
 * {@link IConfigurationWizardExtension} is new in 3.5, that class cannot depend
 * on the extension interface directly. It is, however, adaptable to an
 * {@link IConfigurationWizardExtension} which will simply return an instance of
 * this class, which will proxy its initialization down.
 *
 * Note that this should not extend {@link ShareWizard}, as only the
 * {@link #init(IWorkbench, IProject[])} method is called on this class, then
 * this interface is unused and the {@link ShareWizard} is used again.
 */
public final class ShareWizardExtension implements IConfigurationWizardExtension {
    private final ShareWizard wizard;

    public ShareWizardExtension(final ShareWizard wizard) {
        Check.notNull(wizard, "wizard"); //$NON-NLS-1$

        this.wizard = wizard;
    }

    @Override
    public void init(final IWorkbench workbench, final IProject[] projects) {
        wizard.initInternal(workbench, projects);
    }
}
