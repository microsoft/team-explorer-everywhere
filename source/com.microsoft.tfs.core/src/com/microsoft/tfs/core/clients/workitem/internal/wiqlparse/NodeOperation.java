// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.wiqlparse;

public class NodeOperation extends NodeItem {
    public NodeOperation(final String value) {
        super(NodeType.OPERATION, value);
    }

    @Override
    public DataType getDataType() {
        return DataType.UNKNOWN;
    }

    @Override
    public boolean isConst() {
        return true;
    }

}
