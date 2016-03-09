// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.framework.configuration.internal;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.GUID;
import com.microsoft.tfs.util.base64.Base64;

/**
 * Parses base64-encoded GUIDs and GUID paths as used by the configuration
 * service.
 */
public class Base64GUIDParser {
    private static final Log log = LogFactory.getLog(Base64GUIDParser.class);

    /*
     * A GUID represented in base64 takes exactly 24 characters.
     */
    private static final int BASE64_GUID_LENGTH = 24;

    /**
     * Given a concatenated list of base64 encoded GUID values, returns the list
     * of GUIDs that were specified.
     *
     * @param input
     *        The concatenated list of base64-encoded GUIDs (not
     *        <code>null</code>)
     * @return an array of the GUID paths specified (never <code>null</code>)
     * @throws IllegalArgumentException
     *         if the given string is not composed solely of valid base64
     *         encoded GUID values
     */
    public static GUID[] getGUIDPathFromBase64(final String input) {
        Check.notNull(input, "input"); //$NON-NLS-1$

        final List<GUID> guidList = new ArrayList<GUID>();
        final int inputLen = input.length();

        if ((inputLen % BASE64_GUID_LENGTH) != 0) {
            throw new IllegalArgumentException("The given string is not composed of base64 guid paths"); //$NON-NLS-1$
        }

        for (int i = 0; i < inputLen; i += BASE64_GUID_LENGTH) {
            guidList.add(getGUIDFromBase64(input, i));
        }

        return guidList.toArray(new GUID[guidList.size()]);
    }

    /**
     * Given a single base64 encoded GUID values, returns the GUID that this
     * represents
     *
     * @param base64
     *        The base64-encoded GUIDs (not <code>null</code>)
     * @return the GUID (never <code>null</code>)
     * @throws IllegalArgumentException
     *         if the given string is not a valid base64 encoded GUID values
     */
    public static GUID getGUIDFromBase64(final String base64) {
        Check.notNull(base64, "base64"); //$NON-NLS-1$

        return getGUIDFromBase64(base64, 0);
    }

    private static GUID getGUIDFromBase64(final String input, final int offset) {
        Check.notNull(input, "input"); //$NON-NLS-1$

        if (input.length() < offset + BASE64_GUID_LENGTH) {
            throw new IllegalArgumentException("The given string is not a base64 guid value"); //$NON-NLS-1$
        }

        final String guidPart = input.substring(offset, offset + BASE64_GUID_LENGTH);
        byte[] base64Bytes;

        try {
            base64Bytes = guidPart.getBytes("US-ASCII"); //$NON-NLS-1$
        } catch (final UnsupportedEncodingException e) {
            log.warn(MessageFormat.format(
                "Could not decode guid in encoding US-ASCII, falling back to {0}", //$NON-NLS-1$
                Charset.defaultCharset().name()), e);
            base64Bytes = guidPart.getBytes();
        }

        /*
         * The base64 decoder allows whitespace and non-base64 characters
         * (ignoring them.) We do not, so validate the input.
         */
        if (!Base64.isArrayByteBase64(base64Bytes, false)) {
            throw new IllegalArgumentException("The given string is not a base64 guid value"); //$NON-NLS-1$
        }

        final byte[] decodedBytes = Base64.decodeBase64(base64Bytes);

        return new GUID(decodedBytes);
    }

    private Base64GUIDParser() {
    }
}
