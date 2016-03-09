// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.framework.diagnostics;

import java.util.Locale;

import com.microsoft.tfs.util.LocaleUtil;

public class DiagnosticLocale {
    /**
     * The user locale for diagnostic data (the locale to display in the UI.)
     */
    public static final Locale USER_LOCALE = Locale.getDefault();

    /**
     * The support locale for diagnostic data (not localized for support
     * personnel, used to create the diagnostic zip file.)
     */
    public static final Locale SUPPORT_LOCALE = LocaleUtil.ROOT;
}
