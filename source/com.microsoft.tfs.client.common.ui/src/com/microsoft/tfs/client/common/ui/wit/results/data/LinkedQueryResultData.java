// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.wit.results.data;

public interface LinkedQueryResultData extends QueryResultData {
    public String getLinkTypeName(int displayRowIndex);

    public boolean isLinkLocked(int displayRowIndex);
}
