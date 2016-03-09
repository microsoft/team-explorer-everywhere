// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teambuild;

import junit.framework.TestCase;

public class VersionControlHelperTest extends TestCase {
    public void testDriveRemovalLogic() {
        String path = "C:\\a\\b"; //$NON-NLS-1$
        final int drivePos = path.indexOf(':');
        if (drivePos >= 0) {
            path = path.substring(drivePos + 1);
        }
        assertEquals("\\a\\b", path); //$NON-NLS-1$
    }

    public void testNormalizeLocalPath() {
        assertEquals("C:\\temp", VersionControlHelper.normalizeLocalPath("C:\\temp\\")); //$NON-NLS-1$ //$NON-NLS-2$
        assertEquals("C:\\temp", VersionControlHelper.normalizeLocalPath("C:\\temp")); //$NON-NLS-1$ //$NON-NLS-2$
        assertEquals("C:\\", VersionControlHelper.normalizeLocalPath("C:\\")); //$NON-NLS-1$ //$NON-NLS-2$
        assertEquals("C:\\temp\\a.txt", VersionControlHelper.normalizeLocalPath("C:\\temp\\a.txt")); //$NON-NLS-1$ //$NON-NLS-2$
        assertEquals("C:\\temp\\a", VersionControlHelper.normalizeLocalPath("C:\\temp\\a\\")); //$NON-NLS-1$ //$NON-NLS-2$
        assertEquals("C:\\temp\\a", VersionControlHelper.normalizeLocalPath("C:\\temp\\a")); //$NON-NLS-1$ //$NON-NLS-2$
    }

    public void testIsPrefixedWithFolderPath() {
        assertEquals(true, VersionControlHelper.isPrefixedWithFolderPath("C:\\temp", "c:\\")); //$NON-NLS-1$ //$NON-NLS-2$
        assertEquals(true, VersionControlHelper.isPrefixedWithFolderPath("C:\\temp\\a.txt", "c:\\temp")); //$NON-NLS-1$ //$NON-NLS-2$
        assertEquals(true, VersionControlHelper.isPrefixedWithFolderPath("c:\\temp", "C:\\Temp")); //$NON-NLS-1$ //$NON-NLS-2$
        assertEquals(true, VersionControlHelper.isPrefixedWithFolderPath("C:\\temp\\", "c:\\temp\\")); //$NON-NLS-1$ //$NON-NLS-2$

        assertEquals(false, VersionControlHelper.isPrefixedWithFolderPath("C:\\temp", "c:\\temp\\blah")); //$NON-NLS-1$ //$NON-NLS-2$
        assertEquals(false, VersionControlHelper.isPrefixedWithFolderPath(
            "C:\\temp\\blah\\blah\\blah", //$NON-NLS-1$
            "c:\\temporary\\blah")); //$NON-NLS-1$
    }
}
