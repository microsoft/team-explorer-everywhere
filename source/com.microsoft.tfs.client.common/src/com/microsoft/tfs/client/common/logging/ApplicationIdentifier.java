// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.logging;

public class ApplicationIdentifier {
    public static final String EXPLORER = "explorer"; //$NON-NLS-1$
    public static final String PLUGIN = "plugin"; //$NON-NLS-1$
    public static final String CLC = "clc"; //$NON-NLS-1$
    public static final String ANT = "ant"; //$NON-NLS-1$
    public static final String CLC_TEST = "test-clc"; //$NON-NLS-1$

    public static final String UNKNOWN = "unknown"; //$NON-NLS-1$

    private static String application;

    public static synchronized String getApplication() {
        if (application == null) {
            application = computeApplication();
        }
        return application;
    }

    private static String computeApplication() {
        /*
         * Always honor the teamexplorer.application sysprop. If this has been
         * explicitly set, then we always report it to be our application.
         */
        final String teamExplorerApp = System.getProperty("teamexplorer.application"); //$NON-NLS-1$
        if (teamExplorerApp != null) {
            return teamExplorerApp;
        }

        /*
         * If the eclipse.product sysprop is set, this is the best way to
         * determine the application. It will be set for normal explorer and
         * teamplugin usage, but will NOT be set for explorer or teamplugin when
         * running an instance through the Eclipse IDE as of 3.2.
         */
        final String eclipseProduct = System.getProperty("eclipse.product"); //$NON-NLS-1$
        if (eclipseProduct != null) {
            return eclipseProduct.indexOf("explorer") == -1 ? PLUGIN : EXPLORER; //$NON-NLS-1$
        }

        /*
         * At this point classloader tricks can at least tell us if we're
         * running as ant or clc.
         */

        if (ableToLoadClass("com.microsoft.tfs.client.clc.vc.Main")) //$NON-NLS-1$
        {
            return CLC;
        }

        if (ableToLoadClass("com.microsoft.tfs.test.clc.TEETestRunner")) //$NON-NLS-1$
        {
            return CLC_TEST;
        }

        /*
         * we could likely be running explorer or teamplugin from within the
         * Eclipse IDE here, but I haven't found any reliable way to detect that
         * in all cases
         */
        return UNKNOWN;
    }

    private static boolean ableToLoadClass(final String className) {
        try {
            Thread.currentThread().getContextClassLoader().loadClass(className);
            return true;
        } catch (final Exception ex) {
            try {
                ApplicationIdentifier.class.getClassLoader().loadClass(className);
                return true;
            } catch (final Exception ex2) {

            }
        }

        return false;
    }
}
