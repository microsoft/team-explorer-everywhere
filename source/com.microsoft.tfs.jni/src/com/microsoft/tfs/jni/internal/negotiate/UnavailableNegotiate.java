// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.jni.internal.negotiate;

import com.microsoft.tfs.jni.Messages;
import com.microsoft.tfs.jni.Negotiate;

/**
 * An implementation of {@link Negotiate} that throws a runtime exception for
 * most methods.
 *
 * @threadsafety immutable
 */
public class UnavailableNegotiate implements Negotiate {
    private static String MESSAGE = Messages.getString("UnavailableNegotiate.Message"); //$NON-NLS-1$

    @Override
    public boolean supportsCredentialsDefault() {
        return false;
    }

    @Override
    public boolean supportsCredentialsSpecified() {
        return false;
    }

    @Override
    public String getCredentialsDefault() {
        throw new RuntimeException(MESSAGE);
    }

    @Override
    public NegotiateState initialize() throws NegotiateException {
        throw new RuntimeException(MESSAGE);
    }

    @Override
    public void setCredentialsDefault(final NegotiateState state) throws NegotiateException {
        throw new RuntimeException(MESSAGE);
    }

    @Override
    public void setCredentialsSpecified(
        final NegotiateState state,
        final String username,
        final String domain,
        final String password) throws NegotiateException {
        throw new RuntimeException(MESSAGE);
    }

    @Override
    public void setTarget(final NegotiateState state, final String target) throws NegotiateException {
        throw new RuntimeException(MESSAGE);
    }

    @Override
    public void setLocalhost(final NegotiateState state, final String localhost) throws NegotiateException {
        throw new RuntimeException(MESSAGE);
    }

    @Override
    public byte[] getToken(final NegotiateState state, final byte[] inputToken) throws NegotiateException {
        throw new RuntimeException(MESSAGE);
    }

    @Override
    public boolean isComplete(final NegotiateState state) throws NegotiateException {
        throw new RuntimeException(MESSAGE);
    }

    @Override
    public String getErrorMessage(final NegotiateState state) {
        throw new RuntimeException(MESSAGE);
    }

    @Override
    public void dispose(final NegotiateState state) throws NegotiateException {
        throw new RuntimeException(MESSAGE);
    }
}
