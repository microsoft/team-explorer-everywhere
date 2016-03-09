// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.rules;

import java.util.Collection;

public interface IFieldPickListSupport {
    public void reset();

    public void addAllowedValues(Collection<String> values);

    public void addSuggestedValues(Collection<String> values);

    public void addProhibitedValues(Collection<String> values);
}
