// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.util;

import java.text.MessageFormat;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;

import com.microsoft.tfs.util.Check;

/**
 * Utilities to simplify finding and loading executable content from other
 * plug-ins via extension points.
 *
 * @threadsafety unknown
 */
public abstract class ExtensionLoader {
    private ExtensionLoader() {
    }

    /**
     * @equivalence loadSingleExtensionClass(extensionPointID, true);
     */
    public static Object loadSingleExtensionClass(final String extensionPointID) {
        return loadSingleExtensionClass(extensionPointID, true);
    }

    /**
     * Finds exactly one contribution for the specified extension point ID and
     * instantiates an object for the <code>class</code> element in that
     * contribution.
     *
     * @param extensionPointID
     *        the extension point ID to load an extension for (must not be
     *        <code>null</code>)
     * @param throwForZeroElements
     *        if <code>true</code> and no elements that contribute to the
     *        specified ID were found, an exception is thrown; if false no error
     *        is thrown if no elements are found and <code>null</code> is
     *        returned
     * @return the instance of the type the
     *         <code>class</element> element specified, or <code>null</code> if
     *         throwForZeroElements is false and no elements were found
     * @throws RuntimeException
     *         if the extension could not be loaded (0 or > 1 contributions
     *         available, or class instantiation error)
     */
    public static Object loadSingleExtensionClass(final String extensionPointID, final boolean throwForZeroElements) {
        Check.notNull(extensionPointID, "extensionPointID"); //$NON-NLS-1$

        final IExtensionRegistry registry = Platform.getExtensionRegistry();
        final IExtensionPoint extensionPoint = registry.getExtensionPoint(extensionPointID);

        final IConfigurationElement[] elements = extensionPoint.getConfigurationElements();

        if (!throwForZeroElements && elements.length == 0) {
            return null;
        }

        if (elements.length == 0) {
            throw new RuntimeException(
                MessageFormat.format("No provider is configured for extension point id {0}", extensionPointID)); //$NON-NLS-1$
        } else if (elements.length > 1) {
            throw new RuntimeException(
                MessageFormat.format("Multiple providers are configured for extension point id {0}", extensionPointID)); //$NON-NLS-1$
        }

        try {
            final Object provider = elements[0].createExecutableExtension("class"); //$NON-NLS-1$

            if (provider == null) {
                throw new RuntimeException(
                    MessageFormat.format(
                        "Could not instantiate provider for extension point id {0}", //$NON-NLS-1$
                        extensionPointID));
            }

            return provider;
        } catch (final CoreException e) {
            final String message =
                MessageFormat.format("Could not instantiate provider for extension point id {0}", extensionPointID); //$NON-NLS-1$

            throw new RuntimeException(message, e);
        }
    }
}
