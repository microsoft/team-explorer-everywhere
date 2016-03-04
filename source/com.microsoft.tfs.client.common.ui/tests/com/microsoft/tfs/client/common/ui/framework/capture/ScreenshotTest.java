// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.framework.capture;

import java.io.File;

import junit.framework.TestCase;

public class ScreenshotTest extends TestCase {

    public void testSaveScreenShot() {
        final Screenshot screenshot = new Screenshot();
        final String screenshotDir = System.getProperty("java.io.tmpdir"); //$NON-NLS-1$
        final File saveFile = screenshot.saveScreenShot(screenshotDir, "TEE-test", 0); //$NON-NLS-1$

        System.out.println(saveFile.getAbsolutePath());

        assertTrue(saveFile.exists());

    }

}
