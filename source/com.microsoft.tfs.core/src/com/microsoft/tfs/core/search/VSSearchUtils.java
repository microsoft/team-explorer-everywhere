// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.search;

import com.microsoft.tfs.core.search.internal.VSSearchQueryParser;

public abstract class VSSearchUtils {
    /**
     * @return a new {@link IVSSearchQueryParser} instance
     */
    public static IVSSearchQueryParser createSearchQueryParser() {
        return new VSSearchQueryParser();
    }
}
