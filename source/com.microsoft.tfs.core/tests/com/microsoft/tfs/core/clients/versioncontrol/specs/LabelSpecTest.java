// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.specs;

import junit.framework.TestCase;

public class LabelSpecTest extends TestCase {
    public void testConstruction() {
        // Valid constructions
        new LabelSpec("a", null); //$NON-NLS-1$
        new LabelSpec("a", "$/"); //$NON-NLS-1$ //$NON-NLS-2$
        new LabelSpec("a", "$/SomeProject"); //$NON-NLS-1$ //$NON-NLS-2$
        new LabelSpec("a", "$/SomeProject/folder"); //$NON-NLS-1$ //$NON-NLS-2$
        // Wildcards and special characters are valid for construction
        new LabelSpec("*.,2m,;lFLI#(*!", "$/SomeProject/folder"); //$NON-NLS-1$ //$NON-NLS-2$

        // Invalid arguments
        try {
            new LabelSpec(null, null);
            assertTrue("label should not be null", false); //$NON-NLS-1$
        } catch (final NullPointerException e) {
            // expected
        }

    }

    public void testParseFailures() {
        try {
            LabelSpec.parse(null, null, false);
            assertTrue("spec string must not be null", false); //$NON-NLS-1$
        } catch (final NullPointerException e) {
            // expected
        }

        try {
            LabelSpec.parse("", null, false); //$NON-NLS-1$
            assertTrue("spec string must not be empty", false); //$NON-NLS-1$
        } catch (final IllegalArgumentException e) {
            // expected
        }

        try {
            LabelSpec.parse("*", null, false); //$NON-NLS-1$
            assertTrue("wildcards not permitted", false); //$NON-NLS-1$
        } catch (final LabelSpecParseException e) {
            // expected
        }

        try {
            LabelSpec.parse("?", null, false); //$NON-NLS-1$
            assertTrue("wildcards not permitted", false); //$NON-NLS-1$
        } catch (final LabelSpecParseException e) {
            // expected
        }

        try {
            LabelSpec.parse("abcdef*", null, false); //$NON-NLS-1$
            assertTrue("wildcards not permitted", false); //$NON-NLS-1$
        } catch (final LabelSpecParseException e) {
            // expected
        }

        try {
            LabelSpec.parse("abcdef@$/Folder/*", null, false); //$NON-NLS-1$
            assertTrue("wildcards never permitted in scopes", false); //$NON-NLS-1$
        } catch (final LabelSpecParseException e) {
            // expected
        }

        try {
            LabelSpec.parse("abcdef@$/Folder/*", null, true); //$NON-NLS-1$
            assertTrue("wildcards never permitted in scopes", false); //$NON-NLS-1$
        } catch (final LabelSpecParseException e) {
            // expected
        }

        try {
            LabelSpec.parse("@$/Project", null, false); //$NON-NLS-1$
            assertTrue("missing label name)", false); //$NON-NLS-1$
        } catch (final LabelSpecParseException e) {
            // expected
        }

        try {
            LabelSpec.parse("abc@$", null, false); //$NON-NLS-1$
            assertTrue("invalid label scope (must be server path)", false); //$NON-NLS-1$
        } catch (final LabelSpecParseException e) {
            // expected
        }

        try {
            LabelSpec.parse("abc@def", null, false); //$NON-NLS-1$
            assertTrue("invalid label scope (must be server path)", false); //$NON-NLS-1$
        } catch (final LabelSpecParseException e) {
            // expected
        }

    }

    public void testParseSuccesses() {
        LabelSpec spec;

        // Name, no scope, wildcards and not (should be the same)
        spec = LabelSpec.parse("abc", null, false); //$NON-NLS-1$
        assertEquals("abc", spec.getLabel()); //$NON-NLS-1$
        assertNull(spec.getScope());

        spec = LabelSpec.parse("abc", null, true); //$NON-NLS-1$
        assertEquals("abc", spec.getLabel()); //$NON-NLS-1$
        assertNull(spec.getScope());

        // Empty scope should parse into null
        spec = LabelSpec.parse("abc@", null, false); //$NON-NLS-1$
        assertEquals("abc", spec.getLabel()); //$NON-NLS-1$
        assertNull(spec.getScope());

        // Fallback scope
        spec = LabelSpec.parse("abc", "$/Right", false); //$NON-NLS-1$ //$NON-NLS-2$
        assertEquals("abc", spec.getLabel()); //$NON-NLS-1$
        assertEquals("$/Right", spec.getScope()); //$NON-NLS-1$

        spec = LabelSpec.parse("abc@$/Right", "$/Wrong", false); //$NON-NLS-1$ //$NON-NLS-2$
        assertEquals("abc", spec.getLabel()); //$NON-NLS-1$
        assertEquals("$/Right", spec.getScope()); //$NON-NLS-1$

        // Scopes

        spec = LabelSpec.parse("abc@$/", null, false); //$NON-NLS-1$
        assertEquals("abc", spec.getLabel()); //$NON-NLS-1$
        assertEquals("$/", spec.getScope()); //$NON-NLS-1$

        spec = LabelSpec.parse("abc@$/Project", null, false); //$NON-NLS-1$
        assertEquals("abc", spec.getLabel()); //$NON-NLS-1$
        assertEquals("$/Project", spec.getScope()); //$NON-NLS-1$

        spec = LabelSpec.parse("abc@$/Project/folder", null, false); //$NON-NLS-1$
        assertEquals("abc", spec.getLabel()); //$NON-NLS-1$
        assertEquals("$/Project/folder", spec.getScope()); //$NON-NLS-1$

        // Wildcards in names
        spec = LabelSpec.parse("f?ile*", null, true); //$NON-NLS-1$
        assertEquals("f?ile*", spec.getLabel()); //$NON-NLS-1$
        assertNull(spec.getScope());
    }
}
