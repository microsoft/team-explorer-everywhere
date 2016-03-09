// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.internal.fileattributes;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.NewlineUtils;

/**
 * Contains some constant file attribute values to match against during
 * attribute lookup.
 */
public class FileAttributeValues {
    public static final String EOL_LF = "lf"; //$NON-NLS-1$
    public static final String EOL_CRLF = "crlf"; //$NON-NLS-1$
    public static final String EOL_CR = "cr"; //$NON-NLS-1$
    public static final String EOL_NATIVE = "native"; //$NON-NLS-1$
    public static final String EOL_NONE = "none"; //$NON-NLS-1$

    private static final Log log = LogFactory.getLog(FileAttributeValues.class);

    /**
     * Takes a {@link StringPairFileAttribute} whose attribute name is
     * {@link FileAttributeNames#CLIENT_EOL} or
     * {@link FileAttributeNames#SERVER_EOL} and returns the literal end-of-line
     * string that the attribute's value specifies.
     *
     * @param attribute
     *        the {@link FileAttributeNames#CLIENT_EOL} or
     *        {@link FileAttributeNames#SERVER_EOL} attribute (must not be
     *        <code>null</code>)
     * @return the literal end-of-line string specified by the attribute, or
     *         null if the attribute's value is unknown.
     */
    public static String getEndOfLineStringForAttributeValue(final StringPairFileAttribute attribute) {
        Check.notNull(attribute, "attribute"); //$NON-NLS-1$

        if (attribute.getValue().equalsIgnoreCase(FileAttributeValues.EOL_LF)) {
            return "" + NewlineUtils.LINE_FEED; //$NON-NLS-1$
        } else if (attribute.getValue().equalsIgnoreCase(FileAttributeValues.EOL_CRLF)) {
            return "" + NewlineUtils.CARRIAGE_RETURN + NewlineUtils.LINE_FEED; //$NON-NLS-1$
        } else if (attribute.getValue().equalsIgnoreCase(FileAttributeValues.EOL_CR)) {
            return "" + NewlineUtils.CARRIAGE_RETURN; //$NON-NLS-1$
        } else if (attribute.getValue().equalsIgnoreCase(FileAttributeValues.EOL_NATIVE)) {
            return "" + NewlineUtils.PLATFORM_NEWLINE; //$NON-NLS-1$
        } else if (attribute.getValue().equalsIgnoreCase(FileAttributeValues.EOL_NONE)) {
            return ""; //$NON-NLS-1$
        }

        return null;
    }
}
