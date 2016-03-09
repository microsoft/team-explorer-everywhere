// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.ws.runtime.exceptions;

import javax.xml.namespace.QName;

/**
 * Holds a SOAP fault subcode.
 *
 * This class is immutable (and therefore thread-safe).
 */
public class SOAPFaultSubCode {
    private final QName subCode;

    public SOAPFaultSubCode(final QName subCode) {
        this.subCode = subCode;
    }

    public QName getSubCode() {
        return subCode;
    }
}
