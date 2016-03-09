// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.externaltools;

import java.io.File;
import java.util.Arrays;

import com.microsoft.tfs.core.externaltools.formatters.CompareToolArgumentFormatter;

import junit.framework.TestCase;

public class ExternalToolsetTest extends TestCase {
    public void testEmptyToolset() {
        final ExternalToolset toolset = new ExternalToolset();
        assertEquals(0, toolset.size());
    }

    public void testOneToolset() {
        final ExternalToolset toolset = new ExternalToolset();
        toolset.addAssociation(new ExternalToolAssociation(new String[] {
            "java" //$NON-NLS-1$
        }, new ExternalTool("diffmerge %1 %2"))); //$NON-NLS-1$

        assertEquals(1, toolset.size());
        assertEquals("diffmerge", toolset.findTool("fun.java").getCommand()); //$NON-NLS-1$ //$NON-NLS-2$
        assertEquals("diffmerge", toolset.findTool(".java").getCommand()); //$NON-NLS-1$ //$NON-NLS-2$

        assertTrue(Arrays.equals(new String[] {
            "a", //$NON-NLS-1$
            "b" //$NON-NLS-1$
        }, new CompareToolArgumentFormatter().formatArguments(toolset.findTool(".java"), new String[] //$NON-NLS-1$
        {
            "a", //$NON-NLS-1$
            "b" //$NON-NLS-1$
        })));

        assertNull("should be no matches for this extension", toolset.findTool("ack.unknownExtension")); //$NON-NLS-1$ //$NON-NLS-2$
    }

    public void testMultiToolset() {
        final ExternalToolset toolset = new ExternalToolset();
        toolset.addAssociation(new ExternalToolAssociation(new String[] {
            "java" //$NON-NLS-1$
        }, new ExternalTool("diffmerge %1 %2"))); //$NON-NLS-1$

        toolset.addAssociation(new ExternalToolAssociation(new String[] {
            "txt" //$NON-NLS-1$
        }, new ExternalTool("woo %1 %2"))); //$NON-NLS-1$

        assertEquals(2, toolset.size());

        assertEquals("diffmerge", toolset.findTool("fun.java").getCommand()); //$NON-NLS-1$ //$NON-NLS-2$
        assertEquals("woo", toolset.findTool("fun.txt").getCommand()); //$NON-NLS-1$ //$NON-NLS-2$

        assertNull("should be no matches for this extension", toolset.findTool("ack.unknownExtension")); //$NON-NLS-1$ //$NON-NLS-2$

        try {
            toolset.findTool((String) null);
            assertTrue(false);
        } catch (final Exception e) {
        }

        try {
            toolset.findTool((File) null);
            assertTrue(false);
        } catch (final Exception e) {
        }
    }

    public void testExtensionCaseInsensitivity() {
        final ExternalToolset toolset = new ExternalToolset();

        toolset.addAssociation(new ExternalToolAssociation(new String[] {
            "JAVA" //$NON-NLS-1$
        }, new ExternalTool("diffmerge %1%2"))); //$NON-NLS-1$

        assertEquals(1, toolset.size());
        assertEquals("diffmerge", toolset.findTool("fun.java").getCommand()); //$NON-NLS-1$ //$NON-NLS-2$
        assertEquals("diffmerge", toolset.findTool("fun.JaVa").getCommand()); //$NON-NLS-1$ //$NON-NLS-2$
        assertEquals("diffmerge", toolset.findTool("fun.Java").getCommand()); //$NON-NLS-1$ //$NON-NLS-2$
        assertEquals("diffmerge", toolset.findTool("fun.JAVA").getCommand()); //$NON-NLS-1$ //$NON-NLS-2$
    }

    public void testFindWildcard() {
        final ExternalToolset toolset = new ExternalToolset();

        toolset.addAssociation(new ExternalToolAssociation(new String[] {
            "java" //$NON-NLS-1$
        }, new ExternalTool("diffmerge %1%2"))); //$NON-NLS-1$

        toolset.addAssociation(new ExternalToolAssociation(new String[] {
            "txt" //$NON-NLS-1$
        }, new ExternalTool("woo %1%2"))); //$NON-NLS-1$

        assertNull(toolset.findTool("some.other.unknownExtension")); //$NON-NLS-1$

        toolset.addAssociation(new ExternalToolAssociation(new String[] {
            ExternalToolset.WILDCARD_EXTENSION
        }, new ExternalTool("wild %1%2"))); //$NON-NLS-1$

        assertEquals("diffmerge", toolset.findTool("some.java").getCommand()); //$NON-NLS-1$ //$NON-NLS-2$
        assertEquals("woo", toolset.findTool("some.txt").getCommand()); //$NON-NLS-1$ //$NON-NLS-2$
        assertEquals("wild", toolset.findTool("some.other.unknownExtension").getCommand()); //$NON-NLS-1$ //$NON-NLS-2$
    }

    public void testFindDirectory() {
        final ExternalToolset toolset = new ExternalToolset();

        toolset.addAssociation(new ExternalToolAssociation(new String[] {
            "java" //$NON-NLS-1$
        }, new ExternalTool("diffmerge %1%2"))); //$NON-NLS-1$

        toolset.addAssociation(new ExternalToolAssociation(new String[] {
            "txt" //$NON-NLS-1$
        }, new ExternalTool("woo %1%2"))); //$NON-NLS-1$

        assertNull(toolset.findToolForDirectory());

        toolset.addAssociation(new ExternalToolAssociation(new String[] {
            ExternalToolset.DIRECTORY_EXTENSION
        }, new ExternalTool("diffmerge %1%2"))); //$NON-NLS-1$

        assertEquals("diffmerge", toolset.findToolForDirectory().getCommand()); //$NON-NLS-1$
    }

    public void testFindWildcardAndDirectory() {
        final ExternalToolset toolset = new ExternalToolset();

        toolset.addAssociation(new ExternalToolAssociation(new String[] {
            "java" //$NON-NLS-1$
        }, new ExternalTool("diffmerge %1%2"))); //$NON-NLS-1$

        toolset.addAssociation(new ExternalToolAssociation(new String[] {
            "txt" //$NON-NLS-1$
        }, new ExternalTool("woo %1%2"))); //$NON-NLS-1$

        assertNull(toolset.findTool("some.other.unknownExtension")); //$NON-NLS-1$
        assertNull(toolset.findToolForDirectory());

        toolset.addAssociation(new ExternalToolAssociation(new String[] {
            ExternalToolset.WILDCARD_EXTENSION
        }, new ExternalTool("wild %1%2"))); //$NON-NLS-1$

        toolset.addAssociation(new ExternalToolAssociation(new String[] {
            ExternalToolset.DIRECTORY_EXTENSION
        }, new ExternalTool("diffmerge %1%2"))); //$NON-NLS-1$

        assertEquals("diffmerge", toolset.findTool("some.java").getCommand()); //$NON-NLS-1$ //$NON-NLS-2$
        assertEquals("woo", toolset.findTool("some.txt").getCommand()); //$NON-NLS-1$ //$NON-NLS-2$
        assertEquals("wild", toolset.findTool("some.other.unknownExtension").getCommand()); //$NON-NLS-1$ //$NON-NLS-2$
    }
}
