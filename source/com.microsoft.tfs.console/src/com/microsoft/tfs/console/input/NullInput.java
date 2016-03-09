// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.console.input;

import java.io.InputStream;

/**
 * Implements {@link Input} but offers no functionality.
 */
public class NullInput implements Input {
    @Override
    public InputStream getInputStream() {
        return null;
    }
}
