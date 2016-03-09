// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.config.webservice;

import java.text.MessageFormat;

import com.microsoft.tfs.core.Messages;
import com.microsoft.tfs.core.exceptions.TECoreException;

/**
 * Thrown by {@link WebServiceFactory} when the requested web service is
 * unknown.
 *
 * @since TEE-SDK-10.1
 * @threadsafety thread-safe
 */
public class UnknownWebServiceException extends TECoreException {
    public UnknownWebServiceException(final Class webServiceType) {
        super(MessageFormat.format(
            Messages.getString("UnknownWebServiceException.UnknownWebServiceFormat"), //$NON-NLS-1$
            webServiceType.getName()));
    }
}
