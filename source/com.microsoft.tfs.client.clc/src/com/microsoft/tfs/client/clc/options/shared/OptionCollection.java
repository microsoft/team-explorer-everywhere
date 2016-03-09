// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.clc.options.shared;

import com.microsoft.tfs.client.clc.options.URIValueOption;

/**
 * Introduced in TFS 2010, supersedes {@link OptionServer}. {@link OptionServer}
 * is incompatible with {@link OptionCollection}, and code which uses either
 * should error if both are present.
 */
public class OptionCollection extends URIValueOption {
    public OptionCollection() {
        super();
    }
}
