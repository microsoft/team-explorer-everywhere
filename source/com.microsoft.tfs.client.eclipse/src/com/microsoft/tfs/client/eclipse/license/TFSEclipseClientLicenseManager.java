// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse.license;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.microsoft.tfs.client.common.util.ExtensionLoader;

public class TFSEclipseClientLicenseManager {
    private static final Log log = LogFactory.getLog(TFSEclipseClientLicenseManager.class);
    public static final String EXTENSION_POINT_ID = "com.microsoft.tfs.client.eclipse.licenseChecker"; //$NON-NLS-1$

    private static final Object licenseCheckerLock = new Object();
    private static TFSEclipseClientLicenseChecker licenseChecker;

    public static boolean isLicensed() {
        return getLicenseChecker().isLicensed();
    }

    private static final TFSEclipseClientLicenseChecker getLicenseChecker() {
        synchronized (licenseCheckerLock) {
            if (licenseChecker == null) {
                try {
                    licenseChecker =
                        (TFSEclipseClientLicenseChecker) ExtensionLoader.loadSingleExtensionClass(EXTENSION_POINT_ID);
                } catch (final Exception e) {
                    log.error("Could not load license checker extension, using default", e); //$NON-NLS-1$
                    licenseChecker = new TFSEclipseClientLicenseChecker();
                }
            }

            return licenseChecker;
        }
    }
}
