// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.clc.options;

import com.microsoft.tfs.client.clc.exceptions.InvalidOptionValueException;
import com.microsoft.tfs.core.util.CodePageMapping;
import com.microsoft.tfs.core.util.FileEncoding;

public abstract class FileEncodingValueOption extends SingleValueOption {
    public FileEncodingValueOption() {
        super();
    }

    public FileEncoding getValueAsEncoding() throws InvalidOptionValueException {
        String v = getValue().trim();

        if (v.length() == 0) {
            return null;
        }

        /*
         * If the string is one of our special types ("auto", "binary", "text"),
         * then we do special things.
         */
        if (v.equalsIgnoreCase("binary")) //$NON-NLS-1$
        {
            return FileEncoding.BINARY;
        } else if (v.equalsIgnoreCase("text")) //$NON-NLS-1$
        {
            return FileEncoding.DEFAULT_TEXT;
        } else if (v.equalsIgnoreCase("auto")) //$NON-NLS-1$
        {
            return FileEncoding.AUTOMATICALLY_DETECT;
        } else {
            /*
             * The user gave us a string that names a code page (or encoding
             * type). If we can parse the string value as an integer we trust it
             * ultimately and create an encoding for it. Otherwise we look it
             * up, and failing that, use binary.
             */

            try {
                final int codePage = Integer.parseInt(v);
                return new FileEncoding(codePage);
            } catch (final NumberFormatException e) {
            }

            /*
             * TFS/Windows and Java default to different byte orders when told
             * to use the endian-unspecified encoding name "UTF-16" (and
             * "UTF-32"). TFS/Windows considers "UTF-16" and "UTF-32" to mean
             * "little-endian if no BOM" when parsed as this option. Java's
             * Charset class and our CodePageMapping class do the opposite:
             * assume big-endian if no BOM. (See CodePageMapping for details on
             * this situation.)
             *
             * We use the the TFS/Windows convention of defaulting to
             * little-endian when parsing "UTF-16" or "UTF-32" to be compatible
             * with the VS CLC running on Windows. This differs from the UI
             * Eclipse presents, where UTF-16 means big-endian. Compatibility
             * with the VS CLC is more important here.
             */
            if ("UTF-16".equalsIgnoreCase(v) || "UTF16".equalsIgnoreCase(v) || "UTF_16".equalsIgnoreCase(v)) //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            {
                v = "UTF-16LE"; //$NON-NLS-1$
            } else if ("UTF-32".equalsIgnoreCase(v) || "UTF32".equalsIgnoreCase(v) || "UTF_32".equalsIgnoreCase(v)) //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            {
                v = "UTF-32LE"; //$NON-NLS-1$
            }

            final FileEncoding e = new FileEncoding(CodePageMapping.getCodePage(v, false));
            return (e.getCodePage() != 0) ? e : FileEncoding.BINARY;
        }
    }

}