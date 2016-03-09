// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.metadata;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.microsoft.tfs.core.clients.workitem.internal.rules.MatchPattern;
import com.microsoft.tfs.core.internal.db.DBConnection;
import com.microsoft.tfs.core.internal.db.DBTask;
import com.microsoft.tfs.core.internal.db.ResultHandler;

public class ConstantSet implements IConstantSet {
    private static final Log log = LogFactory.getLog(ConstantSet.class);

    /*
     * The Set<Integer> of all distinct constant set IDs in the WIT metadata
     * cache. In other words, this Set contains each Constant ID that refers to
     * a Constant is a set (has children Constants as members). This value is
     * used during population of each ConstantSet and is shared by all
     * ConstantSets.
     */
    private Set<Integer> distinctConstantSetIds;

    /*
     * The Set<String> of values that make up this ConstantSet.
     */
    private final Set<String> values = new HashSet<String>();

    /*
     * The Set<Integer> of Constant IDs that refer to the Constants that make up
     * this ConstantSet.
     */
    private final Set<Integer> constIds = new HashSet<Integer>();

    /*
     * Kept for debugging purposes - keep track of how many queries we needed to
     * perform against the WIT metadata to populate this ConstantSet. Will be -1
     * when this ConstantSet is artificially constructed without doing queries.
     */
    private int queryCount;

    /**
     * A convenience method for unit tests.
     */
    public static ConstantSet newSingletonSet(final int constId, final String value) {
        return new ConstantSet(new String[] {
            value
        }, new int[] {
            constId
        }, -1);
    }

    /**
     * A convenience method for unit tests.
     */
    public static ConstantSet newSet(final int[] constIds, final String[] values) {
        return new ConstantSet(values, constIds, -1);
    }

    public ConstantSet(
        final Metadata metadata,
        final int rootId,
        final boolean oneLevel,
        final boolean twoPlusLevels,
        final boolean leaf,
        final boolean interior) {
        this(metadata, new int[] {
            rootId
        }, oneLevel, twoPlusLevels, leaf, interior);
    }

    public ConstantSet(
        final Metadata metadata,
        final int[] rootIds,
        final boolean oneLevel,
        final boolean twoPlusLevels,
        final boolean leaf,
        final boolean interior) {
        distinctConstantSetIds = metadata.getDistinctConstantSetIDs();

        metadata.getConnectionPool().executeWithPooledConnection(new DBTask() {
            @Override
            public void performTask(final DBConnection connection) {
                populate(connection, rootIds, oneLevel, twoPlusLevels, leaf, interior);
            }
        });
    }

    private ConstantSet(final String[] values, final int[] constIds, final int queryCount) {
        this.values.addAll(Arrays.asList(values));

        for (int i = 0; i < constIds.length; i++) {
            this.constIds.add(new Integer(constIds[i]));
        }

        this.queryCount = queryCount;
    }

    @Override
    public int getSize() {
        return values.size();
    }

    @Override
    public int getQueryCount() {
        return queryCount;
    }

    @Override
    public String[] toArray() {
        return values.toArray(new String[values.size()]);
    }

    /*
     * A note:
     *
     * The main use case for multiple root ids (starting ids) is to improve
     * performance when calculating allowed values for a field definition.
     *
     * See AllowedValuesHelper for the details on this scenario.
     */

    private void populate(
        final DBConnection connection,
        final int[] startingRootIds,
        final boolean oneLevel,
        final boolean twoPlusLevels,
        final boolean leaf,
        final boolean interior) {
        if (log.isDebugEnabled()) {
            final StringBuffer sb = new StringBuffer("["); //$NON-NLS-1$
            for (int i = 0; i < startingRootIds.length; i++) {
                sb.append(i);
                if (i < startingRootIds.length - 1) {
                    sb.append(","); //$NON-NLS-1$
                }
            }
            sb.append("]"); //$NON-NLS-1$

            log.debug(
                MessageFormat.format(
                    "populate ConstantSet startingRootIds={0} oneLevel={1} twoPlusLevels={2} leaf={3} interior={4}", //$NON-NLS-1$
                    sb.toString(),
                    oneLevel,
                    twoPlusLevels,
                    leaf,
                    interior));
        }

        if (!oneLevel && !twoPlusLevels) {
            /*
             * singleton case: each root id is a singleton constant
             */

            final StringBuffer sb = new StringBuffer("("); //$NON-NLS-1$
            for (int i = 0; i < startingRootIds.length; i++) {
                sb.append(startingRootIds[i]);
                if (i < (startingRootIds.length - 1)) {
                    sb.append(","); //$NON-NLS-1$
                }
            }
            sb.append(")"); //$NON-NLS-1$

            final String SQL = "select ConstID, String, DisplayName from Constants where ConstID in " + sb.toString(); //$NON-NLS-1$

            ++queryCount;
            connection.createStatement(SQL).executeQuery(new ResultHandler() {
                @Override
                public void handleRow(final ResultSet rset) throws SQLException {
                    final int constId = rset.getInt(1);
                    final String string = rset.getString(2);
                    final String displayName = rset.getString(3);
                    values.add(displayName != null ? displayName : string);
                    constIds.add(new Integer(constId));
                }
            });
        } else {
            /*
             * We keep track of all parent IDs. This is because cycles can exist
             * in the graph and we only want to visit each parent once.
             */
            final Set<Integer> allParentIds = new HashSet<Integer>();

            /*
             * create an initial set of root IDs
             */
            Set<Integer> rootIds = new HashSet<Integer>();
            for (int i = 0; i < startingRootIds.length; i++) {
                rootIds.add(new Integer(startingRootIds[i]));
            }
            allParentIds.addAll(rootIds);

            rootIds = query(
                connection,
                rootIds,
                (oneLevel && leaf), // do we
                // want
                // first-level
                // leaf
                // nodes?
                (oneLevel && interior), // do we want first-level non-leaf
                // nodes?
                twoPlusLevels); // do we want to process deeper levels?

            rootIds.removeAll(allParentIds);
            allParentIds.addAll(rootIds);

            while (rootIds.size() > 0) {
                rootIds = query(connection, rootIds, leaf, interior, true);

                rootIds.removeAll(allParentIds);
                allParentIds.addAll(rootIds);
            }
        }
    }

