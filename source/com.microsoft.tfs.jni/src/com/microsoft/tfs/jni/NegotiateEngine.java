// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.jni;

import java.text.MessageFormat;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.microsoft.tfs.jni.Negotiate.NegotiateState;
import com.microsoft.tfs.jni.internal.negotiate.NativeNegotiate;
import com.microsoft.tfs.jni.internal.negotiate.NegotiateException;
import com.microsoft.tfs.jni.internal.negotiate.UnavailableNegotiate;

/**
 * This handles the SPNEGO ("Negotiate") authentication protocol. This class
 * differs slightly from the other JNI *Utils classes in that it does not
 * provide a 1:1 mapping between class methods and the corresponding native (or
 * fallback) methods. Instead it implements the AuthenticationEngine interface
 * to provide a more abstract and OO experience for callers.
 */
public class NegotiateEngine implements AuthenticationEngine {
    private static final Log log = LogFactory.getLog(NegotiateEngine.class);

    private static final NegotiateEngine instance = new NegotiateEngine();

    /**
     * @return the system's NegotiateEngine
     */
    public static NegotiateEngine getInstance() {
        return NegotiateEngine.instance;
    }

    /**
     * Can be {@link NativeNegotiate} or {@link UnavailableNegotiate}.
     */
    private final Negotiate impl;

    private NegotiateEngine() {
        Negotiate i = null;
        try {
            if (NativeNegotiate.isAvailable()) {
                i = new NativeNegotiate();
            }
        } catch (final Exception e) {
            log.warn(MessageFormat.format(
                "{0} reported itself available, but failed to load", //$NON-NLS-1$
                NativeNegotiate.class.getName()));
        }

        if (i == null) {
            i = new UnavailableNegotiate();
        }

        impl = i;
    }

    @Override
    public boolean isAvailable() {
        return NativeNegotiate.isAvailable();
    }

    @Override
    public boolean supportsCredentialsDefault() {
        return impl.supportsCredentialsDefault();
    }

    @Override
    public boolean supportsCredentialsSpecified() {
        return impl.supportsCredentialsSpecified();
    }

    @Override
    public String getCredentialsDefault() {
        return impl.getCredentialsDefault();
    }

    @Override
    public AuthenticationClient newClient() throws AuthenticationException {
        return new NegotiateClient();
    }

    public class NegotiateClient implements AuthenticationClient {
        private final NegotiateState state;

        private NegotiateClient() throws NegotiateException {
            state = impl.initialize();
        }

        @Override
        public void setCredentialsDefault() throws AuthenticationException {
            impl.setCredentialsDefault(state);
        }

        @Override
        public void setCredentialsSpecified(final String username, final String domain, final String password)
            throws AuthenticationException {
            impl.setCredentialsSpecified(state, username, domain, password);
        }

        @Override
        public void setTarget(final String target) throws AuthenticationException {
            impl.setTarget(state, target);
        }

        public void setLocalhost(final String localhost) throws AuthenticationException {
            impl.setLocalhost(state, localhost);
        }

        @Override
        public byte[] getToken(final byte[] inputToken) throws AuthenticationException {
            return impl.getToken(state, inputToken);
        }

        @Override
        public boolean isComplete() throws AuthenticationException {
            return impl.isComplete(state);
        }

        @Override
        public String getErrorMessage() {
            return impl.getErrorMessage(state);
        }

        @Override
        public void dispose() {
            try {
                impl.dispose(state);
            } catch (final Exception e) {
            }
        }
    }
}