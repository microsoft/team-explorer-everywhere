// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.externaltools;

import com.microsoft.tfs.core.externaltools.formatters.CompareToolArgumentFormatter;

import junit.framework.TestCase;

public class ExternalToolTest extends TestCase {
    public void testSimpleConstruction() {
        new ExternalTool("command %1 %2"); //$NON-NLS-1$
        new ExternalTool("command -1 %1 -2 %2"); //$NON-NLS-1$
        new ExternalTool("command %1 %2"); //$NON-NLS-1$
        new ExternalTool("command -1 %1 -2 %2"); //$NON-NLS-1$
    }

    public void testOriginalCommandAndArguments() {
        final String original = "fun and bar %1 %2 \"%3\" \"ack\"\"blah\"     zip"; //$NON-NLS-1$
        final ExternalTool tool = new ExternalTool(original);
        assertEquals(original, tool.getOriginalCommandAndArguments());
    }

    public void testBadConstruction() {
        try {
            new ExternalTool(null);
            assertTrue("should have thrown for null string", false); //$NON-NLS-1$
        } catch (final Exception e) {
        }

        try {
            new ExternalTool(""); //$NON-NLS-1$
            assertTrue("should have thrown for empty string", false); //$NON-NLS-1$
        } catch (final Exception e) {
        }

        try {
            new ExternalTool("    "); //$NON-NLS-1$
            assertTrue("should have thrown for no command or arguments", false); //$NON-NLS-1$
        } catch (final Exception e) {
        }
    }

    public void testCommand() {
        ExternalTool tool = new ExternalTool("command %1 %2"); //$NON-NLS-1$
        assertEquals("command", tool.getCommand()); //$NON-NLS-1$

        tool = new ExternalTool("command %1 %2"); //$NON-NLS-1$
        assertEquals("command", tool.getCommand()); //$NON-NLS-1$
    }

    public void testFormatArguments() {
        ExternalTool tool = new ExternalTool("command %1 %2"); //$NON-NLS-1$
        String[] args = new CompareToolArgumentFormatter().formatArguments(tool, "a", "b", "c", "d"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
        assertEquals(2, args.length);
        assertEquals("a", args[0]); //$NON-NLS-1$
        assertEquals("b", args[1]); //$NON-NLS-1$

        tool = new ExternalTool("command -1 %1 -2 %2 extra"); //$NON-NLS-1$
        args = new CompareToolArgumentFormatter().formatArguments(tool, "fun", "bar", "c", "d"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
        assertEquals(5, args.length);
        assertEquals("-1", args[0]); //$NON-NLS-1$
        assertEquals("fun", args[1]); //$NON-NLS-1$
        assertEquals("-2", args[2]); //$NON-NLS-1$
        assertEquals("bar", args[3]); //$NON-NLS-1$
        assertEquals("extra", args[4]); //$NON-NLS-1$
    }

    public void testArgumentQuoting() {
        ExternalTool tool = new ExternalTool("\"command with spaces\" \"%1 %2\""); //$NON-NLS-1$
        String[] args = new CompareToolArgumentFormatter().formatArguments(tool, "a", "b", "c", "d"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
        assertEquals(1, args.length);
        assertEquals("a b", args[0]); //$NON-NLS-1$

        /*
         * Quote embedded in argument.
         */
        tool = new ExternalTool("\"command with spaces\" \"%1 \"\" %2\""); //$NON-NLS-1$
        args = new CompareToolArgumentFormatter().formatArguments(tool, "a", "b", "c", "d"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
        assertEquals(1, args.length);
        assertEquals("a \" b", args[0]); //$NON-NLS-1$
    }
}
