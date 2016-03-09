// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.jni.internal.console;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import com.microsoft.tfs.jni.AllNativeTests;

import junit.framework.TestCase;

public class NativeConsoleTest extends TestCase {
    private NativeConsole nativeImpl;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        this.nativeImpl = new NativeConsole();
    }

    public void testGetConsoleColumns() throws Exception {
        // In this context "interactive" means an interactive shell/terminal,
        // which we may not have via SSH exec
        if (AllNativeTests.interactiveTestsDisabled()) {
            return;
        }

        System.out.println("Console screen buffer has " //$NON-NLS-1$
            + nativeImpl.getConsoleColumns()
            + " columns (unable to test programmatically)"); //$NON-NLS-1$

        /*
         * This test fails in Eclipse because its console is odd sizes.
         */
        assertTrue(nativeImpl.getConsoleColumns() > 0);
        assertTrue(nativeImpl.getConsoleColumns() < 10000);
    }

    public void testGetConsoleRows() throws Exception {
        // In this context "interactive" means an interactive shell/terminal,
        // which we may not have via SSH exec
        if (AllNativeTests.interactiveTestsDisabled()) {
            return;
        }

        System.out.println(
            "Console window has " + nativeImpl.getConsoleRows() + " rows (unable to test programmatically)"); //$NON-NLS-1$ //$NON-NLS-2$

        /*
         * This test fails in Eclipse because its console is odd sizes.
         */
        assertTrue(nativeImpl.getConsoleRows() > 0);
        assertTrue(nativeImpl.getConsoleRows() < 10000);
    }

    public void testReadPasswordLine() throws Exception {
        if (AllNativeTests.interactiveTestsDisabled()) {
            return;
        }

        try {
            // Test echo off.

            System.out.println();
            System.out.print("Type \"fun\" (without quotes) and hit enter.\n" + "Ensure it does not echo. > "); //$NON-NLS-1$ //$NON-NLS-2$

            nativeImpl.disableEcho();

            BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
            String line = br.readLine();
            // Don't close br because it closes System.in.

            nativeImpl.enableEcho();

            System.out.println();
            assertEquals("Didn't get the correct password.", "fun", line); //$NON-NLS-1$ //$NON-NLS-2$
            System.out.println();

            // Test echo back on.

            System.out.println();
            System.out.print("Type \"fun\" (without quotes) and hit enter.\n" + "Ensure it DOES echo. > "); //$NON-NLS-1$ //$NON-NLS-2$

            br = new BufferedReader(new InputStreamReader(System.in));
            line = br.readLine();
            // Don't close br because it closes System.in.

            System.out.println();
            assertEquals("Didn't get the correct password", "fun", line); //$NON-NLS-1$ //$NON-NLS-2$
            System.out.println();

        } finally {
            /* An extra call doesn't hurt for safety. */
            nativeImpl.enableEcho();
        }

    }
}