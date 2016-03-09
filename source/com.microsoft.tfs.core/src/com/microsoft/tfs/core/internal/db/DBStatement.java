// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.internal.db;

import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;
import java.lang.reflect.Array;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class DBStatement {
    private static final Log log = LogFactory.getLog(DBStatement.class);

    private final Connection connection;
    private final String sql;
    private PreparedStatement ps;

    public DBStatement(final Connection connection, final String sql) {
        this.connection = connection;
        this.sql = sql;
    }

    /**
     * The basic update method.
     */
    public int executeUpdate(final Object params) {
        boolean closePs = false;

        final long startTime = System.currentTimeMillis();
        int rowCount = 0;

        try {
            if (ps == null) {
                ps = connection.prepareStatement(sql);
                closePs = true;
            }

            setParameters(ps, params);

            rowCount = ps.executeUpdate();
            return rowCount;
        } catch (final Exception ex) {
            closePs = true;
            throw new DBException(ex, sql);
        } finally {
            if (closePs && ps != null) {
                try {
                    ps.close();
                } catch (final SQLException e) {
                }
            }

            if (log.isTraceEnabled()) {
                final long elapsed = System.currentTimeMillis() - startTime;
                log.trace(MessageFormat.format("query (elapsed={0} rows={1}): {2}", elapsed, rowCount, sql)); //$NON-NLS-1$
            }
        }
    }

    /**
     * Convenience update with no query parameters.
     */
    public int executeUpdate() {
        return executeUpdate(null);
    }

    /**
     * The basic query method.
     */
    public void executeQuery(final Object params, final ResultHandler handler) {
        boolean closePs = false;
        ResultSet rset = null;

        final long startTime = System.currentTimeMillis();
        int rowCount = 0;

        try {
            if (ps == null) {
                ps = connection.prepareStatement(sql);
                closePs = true;
            }

            setParameters(ps, params);

            rset = ps.executeQuery();
            while (rset.next()) {
                ++rowCount;
                handler.handleRow(rset);
            }
        } catch (final Exception ex) {
            closePs = true;
            throw new DBException(ex, sql);
        } finally {
            if (rset != null) {
                try {
                    rset.close();
                } catch (final SQLException e) {
                }
            }

            if (closePs && ps != null) {
                try {
                    ps.close();
                } catch (final SQLException e) {
                }
            }

            if (log.isTraceEnabled()) {
                final long elapsed = System.currentTimeMillis() - startTime;

                log.trace(MessageFormat.format("query (elapsed={0} rows={1}): {2}", elapsed, rowCount, sql)); //$NON-NLS-1$
            }
        }
    }

    /**
     * Convenience query with no parameters.
     */
    public void executeQuery(final ResultHandler handler) {
        executeQuery(null, handler);
    }

    /**
     * Convenience query with a single String result.
     */
    public String executeStringQuery(final Object params) {
        final SingleRowSingleColumnHandler h = new SingleRowSingleColumnHandler();
        executeQuery(params, h);
        final Object obj = h.result;
        if (obj == null) {
            return null;
        }
        if (obj instanceof String) {
            return (String) obj;
        }
        if (obj instanceof Clob) {
            try {
                return clobToString((Clob) obj);
            } catch (final SQLException e) {
                throw new DBException("Error converting Clob to String", e); //$NON-NLS-1$
            } catch (final IOException e) {
                throw new DBException("Error converting Clob to String", e); //$NON-NLS-1$
            }
        } else {
            throw new DBException(MessageFormat.format(
                "unknown return type [{0}] from query [{1}]", //$NON-NLS-1$
                obj.getClass().getName(),
                sql));
        }
    }

    private String clobToString(final Clob clob) throws SQLException, IOException {
        final StringWriter writer = new StringWriter();
        final Reader reader = clob.getCharacterStream();
        final char[] buf = new char[2048];
        int len;
        while ((len = reader.read(buf)) != -1) {
            writer.write(buf, 0, len);
        }

        return writer.toString();
    }

    /**
     * Convenience query with no parameters and a single String result.
     */
    public String executeStringQuery() {
        return executeStringQuery(null);
    }

    /**
     * Convenience query with a single Long or Integer result.
     */
    public long executeNumericQuery(final Object params) {
        final SingleRowSingleColumnHandler h = new SingleRowSingleColumnHandler();
        executeQuery(params, h);
        final Object r = h.result;

        if (r instanceof Long) {
            return ((Long) r).longValue();
        } else if (r instanceof Integer) {
            return ((Integer) r).longValue();
        } else if (r instanceof Short) {
            return ((Short) r).longValue();
        } else if (r instanceof Byte) {
            return ((Byte) r).longValue();
        }

        log.error(
            "Unexpected result type of a numeric query - " + r.getClass().getName(), //$NON-NLS-1$
            new Exception("Fake exception to display the call stack")); //$NON-NLS-1$

        return 0;
    }

    /**
     * Convenience query with no parameters and a single Long or Integer result.
     */
    public long executeNumericQuery() {
        return executeNumericQuery(null);
    }

    /**
     * Convenience query with a single Long result.
     */
    public Long executeLongQuery(final Object params) {
        final SingleRowSingleColumnHandler h = new SingleRowSingleColumnHandler();
        executeQuery(params, h);
        return ((Long) h.result);
    }

    /**
     * Convenience query with no parameters and a single Long result.
     */
    public Long executeLongQuery() {
        return executeLongQuery(null);
    }

    /**
     * Convenience query with a single Integer result.
     */
    public Integer executeIntQuery(final Object params) {
        final SingleRowSingleColumnHandler h = new SingleRowSingleColumnHandler();
        executeQuery(params, h);
        return ((Integer) h.result);
    }

    /**
     * Convenience query with no parameters and a single Integer result.
     */
    public Integer executeIntQuery() {
        return executeIntQuery(null);
    }

    /**
     * Convenience query with multiple columns in the result.
     */
    public Object[] executeMultiColumnQuery(final Object params) {
        final SingleRowMultipleColumnsHandler h = new SingleRowMultipleColumnsHandler();
        executeQuery(params, h);
        return h.results;
    }

    /**
     * Convenience query with multiple columns in the result and no parameters.
     */
    public Object[] executeMultiColumnQuery() {
        return executeMultiColumnQuery(null);
    }

    /**
     * Convenience query that returns an array of a primitive component type.
     */
    public Object executeQueryForPrimitiveArray(final Object params, final Class primitiveType) {
        if (primitiveType == null || !primitiveType.isPrimitive()) {
            throw new IllegalArgumentException("primitiveType must be a non-null primitive type"); //$NON-NLS-1$
        }

        final List results = new ArrayList();

        executeQuery(params, new ResultHandler() {
            @Override
            public void handleRow(final ResultSet rset) throws SQLException {
                results.add(rset.getObject(1));
            }
        });

        final int len = results.size();
        final Object returnArray = Array.newInstance(primitiveType, len);

        for (int i = 0; i < len; i++) {
            Array.set(returnArray, i, results.get(i));
        }

        return returnArray;
    }

    /**
     * Convenience query with no parameters that returns an array of a primitive
     * component type.
     */
    public Object executeQueryForPrimitiveArray(final Class primitiveType) {
        return executeQueryForPrimitiveArray(null, primitiveType);
    }

    private void setParameters(final PreparedStatement ps, final Object args) throws SQLException {
        if (args == null) {
            return;
        }

        if (!args.getClass().isArray()) {
            ps.setObject(1, args);
        } else {
            final int len = Array.getLength(args);
            for (int i = 0; i < len; i++) {
                ps.setObject(i + 1, Array.get(args, i));
            }
        }
    }

    private class SingleRowSingleColumnHandler implements ResultHandler {
        public Object result = null;
        private int rowCount = 0;

        @Override
        public void handleRow(final ResultSet rset) throws SQLException {
            ++rowCount;
            if (rowCount > 1) {
                throw new RuntimeException(MessageFormat.format("the query [{0}] returned more than one row", sql)); //$NON-NLS-1$
            }
            result = rset.getObject(1);
        }
    }

    private class SingleRowMultipleColumnsHandler implements ResultHandler {
        public Object[] results = null;
        private int rowCount = 0;

        @Override
        public void handleRow(final ResultSet rset) throws SQLException {
            ++rowCount;
            if (rowCount > 1) {
                throw new RuntimeException(MessageFormat.format("the query [{0}] returned more than one row", sql)); //$NON-NLS-1$
            }

            final int colCount = rset.getMetaData().getColumnCount();

            results = new Object[colCount];

            for (int i = 0; i < results.length; i++) {
                results[i] = rset.getObject(i + 1);
            }
        }
    }

    public void beginBatch() {
        try {
            ps = connection.prepareStatement(sql);
        } catch (final SQLException e) {
            throw new DBException(e, sql);
        }
    }

    public void finishBatch() {
        if (ps != null) {
            try {
                ps.close();
            } catch (final SQLException e) {
                throw new DBException(e, sql);
            }
        }
    }
}
