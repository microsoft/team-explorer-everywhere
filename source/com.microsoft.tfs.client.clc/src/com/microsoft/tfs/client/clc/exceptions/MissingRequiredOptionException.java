// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.clc.exceptions;

/**
 *         Thrown when a required option is missing. This doesn't sound logical,
 *         since an option should be optional (not required), but the Java CLC
 *         can't do some things on non-Windows platforms that the Microsoft
 *         client can do (like get existing Windows user credentials for login,
 *         so the -login option is required).
 *
 *         TODO In the future, look at code that can throw this exception and
 *         rework it to cope with the missing option data.
 */
public final class MissingRequiredOptionException extends ArgumentException {
    static final long serialVersionUID = 7447534879701026767L;

    public MissingRequiredOptionException() {
        super();
    }

    public MissingRequiredOptionException(final String message) {
        super(message);
    }
}
