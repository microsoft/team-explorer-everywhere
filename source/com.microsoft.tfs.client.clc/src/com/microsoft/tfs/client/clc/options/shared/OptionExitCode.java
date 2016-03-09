// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.clc.options.shared;

import com.microsoft.tfs.client.clc.options.NoValueOption;

/**
 *         When found on the command line, the exit code is printed to the
 *         display when the command line client exits. Not to be confused with
 *         the OptionListExitCodes that can be given to the Help command.
 */
public final class OptionExitCode extends NoValueOption {
    public OptionExitCode() {
        super();
    }
}
