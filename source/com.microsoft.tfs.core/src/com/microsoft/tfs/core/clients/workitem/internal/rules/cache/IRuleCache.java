// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.rules.cache;

import com.microsoft.tfs.core.clients.workitem.internal.rules.cache.RuleCache.RuleCacheResults;

public interface IRuleCache {
    public RuleCacheResults getRules(int areaId);

    public RuleCacheResults getRules(int areaId, int changedFieldId);
}
