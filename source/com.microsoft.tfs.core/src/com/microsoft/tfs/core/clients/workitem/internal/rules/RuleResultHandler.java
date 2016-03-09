// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.rules;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import com.microsoft.tfs.core.internal.db.ResultHandler;

public class RuleResultHandler implements ResultHandler {
    private final List<Rule> rules;

    public RuleResultHandler(final List<Rule> rules) {
        this.rules = rules;
    }

    @Override
    public void handleRow(final ResultSet rset) throws SQLException {
        rules.add(Rule.fromRow(rset));
    }
}
