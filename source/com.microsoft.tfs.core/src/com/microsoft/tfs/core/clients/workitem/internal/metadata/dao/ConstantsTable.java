// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.metadata.dao;

public interface ConstantsTable {
    public ConstantMetadata getConstantByString(String string);

    public String getConstantByID(int id);

    public Integer getIDByConstant(String constant);

    public String[] getUserGroupDisplayNames(final String serverGuid, final String projectGuid);
}