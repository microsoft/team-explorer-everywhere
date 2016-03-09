// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.exceptions;

/**
 * Exception is thrown when a checkin note is provided that is not valid.
 * Checkin Notes must not contain any control characters and must be 64
 * characters or less.
 *
 * @since TEE-SDK-10.1
 */
public class CheckinNoteNameValidationException extends VersionControlException {
    private String invalidName;

    public CheckinNoteNameValidationException() {
        super();
    }

    public CheckinNoteNameValidationException(final String message) {
        super(message);
    }

    public CheckinNoteNameValidationException(final Throwable cause) {
        super(cause);
    }

    public CheckinNoteNameValidationException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public CheckinNoteNameValidationException(final String message, final String invalidName) {
        this(message);
        this.invalidName = invalidName;
    }

    public String getInvalidName() {
        return invalidName;
    }

}
