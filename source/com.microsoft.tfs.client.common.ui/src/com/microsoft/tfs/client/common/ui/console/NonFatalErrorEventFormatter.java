// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.console;

import com.microsoft.tfs.client.common.ui.console.Message.MessageType;
import com.microsoft.tfs.core.clients.versioncontrol.events.NonFatalErrorEvent;

public class NonFatalErrorEventFormatter {
    public static Message getMessage(final NonFatalErrorEvent event) {
        return new Message(MessageType.ERROR, event.getMessage());
    }
}
