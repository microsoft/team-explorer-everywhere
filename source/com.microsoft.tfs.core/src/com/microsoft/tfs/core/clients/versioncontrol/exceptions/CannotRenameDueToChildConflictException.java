// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.exceptions;

import java.text.MessageFormat;

import com.microsoft.tfs.core.Messages;

public class CannotRenameDueToChildConflictException extends VersionControlException {
    private static final long serialVersionUID = 7685947625859810684L;

    public CannotRenameDueToChildConflictException(final String renamedItem, final String offendingItem) {
        super(
            MessageFormat.format(
                Messages.getString("CannotRenameDueToChildConflictException.CannotUndoRenameDueToChildConflictFormat"), //$NON-NLS-1$
                renamedItem,
                offendingItem));
    }
}
