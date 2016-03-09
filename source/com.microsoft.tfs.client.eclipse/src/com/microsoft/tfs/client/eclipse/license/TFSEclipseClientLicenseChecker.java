// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse.license;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.microsoft.tfs.client.common.license.LicenseManager;
import com.microsoft.tfs.client.eclipse.Messages;

/**
 * Checks that the running product is licensed. May be subclassed to prompt the
 * user for information during
 * {@link TFSEclipseClientLicenseChecker#isLicensed()}.
 *
 * @threadsafety unknown
 */
public class TFSEclipseClientLicenseChecker {
    public static final Log log = LogFactory.getLog(TFSEclipseClientLicenseChecker.class);

    /**
     * Queries the license manager to see if the product is licensed: that is,
     * the EULA has been accepted and a valid product id is installed.
     *
     * @return true if we should continue, false if we should fail due to
     *         license errors
     */
    public boolean isLicensed() {
        if (!LicenseManager.getInstance().isEULAAccepted()) {
            log.error(Messages.getString("TFSEclipseClientLicenseProvider.EULANotAccepted")); //$NON-NLS-1$
            return false;
        }

        return true;
    }
}
