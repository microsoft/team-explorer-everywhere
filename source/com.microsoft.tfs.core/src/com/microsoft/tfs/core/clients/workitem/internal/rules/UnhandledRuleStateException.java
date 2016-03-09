// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.rules;

import java.text.MessageFormat;

import com.microsoft.tfs.core.clients.workitem.exceptions.WorkItemException;

public class UnhandledRuleStateException extends WorkItemException {
    private static final long serialVersionUID = -3709025025520504193L;

    private final Rule rule;

    public UnhandledRuleStateException(final Rule rule, final String subMessage) {
        super(MessageFormat.format(
            "unhandled rule state \"{0}\" (rule {1})", //$NON-NLS-1$
            subMessage,
            Integer.toString(rule.getRuleID())));
        this.rule = rule;
    }

    public Rule getRule() {
        return rule;
    }
}
