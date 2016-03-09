// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.ws.runtime.exceptions;

public class SOAPSerializationException extends ProxyException {
    public SOAPSerializationException() {
        super();
    }

    public SOAPSerializationException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public SOAPSerializationException(final String message) {
        super(message);
    }

    public SOAPSerializationException(final Throwable cause) {
        super(cause);
    }

}
