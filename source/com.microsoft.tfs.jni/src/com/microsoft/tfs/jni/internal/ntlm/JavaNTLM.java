// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.jni.internal.ntlm;

import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.SecureRandom;
import java.text.MessageFormat;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;
import javax.naming.AuthenticationException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.microsoft.tfs.jni.Messages;
import com.microsoft.tfs.jni.NTLM;
import com.microsoft.tfs.jni.PlatformMiscUtils;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.HashUtils;

/**
 * Provides an implementation of the LM, NTLM and NTLMv2 protocols over NTLMSSP.
 * This is a drop-in replacement for Apache Jakarta Commons HTTPClient's
 * existing LM authentication mechanism.
 *
 * This implementation is based on the NTLMv2 documentation available from
 * "Implementing CIFS: The Common Internet FileSystem" Christopher R Hertel,
 * ISBN 0-13-047116-X.
 */
public final class JavaNTLM implements NTLM {
    private static final Log LOG = LogFactory.getLog(JavaNTLM.class);

    /* DO NOT SUPPORT NTLM (version 1): see bug 828. */
    private static boolean SUPPORT_NTLM = false;

    /*
     * Use a secure PRNG. Do not specify a PRNG type, as some of them use a
     * blocking random device on Linux. See
     * http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6202721
     */
    private static final SecureRandom prng = new SecureRandom();

    /*
     * NTLM2 MESSAGE FLAGS
     */

    /* Unicode strings are supported in security buffers */
    static final int FLAGS_NEGOTIATE_UNICODE = 0x00000001;

    /* OEM (ANSI) strings are supported in security buffers */
    static final int FLAGS_NEGOTIATE_OEM = 0x00000002;

    /* Request the target realm from the server */
    static final int FLAGS_REQUEST_TARGET = 0x00000004;

    /* NTLM authentication is supported */
    static final int FLAGS_NEGOTIATE_NTLM = 0x00000200;

    /* Negotiate domain name */
    static final int FLAGS_NEGOTIATE_DOMAIN = 0x00001000;

    /* Negotiate workstation (client) name */
    static final int FLAGS_NEGOTIATE_WORKSTATION = 0x00002000;

    /* Indicates local (as opposed to pass-through) authentication? */
    static final int FLAGS_NEGOTIATE_LOCAL_CALL = 0x00004000;

    /* Request a dummy signature */
    static final int FLAGS_NEGOTIATE_ALWAYS_SIGN = 0x00008000;

    /* Target (server) is a domain */
    final int FLAGS_NEGOTIATE_TYPE_DOMAIN = 0x00010000;

    /* NTLM2 authentication is supported */
    static final int FLAGS_NEGOTIATE_NTLM2 = 0x00080000;

    private static final Provider MD4_PROVIDER = new MD4Provider();

    public JavaNTLM() {
    }

    /*
     * Methods to implement the NTLM interface
     */

    public boolean isImplementationAvailable() {
        return true;
    }

    @Override
    public boolean supportsCredentialsDefault() {
        return false;
    }

    @Override
    public boolean supportsCredentialsSpecified() {
        return true;
    }

    @Override
    public String getCredentialsDefault() {
        return null;
    }

    @Override
    public NTLMState initialize() {
        return new JavaNTLMState();
    }

    @Override
    public void setCredentialsDefault(final NTLMState state) throws NTLMException {
        throw new NTLMException(Messages.getString("JavaNTLM.DefaultCredentialsNotSupported")); //$NON-NLS-1$
    }

    @Override
    public void setCredentialsSpecified(
        final NTLMState state,
        final String username,
        final String domain,
        final String password) {
        Check.notNull(state, "state"); //$NON-NLS-1$
        Check.isTrue(state instanceof JavaNTLMState, "state instanceof JavaNTLMState"); //$NON-NLS-1$

        ((JavaNTLMState) state).username = username;
        ((JavaNTLMState) state).domain = domain;
        ((JavaNTLMState) state).password = password;
    }

    @Override
    public void setTarget(final NTLMState state, final String target) {
        Check.notNull(state, "state"); //$NON-NLS-1$
        Check.isTrue(state instanceof JavaNTLMState, "state instanceof JavaNTLMState"); //$NON-NLS-1$

        ((JavaNTLMState) state).target = target;
    }

    @Override
    public void setLocalhost(final NTLMState state, final String localhost) {
        Check.notNull(state, "state"); //$NON-NLS-1$
        Check.isTrue(state instanceof JavaNTLMState, "state instanceof JavaNTLMState"); //$NON-NLS-1$

        ((JavaNTLMState) state).localhost = localhost;
    }

