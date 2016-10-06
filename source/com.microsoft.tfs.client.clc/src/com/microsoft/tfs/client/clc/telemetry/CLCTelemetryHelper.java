// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.clc.telemetry;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

import com.microsoft.tfs.client.clc.ExitCode;
import com.microsoft.tfs.client.clc.commands.Command;
import com.microsoft.tfs.core.telemetry.TfsTelemetryConstants;
import com.microsoft.tfs.core.telemetry.TfsTelemetryHelper;

public class CLCTelemetryHelper extends TfsTelemetryHelper {
    public static void sendCommandFinishedEvent(final Command command, final int retCode) {
        if (sendingTelemetryDisabled) {
            // Don't send any telemetry
            return;
        }

        final String commandName;
        if (command == null) {
            commandName = "**UNKNOWN**"; //$NON-NLS-1$
        } else {
            commandName = command.getCanonicalName();
        }

        final String eventName =
            MessageFormat.format(TfsTelemetryConstants.CLC_COMMAND_EVENT_NAME_FORMAT, getName(command));

        final Map<String, String> properties = new HashMap<String, String>();

        properties.put(TfsTelemetryConstants.CLC_EVENT_PROPERTY_COMMAND_NAME, commandName);
        properties.put(
            TfsTelemetryConstants.CLC_EVENT_PROPERTY_IS_SUCCESS,
            Boolean.toString(retCode == ExitCode.SUCCESS));

        if (command != null) {
            addContextProperties(properties, command.getCollection());
        }

        sendEvent(eventName, properties);
    }
}
