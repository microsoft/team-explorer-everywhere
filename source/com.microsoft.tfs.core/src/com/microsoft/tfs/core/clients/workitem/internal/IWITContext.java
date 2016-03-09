// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal;

import com.microsoft.tfs.core.clients.workitem.internal.metadata.IMetadata;
import com.microsoft.tfs.core.clients.workitem.internal.rules.cache.IRuleCache;

public interface IWITContext {
    public IRuleCache getRuleCache();

    public String getCurrentUserDisplayName();

    public IMetadata getMetadata();
}
