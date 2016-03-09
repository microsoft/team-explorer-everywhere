// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse.ui.history;

import java.lang.reflect.Method;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.IAdapterFactory;

public class TFSLocalHistoryAdapterFactory implements IAdapterFactory {
    private static final Log log = LogFactory.getLog(TFSLocalHistoryAdapterFactory.class);

    @Override
    @SuppressWarnings("rawtypes")
    public Object getAdapter(final Object adaptableObject, final Class adapterType) {
        /*
         * HACK: Eclipse local history view (GenericHistoryView) refuses to
         * allow the sync w/ editor button to work properly if there is a team
         * provider for a resource and it doesn't adapt IHistoryPageSource. So
         * we adapt to IHistoryPageSource and simply return the Local History
         * Provider page source (if possible.)
         */
        try {
            if (adapterType != null && adapterType.equals(org.eclipse.team.ui.history.IHistoryPageSource.class)) {
                final Class localHistoryClass =
                    Class.forName("org.eclipse.team.internal.ui.history.LocalHistoryPageSource"); //$NON-NLS-1$

                if (localHistoryClass != null) {
                    @SuppressWarnings("unchecked")
                    Method instanceMethod = null;
                    try {
                        instanceMethod = localHistoryClass.getMethod("getInstance"); //$NON-NLS-1$
                    } catch (final NoSuchMethodException e) {
                        // It might fail for older versions of eclipse
                        log.warn("Could not get method 'getIntance' for LocalHistoryPageSource class", e); //$NON-NLS-1$
                    }

                    Object historyPageSource = null;
                    if (instanceMethod != null) {
                        historyPageSource = instanceMethod.invoke(localHistoryClass);
                    } else {
                        // For back compatibility
                        historyPageSource = localHistoryClass.newInstance();
                    }
                    if (historyPageSource != null
                        && historyPageSource instanceof org.eclipse.team.ui.history.IHistoryPageSource) {
                        return historyPageSource;
                    }

                }
            }
        } catch (final Exception e) {
            log.warn("Could not obtain Eclipse local history page source provider", e); //$NON-NLS-1$
        }

        return null;
    }

    @Override
    @SuppressWarnings("rawtypes")
    public Class[] getAdapterList() {
        return new Class[] {
            org.eclipse.team.ui.history.IHistoryPageSource.class
        };
    }
}
