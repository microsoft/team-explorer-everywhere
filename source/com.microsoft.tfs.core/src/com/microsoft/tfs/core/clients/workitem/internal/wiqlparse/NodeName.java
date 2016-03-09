// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.wiqlparse;

public class NodeName extends NodeItem {
    public NodeName(final String string) {
        super(NodeType.NAME, string);
    }

    @Override
    public void appendTo(final StringBuffer b) {
        Tools.AppendName(b, super.getValue());
    }

    @Override
    public DataType getDataType() {
        return DataType.UNKNOWN;
    }

    @Override
    public boolean isConst() {
        return false;
    }
}
