// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.clc;

import com.microsoft.tfs.client.clc.vc.commands.ScriptCommandExit;

public abstract class ExitCode {
    /*
     * If you add an exit code, it should be an integer in the range [0,255].
     * Windows uses a signed 32-bit integer as the exit code, but Unix uses a
     * single unsigned byte so they need to fit in that range.
     *
     * Make sure the value matches VS's tf.exe's exit status code if there's an
     * equivalent there.
     *
     * Also update Help.showExitCodes() with new text if you're adding a new
     * code that may be returned to the system!
     */

    // UNKNOWN is a Command's initial value; converted to SUCCESS when the
    // command has finished running if no other value was set.

    public static final int UNKNOWN = -1;

    // Visual Studio tf.exe standard values.

    public static final int SUCCESS = 0;
    public static final int PARTIAL_SUCCESS = 1;
    public static final int UNRECOGNIZED_COMMAND = 2;
    public static final int NOT_ATTEMPTED = 3;
    public static final int FAILURE = 100;

    // These are Team Explorer Everywhere additions.

    /**
     * This code is used internally by {@link ScriptCommandExit} when scripts
     * are being run. Always results in {@link #SUCCESS} being set as the
     * process's exit code.
     */
    public static final int SUCCESS_BUT_STOP_NOW = 70;
}
