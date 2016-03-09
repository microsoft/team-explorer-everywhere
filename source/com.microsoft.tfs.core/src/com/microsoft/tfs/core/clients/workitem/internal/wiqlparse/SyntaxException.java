// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.wiqlparse;

import java.text.MessageFormat;

import com.microsoft.tfs.core.Messages;
import com.microsoft.tfs.core.clients.workitem.exceptions.WorkItemException;

public class SyntaxException extends WorkItemException {
    private static final long serialVersionUID = 6274151562111107177L;

    private SyntaxError syntaxError;
    private Node node;

    public SyntaxException() {

    }

    public SyntaxException(final Node node, final SyntaxError syntaxError) {
        super(syntaxError.getMessage());
        this.node = node;
        this.syntaxError = syntaxError;
    }

    public String getDetails() {
        if (node != null) {
            return MessageFormat.format(
                Messages.getString("SyntaxException.FullMessageSentenceThenErrorIsCausedByNodePathFormat"), //$NON-NLS-1$
                getMessage(),
                node.toString());
        }

        return getMessage();
    }

    public Node getNode() {
        return node;
    }

    public SyntaxError getSyntaxError() {
        return syntaxError;
    }
}
