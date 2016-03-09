// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.util;

import java.util.regex.Pattern;

import junit.framework.TestCase;

public class LicenseTextTest extends TestCase {
    public void testReadLicenseText() {
        final String licenseText = EULAText.getEULAText();
        System.out.println(licenseText);

        /*
         * Pattern catches RTM ("MICROSOFT SOFTWARE LICENSE TERMS") and
         * Pre-release license text (
         * "MICROSOFT PRE-RELEASE SOFTWARE LICENSE TERMS"), which will appear on
         * the first line. DOTALL lets ".*" match newlines.
         */
        assertTrue(Pattern.compile(
            "^MICROSOFT (PRE-RELEASE )?SOFTWARE LICENSE TERMS$.*", //$NON-NLS-1$
            Pattern.DOTALL | Pattern.MULTILINE).matcher(licenseText).matches());
    }
}
