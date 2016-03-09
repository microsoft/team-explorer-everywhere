// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem;

import java.util.HashSet;
import java.util.Set;

import com.microsoft.tfs.util.Check;

/**
 * This class exposes information about the server.
 *
 * @since TEE-SDK-10.1
 */
public final class ServerInfo {
    private final Set<String> features = new HashSet<String>();

    public ServerInfo(final WorkItemServerVersion version) {
        Check.notNull(version, "version"); //$NON-NLS-1$

        /*
         * This format was copied for compatibility, even though we don't use
         * the V2 version ever. Note that we assume versions will continue to
         * build on each other for future releases. This may not be a valid
         * assumption.
         */
        if (version.getValue() >= WorkItemServerVersion.V2.getValue()) {
            features.add(SupportedFeatures.WORK_ITEM_LINKS);
            features.add(SupportedFeatures.QUERY_FOLDERS);
            features.add(SupportedFeatures.QUERY_IN_GROUP_FILTER);
        }

        if (version.getValue() >= WorkItemServerVersion.V3.getValue()) {
            features.add(SupportedFeatures.GUID_FIELDS);
            features.add(SupportedFeatures.BOOLEAN_FIELDS);
            features.add(SupportedFeatures.QUERY_FOLDERS);
            features.add(SupportedFeatures.QUERY_FOLDER_PERMISSIONS);
            features.add(SupportedFeatures.QUERY_FOLDER_SET_OWNER);
            features.add(SupportedFeatures.QUERY_FIELDS_COMPARISON);
            features.add(SupportedFeatures.QUERY_HISTORICAL_REVISIONS);
            features.add(SupportedFeatures.WORK_ITEM_TYPE_CATEGORIES);
            features.add(SupportedFeatures.WORK_ITEM_TYPE_CATEGORY_MEMBERS);
            features.add(SupportedFeatures.WORK_ITEM_LINKS);
            features.add(SupportedFeatures.WORK_ITEM_LINK_LOCKS);
            features.add(SupportedFeatures.QUERY_IN_GROUP_FILTER);
            features.add(SupportedFeatures.BATCH_SAVE_WORK_ITEMS_FROM_DIFFERENT_PROJECTS);
            features.add(SupportedFeatures.SYNC_NAME_CHANGES);
            features.add(SupportedFeatures.REPORTING_NAMES);
            features.add(SupportedFeatures.SET_REPORTING_TYPE_TO_NONE);
        }

        if (version.getValue() >= WorkItemServerVersion.V5.getValue()) {
            features.add(SupportedFeatures.QUERY_RECURSIVE_RETURN_MATCHING_CHILDREN);
            features.add(SupportedFeatures.PROVISION_PERMISSION);
            features.add(SupportedFeatures.CONFIGURABLE_BULK_UPDATE_BATCH_SIZE);
        }

        if (version.getValue() >= WorkItemServerVersion.V8.getValue()) {
            features.add(SupportedFeatures.WIQL_EVALUATION_ON_SERVER);
        }
    }

    /**
     * Tests whether the server supports the specified feature.
     *
     * @see SupportedFeatures
     *
     * @param feature
     *        the feature to test (must not be <code>null</code>)
     * @return <code>true</code> if the server supports the specified feature,
     *         <code>false</code> if it does not
     */
    public boolean isSupported(final String feature) {
        Check.notNull(feature, "feature"); //$NON-NLS-1$

        return features.contains(feature);
    }
}