    @Override
    public byte[] getToken(final NTLMState state, final byte[] inputToken) throws NTLMException {
        Check.notNull(state, "state"); //$NON-NLS-1$
        Check.isTrue(state instanceof JavaNTLMState, "state instanceof JavaNTLMState"); //$NON-NLS-1$

        if (inputToken == null || inputToken.length == 0 && ((JavaNTLMState) state).tokensComputed == 0) {
            ((JavaNTLMState) state).tokensComputed++;
            return JavaNTLM.getType1Message((JavaNTLMState) state);
        } else if (inputToken != null && inputToken.length > 0 && ((JavaNTLMState) state).tokensComputed == 1) {
            ((JavaNTLMState) state).tokensComputed++;
            return JavaNTLM.getType3Message((JavaNTLMState) state, inputToken);
        } else {
            throw new NTLMException("Authentication routines called out of order"); //$NON-NLS-1$
        }
    }

    @Override
    public boolean isComplete(final NTLMState state) {
        /*
         * We are "complete" after delivering a type 3 message, our second token
         */
        return (((JavaNTLMState) state).tokensComputed >= 2);
    }

    @Override
    public String getErrorMessage(final NTLMState state) {
        return null;
    }

    @Override
    public void dispose(final NTLMState state) {
    }

    /**
     * Create an initial NTLM authentication message (a "Type 1" message). This
     * message provides the authenticating username and capabilities of this
     * client.
     *
     * @return The NTLM request in base64 format
     * @throws NTLMException
     */
    private static byte[] getType1Message(final JavaNTLMState state) throws NTLMException {
        String hostname = state.getTarget().toUpperCase();
        final String domain = state.getDomain();

        if (hostname.indexOf(".") >= 0) //$NON-NLS-1$
        {
            hostname = hostname.substring(0, hostname.indexOf(".")); //$NON-NLS-1$
        }

        /*
         * we always use US-ASCII here, since we don't yet know if the server
         * can support unicode. (that information is delivered in the server's
         * challenge)
         */
        final byte[] hostBytes = getBytes(hostname.toUpperCase(), "US-ASCII"); //$NON-NLS-1$
        final int hostLen = hostBytes.length;
        final int hostOffset = hostLen > 0 ? 32 : 0;

        final byte[] domainBytes = getBytes(domain.toUpperCase(), "US-ASCII"); //$NON-NLS-1$
        final int domainLen = domainBytes.length;
        final int domainOffset = domainLen > 0 ? (32 + hostLen) : 0;

        // type 1 message is 32 bytes + length of host and domain
        final int type1Length = 32 + hostBytes.length + domainBytes.length;
        final byte[] type1 = new byte[type1Length];
        int flags = 0;

        /*
         * there are lots of negotiate flags weDON'T need since we require the
         * users to explicitly specify domain name
         */
        flags |= FLAGS_NEGOTIATE_UNICODE;
        flags |= FLAGS_NEGOTIATE_OEM;
        flags |= FLAGS_NEGOTIATE_ALWAYS_SIGN;
        flags |= FLAGS_NEGOTIATE_NTLM2;

        if (SUPPORT_NTLM) {
            flags |= FLAGS_NEGOTIATE_NTLM;
        }

        // header
        addBytes(type1, 0, getBytes("NTLMSSP", "US-ASCII")); //$NON-NLS-1$ //$NON-NLS-2$

        // header
        addLong(type1, 8, 1); // Type 1 message indicator

        // flags
        addLong(type1, 12, flags);

        // domain information: length, length, location (offset) in message
        // (after host), 0x0000
        addShort(type1, 16, domainLen); // length
        addShort(type1, 18, domainLen); // length
        addShort(type1, 20, domainOffset); // offset
        addShort(type1, 22, 0); // null

        // host information: length, length, location (offset) in message (32),
        // 0x0000
        addShort(type1, 24, hostLen); // length
        addShort(type1, 26, hostLen); // length
        addShort(type1, 28, hostOffset); // offset
        addShort(type1, 30, 0); // null

        // host name
        addBytes(type1, 32, hostBytes);

        // domain name
        addBytes(type1, (32 + hostBytes.length), domainBytes);

        return type1;
    }

