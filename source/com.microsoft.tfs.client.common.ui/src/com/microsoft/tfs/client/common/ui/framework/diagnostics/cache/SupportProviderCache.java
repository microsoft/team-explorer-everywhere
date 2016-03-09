// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.framework.diagnostics.cache;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.resource.ImageDescriptor;

import com.microsoft.tfs.client.common.ui.TFSCommonUIClientPlugin;
import com.microsoft.tfs.client.common.ui.framework.diagnostics.Constants;
import com.microsoft.tfs.client.common.ui.framework.diagnostics.SupportManager;
import com.microsoft.tfs.client.common.ui.framework.diagnostics.Utils;

public class SupportProviderCache {
    private boolean populated = false;
    private SupportProvider supportProvider;

    private final Map supportContactCategories = new HashMap();

    public synchronized SupportProvider getSupportProvider() {
        if (!populated) {
            populate();
        }
        return supportProvider;
    }

    private void populate() {
        populated = true;

        final IExtension[] extensions = Platform.getExtensionRegistry().getExtensionPoint(
            TFSCommonUIClientPlugin.PLUGIN_ID,
            Constants.SUPPORT_PROVIDERS_EXTENSION_POINT_NAME).getExtensions();

        IExtension extension;

        if (extensions.length == 0) {
            SupportManager.log(IStatus.ERROR, "no support providers are available"); //$NON-NLS-1$
            return;
        } else if (extensions.length > 1) {
            for (int i = 0; i < extensions.length; i++) {
                final String messageFormat = "multiple support providers are registered {0}"; //$NON-NLS-1$
                final String message =
                    MessageFormat.format(
                        messageFormat,
                        Utils.messageFor(
                            extensions[i].getConfigurationElements()[0],
                            Constants.SUPPORT_PROVIDERS_EXTENSION_POINT_NAME));

                SupportManager.log(IStatus.WARNING, message);
            }
        }
        extension = extensions[0];

        final IConfigurationElement[] configElements = extension.getConfigurationElements();

        for (int j = 0; j < configElements.length; j++) {
            final IConfigurationElement configElement = configElements[j];
            if (Constants.ELEMENT_NAME_PROVIDER.equals(configElement.getName())) {
                populateSupportContactCategories(configElement);
                supportProvider = buildSupportProvider(configElement);
            }
        }
    }

    private void populateSupportContactCategories(final IConfigurationElement configElement) {
        final IConfigurationElement[] children = configElement.getChildren();
        for (int i = 0; i < children.length; i++) {
            if (Constants.ELEMENT_NAME_SUPPORT_CONTACT_CATEGORY.equals(children[i].getName())) {
                final SupportContactCategory supportContactCategory = buildSupportContactCategory(children[i]);
                if (supportContactCategory != null) {
                    if (supportContactCategories.containsKey(supportContactCategory.getID())) {
                        final String messageFormat = "support contact category [{0}] is defined multiple times {1}"; //$NON-NLS-1$
                        final String message = MessageFormat.format(
                            messageFormat,
                            supportContactCategory.getID(),
                            Utils.messageFor(children[i], Constants.SUPPORT_PROVIDERS_EXTENSION_POINT_NAME));

                        SupportManager.log(IStatus.WARNING, message);
                    }
                    supportContactCategories.put(supportContactCategory.getID(), supportContactCategory);
                }
            }
        }
    }

    private SupportContactCategory buildSupportContactCategory(final IConfigurationElement element) {
        final String id = Utils.getRequiredAttribute(
            element,
            Constants.ATTRIBUTE_NAME_ID,
            Constants.SUPPORT_PROVIDERS_EXTENSION_POINT_NAME);

        if (id == null) {
            return null;
        }

        final String label = Utils.getRequiredAttribute(
            element,
            Constants.ATTRIBUTE_NAME_LABEL,
            Constants.SUPPORT_PROVIDERS_EXTENSION_POINT_NAME);

        if (label == null) {
            return null;
        }

        return new SupportContactCategory(id, label);
    }

