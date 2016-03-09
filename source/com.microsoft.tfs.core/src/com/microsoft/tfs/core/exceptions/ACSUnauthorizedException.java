// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.exceptions;

import java.text.MessageFormat;

import com.microsoft.tfs.core.Messages;
import com.microsoft.tfs.util.Check;

/**
 * <p>
 * Thrown when Azure ACS returns a non-success HTTP status code (for getting a
 * WRAP access token, etc.) because our credentials are not accepted.
 * </p>
 *
 * @since TEE-SDK-11.0
 * @threadsafety thread-safe
 */
public class ACSUnauthorizedException extends TECoreException {
    /**
     * Creates an {@link ACSUnauthorizedException} for the given WRAP name and
     * detail message (from ACS).
     *
     * @param wrapName
     *        the WRAP name submitted to the server (must not be
     *        <code>null</code>)
     * @param detailMessage
     *        the detail error message from the server (may be <code>null</code>
     *        )
     */
    public ACSUnauthorizedException(final String wrapName, final String detailMessage) {
        super(buildMessage(wrapName, detailMessage));
    }

    private static String buildMessage(final String wrapName, final String detailMessage) {
        Check.notNull(wrapName, "wrapName"); //$NON-NLS-1$

        if (detailMessage == null) {
            return MessageFormat.format(
                Messages.getString("ACSUnauthorizedException.AccessDeniedAuthenticatingAsNoDetailsFormat"), //$NON-NLS-1$
                wrapName);
        }

        return MessageFormat.format(
            Messages.getString("ACSUnauthorizedException.AccessDeniedAuthenticatingAsWithDetailsFormat"), //$NON-NLS-1$
            wrapName,
            detailMessage);
    }
}