    /**
     * Creates an LM/NTLM/NTLM2 response ("Type 3") message.
     *
     * @param challenge
     *        The challenge received from the server
     * @return The NTLM response in base64 format
     */
    private static byte[] getType3Message(final JavaNTLMState state, final byte[] challenge) throws NTLMException {
        NTLMType2Message type2;
        try {
            type2 = parseType2(challenge);
        } catch (final Exception e) {
            throw new NTLMException(e.getMessage());
        }

        /* Get Credentials */
        final String username = state.getUsername();
        final String password = state.getPassword();
        final String domain = state.getDomain();
        final String hostname = state.getLocalHost();

        // if the server supports unicode, use that
        final String charset =
            ((type2.flags & FLAGS_NEGOTIATE_UNICODE) == FLAGS_NEGOTIATE_UNICODE) ? "UTF-16LE" : "US-ASCII"; //$NON-NLS-1$ //$NON-NLS-2$

        final byte[] domainBytes = getBytes(domain.toUpperCase(), charset);
        final int domainLen = domainBytes.length;
        final int domainOffset = domainLen > 0 ? 64 : 0;

        final byte[] usernameBytes = getBytes(username.toUpperCase(), charset);
        final int usernameLen = usernameBytes.length;
        final int usernameOffset = usernameLen > 0 ? (64 + domainLen) : 0;

        final byte[] hostBytes = getBytes(hostname.toUpperCase(), charset);
        final int hostnameLen = hostBytes.length;
        final int hostnameOffset = hostnameLen > 0 ? (64 + domainLen + usernameLen) : 0;

        byte[] lmResponse, ntlmResponse;

        int flags = 0;

        // if we can do ntlm2, do ntlm2
        if ((type2.flags & FLAGS_NEGOTIATE_NTLM2) == FLAGS_NEGOTIATE_NTLM2) {
            lmResponse = createLm2Response(username, password, domain, type2);
            ntlmResponse = createNtlm2Response(username, password, domain, type2);

            flags |= FLAGS_NEGOTIATE_NTLM2;
        }
        // otherwise we're stuck with ntlm
        else {
            if (!SUPPORT_NTLM) {
                throw new NTLMVersionException(
                    Messages.getString("JavaNTLM.NTLMVersion1NotSupportedContactAdministrator")); //$NON-NLS-1$
            }

            lmResponse = createLmResponse(password, type2);

            // MD4 may not be implemented, but we don't necessarily need the
            // ntlm
            // response if we're sending the lmResponse, treat it as a non-fatal
            // this probably will work for servers that accept ntlm auth.
            try {
                ntlmResponse = createNtlmResponse(password, type2);
            } catch (final NoSuchAlgorithmException e) {
                ntlmResponse = new byte[0];
            }

            flags |= FLAGS_NEGOTIATE_NTLM;
        }

        final int lmResponseLen = lmResponse.length;
        final int lmResponseOffset = (64 + domainLen + usernameLen + hostnameLen);

        final int ntlmResponseLen = ntlmResponse.length;
        final int ntlmResponseOffset = (64 + domainLen + usernameLen + hostnameLen + lmResponseLen);

        final byte[] type3 = new byte[64 + domainLen + usernameLen + hostnameLen + lmResponseLen + ntlmResponseLen];

        // if the server supports unicode, we want to send unicode strings
        if ((type2.flags & FLAGS_NEGOTIATE_UNICODE) == FLAGS_NEGOTIATE_UNICODE) {
            flags |= FLAGS_NEGOTIATE_UNICODE;
        } else {
            flags |= FLAGS_NEGOTIATE_OEM;
        }

        // header
        addBytes(type3, 0, getBytes("NTLMSSP", "US-ASCII")); //$NON-NLS-1$ //$NON-NLS-2$
        // header
        addLong(type3, 8, 3); // Type 3 message indicator

        // lm/lm2 response information (length, length, offset, 0)
        addShort(type3, 12, lmResponseLen);
        addShort(type3, 14, lmResponseLen);
        addShort(type3, 16, lmResponseOffset);
        addShort(type3, 18, 0);

        // ntlm/ntlm2 response information (length, length, offset, 0)
        addShort(type3, 20, ntlmResponseLen);
        addShort(type3, 22, ntlmResponseLen);
        addShort(type3, 24, ntlmResponseOffset);
        addShort(type3, 26, 0);

        // domain information (length, length, offset, 0)
        addShort(type3, 28, domainLen);
        addShort(type3, 30, domainLen);
        addShort(type3, 32, domainOffset);
        addShort(type3, 34, 0);

        // user information (length, length, offset, 0)
        addShort(type3, 36, usernameLen);
        addShort(type3, 38, usernameLen);
        addShort(type3, 40, usernameOffset);
        addShort(type3, 42, 0);

        // hostname information (length, length, offset, 0)
        addShort(type3, 44, hostnameLen);
        addShort(type3, 46, hostnameLen);
        addShort(type3, 48, hostnameOffset);
        addShort(type3, 50, 0);

        // session key information (length, length, offset, 0)
        addShort(type3, 52, 0);
        addShort(type3, 54, 0);
        addShort(type3, 56, (64 + ntlmResponseOffset + ntlmResponseLen));
        addShort(type3, 58, 0);

        // flags
        addLong(type3, 60, flags);

        // domain
        addBytes(type3, domainOffset, domainBytes);

        // username
        addBytes(type3, usernameOffset, usernameBytes);

        // host name
        addBytes(type3, hostnameOffset, hostBytes);

        // lmResponse
        addBytes(type3, lmResponseOffset, lmResponse);

        // ntlmResponse
        addBytes(type3, ntlmResponseOffset, ntlmResponse);

        return type3;
    }

