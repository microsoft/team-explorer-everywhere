// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.clc.exceptions;

/**
 *         A base class for all the argument parsing exceptions to extend.
 */
public abstract class ArgumentException extends Exception {

    public ArgumentException() {
        super();
    }

    public ArgumentException(final String msg) {
        super(msg);
    }

}
