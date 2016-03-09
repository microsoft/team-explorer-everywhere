// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.rules;

import com.microsoft.tfs.core.clients.workitem.exceptions.WorkItemException;
import com.microsoft.tfs.core.clients.workitem.internal.metadata.SpecialConstantIDs;

public class UnhandledSpecialConstantIDException extends WorkItemException {
    private static final long serialVersionUID = -1807367214426151839L;

    private final int constantId;
    private final Rule rule;

    public UnhandledSpecialConstantIDException(final int id, final Rule rule, final String subMessage) {
        super(SpecialConstantIDs.makeErrorMessage(id, rule, subMessage));
        constantId = id;
        this.rule = rule;
    }

    public int getConstantID() {
        return constantId;
    }

    public Rule getRule() {
        return rule;
    }
}
