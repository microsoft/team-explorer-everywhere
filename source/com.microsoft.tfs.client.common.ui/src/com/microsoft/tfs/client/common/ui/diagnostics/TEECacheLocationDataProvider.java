// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.diagnostics;

import com.microsoft.tfs.client.common.ui.framework.diagnostics.extend.DataProvider;
import com.microsoft.tfs.client.common.ui.framework.diagnostics.extend.NonLocalizedDataProvider;
import com.microsoft.tfs.core.config.persistence.DefaultPersistenceStoreProvider;

public class TEECacheLocationDataProvider extends NonLocalizedDataProvider implements DataProvider {
    @Override
    public Object getData() {
        return DefaultPersistenceStoreProvider.INSTANCE.getCachePersistenceStore().getStoreFile();
    }
}
