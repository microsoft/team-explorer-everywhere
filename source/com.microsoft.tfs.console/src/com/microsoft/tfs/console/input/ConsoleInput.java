// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.console.input;

import java.io.InputStream;

/**
 * An input that just wraps standard input of the JVM's default console.
 */
public class ConsoleInput implements Input {
    /**
     * {@inheritDoc}
     */
    @Override
    public InputStream getInputStream() {
        return System.in;
    }
}
