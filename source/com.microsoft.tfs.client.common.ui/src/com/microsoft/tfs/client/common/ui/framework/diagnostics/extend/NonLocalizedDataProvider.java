// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.framework.diagnostics.extend;

/**
 * Used for data providers that do not have localized data (ie, raw log files,
 * etc.) Simply invokes the {@link DataProvider#getData()} method for the
 * non-localized version.
 */
public abstract class NonLocalizedDataProvider implements DataProvider {
    @Override
    public final Object getDataNOLOC() {
        return getData();
    }
}
