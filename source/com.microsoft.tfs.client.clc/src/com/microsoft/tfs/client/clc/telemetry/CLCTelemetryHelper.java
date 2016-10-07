// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.clc.telemetry;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

import com.microsoft.tfs.client.clc.EnvironmentVariables;
import com.microsoft.tfs.client.clc.ExitCode;
import com.microsoft.tfs.client.clc.commands.Command;
import com.microsoft.tfs.core.telemetry.TfsTelemetryConstants;
import com.microsoft.tfs.core.telemetry.TfsTelemetryHelper;

public class CLCTelemetryHelper extends TfsTelemetryHelper {
    // A property to define whether collecting telemetry should be skipped.
    private static final String NO_TELEMETRY_PROPERTY_NAME = "com.microsoft.tfs.client.clc.telemetry.notelemetry"; //$NON-NLS-1$

    public synchronized static void checkNoTelemetryProperty() {
        // Unfortunately, we can't push the disabled logic onto the
        // TelemetryClient itself because the initialization code for
        // that object simply takes too much time. So, we have to stop
        // the sending of telemetry in the helper classes.

        // If the system property exists or the env var is true, then we disable
        // telemetry.
        setTelemetryDisabled(
            System.getProperty(NO_TELEMETRY_PROPERTY_NAME) != null
                || EnvironmentVariables.getBoolean(EnvironmentVariables.NO_TELEMETRY, false));
    }

    public static void sendCommandFinishedEvent(final Command command, final int retCode) {
        if (isTelemetryDisabled()) {
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
