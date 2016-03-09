// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.checkinpolicies.workitemquerypolicy;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.microsoft.tfs.client.common.ui.teamexplorer.helpers.WorkItemHelpers;
import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.core.checkinpolicies.PolicyBase;
import com.microsoft.tfs.core.checkinpolicies.PolicyContext;
import com.microsoft.tfs.core.checkinpolicies.PolicyContextKeys;
import com.microsoft.tfs.core.checkinpolicies.PolicyEditArgs;
import com.microsoft.tfs.core.checkinpolicies.PolicyEvaluationCancelledException;
import com.microsoft.tfs.core.checkinpolicies.PolicyFailure;
import com.microsoft.tfs.core.checkinpolicies.PolicyType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.WorkItemCheckinInfo;
import com.microsoft.tfs.core.clients.workitem.CoreFieldReferenceNames;
import com.microsoft.tfs.core.clients.workitem.WorkItemClient;
import com.microsoft.tfs.core.clients.workitem.WorkItemQueryUtils;
import com.microsoft.tfs.core.clients.workitem.query.Query;
import com.microsoft.tfs.core.clients.workitem.query.StoredQuery;
import com.microsoft.tfs.core.clients.workitem.query.WorkItemCollection;
import com.microsoft.tfs.core.memento.Memento;
import com.microsoft.tfs.core.pendingcheckin.PendingCheckin;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.GUID;
import com.microsoft.tfs.util.GUID.GUIDStringFormat;

public class WorkItemQueryPolicy extends PolicyBase {
    private final static PolicyType TYPE =
        new PolicyType(
            "com.teamprise.checkinpolicies.workitemquerypolicy.WorkItemQueryPolicy-1", //$NON-NLS-1$

            Messages.getString("WorkItemQueryPolicy.Name"), //$NON-NLS-1$

            Messages.getString("WorkItemQueryPolicy.ShortDescription"), //$NON-NLS-1$

            Messages.getString("WorkItemQueryPolicy.LongDescription"), //$NON-NLS-1$

            Messages.getString("WorkItemQueryPolicy.InstallInstructions")); //$NON-NLS-1$

    private static final String QUERY_GUID_ATTRIBUTE = "queryGUID"; //$NON-NLS-1$

    /*
     * The time in seconds after which old query results are discarded and the
     * query is re-run during evaluation.
     */
    private static final int QUERY_RESULT_CACHE_TIMEOUT_SECONDS = 30;

    /*
     * Updated during edit, and required for initialize and evaluation.
     */
    private volatile GUID queryGUID;

    /*
     * Some run-time cached data to keep our round-trips down. All access should
     * be synchronized on queryLock.
     */
    private long queryLastRun;
    private Query query;
    private String queryName;
    private String queryProjectName;
    private int[] cachedWorkItemIDs;
    private int[] queryResults;
    private final List failuresList = new ArrayList();
    private final Object queryLock = new Object();