    private static byte[] createLmResponse(final String password, final NTLMType2Message type2) throws NTLMException {
        return lmNtlmResponse(lmHash(password), type2);
    }

    private static byte[] createNtlmResponse(final String password, final NTLMType2Message type2)
        throws NTLMException,
            NoSuchAlgorithmException {
        return lmNtlmResponse(ntlmHash(password), type2);
    }

    private static byte[] createLm2Response(
        final String username,
        final String password,
        final String domain,
        final NTLMType2Message type2) throws NTLMException {
        final byte[] ntlm2Hash = ntlm2Hash(username, password, domain);
        final byte[] clientNonce = createClientNonce();

        final byte[] challenges = new byte[type2.challenge.length + clientNonce.length];
        addBytes(challenges, 0, type2.challenge);
        addBytes(challenges, type2.challenge.length, clientNonce);

        // used HMAC-MD5 on the concatenated challenges w/ the NTLMv2 hash as a
        // key
        byte[] hashedChallenges;
        try {
            final Mac mac = Mac.getInstance("HmacMD5"); //$NON-NLS-1$
            mac.init(new SecretKeySpec(ntlm2Hash, "HmacMD5")); //$NON-NLS-1$
            hashedChallenges = mac.doFinal(challenges);
        } catch (final Exception e) {
            LOG.error("Could not load HmacMD5 for NTLM", e); //$NON-NLS-1$
            throw new NTLMException(e.getMessage());
        }

        // concatenate the hashed challenges with the client nonce
        final byte[] lm2Response = new byte[hashedChallenges.length + clientNonce.length];
        addBytes(lm2Response, 0, hashedChallenges);
        addBytes(lm2Response, hashedChallenges.length, clientNonce);

        return lm2Response;
    }

    private static byte[] createNtlm2Response(
        final String username,
        final String password,
        final String domain,
        final NTLMType2Message type2) throws NTLMException {
        final byte[] ntlm2Hash = ntlm2Hash(username, password, domain);

        final int targetInfoLen = type2.targetInfo != null ? type2.targetInfo.length : 0;
        final byte[] ntlm2Blob = new byte[40 + targetInfoLen];

        // construct the "blob"
        addBytes(ntlm2Blob, 0, new byte[] {
            0x01,
            0x01,
            0x00,
            0x00
        }); // "blob" signature
        addLong(ntlm2Blob, 4, 0); // "reserved"
        addBytes(ntlm2Blob, 8, createTimestamp());
        addBytes(ntlm2Blob, 16, createClientNonce());
        addBytes(ntlm2Blob, 24, new byte[] {
            (byte) 0xad,
            (byte) 0xde,
            (byte) 0x15,
            (byte) 0xed
        }); // unknown

        if (targetInfoLen > 0) {
            addBytes(ntlm2Blob, 28, type2.targetInfo);
        }

        // insert obligatory pixies reference here
        addBytes(ntlm2Blob, (28 + targetInfoLen), new byte[] {
            (byte) 0xad,
            (byte) 0xde,
            (byte) 0x15,
            (byte) 0xed
        }); // again unknown
        // the end? of the blob

        // concatenate the type 2 message's challenge with the blob
        final byte[] challengedBlob = new byte[type2.challenge.length + ntlm2Blob.length];
        addBytes(challengedBlob, 0, type2.challenge);
        addBytes(challengedBlob, type2.challenge.length, ntlm2Blob);

        // now we get the HMAC-MD5 of the blob using the ntlm2 hash as a key
        // ick.
        byte[] blobHash;
        try {
            final Mac mac = Mac.getInstance("HmacMD5"); //$NON-NLS-1$
            mac.init(new SecretKeySpec(ntlm2Hash, "HmacMD5")); //$NON-NLS-1$
            blobHash = mac.doFinal(challengedBlob);
        } catch (final Exception e) {
            LOG.error("Could not load HmacMD5 for NTLM", e); //$NON-NLS-1$
            throw new NTLMException(e.getMessage());
        }

        final byte[] ntlm2Response = new byte[blobHash.length + ntlm2Blob.length];

        // concatenate the blob with its hash
        addBytes(ntlm2Response, 0, blobHash);
        addBytes(ntlm2Response, blobHash.length, ntlm2Blob);

        return ntlm2Response;
    }

