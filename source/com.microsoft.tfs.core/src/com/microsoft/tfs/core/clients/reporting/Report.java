// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.reporting;

import ms.sql.reporting.reportingservices._CatalogItem;

/**
 * Class representing a report from the reporting service.
 *
 * @since TEE-SDK-10.1
 * @threadsafety thread-compatible
 */
public class Report extends ReportNode {
    public Report(final String projectName, final _CatalogItem item) {
        super(projectName, item);
    }

    /**
     * A report never has children.
     */
    @Override
    public boolean hasChildren() {
        return false;
    }

}
