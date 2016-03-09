// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.metadata;

import java.util.Set;

public interface IConstantSet {
    public Set<String> getValues();

    public boolean patternMatch(Object input, String debuggingInfo);

    public boolean contains(String valueToTest);

    public boolean containsConstID(int constId);

    public int getSize();

    public int getQueryCount();

    public String[] toArray();
}
