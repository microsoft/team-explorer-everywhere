// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.exceptions;

import java.text.MessageFormat;

import com.microsoft.tfs.core.Messages;

/**
 * Exception raised when a corrupt local workspace baseline file.
 *
 *
 * @threadsafety unknown
 */
public class CorruptBaselineException extends VersionControlException {
    private static final long serialVersionUID = -4299580651395860870L;
    private final String targetLocalItem;

    public CorruptBaselineException(final String targetLocalItem, final String additionalInformationMessage) {
        super(MessageFormat.format(
            Messages.getString("CorruptBaselineException.CorruptBaselineFormat"), //$NON-NLS-1$
            targetLocalItem,
            additionalInformationMessage));
        this.targetLocalItem = targetLocalItem;
    }

    public CorruptBaselineException(final String targetLocalItem, final Exception innerException) {
        this(targetLocalItem, innerException.getMessage());
    }

    public String getTargetLocalItem() {
        return targetLocalItem;
    }
}