    /**
     * Calculates the "LM Hash" based on password
     *
     * @param password
     * @param challenge
     * @return
     * @throws AuthenticationException
     */
    private static byte[] lmHash(final String password) throws NTLMException {
        // LM password "magic"
        final byte[] magic = {
            (byte) 0x4B,
            (byte) 0x47,
            (byte) 0x53,
            (byte) 0x21,
            (byte) 0x40,
            (byte) 0x23,
            (byte) 0x24,
            (byte) 0x25
        };

        final byte[] passwordBytes = getBytes(password.toUpperCase(), "US-ASCII"); //$NON-NLS-1$
        final byte[] password1 = new byte[7];
        final byte[] password2 = new byte[7];

        // split the password into two 7 byte arrays (null-padded)
        for (int i = 0; i < 7; i++) {
            password1[i] = (passwordBytes.length > i) ? passwordBytes[i] : 0;
            password2[i] = (passwordBytes.length > 7 + i) ? passwordBytes[7 + i] : 0;
        }

        byte[] hashed1, hashed2;
        try {
            // hash the magic with the two halves of the password, then
            // concatenate
            final Cipher desCipher = Cipher.getInstance("DES/ECB/NoPadding"); //$NON-NLS-1$

            // encrypt magic w/ first half of password
            desCipher.init(Cipher.ENCRYPT_MODE, createDesKey(password1));
            hashed1 = desCipher.doFinal(magic);

            // encrypt magic w/ second half of password
            desCipher.init(Cipher.ENCRYPT_MODE, createDesKey(password2));
            hashed2 = desCipher.doFinal(magic);
        } catch (final Exception e) {
            LOG.error("Could not load DES for NTLM", e); //$NON-NLS-1$
            throw new NTLMException(e.getMessage());
        }

        // concatenate the top and bottom
        final byte[] lmHash = new byte[16];
        for (int i = 0; i < 8; i++) {
            lmHash[i] = (hashed1.length > i) ? hashed1[i] : 0;
            lmHash[8 + i] = (hashed2.length > i) ? hashed2[i] : 0;
        }

        return lmHash;
    }

    private static byte[] ntlmHash(final String password) throws NTLMException {
        try {
            return HashUtils.hashString(password, "UTF-16LE", "MD4", MD4_PROVIDER); //$NON-NLS-1$ //$NON-NLS-2$
        } catch (final Exception e) {
            LOG.error("Could not load MD4 for NTLM", e); //$NON-NLS-1$
            throw new NTLMException(e.getLocalizedMessage());
        }
    }

    private static byte[] ntlm2Hash(final String username, final String password, final String domain)
        throws NTLMException {
        // we must get the ntlmHash here, which depends on MD4 which
        // we sneakily implemented using Cryptix's implementation
        // this is a requirement for the ntlm2 response (unlike the
        // type3 ntlm response, which may work despite having no
        // ntlm message)
        final byte[] ntlmHash = ntlmHash(password);

        // we need the username and domain concatenated
        final byte[] usernameBytes = getBytes(username.toUpperCase(), "UTF-16LE"); //$NON-NLS-1$
        final byte[] domainBytes = getBytes(domain.toUpperCase(), "UTF-16LE"); //$NON-NLS-1$

        final byte[] usernameDomainBytes = new byte[usernameBytes.length + domainBytes.length];
        int i;
        for (i = 0; i < usernameBytes.length; i++) {
            usernameDomainBytes[i] = usernameBytes[i];
        }
        for (int j = 0; j < domainBytes.length; j++) {
            usernameDomainBytes[i + j] = domainBytes[j];
        }

        // ntlm2 hash is created by running HMAC-MD5 on the unicode
        // username and domain (uppercased), with the ntlmHash as a
        // key
        byte[] ntlm2Hash;
        try {
            final Mac mac = Mac.getInstance("HmacMD5"); //$NON-NLS-1$
            mac.init(new SecretKeySpec(ntlmHash, "HmacMD5")); //$NON-NLS-1$
            ntlm2Hash = mac.doFinal(usernameDomainBytes);
        } catch (final Exception e) {
            LOG.error("Could not load HmacMD5 for NTLM", e); //$NON-NLS-1$
            throw new NTLMException(e.getMessage());
        }

        return ntlm2Hash;
    }

