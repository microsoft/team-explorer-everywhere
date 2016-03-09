// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.framework.diagnostics.extend;

import java.util.Locale;

import com.microsoft.tfs.util.LocaleUtil;

public interface DataProvider {
    /**
     * Gets diagnostic data in the user's current locale (
     * {@link Locale#getDefault()}.
     *
     * @return An {@link Object} representing diagnostic data in the user's
     *         current locale.
     */
    public Object getData();

    /**
     * Gets diagnostic data in the appropriate locale for support personnel
     * (typically {@link LocaleUtil#ROOT}.)
     *
     * @return An {@link Object} representing diagnostic data in the support
     *         locale.
     */
    public Object getDataNOLOC();
}
