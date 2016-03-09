// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.search;

public interface VSSearchParseError {
    public final static int NONE = 0;
    public final static int UNMATCHED_QUOTES = 1;
    public final static int INVALID_ESCAPE = 2;
    public final static int EMPTY_FILTER_FIELD = 4;
    public final static int EMPTY_FILTER_VALUE = 8;
}