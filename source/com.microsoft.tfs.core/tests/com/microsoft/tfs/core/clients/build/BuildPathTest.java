// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.build;

import com.microsoft.tfs.core.clients.build.utils.BuildPath;

import junit.framework.TestCase;

public class BuildPathTest extends TestCase {

    public void testGetItemName() {
        assertEquals("ItemName", BuildPath.getItemName("\\TeamProject\\ItemName")); //$NON-NLS-1$ //$NON-NLS-2$
    }

    public void testGetItemNameForTeamProjectPath() {
        assertEquals("", BuildPath.getItemName("\\TeamProject")); //$NON-NLS-1$ //$NON-NLS-2$
    }

    public void testGetTeamProject() {
        assertEquals("TeamProject", BuildPath.getTeamProject("\\TeamProject\\ItemName")); //$NON-NLS-1$ //$NON-NLS-2$
    }

    public void testGetTeamProjectInvalidPath() {
        String errorMessage = null;
        try {
            BuildPath.getTeamProject("InvalidPath"); //$NON-NLS-1$
        } catch (final IllegalArgumentException e) {
            errorMessage = e.getMessage();
        } finally {
            assertNotNull("IllegalArgument exception expected for invalid path", errorMessage); //$NON-NLS-1$
            assertEquals("TF209019: The path \"InvalidPath\" must begin with a '\'.", errorMessage); //$NON-NLS-1$
        }
    }

    public void testGetTeamProjectForTeamProjectOnly() {
        assertEquals("TeamProject", BuildPath.getTeamProject("\\TeamProject")); //$NON-NLS-1$ //$NON-NLS-2$
    }

    public void testRoot() {
        final String a = "a"; //$NON-NLS-1$
        final String b = "b"; //$NON-NLS-1$
        final StringBuilder sb = new StringBuilder();
        String expected;
        String relative;

        sb.append(BuildPath.ROOT_FOLDER);
        sb.append(a);
        sb.append(BuildPath.PATH_SEPERATOR_CHAR);
        sb.append(b);
        expected = sb.toString();
        relative = b;

        // Should strip append relative as is.
        assertEquals(expected, BuildPath.root(a, relative));

        sb.setLength(0);
        sb.append(BuildPath.PATH_SEPERATOR_CHAR);
        sb.append(b);
        relative = sb.toString();

        // Should strip the leading path char from relative.
        assertEquals(expected, BuildPath.root(a, relative));

        sb.setLength(0);
        sb.append(BuildPath.PATH_SEPERATOR_CHAR);
        sb.append(BuildPath.PATH_SEPERATOR_CHAR);
        sb.append(b);
        relative = sb.toString();

        // Should strip all leading path chars from relative.
        assertEquals(expected, BuildPath.root(a, relative));
    }
}
