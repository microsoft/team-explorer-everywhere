// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.vss.client.core.model;

import com.microsoft.vss.client.core.utils.StringUtil;

/**
 * <p>
 * Base class for all custom exceptions thrown from Vss and Tfs code.
 * </p>
 * <p>
 * All Exceptions in the VSS space -- any exception that flows across a REST API
 * boudary -- should derive from VssServiceException. This is likely almost ALL
 * new exceptions. Legacy TFS exceptions that do not flow through REST derive
 * from TeamFoundationServerException or TeamFoundationServiceException
 * </p>
 */
public abstract class VssException extends RuntimeException {

    private int errorCode;
    private boolean logException;
    private int eventId;
    private String helpLink;

    public VssException() {
        super();
    }

    public VssException(final int errorCode) {
        this(errorCode, false);
    }

    public VssException(final int errorCode, final boolean logException) {
        this.errorCode = errorCode;
        this.logException = logException;
    }

    public VssException(final String message, final int errorCode, final boolean logException) {
        super(StringUtil.ScrubPassword(message));
        this.errorCode = errorCode;
        this.logException = logException;
    }

    public VssException(final String message) {
        this(message, 0, false);
    }

    public VssException(final String message, final int errorCode) {
        this(message, errorCode, false);
    }

    public VssException(final String message, final boolean logException) {
        this(message, 0, logException);
    }

    public VssException(
        final String message,
        final int errorCode,
        final boolean logException,
        final Exception innerException) {
        super(StringUtil.ScrubPassword(message), innerException);
        this.errorCode = errorCode;
        this.logException = logException;
    }

    public VssException(final String message, final Exception innerException) {
        this(StringUtil.ScrubPassword(message), 0, false, innerException);
    }

    public VssException(final String message, final int errorCode, final Exception innerException) {
        this(StringUtil.ScrubPassword(message), errorCode, false, innerException);
    }

    public VssException(final String message, final boolean logException, final Exception innerException) {
        this(StringUtil.ScrubPassword(message), 0, logException, innerException);
    }

    public int getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(final int errorCode) {
        this.errorCode = errorCode;
    }

    public boolean getLogException() {
        return logException;
    }

    public void setLogException(final boolean logException) {
        this.logException = logException;
    }

    public int getEventId() {
        return eventId;
    }

    public void setEventId(final int eventId) {
        this.eventId = eventId;
    }

    public String getHelpLink() {
        return helpLink;
    }

    public void setHelpLink(final String helpLink) {
        this.helpLink = helpLink;
    }
}
