// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.clc.vc.options;

import com.microsoft.tfs.client.clc.options.NoValueOption;

/**
 * Option to bypass check-in validation (useful for gated check-in builds where
 * you don't want to queue a build).
 */
public final class OptionBypass extends NoValueOption {
    public OptionBypass() {
        super();
    }
}
