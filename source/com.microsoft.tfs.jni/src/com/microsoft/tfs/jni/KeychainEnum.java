// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.jni;

import java.io.UnsupportedEncodingException;
import java.text.MessageFormat;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.TypesafeEnum;

/**
 * Mac OS uses what's called "FourCharCode"s as enumeration values, basically
 * mapping four chars to integers.
 *
 * (In C, you can use single quotes around more than one character to end up
 * with this value represented as larger values - that is, 'http' == ('h' << 24)
 * | ('t' << 16) | ('t' << 8) | 'p'.)
 *
 * @threadsafety unknown
 */
public class KeychainEnum extends TypesafeEnum {
    private static final Log log = LogFactory.getLog(KeychainEnum.class);

    protected KeychainEnum(final int value) {
        super(value);
    }

    protected static int computeValue(final String fourCharCode) {
        Check.notNull(fourCharCode, "fourCharCode"); //$NON-NLS-1$
        Check.isTrue(fourCharCode.length() == 4, "fourCharCode.length == 4"); //$NON-NLS-1$

        byte[] charValues = new byte[] {
            0,
            0,
            0,
            0
        };

        try {
            charValues = fourCharCode.getBytes("US-ASCII"); //$NON-NLS-1$
        } catch (final UnsupportedEncodingException e) {
            log.warn(MessageFormat.format("Could not get ascii representation of FourCharCode: {0}", fourCharCode), e); //$NON-NLS-1$
        }

        return (charValues[0] << 24) | (charValues[1] << 16) | (charValues[2] << 8) | (charValues[3]);
    }
}
