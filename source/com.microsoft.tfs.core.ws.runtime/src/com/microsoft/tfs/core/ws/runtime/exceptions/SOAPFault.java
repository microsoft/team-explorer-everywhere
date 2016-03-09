// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.ws.runtime.exceptions;

import javax.xml.namespace.QName;

import org.w3c.dom.Node;

/**
 * Wraps a formal SOAP fault message with an Exception so it can be handled
 * natively via Java's exception handling.
 */
public class SOAPFault extends ProxyException {
    private static final long serialVersionUID = 4437034068170167817L;

    private final QName code;
    private final String actor;
    private final String language;
    private final Node detail;
    private final SOAPFaultSubCode subCode;

    /**
     * Constructs a SOAPFault. All fields may be null.
     *
     * @param message
     *        the human-readable SOAP message.
     * @param code
     *        the SOAP fault code.
     * @param actor
     *        the URI of the object that caused the fault.
     * @param lalanguageng
     *        the language of the fault.
     * @param detail
     *        any details associated with the fault.
     * @param subCode
     *        the sub code associated with the fault.
     * @param throwable
     *        an optional inner exception.
     */
    public SOAPFault(
        final String message,
        final QName code,
        final String actor,
        final String language,
        final Node detail,
        final SOAPFaultSubCode subCode,
        final Throwable throwable) {
        super(message, throwable);

        this.code = code;
        this.actor = actor;
        this.language = language;
        this.detail = detail;
        this.subCode = subCode;
    }

    public String getActor() {
        return actor;
    }

    public QName getCode() {
        return code;
    }

    public Node getDetail() {
        return detail;
    }

    public String getLanguage() {
        return language;
    }

    public SOAPFaultSubCode getSubCode() {
        return subCode;
    }
}
