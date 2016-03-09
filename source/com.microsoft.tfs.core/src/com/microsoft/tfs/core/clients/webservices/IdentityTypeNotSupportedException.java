// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.webservices;

import com.microsoft.tfs.core.Messages;

/**
 * @since TEE-SDK-11.0
 */
public class IdentityTypeNotSupportedException extends IdentityManagementException {
    public IdentityTypeNotSupportedException(final String identityType) {
        super(String.format(Messages.getString("IdentityTypeNotSupportedException.MessageFormat"), identityType)); //$NON-NLS-1$
    }
}
