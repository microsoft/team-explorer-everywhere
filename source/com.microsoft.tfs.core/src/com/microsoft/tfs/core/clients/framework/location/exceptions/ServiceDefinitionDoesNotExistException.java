// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.framework.location.exceptions;

import java.text.MessageFormat;

import com.microsoft.tfs.core.Messages;
import com.microsoft.tfs.util.GUID;

/**
 * Exception raised when a service definition cannot be found.
 */
public class ServiceDefinitionDoesNotExistException extends LocationException {
    public ServiceDefinitionDoesNotExistException(final String serviceType, final GUID serviceIdentifier) {
        super(MessageFormat.format(
            //@formatter:off
            Messages.getString("ServiceDefinitionDoesNotExistException.ServiceDefinitionWithTypeAndIdentifierDoesNotExistFormat"), //$NON-NLS-1$
            //@formatter:on
            serviceType,
            serviceIdentifier.getGUIDString()));
    }
}
