// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.clc.vc.printers;

import java.text.MessageFormat;

import com.microsoft.tfs.client.clc.Messages;
import com.microsoft.tfs.console.display.Display;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.LabelResult;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.LabelResultStatus;
import com.microsoft.tfs.core.clients.versioncontrol.specs.LabelSpec;

/**
 * Helps us print out label creation, deletion, and update results, which are
 * printed sort of like like change set results, but not quite (because they're
 * not part of a changeset).
 */
public final class LabelResultPrinter {
    public static void printLabelResults(final LabelResult[] results, final Display display) {
        if (results == null) {
            return;
        }

        for (int i = 0; i < results.length; i++) {
            final LabelResult result = results[i];

            if (result == null || result.getStatus() == null) {
                continue;
            }

            final String labelString = new LabelSpec(result.getLabel(), result.getScope()).toString();

            if (result.getStatus() == LabelResultStatus.CREATED) {
                final String messageFormat = Messages.getString("LabelResultPrinter.CreatedLabelFormat"); //$NON-NLS-1$
                final String message = MessageFormat.format(messageFormat, labelString);
                display.printLine(message);
            } else if (result.getStatus() == LabelResultStatus.UPDATED) {
                final String messageFormat = Messages.getString("LabelResultPrinter.UpdatedLabelFormat"); //$NON-NLS-1$
                final String message = MessageFormat.format(messageFormat, labelString);
                display.printLine(message);
            } else if (result.getStatus() == LabelResultStatus.DELETED) {
                final String messageFormat = Messages.getString("LabelResultPrinter.DeletedLabelFormat"); //$NON-NLS-1$
                final String message = MessageFormat.format(messageFormat, labelString);
                display.printLine(message);
            } else {
                final String messageFormat = Messages.getString("LabelResultPrinter.UnknownLabelStatusFormat"); //$NON-NLS-1$
                final String message = MessageFormat.format(messageFormat, result.getStatus().toString(), labelString);
                display.printErrorLine(message);
            }
        }
    }
}
