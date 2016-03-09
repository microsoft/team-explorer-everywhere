// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.framework.diagnostics;

import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.swt.widgets.Shell;

import com.microsoft.tfs.client.common.ui.framework.diagnostics.cache.DataProviderCollection;
import com.microsoft.tfs.client.common.ui.framework.diagnostics.cache.SupportProvider;
import com.microsoft.tfs.client.common.ui.framework.diagnostics.ui.SupportDialog;

public class SupportFacade {
    private static final Log supportLog =
        LogFactory.getLog("com.microsoft.tfs.client.common.ui.diagnostics.SupportLog"); //$NON-NLS-1$

    public static Log getSupportLog() {
        return supportLog;
    }

    public static boolean openSupportDialog(
        final Shell shell,
        final ClassLoader callerClassLoader,
        final Map contextObjects) {
        final DataProviderCollection collection = InternalSupportUtils.createDataProviderCollection(shell);

        if (collection == null) {
            /*
             * cancelled
             */
            return false;
        }

        final SupportProvider supportProvider =
            SupportManager.getInstance().getSupportProviderCache().getSupportProvider();
        if (supportProvider == null) {
            throw new IllegalStateException();
        }

        final SupportDialog dialog =
            new SupportDialog(shell, supportProvider, collection, callerClassLoader, contextObjects);
        dialog.open();

        return true;
    }

    public static boolean promptAndPerformExport(final Shell shell) {
        final DataProviderCollection collection = InternalSupportUtils.createDataProviderCollection(shell);

        if (collection == null) {
            /*
             * cancelled
             */
            return false;
        }

        return InternalSupportUtils.promptAndPerformExport(shell, collection);
    }
}
