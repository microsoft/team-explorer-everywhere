// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.fields;

import com.microsoft.tfs.core.clients.workitem.fields.AllowedValuesCollection;

public class AllowedValuesCollectionImpl extends ValuesCollectionImpl implements AllowedValuesCollection {
    public AllowedValuesCollectionImpl(final String[] values, final int psType) {
        super(values, psType);
    }
}
