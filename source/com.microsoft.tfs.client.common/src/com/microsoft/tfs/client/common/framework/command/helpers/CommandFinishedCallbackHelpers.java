// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.framework.command.helpers;

import java.text.MessageFormat;

import org.eclipse.core.runtime.IStatus;

import com.microsoft.tfs.client.common.Messages;

/**
 * Utility class for command finished callbacks.
 *
 * @threadsafety unknown
 */
public class CommandFinishedCallbackHelpers {
    /**
     * Given an IStatus, creates a descriptive string. Useful for dealing with
     * MultiStatuses, as it will recurse down through all the child statuses.
     *
     * @param status
     *        An IStatus to describe as a string
     * @return A string describing the status message(s).
     */
    public static String getMessageForStatus(final IStatus status) {
        if (!status.isMultiStatus()) {
            // a Status message will never be null, but can be empty.
            if (status.getMessage().length() > 0 && status.getException() != null) {
                return MessageFormat.format(
                    Messages.getString("CommandFinishedCallbackHelpers.MessageAndLocalizedExceptionMessageFormat"), //$NON-NLS-1$
                    status.getMessage(),
                    status.getException().getLocalizedMessage());
            } else if (status.getException() != null) {
                return status.getException().getLocalizedMessage();
            } else {
                return status.getMessage();
            }
        }

        final IStatus[] children = status.getChildren();

        if (children.length == 0) {
            return status.getMessage();
        }

        final StringBuffer message = new StringBuffer();

        message.append(status.getMessage());
        message.append(":"); //$NON-NLS-1$

        for (int i = 0; i < children.length; i++) {
            message.append("\n"); //$NON-NLS-1$
            message.append(getMessageForStatus(children[i]));
        }

        return message.toString();
    }
}
