// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.exceptions.mappers;

import java.net.UnknownHostException;
import java.text.MessageFormat;

import com.microsoft.tfs.core.Messages;
import com.microsoft.tfs.core.exceptions.HTTPProxyUnauthorizedException;
import com.microsoft.tfs.core.exceptions.TECoreException;
import com.microsoft.tfs.core.exceptions.TFSAccessException;
import com.microsoft.tfs.core.exceptions.TFSFederatedAuthException;
import com.microsoft.tfs.core.exceptions.TFSUnauthorizedException;
import com.microsoft.tfs.core.ws.runtime.client.SOAPService;
import com.microsoft.tfs.core.ws.runtime.exceptions.FederatedAuthException;
import com.microsoft.tfs.core.ws.runtime.exceptions.FederatedAuthFailedException;
import com.microsoft.tfs.core.ws.runtime.exceptions.ProxyException;
import com.microsoft.tfs.core.ws.runtime.exceptions.ProxyUnauthorizedException;
import com.microsoft.tfs.core.ws.runtime.exceptions.SOAPFault;
import com.microsoft.tfs.core.ws.runtime.exceptions.TransportException;
import com.microsoft.tfs.core.ws.runtime.exceptions.UnauthorizedException;

/**
 * <p>
 * Basic exception mapper.
 * </p>
 * <p>
 * Exception mappers provide a static method that takes an exception thrown from
 * a lower level (SOAP library, for instance) that must be mapped into a TFS
 * domain-specific exception before it leaves core. This practice prevents
 * exception types from leaking upwards to clients without the proper TFS client
 * context.
 * </p>
 * <p>
 * A mapper is needed because {@link SOAPService}'s methods may throw
 * {@link SOAPFault} exceptions when web service methods are invoked. Because
 * TFS formats the data inside a {@link SOAPFault} differently for each type of
 * web service, core clients must transform this exception into a more specific
 * type of exception to give to the caller. Applications using core will then
 * receive more precise exception types that are easier to filter on and format
 * for display. These mapper classes provide this functionality with
 * client-specific knowledge.
 * </p>
 * <p>
 * You can't call {@link #map(RuntimeException)} directly on this class, so call
 * it on the appropriate client-specific class in this package instead.
 * </p>
 *
 * @since TEE-SDK-10.1
 * @threadsafety immutable
 */
public abstract class TECoreExceptionMapper {
    /**
     * Takes any {@link Exception} and returns a more specific
     * {@link TECoreException} that can be thrown in its place, if a more
     * specific type is known. If no better class is known, the given exception
     * is returned unaltered.
     *
     * @param e
     *        the {@link Exception} to map (find a more specific exception for).
     *        If null, null is returned.
     * @return the most specific {@link TECoreException} that can be used in
     *         place of the given exception, or null if the given exception was
     *         null.
     */
    protected static RuntimeException map(final RuntimeException e) {
        /*
         * Handle common SOAP layer problems.
         */
        if (e instanceof UnauthorizedException) {
            return new TFSUnauthorizedException((UnauthorizedException) e);
        } else if (e instanceof ProxyUnauthorizedException) {
            return new HTTPProxyUnauthorizedException((ProxyUnauthorizedException) e);
        } else if (e instanceof FederatedAuthException) {
            return new TFSFederatedAuthException((FederatedAuthException) e);
        } else if (e instanceof TransportException && e.getCause() instanceof UnknownHostException) {
            return new TECoreException(
                MessageFormat.format(
                    Messages.getString("TECoreExceptionMapper.UnknownHostFormat"), //$NON-NLS-1$
                    e.getCause().getLocalizedMessage()),
                e.getCause());
        } else if (e instanceof FederatedAuthFailedException) {
            return new TFSAccessException((FederatedAuthFailedException) e);
        } else if (e instanceof TECoreException) {
            /*
             * Avoid unnecessary exception type conversion.
             */
            return e;
        } else if (e instanceof ProxyException) {
            /*
             * ProxyException covers all the checked exceptions that escape
             * com.microsoft.tfs.core.ws.runtime. The best we can do is wrap
             * them, since all the interesting SOAPFaults should have been
             * handled by client-specific mappers.
             */
            return new TECoreException(e.getMessage(), e);
        }

        // Handles unknown types including null.
        return e;
    }
}
