// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.util;

import java.text.DateFormat;
import java.util.Date;

public class DateHelper {
    /**
     * The default {@link DateFormat} used to format {@link Date}s that should
     * have both date and time displayed. Controls that render such {@link Date}
     * s should use this instance of {@link DateFormat} by default, but should
     * also allow for clients to set a custom {@link DateFormat}.
     */
    public static DateFormat getDefaultDateTimeFormat() {
        return DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM);
    }

    /**
     * The default {@link DateFormat} to be used in property pages (used by
     * implementations of {@link IPropertySource}).
     */
    public static DateFormat getDefaultPropertyPageDateTimeFormat() {
        return DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.LONG);
    }
}
