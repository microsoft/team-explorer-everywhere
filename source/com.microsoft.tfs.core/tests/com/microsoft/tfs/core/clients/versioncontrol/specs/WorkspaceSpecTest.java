// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.specs;

import junit.framework.TestCase;

public class WorkspaceSpecTest extends TestCase {
    public void testConstruction() {
        // Valid constructions
        new WorkspaceSpec("a", null); //$NON-NLS-1$
        new WorkspaceSpec("a", "b"); //$NON-NLS-1$ //$NON-NLS-2$
        // Wildcards and special characters are valid for construction
        new WorkspaceSpec("!@#$%^&*()", "!@#$%^&*()"); //$NON-NLS-1$ //$NON-NLS-2$

        // Invalid arguments
        try {
            new WorkspaceSpec(null, null);
            assertTrue("name should not be null", false); //$NON-NLS-1$
        } catch (final NullPointerException e) {
            // expected
        }

        try {
            new WorkspaceSpec("", null); //$NON-NLS-1$
            assertTrue("name should not be empty", false); //$NON-NLS-1$
        } catch (final IllegalArgumentException e) {
            // expected
        }
    }

    public void testParseFailures() {
        try {
            WorkspaceSpec.parse(null, null, false);
            assertTrue("spec string must not be null", false); //$NON-NLS-1$
        } catch (final NullPointerException e) {
            // expected
        }

        try {
            WorkspaceSpec.parse("", null, false); //$NON-NLS-1$
            assertTrue("spec string must not be empty", false); //$NON-NLS-1$
        } catch (final IllegalArgumentException e) {
            // expected
        }

        try {
            WorkspaceSpec.parse(";", null, false); //$NON-NLS-1$
            assertTrue("name part missing", false); //$NON-NLS-1$
        } catch (final WorkspaceSpecParseException e) {
            // expected
        }

        try {
            WorkspaceSpec.parse("*", null, false); //$NON-NLS-1$
            assertTrue("wildcards not permitted", false); //$NON-NLS-1$
        } catch (final WorkspaceSpecParseException e) {
            // expected
        }

        try {
            WorkspaceSpec.parse("?", null, false); //$NON-NLS-1$
            assertTrue("wildcards not permitted", false); //$NON-NLS-1$
        } catch (final WorkspaceSpecParseException e) {
            // expected
        }

        try {
            WorkspaceSpec.parse("abcdef*", null, false); //$NON-NLS-1$
            assertTrue("wildcards not permitted", false); //$NON-NLS-1$
        } catch (final WorkspaceSpecParseException e) {
            // expected
        }

        try {
            WorkspaceSpec.parse(";owner", null, false); //$NON-NLS-1$
            assertTrue("missing workspace name)", false); //$NON-NLS-1$
        } catch (final WorkspaceSpecParseException e) {
            // expected
        }
    }

    public void testParseSuccesses() {
        WorkspaceSpec spec;

        // Name, no owner, wildcards and not (should be the same)
        spec = WorkspaceSpec.parse("abc", null, false); //$NON-NLS-1$
        assertEquals("abc", spec.getName()); //$NON-NLS-1$
        assertNull(spec.getOwner());

        spec = WorkspaceSpec.parse("abc", null, true); //$NON-NLS-1$
        assertEquals("abc", spec.getName()); //$NON-NLS-1$
        assertNull(spec.getOwner());

        // Empty owner should parse into null
        spec = WorkspaceSpec.parse("abc;", null, false); //$NON-NLS-1$
        assertEquals("abc", spec.getName()); //$NON-NLS-1$
        assertNull(spec.getOwner());

        // Fallback owner
        spec = WorkspaceSpec.parse("abc", "right", false); //$NON-NLS-1$ //$NON-NLS-2$
        assertEquals("abc", spec.getName()); //$NON-NLS-1$
        assertEquals("right", spec.getOwner()); //$NON-NLS-1$

        spec = WorkspaceSpec.parse("abc;right", "wrong", false); //$NON-NLS-1$ //$NON-NLS-2$
        assertEquals("abc", spec.getName()); //$NON-NLS-1$
        assertEquals("right", spec.getOwner()); //$NON-NLS-1$

        // Wildcards in names
        spec = WorkspaceSpec.parse("workspace?name*", null, true); //$NON-NLS-1$
        assertEquals("workspace?name*", spec.getName()); //$NON-NLS-1$
        assertNull(spec.getOwner());

        spec = WorkspaceSpec.parse("workspace?name*", "right", true); //$NON-NLS-1$ //$NON-NLS-2$
        assertEquals("workspace?name*", spec.getName()); //$NON-NLS-1$
        assertEquals("right", spec.getOwner()); //$NON-NLS-1$

        spec = WorkspaceSpec.parse("workspace?name*;right", null, true); //$NON-NLS-1$
        assertEquals("workspace?name*", spec.getName()); //$NON-NLS-1$
        assertEquals("right", spec.getOwner()); //$NON-NLS-1$

        spec = WorkspaceSpec.parse("workspace?name*;right", "wrong", true); //$NON-NLS-1$ //$NON-NLS-2$
        assertEquals("workspace?name*", spec.getName()); //$NON-NLS-1$
        assertEquals("right", spec.getOwner()); //$NON-NLS-1$
    }
}
