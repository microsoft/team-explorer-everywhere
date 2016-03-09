// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.type;

public class WIIdentityTypeConverter implements WITypeConverter {
    @Override
    public Object translate(final Object input, final WIValueSource valueSource) throws WITypeConverterException {
        return input;
    }

    @Override
    public String toString(final Object data) {
        return (data == null ? null : data.toString());
    }
}
