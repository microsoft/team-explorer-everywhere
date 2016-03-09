// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.console.input;

import java.io.InputStream;

/**
 * Command-line client input.
 */
public interface Input {
    /**
     * @return the input stream supplying input.
     */
    public InputStream getInputStream();
}
