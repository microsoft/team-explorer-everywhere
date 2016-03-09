// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.metadata.dao;

import java.sql.ResultSet;
import java.sql.SQLException;

import com.microsoft.tfs.util.GUID;

public class NodeMetadata {
    public static final String SELECT_STRING = "select " //$NON-NLS-1$
        + "AreaID," //$NON-NLS-1$
        + "ParentID," //$NON-NLS-1$
        + "TypeID," //$NON-NLS-1$
        + "StructureType," //$NON-NLS-1$
        + "Name," //$NON-NLS-1$
        + "GUID" //$NON-NLS-1$
        + " from Hierarchy"; //$NON-NLS-1$

    public static NodeMetadata fromRow(final ResultSet rset) throws SQLException {
        final int id = rset.getInt(1);
        final int parentId = rset.getInt(2);
        final int nodeType = rset.getInt(3);
        final int structureType = rset.getInt(4);
        final String name = rset.getString(5);
        final String guidString = rset.getString(6);

        final GUID guid = guidString.equals("0") ? GUID.EMPTY : new GUID(guidString); //$NON-NLS-1$

        return new NodeMetadata(id, parentId, nodeType, structureType, name, guid);
    }

    private final int id;
    private final int parentId;
    private final int nodeType;
    private final int structureType;
    private final String name;
    private final GUID guid;

    public NodeMetadata(
        final int id,
        final int parentId,
        final int nodeType,
        final int structureType,
        final String name,
        final GUID guid) {
        this.id = id;
        this.parentId = parentId;
        this.nodeType = nodeType;
        this.structureType = structureType;
        this.name = name;
        this.guid = guid;
    }

    public GUID getGUID() {
        return guid;
    }

    public int getID() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getNodeType() {
        return nodeType;
    }

    public int getParentID() {
        return parentId;
    }

    public int getStructureType() {
        return structureType;
    }
}
