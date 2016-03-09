// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.metadata.mapper;

import java.sql.Timestamp;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class HSQLMapper implements SQLMapper {
    private final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S Z"); //$NON-NLS-1$

    @Override
    public String getSQLColumnTypeFromMetadataColumnType(
        final String type,
        final String tableName,
        final String columnName) {
        if (MetadataColumnTypes.GUID.equals(type)) {
            return "varchar(80)"; //$NON-NLS-1$
        } else if (MetadataColumnTypes.INTEGER.equals(type)) {
            return "int"; //$NON-NLS-1$
        } else if (MetadataColumnTypes.STRING.equals(type)) {
            final int len = SQLStringMappings.getStringColumnLength(tableName, columnName);
            if (len == -1) {
                return "longvarchar"; //$NON-NLS-1$
            } else {
                return "varchar(" + len + ")"; //$NON-NLS-1$ //$NON-NLS-2$
            }
        } else if (MetadataColumnTypes.BOOLEAN.equals(type)) {
            return "bit"; //$NON-NLS-1$
        } else if (MetadataColumnTypes.DATE.equals(type)) {
            return "datetime"; //$NON-NLS-1$
        } else if (MetadataColumnTypes.LONG.equals(type)) {
            return "bigint"; //$NON-NLS-1$
        }

        return "text"; //$NON-NLS-1$
    }

    @Override
    public Object getSQLObject(final String type, final String value) {
        if (MetadataColumnTypes.INTEGER.equals(type)) {
            return Integer.valueOf(value);
        } else if (MetadataColumnTypes.BOOLEAN.equals(type)) {
            return "true".equalsIgnoreCase(value) ? Boolean.TRUE : Boolean.FALSE; //$NON-NLS-1$
        } else if (MetadataColumnTypes.LONG.equals(type)) {
            return Long.valueOf(value);
        } else if (MetadataColumnTypes.DATE.equals(type)) {
            if (value == null || value.trim().length() == 0) {
                return null;
            } else {
                Date d = null;
                String pVal = null;
                try {
                    pVal = value.substring(0, 10) + " " + value.substring(11) + " -0000"; //$NON-NLS-1$ //$NON-NLS-2$
                    d = DATE_FORMAT.parse(pVal);
                } catch (final Exception ex) {
                    throw new RuntimeException(MessageFormat.format("unable to parse [{0}]", pVal), ex); //$NON-NLS-1$
                }
                final Timestamp ts = new Timestamp(d.getTime());
                return ts;
            }
        } else {
            return value;
        }

    }
}
