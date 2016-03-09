// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.diagnostics;

import com.microsoft.tfs.client.common.ui.framework.diagnostics.extend.DataProvider;
import com.microsoft.tfs.core.product.ProductInformation;

public class ApplicationDataProvider implements DataProvider {
    @Override
    public Object getData() {
        return ProductInformation.getCurrent().getProductShortName();
    }

    @Override
    public Object getDataNOLOC() {
        return ProductInformation.getCurrent().getProductShortNameNOLOC();
    }
}
