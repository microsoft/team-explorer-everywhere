// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.type;

import java.text.MessageFormat;

import com.microsoft.tfs.util.GUID;

public class WIGUIDTypeConverter implements WITypeConverter {
    @Override
    public Object translate(final Object input, final WIValueSource valueSource) throws WITypeConverterException {
        if (input == null) {
            return null;
        }

        if (input instanceof GUID) {
            return input;
        }

        if (input instanceof String) {
            final String valueToParse = ((String) input).trim();

            try {
                return new GUID(valueToParse);
            } catch (final IllegalArgumentException e) {
                throw new WITypeConverterException(
                    MessageFormat.format(
                        "unable to convert value [{0}] to a GUID (SERVER value)", //$NON-NLS-1$
                        valueToParse),
                    e);
            }
        }

        throw new WITypeConverterException(
            MessageFormat.format(
                "unable to convert value [{0}] to a GUID (SERVER value)", //$NON-NLS-1$
                input.getClass().getName()));
    }

    @Override
    public String toString(final Object data) {
        if (data == null) {
            return null;
        }

        return data.toString();
    }

}
