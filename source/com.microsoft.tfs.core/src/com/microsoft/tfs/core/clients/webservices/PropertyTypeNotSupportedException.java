// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.webservices;

import java.text.MessageFormat;

import com.microsoft.tfs.core.Messages;

/**
 * Exception thrown when a property type is not supported.
 *
 * @since TEE-SDK-11.0
 */
public class PropertyTypeNotSupportedException extends TeamFoundationPropertyValidationException {
    public PropertyTypeNotSupportedException(final String propertyName, final Class<? extends Object> type) {
        super(
            propertyName,
            MessageFormat.format(
                Messages.getString("PropertyTypeNotSupportedException.UnsupportedPropertyValueTypeFormat"), //$NON-NLS-1$
                propertyName,
                type.getName()));
    }
}
