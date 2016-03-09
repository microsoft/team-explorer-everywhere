// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.framework.resources.command;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;

import com.microsoft.tfs.util.listeners.ListenerList;
import com.microsoft.tfs.util.listeners.SingleListenerFacade;
import com.microsoft.tfs.util.listeners.StandardListenerList;

/**
 * Loads all contributors to the resource changing command listener extension
 * point. Caches listeners for subsequent access.
 */
class ResourceChangingCommandListenerLoader {
    public static final String EXTENSION_POINT_ID = "com.microsoft.tfs.client.common.resourceChangingCommandListeners"; //$NON-NLS-1$

    private static final Log log = LogFactory.getLog(ResourceChangingCommandListenerLoader.class);

    private static final Object lock = new Object();
    private static SingleListenerFacade listener;

    /**
     * Returns a {@link SingleListenerFacade} of the resource changing listeners
     * by loading extension points and creating new listeners on demand.
     * Subsequent calls will used cached listener data.
     *
     * @return A {@link SingleListenerFacade} of
     *         {@link ResourceChangingCommandListener}s.
     */
    public static SingleListenerFacade getListener() {
        synchronized (lock) {
            if (listener == null) {
                final IExtensionRegistry registry = Platform.getExtensionRegistry();
                final IExtensionPoint extensionPoint = registry.getExtensionPoint(EXTENSION_POINT_ID);

                final IConfigurationElement[] elements = extensionPoint.getConfigurationElements();

                final ListenerList list = new StandardListenerList();

                for (int i = 0; i < elements.length; i++) {
                    try {
                        list.addListener(elements[i].createExecutableExtension("class")); //$NON-NLS-1$
                    } catch (final CoreException e) {
                        log.warn("Could not create " + EXTENSION_POINT_ID + " class", e); //$NON-NLS-1$ //$NON-NLS-2$
                    }
                }

                listener = new SingleListenerFacade(ResourceChangingCommandListener.class, list);
            }

            return listener;
        }
    }
}
