// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.wiqlparse;

public class NodeMode extends NodeVariableList {

    protected NodeMode() {
        super(NodeType.MODE);
    }

    @Override
    public Priority getPriority() {
        return Priority.COMMA_OPERATOR;
    }

}