    private Set<Integer> getChildIDs(final DBConnection connection, final Set<Integer> parentIds) {
        final StringBuffer sb = new StringBuffer("("); //$NON-NLS-1$
        for (final Iterator<Integer> it = parentIds.iterator(); it.hasNext();) {
            final Integer id = it.next();
            sb.append(id);
            if (it.hasNext()) {
                sb.append(","); //$NON-NLS-1$
            }
        }
        sb.append(")"); //$NON-NLS-1$

        final String SELECT_CHILDREN_IDS = "select ConstID from ConstantSets where ParentID in " + sb.toString(); //$NON-NLS-1$

        final Set<Integer> childIds = new HashSet<Integer>();

        connection.createStatement(SELECT_CHILDREN_IDS).executeQuery(new ResultHandler() {
            @Override
            public void handleRow(final ResultSet rset) throws SQLException {
                final int constId = rset.getInt(1);
                childIds.add(new Integer(constId));
            }
        });

        return childIds;
    }

    private Set<Integer> query(
        final DBConnection connection, // the DB connection to use
        final Set<Integer> rootIds, // these IDs form the starting set
        final boolean addLeaf, // leaf node values should be added to this
        // constant set
        final boolean addNonLeaf, // non-leaf node values should be added to
        // this constant set
        final boolean needChildren) // we need to return children ids that are
    // non-leaf nodes
    {
        final Set<Integer> childIds = getChildIDs(connection, rootIds);

        final Set<Integer> selfContainedIds = new HashSet<Integer>(rootIds);
        selfContainedIds.retainAll(childIds);

        childIds.removeAll(selfContainedIds);

        final Set<Integer> idsToAdd = new HashSet<Integer>();
        Set<Integer> nonLeafChildIds = new HashSet<Integer>();

        if (addLeaf) {
            final Set<Integer> leafIds = new HashSet<Integer>(childIds);
            leafIds.removeAll(distinctConstantSetIds);
            leafIds.addAll(selfContainedIds);
            idsToAdd.addAll(leafIds);
        }

        if (addNonLeaf | needChildren) {
            nonLeafChildIds = new HashSet<Integer>(childIds);
            nonLeafChildIds.retainAll(distinctConstantSetIds);
            if (addNonLeaf) {
                idsToAdd.addAll(nonLeafChildIds);
            }
        }

        if (idsToAdd.size() > 0) {
            final StringBuffer sb =
                new StringBuffer("select ConstID, String, DisplayName from Constants where ConstID in ("); //$NON-NLS-1$
            for (final Iterator<Integer> it = idsToAdd.iterator(); it.hasNext();) {
                final Integer i = it.next();
                sb.append(i);
                if (it.hasNext()) {
                    sb.append(","); //$NON-NLS-1$
                }
            }
            sb.append(")"); //$NON-NLS-1$

            connection.createStatement(sb.toString()).executeQuery(new ResultHandler() {
                @Override
                public void handleRow(final ResultSet rset) throws SQLException {
                    final int constId = rset.getInt(1);
                    final String string = rset.getString(2);
                    final String displayName = rset.getString(3);
                    values.add(displayName != null ? displayName : string);
                    constIds.add(new Integer(constId));
                }
            });
        }

        if (needChildren) {
            return nonLeafChildIds;
        } else {
            return Collections.emptySet();
        }
    }

    @Override
    public boolean patternMatch(final Object input, final String debuggingInfo) {
        /*
         * pattern match rule: each constant in the set is a match pattern, and
         * the input value must match at least one of the patterns in order to
         * return true
         */

        if (input == null) {
            /* null values never match patterns */
            return false;
        } else if (!(input instanceof String)) {
            throw new IllegalStateException(
                MessageFormat.format(
                    "pattern match not possible for value of type [{0}] ({1})", //$NON-NLS-1$
                    input.getClass().getName(),
                    debuggingInfo));
        } else {
            final String inputString = (String) input;
            boolean matched = false;

            final Iterator<String> it = values.iterator();
            while (!matched && it.hasNext()) {
                final String patternString = it.next();
                matched = new MatchPattern(patternString).matches(inputString);
            }
            return matched;
        }
    }

    /**
     * Tests whether a given String value is contained in this ConstantSet. The
     * comparisons between the given value and the values in this set are case
     * insensitive. It is recommended that the caller not pass null, since a
     * ConstantSet never contains a null value.
     *
     * @param valueToTest
     *        the String value to test
     * @return true if the value is contained in this set
     */
    @Override
    public boolean contains(final String valueToTest) {
        for (final String valueInSet : values) {
            /*
             * I18N: need to use a java.text.Collator with a specified Locale
             */
            if (valueInSet.equalsIgnoreCase(valueToTest)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public Set<String> getValues() {
        return Collections.unmodifiableSet(values);
    }

    @Override
    public boolean containsConstID(final int constId) {
        return constIds.contains(new Integer(constId));
    }
}
