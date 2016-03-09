// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core;

public class TFSConnectionClosedException extends RuntimeException {

    private static final long serialVersionUID = -1335233665096550808L;

    public TFSConnectionClosedException() {
        super("The TFSConnection is closed."); //$NON-NLS-1$
    }

}
