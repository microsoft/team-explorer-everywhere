// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.clc.options.shared;

/**
 * This option is now deprecated in favor of {@link OptionCollection}, but still
 * exists for compatibility in scripts, etc.
 *
 * {@link OptionServer} is incompatible with {@link OptionCollection}, and code
 * which uses either should error if both are present.
 */
public final class OptionServer extends OptionCollection {
    public OptionServer() {
        super();
    }
}
