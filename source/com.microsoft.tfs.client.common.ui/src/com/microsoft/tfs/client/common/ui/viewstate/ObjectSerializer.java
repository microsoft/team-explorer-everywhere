// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.viewstate;

public interface ObjectSerializer {
    public String toString(Object object);

    public Object fromString(String string);
}
