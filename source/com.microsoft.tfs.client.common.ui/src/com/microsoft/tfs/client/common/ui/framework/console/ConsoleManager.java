// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.framework.console;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleManager;

/**
 * This is a container for all Consoles created by users. It proxies calls to
 * the Eclipse ConsoleManager. This primarily exists so that Commands in here
 * can write to our product's Console. =/
 */
public class ConsoleManager {
    private static ConsoleManager instance;

    private final Object consoleLock = new Object();

    private Console defaultConsole;
    private final List<Console> consoleList = new ArrayList<Console>();

    public synchronized static ConsoleManager getDefault() {
        if (instance == null) {
            instance = new ConsoleManager();
        }

        return instance;
    }

    public void addConsole(final Console console) {
        synchronized (consoleLock) {
            if (!consoleList.contains(console)) {
                consoleList.add(console);
            }
        }

        getEclipseConsoleManager().addConsoles(new IConsole[] {
            console.getMessageConsole()
        });
    }

    public void setDefaultConsole(final Console console) {
        synchronized (consoleLock) {
            defaultConsole = console;
        }
    }

    public Console getDefaultConsole() {
        synchronized (consoleLock) {
            if (defaultConsole != null) {
                return defaultConsole;
            }

            if (consoleList.size() > 0) {
                return consoleList.get(0);
            }
        }

        return null;
    }

    public void warnOfContentChanges(final Console console) {
        if (console.isDisposed()) {
            throw new IllegalStateException("Console is disposed"); //$NON-NLS-1$
        }

        if (!console.isEnabled()) {
            return;
        }

        getEclipseConsoleManager().warnOfContentChange(console.getMessageConsole());
    }

    public void showConsoleView(final Console console) {
        if (console.isDisposed()) {
            throw new IllegalStateException("Console is disposed"); //$NON-NLS-1$
        }

        if (!console.isEnabled()) {
            return;
        }

        getEclipseConsoleManager().showConsoleView(console.getMessageConsole());
    }

    public void removeConsole(final Console console) {
        synchronized (consoleLock) {
            consoleList.remove(console);
        }

        getEclipseConsoleManager().removeConsoles(new IConsole[] {
            console.getMessageConsole()
        });
    }

    private IConsoleManager getEclipseConsoleManager() {
        return ConsolePlugin.getDefault().getConsoleManager();
    }
}
