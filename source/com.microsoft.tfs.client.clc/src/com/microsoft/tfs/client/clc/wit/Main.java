// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.clc.wit;

import com.microsoft.tfs.client.clc.Application;
import com.microsoft.tfs.client.clc.CommandsMap;
import com.microsoft.tfs.client.clc.OptionsMap;
import com.microsoft.tfs.client.common.logging.TELoggingConfiguration;
import com.microsoft.tfs.util.shutdown.ShutdownManager;

/**
 *         The entry point for the command-line client.
 */
public class Main extends Application {
    public static void main(final String[] args) {
        TELoggingConfiguration.configure();

        /*
         * Please don't do any fancy work in this method, because we have a CLC
         * test harness that calls Main.run() directly, and we can't test
         * functionality in this method (because this method can't return a
         * status code but exits the process directly, which kind of hoses any
         * test framework).
         */

        final Main main = new Main();
        final int ret = main.run(args);

        /*
         * Let the shutdown manager clean up any registered items.
         */
        ShutdownManager.getInstance().shutdown();

        System.exit(ret);
    }

    /**
     * Constructs a CLC. See run() for all the interesting stuff.
     */
    public Main() {
    }

    @Override
    protected CommandsMap createCommandsMap() {
        return new WorkItemCommands();
    }

    @Override
    protected OptionsMap createOptionsMap() {
        return new WorkItemOptions();
    }
}
