// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.framework.diagnostics.extend;

import java.util.Locale;

import com.microsoft.tfs.client.common.ui.framework.diagnostics.DiagnosticLocale;

/**
 * Provides an interface for simple data providers to implement to allow
 * localization.
 */
public abstract class LocalizedDataProvider implements DataProvider {
    @Override
    public final Object getData() {
        return getData(DiagnosticLocale.USER_LOCALE);
    }

    @Override
    public final Object getDataNOLOC() {
        return getData(DiagnosticLocale.SUPPORT_LOCALE);
    }

    protected abstract Object getData(Locale locale);
}