    /**
     * Create a LanMan hash of the pasword (warning: weak!)
     *
     * @param password
     *        password to hash
     * @param challenge
     *        the ntlm challenge (unused)
     * @return
     * @throws NoSuchPaddingException
     * @throws NoSuchAlgorithmException
     * @throws InvalidKeyException
     */
    private static byte[] lmNtlmResponse(final byte[] hash, final NTLMType2Message type2) throws NTLMException {
        // create three 7 byte keys from the hash
        final byte[] password1 = new byte[7];
        final byte[] password2 = new byte[7];
        final byte[] password3 = new byte[7];

        for (int i = 0; i < 7; i++) {
            password1[i] = (hash.length > i) ? hash[i] : 0;
            password2[i] = (hash.length > 7 + i) ? hash[7 + i] : 0;
            password3[i] = (hash.length > 14 + i) ? hash[14 + i] : 0;
        }

        byte[] crypt1, crypt2, crypt3;
        try {
            // hash the magic with the two halves of the password, then
            // concatenate
            final Cipher desCipher = Cipher.getInstance("DES/ECB/NoPadding"); //$NON-NLS-1$

            // encrypt challenge w/ first third of hash
            desCipher.init(Cipher.ENCRYPT_MODE, createDesKey(password1));
            crypt1 = desCipher.doFinal(type2.challenge);

            // encrypt challenge w/ second third of hash
            desCipher.init(Cipher.ENCRYPT_MODE, createDesKey(password2));
            crypt2 = desCipher.doFinal(type2.challenge);

            // encrypt challenge w/ final third of hash
            desCipher.init(Cipher.ENCRYPT_MODE, createDesKey(password3));
            crypt3 = desCipher.doFinal(type2.challenge);
        } catch (final Exception e) {
            LOG.error("Could not load DES for NTLM", e); //$NON-NLS-1$
            throw new NTLMException(e.getMessage());
        }

        // concatenate triples
        final byte[] lmResponse = new byte[24];
        for (int i = 0; i < 8; i++) {
            lmResponse[i] = crypt1[i];
            lmResponse[8 + i] = crypt2[i];
            lmResponse[16 + i] = crypt3[i];
        }

        return lmResponse;
    }

    private static byte[] createTimestamp() {
        // get millisecs since the epoch
        long timestamp = System.currentTimeMillis();

        // add 11644473600 secs to get us to January 1, 1601
        timestamp += 11644473600000L;

        // multiplying by 10000 gives us usecs
        timestamp *= 10000;

        final byte[] timeBytes = new byte[8];

        timeBytes[0] = (byte) ((timestamp & 0x00000000000000FFL));
        timeBytes[1] = (byte) ((timestamp & 0x000000000000FF00L) >> 8);
        timeBytes[2] = (byte) ((timestamp & 0x0000000000FF0000L) >> 16);
        timeBytes[3] = (byte) ((timestamp & 0x00000000FF000000L) >> 24);
        timeBytes[4] = (byte) ((timestamp & 0x000000FF00000000L) >> 32);
        timeBytes[5] = (byte) ((timestamp & 0x0000FF0000000000L) >> 40);
        timeBytes[6] = (byte) ((timestamp & 0x00FF000000000000L) >> 48);
        timeBytes[7] = (byte) ((timestamp & 0xFF00000000000000L) >> 56);

        return timeBytes;
    }

    private static byte[] createClientNonce() {
        final byte[] nonce = new byte[8];

        prng.nextBytes(nonce);

        return nonce;
    }

    private static Key createDesKey(final byte[] ascii) {
        final byte[] keyData = new byte[8];

        keyData[0] = (byte) ((ascii[0] >> 1) & 0xFF);
        keyData[1] = (byte) ((((ascii[0] & 0x01) << 6) | (((ascii[1] & 0xFF) >> 2) & 0xFF)) & 0xFF);
        keyData[2] = (byte) ((((ascii[1] & 0x03) << 5) | (((ascii[2] & 0xFF) >> 3) & 0xFF)) & 0xff);
        keyData[3] = (byte) ((((ascii[2] & 0x07) << 4) | (((ascii[3] & 0xff) >> 4) & 0xFF)) & 0xFF);
        keyData[4] = (byte) ((((ascii[3] & 0x0F) << 3) | (((ascii[4] & 0xFF) >> 5) & 0xFF)) & 0xFF);
        keyData[5] = (byte) ((((ascii[4] & 0x1F) << 2) | (((ascii[5] & 0xFF) >> 6) & 0xFF)) & 0xFF);
        keyData[6] = (byte) ((((ascii[5] & 0x3F) << 1) | (((ascii[6] & 0xFF) >> 7) & 0xFF)) & 0xFF);
        keyData[7] = (byte) (ascii[6] & 0x7F);

        for (int i = 0; i < keyData.length; i++) {
            keyData[i] = (byte) (keyData[i] << 1);
        }

        return new SecretKeySpec(keyData, "DES"); //$NON-NLS-1$
    }

