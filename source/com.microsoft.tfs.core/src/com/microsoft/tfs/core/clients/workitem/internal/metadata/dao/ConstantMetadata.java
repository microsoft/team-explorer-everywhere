// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.metadata.dao;

import java.sql.ResultSet;
import java.sql.SQLException;

public class ConstantMetadata {
    public static final String SELECT_STRING = "select " + "ConstID," + "DisplayName," + "String" + " from Constants"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$

    public static ConstantMetadata fromRow(final ResultSet rset) throws SQLException {
        return new ConstantMetadata(rset.getInt(1), rset.getString(2), rset.getString(3));
    }

    private final int constId;
    private final String displayName;
    private final String string;

    public ConstantMetadata(final int constId, final String displayName, final String string) {
        this.constId = constId;
        this.displayName = displayName;
        this.string = string;
    }

    public int getConstID() {
        return constId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getString() {
        return string;
    }
}
