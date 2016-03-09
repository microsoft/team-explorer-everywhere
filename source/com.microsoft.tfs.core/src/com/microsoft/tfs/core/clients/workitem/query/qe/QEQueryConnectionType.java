// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.query.qe;

/**
 * @since TEE-SDK-10.1
 */
public class QEQueryConnectionType {
    public static final QEQueryConnectionType ACROSS = new QEQueryConnectionType(3);
    public static final QEQueryConnectionType DOWN = new QEQueryConnectionType(2);
    public static final QEQueryConnectionType NONE = new QEQueryConnectionType(0);
    public static final QEQueryConnectionType UP = new QEQueryConnectionType(1);

    private final int type;

    private QEQueryConnectionType(final int type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return String.valueOf(type);
    }
}
