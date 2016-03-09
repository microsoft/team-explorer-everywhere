// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.codemarker;

import java.util.ArrayList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;

/**
 * This class "dispatches" a code marker - basically notifying listeners that a
 * code marker has been hit.
 *
 * This will load eclipse extensions providing CodeMarkerListener(s), and notify
 * them of the code marker. When running the product generally, no code marker
 * listeners should be contributing extensions. When running in the test
 * harness, the test bridge will provide a code marker listener for its own use.
 */
public class CodeMarkerDispatch {
    public static final String EXTENSION_POINT_ID = "com.microsoft.tfs.client.common.codeMarkerListenerProviders"; //$NON-NLS-1$

    private static final Log log = LogFactory.getLog(CodeMarkerDispatch.class);

    private static final Object lock = new Object();
    private static CodeMarkerListener[] listeners = null;
    private static boolean listenersLoaded = false;

    /**
     * Notifies listeners that a code marker has been reached.
     *
     * @param event
     */
    public static void dispatch(final CodeMarker event) {
        synchronized (lock) {
            if (!listenersLoaded) {
                final IExtensionRegistry registry = Platform.getExtensionRegistry();
                final IExtensionPoint extensionPoint = registry.getExtensionPoint(EXTENSION_POINT_ID);

                final IConfigurationElement[] elements = extensionPoint.getConfigurationElements();

                final ArrayList<CodeMarkerListener> listenerList = new ArrayList<CodeMarkerListener>();
                for (int i = 0; i < elements.length; i++) {
                    try {
                        final CodeMarkerListenerProvider provider =
                            (CodeMarkerListenerProvider) elements[i].createExecutableExtension("class"); //$NON-NLS-1$

                        if (provider != null) {
                            final CodeMarkerListener listener = provider.getCodeMarkerListener();

                            if (listener != null) {
                                listenerList.add(listener);
                            }
                        }
                    } catch (final CoreException e) {
                        log.warn("Could not create " + EXTENSION_POINT_ID + " class", e); //$NON-NLS-1$ //$NON-NLS-2$
                    }
                }

                if (listenerList.size() > 0) {
                    listeners = listenerList.toArray(new CodeMarkerListener[listenerList.size()]);
                }

                listenersLoaded = true;
            }

            if (listeners != null) {
                for (int i = 0; i < listeners.length; i++) {
                    try {
                        listeners[i].onCodeMarker(event);
                    } catch (final Throwable t) {
                        log.warn("Exception while providing CodeMarker (" + event.toString() + ")", t); //$NON-NLS-1$ //$NON-NLS-2$
                    }
                }
            }
        }
    }
}
