// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.console;

import com.microsoft.tfs.util.Check;

public class Message {
    public static final class MessageType {
        public static final MessageType INFO = new MessageType("INFO"); //$NON-NLS-1$
        public static final MessageType WARNING = new MessageType("WARNING"); //$NON-NLS-1$
        public static final MessageType ERROR = new MessageType("ERROR"); //$NON-NLS-1$

        private final String s;

        private MessageType(final String s) {
            this.s = s;
        }

        @Override
        public String toString() {
            return s;
        }
    }

    private final MessageType type;
    private final String text;

    public Message(final MessageType type, final String text) {
        Check.notNull(type, "type"); //$NON-NLS-1$

        this.type = type;
        this.text = text;
    }

    public MessageType getType() {
        return type;
    }

    public String getText() {
        return text;
    }
}
