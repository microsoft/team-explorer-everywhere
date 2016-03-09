// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.reporting;

import ms.sql.reporting.reportingservices._CatalogItem;

/**
 * Class representing a folder from the reporting service.
 *
 * @since TEE-SDK-10.1
 * @threadsafety thread-compatible
 */
public class ReportFolder extends ReportNode {
    public ReportFolder(final String projectName, final String label) {
        super(projectName, label);
    }

    public ReportFolder(final String projectName, final _CatalogItem item) {
        super(projectName, item);
    }
}
