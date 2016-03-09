// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.wiqlparse;

import java.util.Locale;
import java.util.TimeZone;

public interface IExternal {
    /*
     * Return null if not found. The return is stored as the tag of
     * NodeFieldName.
     */
    public Object findField(String name, String prefix, Object tableTag);

    /*
     * Return null if not found. The return is stored as the tag of
     * NodeTableName.
     */
    public Object findTable(String name);

    public Object findVariable(String name);

    public DataType getFieldDataType(Object fieldTag);

    public DataType getVariableDataType(Object variableTag);

    /*
     * Called for every node when doing optimize operation.
     */
    public Node optimizeNode(Node node, NodeTableName tableContext, NodeFieldName fieldContext);

    /*
     * Called for every node when doing bind operation.
     */
    public void verifyNode(Node node, NodeTableName tableContext, NodeFieldName fieldContext);

    public Locale getLocale();

    public TimeZone getTimeZone();
}
