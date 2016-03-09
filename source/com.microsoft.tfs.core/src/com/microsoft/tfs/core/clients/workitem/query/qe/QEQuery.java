// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.query.qe;

import com.microsoft.tfs.core.clients.workitem.WorkItemClient;
import com.microsoft.tfs.core.clients.workitem.queryhierarchy.LinkQueryMode;
import com.microsoft.tfs.core.clients.workitem.queryhierarchy.QueryType;

/**
 * <p>
 * A QEQuery is the root of an object model used by the GUI query builder (QE =
 * Query Editor).
 * </p>
 * <p>
 * A QEQuery is produced by a QEQueryFactory.
 * </p>
 *
 * @since TEE-SDK-10.1
 */
public interface QEQuery {
    public QueryType getQueryType();

    public void setQueryType(QueryType queryType);

    public LinkQueryMode getLinkQueryMode();

    public void setLinkQueryMode(LinkQueryMode mode);

    public QEQueryRowCollection getSourceRowCollection();

    public QEQueryRowCollection getTargetRowCollection();

    public boolean getUseSelectedLinkTypes();

    public String[] getLinkQueryLinkTypes();

    public String getTreeQueryLinkType();

    public void addLinkQueryLinkType(String referenceName);

    public void removeLinkQueryLinkType(String referenceName);

    public void setUseSelectedLinkTypes(boolean useSelected);

    public void setTreeQueryLinkType(String referenceName);

    public String getFilterExpression();

    public WorkItemClient getWorkItemClient();

    public void addModifiedListener(QEQueryModifiedListener listener);

    public void removeModifiedListener(QEQueryModifiedListener listener);

    public boolean isValid();

    public String getInvalidMessage();
}
