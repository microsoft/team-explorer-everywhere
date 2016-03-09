// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.jni;

import java.text.MessageFormat;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.microsoft.tfs.jni.NTLM.NTLMState;
import com.microsoft.tfs.jni.internal.ntlm.JavaNTLM;
import com.microsoft.tfs.jni.internal.ntlm.NTLMException;
import com.microsoft.tfs.jni.internal.ntlm.NativeNTLM;
import com.microsoft.tfs.util.Check;

/**
 * This handles the NTLM2 authentication protocol. This class differs slightly
 * from the other JNI *Utils classes in that it does not provide a 1:1 mapping
 * between class methods and the corresponding native (or fallback) methods.
 * Instead it implements the AuthenticationEngine interface to provide a more
 * abstract and OO experience for callers.
 */
public class NTLMEngine implements AuthenticationEngine {
    private static final Log log = LogFactory.getLog(NTLMEngine.class);

    private static final NTLMEngine instance = new NTLMEngine();

    /**
     * @return the system's NegotiateEngine
     */
    public static NTLMEngine getInstance() {
        return NTLMEngine.instance;
    }

    /**
     * Can be {@link NativeNTLM} or {@link JavaNTLM}.
     */
    private final NTLM impl;

    private NTLMEngine() {
        NTLM i = null;
        try {
            if (NativeNTLM.isAvailable()) {
                i = new NativeNTLM();
            }
        } catch (final Exception e) {
            log.warn(MessageFormat.format(
                "{0} reported itself available, but failed to load; falling back to {1}", //$NON-NLS-1$
                NativeNTLM.class.getName(),
                JavaNTLM.class.getName()), e);
        }

        if (i == null) {
            i = new JavaNTLM();
        }

        impl = i;
    }

    @Override
    public boolean isAvailable() {
        // We can always use NativeNTLM or JavaNTLM.
        return true;
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
        return new NTLMClient();
    }

    public class NTLMClient implements AuthenticationClient {
        private final NTLMState state;

        private NTLMClient() throws NTLMException {
            state = impl.initialize();
        }

        @Override
        public void setCredentialsDefault() throws AuthenticationException {
            Check.notNull(state, "state"); //$NON-NLS-1$

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