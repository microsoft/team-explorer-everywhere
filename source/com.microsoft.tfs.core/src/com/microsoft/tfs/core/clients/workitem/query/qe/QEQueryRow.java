// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.query.qe;

/**
 * @since TEE-SDK-10.1
 */
public interface QEQueryRow {
    public String getFieldName();

    public void setFieldName(String fieldName);

    public String getLogicalOperator();

    public void setLogicalOperator(String logicalOperator);

    public String getOperator();

    public void setOperator(String operator);

    public String getValue();

    public void setValue(String value);
}
