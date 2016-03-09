// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.link;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.microsoft.tfs.core.clients.workitem.WorkItem;
import com.microsoft.tfs.core.clients.workitem.internal.WITContext;
import com.microsoft.tfs.core.clients.workitem.link.DescriptionUpdateErrorCallback;
import com.microsoft.tfs.core.clients.workitem.link.RelatedLink;
import com.microsoft.tfs.core.clients.workitem.link.WorkItemLinkUtils;
import com.microsoft.tfs.core.clients.workitem.query.BatchReadParameter;
import com.microsoft.tfs.core.clients.workitem.query.BatchReadParameterCollection;
import com.microsoft.tfs.core.clients.workitem.query.WorkItemCollection;

/**
 * Updates the description of RelatedLinks by running a work item query to
 * return data about the linked work items.
 */
public class RelatedLinkDescriptionUpdater implements LinkDescriptionUpdater {
    private static final Log log = LogFactory.getLog(RelatedLinkDescriptionUpdater.class);

    private final DescriptionUpdateErrorCallback errorCallback;
    private final WITContext witContext;
    private final String[] fieldReferenceNames;
    private final Map idsToLinks = new HashMap();

    public RelatedLinkDescriptionUpdater(
        final String[] fieldReferenceNames,
        final DescriptionUpdateErrorCallback errorCallback,
        final WITContext witContext) {
        this.errorCallback = errorCallback;
        this.witContext = witContext;
        this.fieldReferenceNames = fieldReferenceNames;
    }

    @Override
    public void addLinkToBeUpdated(final LinkImpl link) {
        final Integer id = new Integer(((RelatedLink) link).getTargetWorkItemID());
        if (!idsToLinks.containsKey(id)) {
            idsToLinks.put(id, new ArrayList());
        }

        final ArrayList links = (ArrayList) idsToLinks.get(id);
        links.add(link);
    }

    @Override
    public void run() {
        if (idsToLinks.size() == 0) {
            /*
             * nothing to do
             */
            return;
        }

        final BatchReadParameterCollection batchReadParams = new BatchReadParameterCollection();

        for (final Iterator it = idsToLinks.keySet().iterator(); it.hasNext();) {
            final Integer id = (Integer) it.next();
            batchReadParams.add(new BatchReadParameter(id.intValue()));
        }

        try {
            final StringBuffer sb = new StringBuffer();
            sb.append("SELECT "); //$NON-NLS-1$
            for (int i = 0; i < fieldReferenceNames.length; i++) {
                if (i > 0) {
                    sb.append(", "); //$NON-NLS-1$
                }
                sb.append("["); //$NON-NLS-1$
                sb.append(fieldReferenceNames[i]);
                sb.append("]"); //$NON-NLS-1$
            }
            sb.append(" FROM WorkItems"); //$NON-NLS-1$

            final WorkItemCollection queryResults = witContext.getClient().query(sb.toString(), batchReadParams);

            for (int i = 0; i < queryResults.size(); i++) {
                final WorkItem workItem = queryResults.getWorkItem(i);
                final Integer id = new Integer(workItem.getFields().getID());
                final String description = WorkItemLinkUtils.buildDescriptionFromWorkItem(workItem);

                final ArrayList links = (ArrayList) idsToLinks.get(id);
                for (int linkIndex = 0; linkIndex < links.size(); linkIndex++) {
                    final RelatedLinkImpl relatedLink = (RelatedLinkImpl) links.get(linkIndex);
                    relatedLink.setDescription(description);
                    relatedLink.setWorkItem(workItem);

                    // Touch each required field to ensure computed fields are
                    // properly setup.
                    // For example, a query for AREA_PATH returns only the
                    // AreaId field. A call
                    // to getValue causes the actual area path field to be
                    // created.
                    for (int j = 0; j < fieldReferenceNames.length; j++) {
                        workItem.getFields().getField(fieldReferenceNames[j]).getValue();
                    }
                }
            }
        } catch (final Throwable t) {
            if (errorCallback != null) {
                errorCallback.onDescriptionUpdateError(t);
            } else {
                log.error(t);
            }
        }
    }
}
