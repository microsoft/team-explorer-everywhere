// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.exceptions;

import com.microsoft.tfs.core.Messages;
import com.microsoft.tfs.core.ws.runtime.exceptions.FederatedAuthException;
import com.microsoft.tfs.util.Check;

/**
 * <p>
 * Thrown when a web service class was used and no {@link TransportAuthHandler}
 * successfully authenticated.
 * </p>
 *
 * @since TEE-SDK-11.0
 * @threadsafety thread-safe
 */
public class TFSFederatedAuthException extends TECoreException {
    public TFSFederatedAuthException(final FederatedAuthException e) {
        super(buildMessage(e), e);
    }

    private static String buildMessage(final FederatedAuthException e) {
        Check.notNull(e, "e"); //$NON-NLS-1$

        return Messages.getString("TFSFederatedAuthException.FedAuthRequiredNoMechsAvailable"); //$NON-NLS-1$
    }

}
