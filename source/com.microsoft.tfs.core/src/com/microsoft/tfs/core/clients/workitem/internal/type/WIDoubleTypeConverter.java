// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.type;

import java.text.MessageFormat;

import com.microsoft.tfs.core.clients.workitem.WorkItemUtils;

public class WIDoubleTypeConverter implements WITypeConverter {
    @Override
    public Object translate(final Object input, final WIValueSource valueSource) throws WITypeConverterException {
        Double translatedValue = null;

        if (input != null) {
            if (input instanceof Double) {
                translatedValue = (Double) input;
            } else if (input instanceof String) {
                final String trimmed = ((String) input).trim();
                if (trimmed.length() == 0) {
                    return null;
                }

                try {
                    translatedValue = Double.valueOf(trimmed);
                } catch (final NumberFormatException ex) {
                    throw new WITypeConverterException("unable to convert input to a double", ex); //$NON-NLS-1$
                }
            } else {
                throw new WITypeConverterException(MessageFormat.format(
                    "unable to convert [{0}] to a double", //$NON-NLS-1$
                    input.getClass().getName()));
            }
        }

        return translatedValue;
    }

    @Override
    public String toString(final Object data) {
        if (data == null) {
            return null;
        }

        return WorkItemUtils.objectToString(data);
    }
}
