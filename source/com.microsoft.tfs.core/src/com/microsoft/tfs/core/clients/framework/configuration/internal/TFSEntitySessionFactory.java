// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.framework.configuration.internal;

import java.text.MessageFormat;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.microsoft.tfs.core.TFSConfigurationServer;
import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.core.clients.framework.configuration.TFSEntitySession;
import com.microsoft.tfs.util.Check;

public class TFSEntitySessionFactory {
    private static final Log log = LogFactory.getLog(TFSEntitySessionFactory.class);

    public static TFSEntitySession newEntitySession(final TFSConfigurationServer connection) {
        Check.notNull(connection, "connection"); //$NON-NLS-1$

        return new TFSCatalogEntitySession(connection);
    }

    public static TFSEntitySession newEntitySession(final TFSTeamProjectCollection connection) {
        Check.notNull(connection, "connection"); //$NON-NLS-1$

        if (connection.getConfigurationServer() != null) {
            log.info(MessageFormat.format(
                "Creating catalog-based configuration entity session for {0}", //$NON-NLS-1$
                connection.getName()));
            return new TFSCatalogEntitySession(connection.getConfigurationServer());
        } else {
            log.info(MessageFormat.format(
                "Creating compatibility configuration entity session for {0}", //$NON-NLS-1$
                connection.getName()));
            return new TFSCompatibilityEntitySession(connection);
        }
    }
}
