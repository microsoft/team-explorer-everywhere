// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.webservices;

import com.microsoft.tfs.core.exceptions.InputValidationException;

/**
 * Exception thrown when a property name or value is invalid.
 *
 * @since TEE-SDK-11.0
 */
public class TeamFoundationPropertyValidationException extends InputValidationException {
    private String propertyName;

    public TeamFoundationPropertyValidationException(final String propertyName, final String message) {
        super(message);
        this.propertyName = propertyName;
    }

    public TeamFoundationPropertyValidationException(
        final String propertyName,
        final String message,
        final Throwable throwable) {
        super(message, throwable);
        this.propertyName = propertyName;
    }

    public String getPropertyName() {
        return this.propertyName;
    }

    public void setPropertyName(final String propertyName) {
        this.propertyName = propertyName;
    }
}
