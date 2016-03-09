// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.framework.diagnostics;

import java.text.MessageFormat;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import com.microsoft.tfs.client.common.ui.TFSCommonUIClientPlugin;

public class Utils {
    public static final String messageFor(final IConfigurationElement configElement, final String extensionPointName) {
        final String messageFormat = "(extension of [{0}.{1}] from [{2}])"; //$NON-NLS-1$
        return MessageFormat.format(
            messageFormat,
            TFSCommonUIClientPlugin.PLUGIN_ID,
            extensionPointName,
            getContributorFor(configElement));
    }

    public static final String getContributorFor(final IConfigurationElement element) {
        return element.getDeclaringExtension().getNamespace();
    }

    public static boolean checkConfigElement(
        final IConfigurationElement configElement,
        final String expectedName,
        final String extensionPointName) {
        if (!expectedName.equals(configElement.getName())) {
            final String messageFormat = "invalid element [{0}] {1}"; //$NON-NLS-1$
            final String message = MessageFormat.format(
                messageFormat,
                configElement.getName(),
                Utils.messageFor(configElement, extensionPointName));

            SupportManager.log(IStatus.WARNING, message);
            return false;
        }
        return true;
    }

    public static String getRequiredAttribute(
        final IConfigurationElement configElement,
        final String attributeName,
        final String extensionPointName) {
        final String s = configElement.getAttribute(attributeName);
        if (s == null) {
            final String messageFormat = "element [{0}] is missing required attribute [{1}] {2}"; //$NON-NLS-1$
            final String message = MessageFormat.format(
                messageFormat,
                configElement.getName(),
                attributeName,
                Utils.messageFor(configElement, extensionPointName));

            SupportManager.log(IStatus.WARNING, message);
        }
        return s;
    }

    public static Boolean getRequiredBooleanAttribute(
        final IConfigurationElement configElement,
        final String attributeName,
        final String extensionPointName) {
        final String s = getRequiredAttribute(configElement, attributeName, extensionPointName);
        if (s != null) {
            return Boolean.valueOf(s);
        }
        return null;
    }

    public static boolean getNonRequiredBooleanAttribute(
        final IConfigurationElement configElement,
        final String attributeName,
        final boolean defaultValue) {
        final String s = configElement.getAttribute(attributeName);
        if (s != null) {
            return Boolean.valueOf(s).booleanValue();
        }
        return defaultValue;
    }

    public static String getRequiredSubElementText(
        final IConfigurationElement configElement,
        final String subElementName,
        final String extensionPointName) {
        final IConfigurationElement[] children = configElement.getChildren();

        for (int i = 0; i < children.length; i++) {
            final IConfigurationElement child = children[i];
            if (subElementName.equals(child.getName())) {
                return child.getValue();
            }
        }

        final String messageFormat = "element [{0}] is missing required sub element [{1}] {2}"; //$NON-NLS-1$
        final String message = MessageFormat.format(
            messageFormat,
            configElement.getName(),
            subElementName,
            Utils.messageFor(configElement, extensionPointName));

        SupportManager.log(IStatus.WARNING, message);
        return null;
    }

    public static String getNonRequiredSubElementText(
        final IConfigurationElement configElement,
        final String subElementName) {
        final IConfigurationElement[] children = configElement.getChildren();

        for (int i = 0; i < children.length; i++) {
            final IConfigurationElement child = children[i];
            if (subElementName.equals(child.getName())) {
                return child.getValue();
            }
        }

        return null;
    }

    public static ImageDescriptor getImageDescriptor(
        final IConfigurationElement configElement,
        final String attributeName,
        final String extensionPointName) {
        final String s = configElement.getAttribute(attributeName);
        if (s == null) {
            return null;
        }

        final String contributor = getContributorFor(configElement);

        return AbstractUIPlugin.imageDescriptorFromPlugin(contributor, s);
    }

    public static Object createExecutableExtension(
        final IConfigurationElement configElement,
        final String propertyName,
        final Class expectedType,
        final String extensionPointName) {
        Object obj;

        try {
            obj = configElement.createExecutableExtension(propertyName);
        } catch (final CoreException e) {
            final String messageFormat = "unable to instantiate executable extension [{0}] {1}"; //$NON-NLS-1$
            final String message =
                MessageFormat.format(messageFormat, propertyName, Utils.messageFor(configElement, extensionPointName));

            SupportManager.log(IStatus.WARNING, message, e);
            return null;
        }

        if (!expectedType.isAssignableFrom(obj.getClass())) {
            final String messageFormat = "expected type [{0}] but got [{1}] -- attribute [{2}] {3}"; //$NON-NLS-1$
            final String message = MessageFormat.format(
                messageFormat,
                expectedType.getName(),
                obj.getClass().getName(),
                propertyName,
                Utils.messageFor(configElement, extensionPointName));

            SupportManager.log(IStatus.WARNING, message);
            return null;
        }

        return obj;
    }
}