    private static NTLMType2Message parseType2(final byte[] type2) throws Exception {
        final NTLMType2Message message = new NTLMType2Message();

        // check length
        if (type2.length < 32) {
            throw new Exception("Not an NTLMSSP Type 2 message, or truncated"); //$NON-NLS-1$
        }

        // check for NTLMSSP header
        if (type2[0] != 'N'
            || type2[1] != 'T'
            || type2[2] != 'L'
            || type2[3] != 'M'
            || type2[4] != 'S'
            || type2[5] != 'S'
            || type2[6] != 'P'
            || type2[7] != 0) {
            throw new Exception("Not an NTLMSSP message"); //$NON-NLS-1$
        }

        // get the message indicator
        if ((message.indicator = readShort(type2, 8)) != 2) {
            throw new Exception("Not an NTLMSSP Type 2 message"); //$NON-NLS-1$
        }

        // get the pointer to the target name
        final int targetNameLen = readShort(type2, 12);
        final int targetNameSpace = readShort(type2, 14);
        final int targetNameOffset = readShort(type2, 16);
        final int targetNameZero = readShort(type2, 18);

        // ensure target name namespace is valid
        if (targetNameZero != 0 || type2.length < (targetNameOffset + targetNameSpace)) {
            throw new Exception("Corrupt NTLMSSP Type 2 message, truncated in targetname"); //$NON-NLS-1$
        } else if (targetNameOffset > 0 && targetNameLen > 0) {
            message.targetName = readBytes(type2, targetNameOffset, targetNameLen);
        }

        message.flags = readLong(type2, 20);
        message.challenge = readBytes(type2, 24, 8);

        // if there's some data between the challenge and the target name,
        // assume that
        // it's both the context and the target information security block
        // (the documentation is vague, and claims these two fields are
        // optional, i
        // interpret that as (context AND target information) are optional, not
        // context is optional AND target information is optional
        if (type2.length > 48 && targetNameOffset >= 48) {
            message.context = readBytes(type2, 32, 8);

            final int targetInformationLen = readShort(type2, 40);
            final int targetInformationSpace = readShort(type2, 42);
            final int targetInformationOffset = readShort(type2, 44);
            final int targetInformationZero = readShort(type2, 46);

            // ensure target information namespace is valid
            if (targetInformationZero != 0 || type2.length < (targetInformationOffset + targetInformationSpace)) {
                throw new Exception("Corrupt NTLMSSP Type 2 message, truncated in target information"); //$NON-NLS-1$
            } else if (targetInformationOffset > 0 && targetInformationLen > 0) {
                message.targetInfo = readBytes(type2, targetInformationOffset, targetInformationLen);
            }
        } else {
            LOG.error("No target information in NTLMSSP Type 2 message, NTLM2 must fail"); //$NON-NLS-1$
        }

        return message;
    }

    /**
     * Adds the given byte to the response.
     *
     * @params buf buffer to add bytes to
     * @params pos position to add bytes
     * @param b
     *        the byte to add.
     */
    private static void addByte(final byte[] buf, int pos, final byte b) {
        if (pos < 0 || buf.length < pos + 1) {
            throw new ArrayIndexOutOfBoundsException(
                MessageFormat.format(
                    "Attempt to add one byte at position {0} to array of length {1}", //$NON-NLS-1$
                    pos,
                    buf.length));
        }

        buf[pos++] = b;
    }

    /**
     * Adds the given bytes to the response
     *
     * @params buf buffer to add bytes to
     * @params pos position to add bytes
     * @param bytes
     *        the bytes to add.
     */
    private static void addBytes(final byte[] buf, int pos, final byte[] bytes) {
        if (pos < 0 || buf.length < pos + bytes.length) {
            throw new ArrayIndexOutOfBoundsException(
                MessageFormat.format(
                    "Attempt to add {0} bytes at position {1} to array of length {2}", //$NON-NLS-1$
                    bytes.length,
                    pos,
                    buf.length));
        }

        for (int i = 0; i < bytes.length; i++) {
            buf[pos++] = bytes[i];
        }
    }