    /*
     * (non-Javadoc)
     *
     * @see com.microsoft.tfs.core.checkinpolicies.PolicyBase#canEdit()
     */
    @Override
    public boolean canEdit() {
        return true;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.microsoft.tfs.core.checkinpolicies.PolicyBase#edit(com.microsoft.
     * tfs.core .checkinpolicies.PolicyEditArgs)
     */
    @Override
    public boolean edit(final PolicyEditArgs policyEditArgs) {
        /*
         * Extending classes may override.
         */

        return false;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.microsoft.tfs.core.checkinpolicies.PolicyBase#initialize(com.
     * microsoft .tfs.core.pendingcheckin.PendingCheckin,
     * com.microsoft.tfs.core.checkinpolicies.PolicyContext)
     */
    @Override
    public void initialize(final PendingCheckin pendingCheckin, final PolicyContext context) {
        super.initialize(pendingCheckin, context);

        final TFSTeamProjectCollection connection =
            (TFSTeamProjectCollection) context.getProperty(PolicyContextKeys.TFS_TEAM_PROJECT_COLLECTION);
        if (connection != null) {
            final WorkItemClient client = connection.getWorkItemClient();

            /*
             * Load the stored query the user configured.
             */
            final StoredQuery storedQuery = client.getStoredQuery(queryGUID);

            synchronized (queryLock) {
                /*
                 * We only want the ID results back.
                 */
                query = storedQuery.createQuery(
                    WorkItemQueryUtils.makeContext(storedQuery.getProject(), WorkItemHelpers.getCurrentTeamName()));
                query.getDisplayFieldList().clear();
                query.getDisplayFieldList().add(CoreFieldReferenceNames.ID);

                queryName = storedQuery.getName();
                queryProjectName = storedQuery.getProject().getName();
            }
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.microsoft.tfs.core.checkinpolicies.PolicyBase#evaluate(com.microsoft
     * .tfs.core.checkinpolicies.PolicyContext)
     */
    @Override
    public PolicyFailure[] evaluate(final PolicyContext context) throws PolicyEvaluationCancelledException {
        if (queryGUID == null) {
            return new PolicyFailure[] {
                new PolicyFailure(Messages.getString("WorkItemQueryPolicy.PolicyFailureText"), this) //$NON-NLS-1$
            };
        }

        if (getPendingCheckin().getPendingChanges().getCheckedPendingChanges().length == 0) {
            return new PolicyFailure[0];
        }

        final WorkItemCheckinInfo[] checkedWorkItems = getPendingCheckin().getWorkItems().getCheckedWorkItems();

        if (checkedWorkItems.length == 0) {
            return new PolicyFailure[] {
                new PolicyFailure(
                    Messages.getString("WorkItemQueryPolicy.FailedNoWorkItemsAssociated"), //$NON-NLS-1$
                    this)
            };
        }

        /*
         * Gather the lists of IDs so we can sort and compare.
         */

        final int[] currentWorkItemIDs = new int[checkedWorkItems.length];
        for (int i = 0; i < checkedWorkItems.length; i++) {
            currentWorkItemIDs[i] = checkedWorkItems[i].getWorkItem().getFields().getID();
        }

        Arrays.sort(currentWorkItemIDs);

        synchronized (queryLock) {
            if (Arrays.equals(currentWorkItemIDs, cachedWorkItemIDs) == false) {
                final int[] workItemIDsFromStoredQuery = getWorkItemIDsFromStoredQuery();
                failuresList.clear();

                for (int i = 0; i < currentWorkItemIDs.length; i++) {
                    if (Arrays.binarySearch(workItemIDsFromStoredQuery, currentWorkItemIDs[i]) == -1) {
                        final String messageFormat =
                            Messages.getString("WorkItemQueryPolicy.WorkItemNotInQueryResultFormat"); //$NON-NLS-1$
                        final String message = MessageFormat.format(
                            messageFormat,
                            Integer.toString(currentWorkItemIDs[i]),
                            queryName,
                            queryProjectName);
                        failuresList.add(new PolicyFailure(message, this));
                    }
                }

                cachedWorkItemIDs = currentWorkItemIDs;
            }
        }
        return (PolicyFailure[]) failuresList.toArray(new PolicyFailure[failuresList.size()]);
    }

    private int[] getWorkItemIDsFromStoredQuery() {
        final long elapsedSinceLastQuery = System.currentTimeMillis() - queryLastRun;
        if (elapsedSinceLastQuery > WorkItemQueryPolicy.QUERY_RESULT_CACHE_TIMEOUT_SECONDS * 1000) {
            final WorkItemCollection collection = query.runQuery();
            queryResults = collection.getIDs();
            Arrays.sort(queryResults);

            queryLastRun = System.currentTimeMillis();
        }

        return queryResults;
    }

    @Override
    public PolicyType getPolicyType() {
        return TYPE;
    }

    @Override
    public void loadConfiguration(final Memento configurationMemento) {
        queryGUID = new GUID(configurationMemento.getString(QUERY_GUID_ATTRIBUTE));
    }

    @Override
    public void saveConfiguration(final Memento configurationMemento) {
        configurationMemento.putString(QUERY_GUID_ATTRIBUTE, queryGUID.getGUIDString(GUIDStringFormat.NONE));
    }

    /**
     * @return the configured query GUID.
     */
    public GUID getQueryGUID() {
        return queryGUID;
    }

    /**
     * @param queryGUID
     *        the query to restrict work items to for successful evaluation (not
     *        null).
     */
    public void setQueryGUID(final GUID queryGUID) {
        Check.notNull(queryGUID, "queryGUID"); //$NON-NLS-1$

        this.queryGUID = queryGUID;
    }
}
