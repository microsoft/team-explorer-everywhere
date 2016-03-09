// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol;

import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Failure;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ItemType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.SeverityType;

import junit.framework.TestCase;

public class TeamProjectTest extends TestCase {
    public void testValidateChange() {
        // Bad arguments

        try {
            TeamProject.validateChange(null, null);
            assertTrue("should throw for null path", false); //$NON-NLS-1$
        } catch (final NullPointerException e) {
        }

        try {
            TeamProject.validateChange("", null); //$NON-NLS-1$
            assertTrue("should throw for empty path", false); //$NON-NLS-1$
        } catch (final IllegalArgumentException e) {
        }

        try {
            TeamProject.validateChange("$/Abc/Def", null); //$NON-NLS-1$
            assertTrue("should throw for null item type", false); //$NON-NLS-1$
        } catch (final NullPointerException e) {
        }

        // Invalid paths for changes

        Failure f;

        f = TeamProject.validateChange("$/", ItemType.FOLDER); //$NON-NLS-1$
        assertEquals(f.getSeverity(), SeverityType.ERROR);
        assertEquals(f.getCode(), FailureCodes.CANNOT_CHANGE_ROOT_FOLDER_EXCEPTION);

        f = TeamProject.validateChange("$/file.txt", ItemType.FILE); //$NON-NLS-1$
        assertEquals(f.getSeverity(), SeverityType.ERROR);
        assertEquals(f.getCode(), FailureCodes.CANNOT_CREATE_FILES_IN_ROOT_EXCEPTION);

        f = TeamProject.validateChange("$/Folder", ItemType.FOLDER); //$NON-NLS-1$
        assertEquals(f.getSeverity(), SeverityType.ERROR);
        assertEquals(f.getCode(), FailureCodes.INVALID_PROJECT_PENDING_CHANGE_EXCEPTION);

        // Valid changes

        assertNull(TeamProject.validateChange("$/Team Project/Folder", ItemType.FOLDER)); //$NON-NLS-1$
        assertNull(TeamProject.validateChange("$/Team Project/file.txt", ItemType.FILE)); //$NON-NLS-1$
        assertNull(TeamProject.validateChange("$/Team Project/Deeper/Folder", ItemType.FOLDER)); //$NON-NLS-1$
        assertNull(TeamProject.validateChange("$/Team Project/Deeper/file.txt", ItemType.FILE)); //$NON-NLS-1$
    }
}
