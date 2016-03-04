// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.vss.client.core.model;

import java.util.Date;
import java.util.HashMap;
import java.util.List;

import com.microsoft.vss.client.core.utils.JsonHelper;
import com.microsoft.vss.client.core.utils.StringUtil;

public class NameValueCollection extends HashMap<String, String> {

    public <TValue> void addIfNotEmpty(final String parameterName, final List<TValue> values) {
        if (values != null && !values.isEmpty()) {
            put(parameterName, StringUtil.join(",", values)); //$NON-NLS-1$
        }
    }

    public void addIfNotEmpty(final String parameterName, final String value) {
        if (!StringUtil.isNullOrEmpty(value)) {
            put(parameterName, value);
        }
    }

    public <TValue> void addIfNotNull(final String parameterName, final TValue value) {
        if (value != null) {
            if (value instanceof Date) {
                put(parameterName, JsonHelper.getDateFormat().format((Date) value));
            } else {
                put(parameterName, value.toString());
            }
        }
    }
}
