// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.config;

import java.util.Locale;
import java.util.TimeZone;

import com.microsoft.tfs.core.config.ConnectionInstanceData;
import com.microsoft.tfs.core.config.DefaultConnectionAdvisor;

public class CommonClientConnectionAdvisor extends DefaultConnectionAdvisor {
    /**
     * Creates a {@link CommonClientConnectionAdvisor} that returns the
     * specified {@link Locale} and {@link TimeZone} for all
     * {@link ConnectionInstanceData}s.
     *
     * @param locale
     *        the locale (must not be <code>null</code>)
     * @param timeZone
     *        the time zone (must not be <code>null</code>)
     */
    public CommonClientConnectionAdvisor(final Locale locale, final TimeZone timeZone) {
        super(locale, timeZone);
    }
}
