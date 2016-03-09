// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.clc.exceptions;

/**
 *         Thrown when an invalid workspace was specified as an option, or no
 *         option was specified and the correct workspace cannot be determined
 *         automatically from item paths supplied.
 */
public final class CannotFindWorkspaceException extends ArgumentException {
    private static final long serialVersionUID = 1336265329933416837L;

    public CannotFindWorkspaceException(final String message) {
        super(message);
    }
}
