// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.rules.cache;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.microsoft.tfs.core.clients.workitem.internal.InternalWorkItemConstants;
import com.microsoft.tfs.core.clients.workitem.internal.WITContext;
import com.microsoft.tfs.core.clients.workitem.internal.metadata.IConstantSet;

public class RulePersonScopeCache {
    private static final Log log = LogFactory.getLog(RulePersonScopeCache.class);

    /*
     * maps a rule PersonId to a boolean: TRUE if the current user "matches" the
     * person id FALSE if the current user does not "match" the person id
     *
     * the current user "matches" the person id if the id: -- refers to the
     * current user -- refers to a group that: -- contains the current user --
     * that the current user is reachable from through sub-group membership
     */
    private final Map<Integer, Boolean> personIds = new HashMap<Integer, Boolean>();

    /*
     * The WIT context
     */
    private final WITContext witContext;

    public RulePersonScopeCache(final WITContext witContext) {
        this.witContext = witContext;
    }

    public synchronized void clear() {
        personIds.clear();
    }

    public synchronized boolean isRuleInScope(final int personID, final boolean inversePerson) {
        final Integer key = new Integer(personID);
        Boolean matches = personIds.get(key);
        if (matches == null) {
            matches = Boolean.valueOf(currentUserMatches(personID));
            personIds.put(key, matches);
        }

        return inversePerson != matches.booleanValue();
    }

    private boolean currentUserMatches(final int personID) {
        /*
         * fast path for well-known constant sets that any user is a part of
         */
        if (InternalWorkItemConstants.TFS_EVERYONE_CONSTANT_SET_ID == personID) {
            return true;
        }
        if (InternalWorkItemConstants.AUTHENTICATED_USERS_CONSTANT_SET_ID == personID) {
            return true;
        }

        /*
         * slow path: create a ConstantSet
         */
        long stTime = 0;
        if (log.isDebugEnabled()) {
            stTime = System.currentTimeMillis();
            log.debug(MessageFormat.format(
                "currentUserMatches: getting constant set for personID={0}", //$NON-NLS-1$
                Integer.toString(personID)));
        }

        final IConstantSet constantSet =
            witContext.getMetadata().getConstantHandler().getConstantSet(personID, true, true, true, false, false);

        if (log.isDebugEnabled()) {
            final long elapsed = System.currentTimeMillis() - stTime;
            log.debug(
                MessageFormat.format(
                    "currentUserMatches: got constant set of size={0} for personID={1} queryCount={2} in: {3} ms", //$NON-NLS-1$
                    constantSet.getSize(),
                    Integer.toString(personID),
                    constantSet.getQueryCount(),
                    elapsed));
        }

        return constantSet.containsConstID(witContext.getCurrentUserConstID());
    }
}