    private SupportProvider buildSupportProvider(final IConfigurationElement configElement) {
        final String id = Utils.getRequiredAttribute(
            configElement,
            Constants.ATTRIBUTE_NAME_ID,
            Constants.SUPPORT_PROVIDERS_EXTENSION_POINT_NAME);
        if (id == null) {
            return null;
        }

        final String dialogTitle = Utils.getRequiredAttribute(
            configElement,
            Constants.ATTRIBUTE_NAME_DIALOG_TITLE,
            Constants.SUPPORT_PROVIDERS_EXTENSION_POINT_NAME);
        if (dialogTitle == null) {
            return null;
        }

        final String exportFilenamePrefix = Utils.getRequiredAttribute(
            configElement,
            Constants.ATTRIBUTE_NAME_EXPORT_FILENAME_PREFIX,
            Constants.SUPPORT_PROVIDERS_EXTENSION_POINT_NAME);
        if (exportFilenamePrefix == null) {
            return null;
        }

        final ImageDescriptor dialogImage = Utils.getImageDescriptor(
            configElement,
            Constants.ATTRIBUTE_NAME_DIALOG_IMAGE,
            Constants.SUPPORT_PROVIDERS_EXTENSION_POINT_NAME);

        /* New style compositing of images */
        final ImageDescriptor dialogImageLeft = Utils.getImageDescriptor(
            configElement,
            Constants.ATTRIBUTE_NAME_DIALOG_IMAGE_LEFT,
            Constants.SUPPORT_PROVIDERS_EXTENSION_POINT_NAME);
        final ImageDescriptor dialogImageRight = Utils.getImageDescriptor(
            configElement,
            Constants.ATTRIBUTE_NAME_DIALOG_IMAGE_RIGHT,
            Constants.SUPPORT_PROVIDERS_EXTENSION_POINT_NAME);

        final String dialogText = Utils.getRequiredSubElementText(
            configElement,
            Constants.ELEMENT_NAME_DIALOG_TEXT,
            Constants.SUPPORT_PROVIDERS_EXTENSION_POINT_NAME);
        if (dialogText == null) {
            return null;
        }

        SupportProvider spi;

        if (dialogImageLeft != null && dialogImageRight != null) {
            spi = new SupportProvider(
                id,
                dialogTitle,
                dialogText,
                exportFilenamePrefix,
                dialogImageLeft,
                dialogImageRight);
        } else {
            spi = new SupportProvider(id, dialogTitle, dialogText, exportFilenamePrefix, dialogImage);
        }

        populateSupportContacts(spi, configElement);
        return spi;
    }

    private void populateSupportContacts(final SupportProvider spi, final IConfigurationElement configElement) {
        final IConfigurationElement[] children = configElement.getChildren();
        for (int i = 0; i < children.length; i++) {
            if (Constants.ELEMENT_NAME_SUPPORT_CONTACT.equals(children[i].getName())) {
                final String id = Utils.getRequiredAttribute(
                    children[i],
                    Constants.ATTRIBUTE_NAME_ID,
                    Constants.SUPPORT_PROVIDERS_EXTENSION_POINT_NAME);
                if (id == null) {
                    continue;
                }

                final String label = Utils.getRequiredAttribute(
                    children[i],
                    Constants.ATTRIBUTE_NAME_LABEL,
                    Constants.SUPPORT_PROVIDERS_EXTENSION_POINT_NAME);
                if (label == null) {
                    continue;
                }

                final String value = Utils.getRequiredAttribute(
                    children[i],
                    Constants.ATTRIBUTE_NAME_VALUE,
                    Constants.SUPPORT_PROVIDERS_EXTENSION_POINT_NAME);
                if (value == null) {
                    continue;
                }

                String url = children[i].getAttribute(Constants.ATTRIBUTE_NAME_URL);
                if (url == null) {
                    url = value;
                }

                String description =
                    Utils.getNonRequiredSubElementText(children[i], Constants.ELEMENT_NAME_DESCRIPTION);
                if (description != null) {
                    description = description.trim();
                    if (description.length() == 0) {
                        description = null;
                    }
                }

                final Boolean launchable = Utils.getRequiredBooleanAttribute(
                    children[i],
                    Constants.ATTRIBUTE_NAME_LAUNCHABLE,
                    Constants.SUPPORT_PROVIDERS_EXTENSION_POINT_NAME);
                if (launchable == null) {
                    continue;
                }

                final String categoryId = Utils.getRequiredAttribute(
                    children[i],
                    Constants.ATTRIBUTE_NAME_CATEGORY_ID,
                    Constants.SUPPORT_PROVIDERS_EXTENSION_POINT_NAME);
                if (categoryId == null) {
                    continue;
                }

                final SupportContactCategory category =
                    (SupportContactCategory) supportContactCategories.get(categoryId);

                if (category == null) {
                    final String messageFormat = "unknown support contact category id [{0}] {1}"; //$NON-NLS-1$
                    MessageFormat.format(
                        messageFormat,
                        categoryId,
                        Utils.messageFor(children[i], Constants.SUPPORT_PROVIDERS_EXTENSION_POINT_NAME));

                    SupportManager.log(
                        IStatus.WARNING,
                        MessageFormat.format(
                            "unknown support contact category id [{0}] {1}", //$NON-NLS-1$
                            categoryId,
                            Utils.messageFor(children[i], Constants.SUPPORT_PROVIDERS_EXTENSION_POINT_NAME)));
                    continue;
                }

                final SupportContact supportContact =
                    new SupportContact(id, label, value, url, description, launchable.booleanValue(), category);

                spi.addSupportContact(supportContact);
            }
        }
    }
}
