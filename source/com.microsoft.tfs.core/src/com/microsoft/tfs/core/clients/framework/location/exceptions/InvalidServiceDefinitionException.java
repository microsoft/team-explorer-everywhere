// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.framework.location.exceptions;

import java.text.MessageFormat;

import com.microsoft.tfs.core.Messages;

/**
 * Exception raised when attempting to look up a location by service definition
 * and there are no locations defined for the service.
 */
public class InvalidServiceDefinitionException extends LocationException {
    public InvalidServiceDefinitionException(final String serviceType) {
        super(MessageFormat.format(
            //@formatter:off
            Messages.getString("InvalidServiceDefinitionException.ServiceWithFollowingTypeDoesNotHaveLocationMappingFormat"), //$NON-NLS-1$
            //@formatter:on
            serviceType));
    }
}
