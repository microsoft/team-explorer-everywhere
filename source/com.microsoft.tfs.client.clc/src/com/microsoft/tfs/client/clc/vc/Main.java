// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.clc.vc;

import java.lang.Thread.UncaughtExceptionHandler;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.microsoft.tfs.client.clc.Application;
import com.microsoft.tfs.client.clc.CommandsMap;
import com.microsoft.tfs.client.clc.ExitCode;
import com.microsoft.tfs.client.clc.OptionsMap;
import com.microsoft.tfs.client.common.logging.TELoggingConfiguration;
import com.microsoft.tfs.util.shutdown.ShutdownManager;

/**
 *         The entry point for the command-line client.
 */
public class Main extends Application {
    public static void main(final String[] args) {
        TELoggingConfiguration.configure();

        final Log log = LogFactory.getLog(Main.class);
        log.debug("Entering Main"); //$NON-NLS-1$
        Thread.currentThread().setUncaughtExceptionHandler(new UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(final Thread t, final Throwable e) {
                log.error("Unhandled exception in the thread " + t.getName() + " : ", e); //$NON-NLS-1$ //$NON-NLS-2$
                /*
                 * Let the shutdown manager clean up any registered items.
                 */
                try {
                    log.debug("Shutting down"); //$NON-NLS-1$
                    ShutdownManager.getInstance().shutdown();
                    log.debug("Has shut down"); //$NON-NLS-1$
                } catch (final Exception ex) {
                    log.error("Unhandled exception during shutdown: ", ex); //$NON-NLS-1$
                }
            }
        });
        /*
         * Please don't do any fancy work in this method, because we have a CLC
         * test harness that calls Main.run() directly, and we can't test
         * functionality in this method (because this method can't return a
         * status code but exits the process directly, which kind of hoses any
         * test framework).
         */

        final Main main = new Main();
        int ret = ExitCode.FAILURE;

        try {
            ret = main.run(args);
        } catch (final Throwable e) {
            log.error("Unhandled exception reached Main: ", e); //$NON-NLS-1$
        } finally {
            /*
             * Let the shutdown manager clean up any registered items.
             */
            try {
                log.info("Shutting down"); //$NON-NLS-1$
                ShutdownManager.getInstance().shutdown();
                log.info("Has shut down"); //$NON-NLS-1$
            } catch (final Exception e) {
                log.error("Unhandled exception during shutdown: ", e); //$NON-NLS-1$
            }
        }

        System.exit(ret);
    }

    /**
     * Constructs a CLC. See run() for all the interesting stuff.
     */
    public Main() {
    }

    @Override
    protected CommandsMap createCommandsMap() {
        return new VersionControlCommands();
    }

    @Override
    protected OptionsMap createOptionsMap() {
        return new VersionControlOptions();
    }
}
