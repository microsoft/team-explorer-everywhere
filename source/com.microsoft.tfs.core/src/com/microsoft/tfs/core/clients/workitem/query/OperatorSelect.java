// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.query;

import com.microsoft.tfs.core.clients.workitem.fields.FieldType;

/*
 * TODO This class is unused as of API review on 2010/11/05. Remove?
 */

public interface OperatorSelect {
    public Integer getValue();

    public void setValue(Integer value);

    public FieldType getType();

    public void setType(FieldType type);

    public Integer getOperatorKey();

    public void setAlternate(Integer type);

    public void setUseAlternate(boolean use);

    public boolean useAlternate();
}
