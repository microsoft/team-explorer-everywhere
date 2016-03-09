// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.metadata.impl;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;

import com.microsoft.tfs.core.Messages;
import com.microsoft.tfs.core.clients.workitem.internal.metadata.dao.ConstantMetadata;
import com.microsoft.tfs.core.clients.workitem.internal.metadata.dao.ConstantsTable;
import com.microsoft.tfs.core.clients.workitem.internal.metadata.dao.LookupFailedException;
import com.microsoft.tfs.core.internal.db.ResultHandler;

public class ConstantsTableImpl extends BaseMetadataDAO implements ConstantsTable {
    @Override
    public ConstantMetadata getConstantByString(final String string) {
        final ConstantMetadata[] constantMetadata = new ConstantMetadata[1];

        getConnection().createStatement(
            ConstantMetadata.SELECT_STRING + " where lower(String) = lower(?)").executeQuery( //$NON-NLS-1$
                new Object[] {
                    string
        }, new ResultHandler() {
            @Override
            public void handleRow(final ResultSet rset) throws SQLException {
                if (constantMetadata[0] != null) {
                    throw new IllegalStateException(
                        MessageFormat.format("constant [{0}] returned more than one result", string)); //$NON-NLS-1$
                }

                constantMetadata[0] = ConstantMetadata.fromRow(rset);
            }
        });

        /*
         * return null if we didn't get any rows
         */
        return constantMetadata[0];
    }

    @Override
    public String getConstantByID(final int id) {
        final Object[] results = getConnection().createStatement(
            "select String, DisplayName from Constants where ConstID = ?").executeMultiColumnQuery(new Integer(id)); //$NON-NLS-1$

        if (results == null) {
            throw new LookupFailedException(
                MessageFormat.format(
                    Messages.getString("ConstantsTableImpl.NoConstantExistsWithConstIDFormat"), //$NON-NLS-1$
                    Integer.toString(id)));
        }

        return (String) (results[1] != null ? results[1] : results[0]);
    }

    @Override
    public Integer getIDByConstant(final String constant) {
        return getConnection().createStatement(
            "select ConstID from Constants where lower(String) = lower(?)").executeIntQuery( //$NON-NLS-1$
                new Object[] {
                    constant
        });
    }

    @Override
    public String[] getUserGroupDisplayNames(final String serverGuid, final String projectGuid) {
        // Query the constant IDs. The set of CONSTANTSETS entries with a
        // parentID value of -1 are the groups and users. The set of
        // CONSTANTSETS entries with a parentID value of -2 are users. Query for
        // the groups and users, then exclude the users to get the constant
        // identifiers for groups.

        final HashSet set = new HashSet();
        final String sql =
            "select ConstID from ConstantSets where ParentId = -1 and fDeleted = 0 and ConstID not in (select ConstID From ConstantSets where ParentId = -2 and fDeleted = 0)"; //$NON-NLS-1$

        getConnection().createStatement(sql).executeQuery(new Object[] {}, new ResultHandler() {
            @Override
            public void handleRow(final ResultSet rset) throws SQLException {
                set.add(new Integer(rset.getInt(1)));
            }
        });

        // Now query the CONSTANTS table for the constant entries. Exclude any
        // entries that are not a match for the specified server and project
        // guids.

        boolean needComma = false;
        final StringBuffer sb = new StringBuffer("("); //$NON-NLS-1$
        for (final Iterator it = set.iterator(); it.hasNext();) {
            if (needComma) {
                sb.append(","); //$NON-NLS-1$
            }
            sb.append(((Integer) it.next()).intValue());
            needComma = true;
        }
        sb.append(")"); //$NON-NLS-1$

        final String SQL = "select ConstID, String, DisplayName from Constants where ConstID in " + sb.toString(); //$NON-NLS-1$
        final ArrayList displayNames = new ArrayList();
        getConnection().createStatement(SQL).executeQuery(new ResultHandler() {
            @Override
            public void handleRow(final ResultSet rset) throws SQLException {
                rset.getInt(1);
                final String string = rset.getString(2);
                final String displayName = rset.getString(3);

                if (string.indexOf(serverGuid) >= 0 || string.indexOf(projectGuid) >= 0) {
                    displayNames.add(displayName != null ? displayName : string);
                }
            }
        });

        // Return a sorted list of group display names.
        final String[] values = (String[]) displayNames.toArray(new String[displayNames.size()]);
        Arrays.sort(values);
        return values;
    }
}
