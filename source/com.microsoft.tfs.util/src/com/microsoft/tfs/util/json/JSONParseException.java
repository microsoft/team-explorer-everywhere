// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.util.json;

public class JSONParseException extends RuntimeException {
    public JSONParseException() {
        super();
    }

    public JSONParseException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public JSONParseException(final String message) {
        super(message);
    }

    public JSONParseException(final Throwable cause) {
        super(cause);
    }
}
