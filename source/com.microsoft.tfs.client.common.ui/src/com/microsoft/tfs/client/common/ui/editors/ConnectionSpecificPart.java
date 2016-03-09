// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.editors;

/**
 * Interface to be implementated by workbench parts that are tied to a specific
 * server connection.
 */
public interface ConnectionSpecificPart {
    /**
     * Method indicating if part should automatically close when the server
     * connection is changed.
     *
     * @return true if the part should be closed, false if not.
     */
    public boolean closeOnConnectionChange();

}
