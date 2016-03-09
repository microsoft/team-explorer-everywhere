// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.console;

import java.text.MessageFormat;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Display;

import com.microsoft.tfs.client.common.console.TFSEclipseConsole;
import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.TFSCommonUIClientPlugin;
import com.microsoft.tfs.client.common.ui.framework.console.Console;
import com.microsoft.tfs.client.common.ui.framework.console.ConsoleManager;
import com.microsoft.tfs.client.common.ui.framework.helper.UIHelpers;
import com.microsoft.tfs.client.common.ui.framework.image.ImageHelper;
import com.microsoft.tfs.client.common.ui.prefs.UIPreferenceConstants;

public class TFSConsole extends Console implements TFSEclipseConsole {
    private static final Log log = LogFactory.getLog(TFSConsole.class);

    public final static String WARNING_STREAM_ID = "warning"; //$NON-NLS-1$
    public final static String ERROR_STREAM_ID = "error"; //$NON-NLS-1$

    private static final ImageHelper imageHelper = new ImageHelper(TFSCommonUIClientPlugin.PLUGIN_ID);
    private final IPreferenceStore preferenceStore = TFSCommonUIClientPlugin.getDefault().getPreferenceStore();

    /*
     * Keep a lock and a disposed flag - in theory, we could be disposed before
     * the UI thread ever gets around to executing our runnable in the ctor.
     */
    private final Object lock = new Object();
    private boolean disposed = false;
    private Color red;

    public TFSConsole() {
        super(
            Messages.getString("TFSConsole.TfsMessagesTitle"), //$NON-NLS-1$
            "tfs", //$NON-NLS-1$
            imageHelper.getImageDescriptor("/images/logos/16x16_transparent.png")); //$NON-NLS-1$

        final String messageFormat = "constructed {0}, scheduling UI thread runnable to configure streams"; //$NON-NLS-1$
        final String message = MessageFormat.format(messageFormat, this);
        log.debug(message);

        UIHelpers.runOnUIThread(true, new Runnable() {
            @Override
            public void run() {
                log.debug("configuring streams"); //$NON-NLS-1$

                synchronized (lock) {
                    if (!disposed) {
                        red = Display.getDefault().getSystemColor(SWT.COLOR_RED);
                    }
                }

                addStream(WARNING_STREAM_ID);
                addStream(ERROR_STREAM_ID, red);

                log.debug("streams configured"); //$NON-NLS-1$
            }
        });
    }

    /**
     * Prints a message to the console. The console is not raised.
     *
     * @param message
     *        The message to print
     */
    @Override
    public void printMessage(final String message) {
        printMessage(message, preferenceStore.getBoolean(UIPreferenceConstants.CONSOLE_SHOW_ON_NEW_MESSAGE));
    }

    /**
     * Prints a message to the console and optionally raise the console.
     *
     * @param message
     *        The message to print
     * @param showConsole
     *        <code>true</code> to show the console
     */
    private void printMessage(final String message, final boolean showConsole) {
        final String messageFormat = "default stream: {0}"; //$NON-NLS-1$
        final String messageLog = MessageFormat.format(messageFormat, message);
        log.debug(messageLog);

        getDefaultStream().println(message);

        if (showConsole) {
            showConsole();
        }
    }

    /**
     * Prints a warning message to the console. The console will be raised if it
     * is not visible.
     *
     * @param message
     *        The message to print
     */
    @Override
    public void printWarning(final String message) {
        printWarning(message, preferenceStore.getBoolean(UIPreferenceConstants.CONSOLE_SHOW_ON_NEW_WARNING));
    }

    /**
     * Prints a warning message to the console. The console will be raised if it
     * is not visible.
     *
     * @param message
     *        The message to print
     * @param showConsole
     *        <code>true</code> to show the console
     */
    private void printWarning(final String message, final boolean showConsole) {
        final String messageFormat = "warning stream: {0}"; //$NON-NLS-1$
        final String messageLog = MessageFormat.format(messageFormat, message);
        log.debug(messageLog);

        getStream(WARNING_STREAM_ID).println(message);

        if (showConsole) {
            showConsole();
        }
    }

    /**
     * Prints an error message to the console. The console will be raised if it
     * is not visible.
     *
     * @param message
     *        The message to print
     */
    @Override
    public void printErrorMessage(final String message) {
        printErrorMessage(message, preferenceStore.getBoolean(UIPreferenceConstants.CONSOLE_SHOW_ON_NEW_ERROR));
    }

    /**
     * Prints an error message to the console. The console will be raised if it
     * is not visible.
     *
     * @param message
     *        The message to print
     * @param showConsole
     *        <code>true</code> to show the console
     */
    private void printErrorMessage(final String errorMessage, final boolean showConsole) {
        final String messageFormat = "error stream: {0}"; //$NON-NLS-1$
        final String messageLog = MessageFormat.format(messageFormat, errorMessage);
        log.debug(messageLog);

        getStream(ERROR_STREAM_ID).println(errorMessage);

        if (showConsole) {
            showConsole();
        }
    }

    @Override
    public void showConsole() {
        ConsoleManager.getDefault().showConsoleView(this);
    }

    @Override
    public void dispose() {
        super.dispose();

        synchronized (lock) {
            if (red != null) {
                red.dispose();
            }

            disposed = true;
        }

        imageHelper.dispose();
    }
}
