// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teambuild.wizards;

import java.text.MessageFormat;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.util.SafeRunnable;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.IWizardNode;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.graphics.Point;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import com.microsoft.tfs.client.common.ui.teambuild.Messages;
import com.microsoft.tfs.core.clients.build.GitProperties;
import com.microsoft.tfs.core.clients.build.IBuildDefinition;
import com.microsoft.tfs.core.clients.build.flags.BuildServerVersion;

public class CreateBuildConfigurationWizardNode implements IWizardNode {
    private static final String ATT_ID = "id"; //$NON-NLS-1$
    private static final String ATT_NAME = "name"; //$NON-NLS-1$
    private static final String ATT_CLASS = "class"; //$NON-NLS-1$
    private static final String ATT_ICON = "icon"; //$NON-NLS-1$
    private static final String ATT_FROM_VER = "fromTFSVersion"; //$NON-NLS-1$
    private static final String ATT_TO_VER = "toTFSVersion"; //$NON-NLS-1$

    private final IConfigurationElement configElement;
    private final String id;
    private final String name;
    private ImageDescriptor icon;
    private final int fromServerVersion;
    private final int toServerVersion;
    private final String pluginId;
    private WizardPage parentWizardPage;
    private final IBuildDefinition buildDefinition;

    private ICreateBuildConfigurationWizard wizard;

    public CreateBuildConfigurationWizardNode(
        final IConfigurationElement configElement,
        final IBuildDefinition buildDefinition) {
        this.configElement = configElement;
        this.buildDefinition = buildDefinition;
        id = getStringAttribute(this.configElement, ATT_ID, null);
        name = getStringAttribute(this.configElement, ATT_NAME, id);

        // Make sure class is defined - do not load yet.
        getStringAttribute(this.configElement, ATT_CLASS, null);
        pluginId = configElement.getDeclaringExtension().getNamespace();

        fromServerVersion = getIntAttribute(this.configElement, ATT_FROM_VER, 0);
        toServerVersion = getIntAttribute(this.configElement, ATT_TO_VER, Integer.MAX_VALUE);
    }

    private static String getStringAttribute(
        final IConfigurationElement configElement,
        final String name,
        final String defaultValue) {
        final String value = configElement.getAttribute(name);
        if (value != null) {
            return value;
        }
        if (defaultValue != null) {
            return defaultValue;
        }

        final String messageFormat = "Missing {0} attribute."; //$NON-NLS-1$
        final String message = MessageFormat.format(messageFormat, name);
        throw new IllegalArgumentException(message);
    }

    private static int getIntAttribute(
        final IConfigurationElement configElement,
        final String name,
        final int defaultValue) {
        final String valueString = configElement.getAttribute(name);
        if (valueString == null || valueString.length() == 0) {
            return defaultValue;
        }
        return Integer.parseInt(valueString);
    }

    public ImageDescriptor getIcon() {
        if (icon != null) {
            return icon;
        }

        final String iconPath = configElement.getAttribute(ATT_ICON);
        if (iconPath == null) {
            return null;
        }

        icon = AbstractUIPlugin.imageDescriptorFromPlugin(pluginId, iconPath);
        return icon;
    }

    public void setIcon(final ImageDescriptor icon) {
        this.icon = icon;
    }

    public IConfigurationElement getConfigElement() {
        return configElement;
    }

    public String getID() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getFromServerVersion() {
        return fromServerVersion;
    }

    public int getToServerVersion() {
        return toServerVersion;
    }

    public boolean isVersionSuitable(final BuildServerVersion serverVersion) {
        return (fromServerVersion <= serverVersion.getVersion() && toServerVersion >= serverVersion.getVersion());
    }

    /**
     * Use id name as a criteria for git related wizards, might need to find a
     * better way to improve here
     *
     * @return
     */
    public boolean isGitNode() {
        return id.toLowerCase().indexOf(GitProperties.GIT) != -1;
    }

    @Override
    public void dispose() {
        if (wizard != null) {
            wizard.dispose();
        }
    }

    @Override
    public Point getExtent() {
        return new Point(-1, -1);
    }

    @Override
    public IWizard getWizard() {
        if (wizard != null) {
            return wizard;
        }

        final ICreateBuildConfigurationWizard[] wizards = new ICreateBuildConfigurationWizard[1];
        final IStatus statuses[] = new IStatus[1];
        BusyIndicator.showWhile(parentWizardPage.getShell().getDisplay(), new Runnable() {
            @Override
            public void run() {
                org.eclipse.core.runtime.Platform.run(new SafeRunnable() {

                    @Override
                    public void run() throws Exception {
                        try {
                            wizards[0] =
                                (ICreateBuildConfigurationWizard) configElement.createExecutableExtension(ATT_CLASS);
                        } catch (final CoreException e) {
                            statuses[0] = new Status(IStatus.ERROR, pluginId, IStatus.OK, e.getMessage() == null ? "" //$NON-NLS-1$
                                : e.getMessage(), e);
                        }
                    }

                    @Override
                    public void handleException(final Throwable e) {
                        statuses[0] = new Status(IStatus.ERROR, pluginId, IStatus.OK, e.getMessage() == null ? "" //$NON-NLS-1$
                            : e.getMessage(), e);
                    }

                });
            }
        });

        if (statuses[0] != null) {
            parentWizardPage.setErrorMessage(
                Messages.getString("CreateBuildConfigurationWizardNode.ErrorOpeningWizard")); //$NON-NLS-1$
            ErrorDialog.openError(
                parentWizardPage.getShell(),
                Messages.getString("CreateBuildConfigurationWizardNode.CouldNotOpenWizard"), //$NON-NLS-1$
                null,
                statuses[0]);
            return null;
        }

        wizards[0].init(buildDefinition);
        wizard = wizards[0];

        return wizard;
    }

    @Override
    public boolean isContentCreated() {
        return wizard != null;
    }

    public WizardPage getParentWizardPage() {
        return parentWizardPage;
    }

    public void setParentWizardPage(final WizardPage parentWizardPage) {
        this.parentWizardPage = parentWizardPage;
    }

}
