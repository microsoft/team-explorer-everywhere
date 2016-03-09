// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.clc.options.shared;

import com.microsoft.tfs.client.clc.options.NoValueOption;

/**
 * Causes command file processing to continue processing after an error is
 * encountered. Normal behavior is to stop after the first error.
 */
public final class OptionContinueOnError extends NoValueOption {
    public OptionContinueOnError() {
        super();
    }
}