    /**
     * Adds the given bytes of a "long" (32 bit int, little endian) to the
     * response.
     *
     * @param buf
     *        buffer to add bytes to
     * @param pos
     *        position to add bytes
     * @param longnum
     *        number to add to the byte stream
     */
    private static void addLong(final byte[] buf, final int pos, final int longnum) {
        if (pos < 0 || buf.length < pos + 4) {
            throw new ArrayIndexOutOfBoundsException(
                MessageFormat.format(
                    "Attempt to add long (4 bytes) at position {0} to array of length {1}", //$NON-NLS-1$
                    pos,
                    buf.length));
        }

        addByte(buf, pos, (byte) ((longnum & 0x000000FF)));
        addByte(buf, pos + 1, (byte) ((longnum & 0x0000FF00) >> 8));
        addByte(buf, pos + 2, (byte) ((longnum & 0x00FF0000) >> 16));
        addByte(buf, pos + 3, (byte) ((longnum & 0xFF000000) >> 24));
    }

    /**
     * Adds the given bytes of a "short" (16 bit int, little endian) to the
     * response
     *
     * @param buf
     *        buffer to add bytes to
     * @param pos
     *        position to add bytes
     * @param shortnum
     *        number to add to the byte stream
     */
    private static void addShort(final byte[] buf, final int pos, final int shortnum) {
        if (pos < 0 || buf.length < pos + 2) {
            throw new ArrayIndexOutOfBoundsException(
                MessageFormat.format(
                    "Attempt to add short (2 bytes) at position {0} to array of length {1}", //$NON-NLS-1$
                    pos,
                    buf.length));
        }

        addByte(buf, pos, (byte) ((shortnum & 0x000000FF)));
        addByte(buf, pos + 1, (byte) ((shortnum & 0x0000FF00) >> 8));
    }

    private static int readLong(final byte[] buf, final int pos) {
        if (pos < 0 || (pos + 4) > buf.length) {
            throw new ArrayIndexOutOfBoundsException(
                MessageFormat.format(
                    "Attempt to read long (4 bytes) at position {0} from array of length {1}", //$NON-NLS-1$
                    pos,
                    buf.length));
        }

        return (((buf[pos + 3] & 0xFF) << 24)
            | ((buf[pos + 2] & 0xFF) << 16)
            | ((buf[pos + 1] & 0xFF) << 8)
            | (buf[pos] & 0xFF));
    }

    private static short readShort(final byte[] buf, final int pos) {
        if (pos < 0 || (pos + 2) > buf.length) {
            throw new ArrayIndexOutOfBoundsException(
                MessageFormat.format(
                    "Attempt to read short (2 bytes) at position {0} from array of length {1}", //$NON-NLS-1$
                    pos,
                    buf.length));
        }

        return (short) (((buf[pos + 1] & 0xFF) << 8) | (buf[pos] & 0xFF));
    }

    private static byte[] readBytes(final byte[] buf, final int pos, final int len) {
        if (pos < 0 || (pos + len) > buf.length) {
            throw new ArrayIndexOutOfBoundsException(
                MessageFormat.format(
                    "Attempt to read {0} bytes at position {1} from array of length {2}", //$NON-NLS-1$
                    len,
                    pos,
                    buf.length));
        }

        final byte[] result = new byte[len];

        for (int i = 0; i < len; i++) {
            result[i] = buf[pos + i];
        }

        return result;
    }

    private static byte[] getBytes(final String string, final String encoding) throws NTLMException {
        try {
            return string.getBytes(encoding);
        } catch (final UnsupportedEncodingException e) {
            throw new NTLMException(e.getMessage(), e);
        }
    }

    /**
     * NTLM Type2 message structure
     */
    private static class NTLMType2Message {
        public int indicator = 0;
        public int flags = 0;
        public byte[] challenge = null;
        public byte[] context = null;
        public byte[] targetName = null;
        public byte[] targetInfo = null;
    }

    private class JavaNTLMState extends NTLMState {
        private int tokensComputed = 0;

        private String username;
        private String domain;
        private String password;

        private String target;
        private String localhost;

        private String getUsername() {
            return (username == null) ? "" : username; //$NON-NLS-1$
        }

        private String getDomain() {
            return (domain == null) ? "" : domain; //$NON-NLS-1$
        }

        private String getPassword() {
            return (password == null) ? "" : password; //$NON-NLS-1$
        }

        private String getTarget() {
            return (target == null) ? "" : target; //$NON-NLS-1$
        }

        private String getLocalHost() {
            String host = (localhost == null) ? PlatformMiscUtils.getInstance().getComputerName() : localhost;

            if (host.indexOf(".") >= 0) //$NON-NLS-1$
            {
                host = host.substring(0, host.indexOf(".")); //$NON-NLS-1$
            }

            return host.toUpperCase();
        }
    }
}
