// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.wit.form.link;

import java.util.HashMap;
import java.util.Map;

import com.microsoft.tfs.client.common.server.TFSServer;
import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.core.clients.workitem.WorkItem;
import com.microsoft.tfs.core.clients.workitem.form.WIFormLinksControlWITypeFilters;
import com.microsoft.tfs.core.clients.workitem.link.RegisteredLinkTypeNames;

public class LinkUIRegistry {
    private final Map<String, LinkControlProvider> mapLinkControlProviders = new HashMap<String, LinkControlProvider>();

    private final RelatedWorkitemControlProvider relatedWorkitemControlProvider;
    private final HyperlinkControlProvider hyperlinkControlProvider;
    private final ChangesetLinkControlProvider changesetLinkControlProvider;
    private final VersionedItemLinkProvider versionedItemLinkProvider;
    private final TestResultLinkControlProvider testResultLinkControlProvider;
    private final ResultAttachmentLinkControlProvider resultAttachmentLinkControlProvider;
    private final StoryboardLinkControlProvider storyboardLinkControlProvider;
    private final DefaultLinkControlProvider defaultLinkControlProvider;

    private static Map<String, String> mapDisplayNames;
    public final static String WORKITEM_PROVIDER_DISPLAY_NAME =
        Messages.getString("LinkUIRegistry.WorkItemDisplayName"); //$NON-NLS-1$
    public final static String HYPERLINK_PROVIDER_DISPLAY_NAME =
        Messages.getString("LinkUIRegistry.HyperlinkDisplayName"); //$NON-NLS-1$
    public final static String VERSIONEDITEM_PROVIDER_DISPLAY_NAME =
        Messages.getString("LinkUIRegistry.VersionedItemDisplayName"); //$NON-NLS-1$
    public final static String CHANGESET_PROVIDER_DISPLAY_NAME =
        Messages.getString("LinkUIRegistry.ChangesetDisplayName"); //$NON-NLS-1$
    public final static String TESTRESULT_PROVIDER_DISPLAY_NAME =
        Messages.getString("LinkUIRegistry.TestResultDisplayName"); //$NON-NLS-1$
    public final static String RESULTATTACHMENT_PROVIDER_DISPLAY_NAME =
        Messages.getString("LinkUIRegistry.ResultAttachmentDisplayName"); //$NON-NLS-1$
    public final static String STORYBOARD_PROVIDER_DISPLAY_NAME =
        Messages.getString("LinkUIRegistry.StoryboardDisplayName"); //$NON-NLS-1$
    public final static String COMMIT_PROVIDER_DISPLAY_NAME = Messages.getString("LinkUIRegistry.CommitDisplayName"); //$NON-NLS-1$

    static {
        mapDisplayNames = new HashMap<String, String>();
        mapDisplayNames.put(RegisteredLinkTypeNames.WORKITEM, WORKITEM_PROVIDER_DISPLAY_NAME);
        mapDisplayNames.put(RegisteredLinkTypeNames.HYPERLINK, HYPERLINK_PROVIDER_DISPLAY_NAME);
        mapDisplayNames.put(RegisteredLinkTypeNames.CHANGESET, CHANGESET_PROVIDER_DISPLAY_NAME);
        mapDisplayNames.put(RegisteredLinkTypeNames.VERSIONED_ITEM, VERSIONEDITEM_PROVIDER_DISPLAY_NAME);
        mapDisplayNames.put(RegisteredLinkTypeNames.TEST_RESULT, TESTRESULT_PROVIDER_DISPLAY_NAME);
        mapDisplayNames.put(RegisteredLinkTypeNames.RESULT_ATTACHMENT, RESULTATTACHMENT_PROVIDER_DISPLAY_NAME);
        mapDisplayNames.put(RegisteredLinkTypeNames.STORYBOARD, STORYBOARD_PROVIDER_DISPLAY_NAME);
        mapDisplayNames.put(RegisteredLinkTypeNames.COMMIT, COMMIT_PROVIDER_DISPLAY_NAME);
    }

    public LinkUIRegistry(
        final TFSServer server,
        final WorkItem sourceWorkItem,
        final WIFormLinksControlWITypeFilters wiFilters) {
        relatedWorkitemControlProvider = new RelatedWorkitemControlProvider(server, sourceWorkItem, wiFilters);
        hyperlinkControlProvider = new HyperlinkControlProvider();
        changesetLinkControlProvider = new ChangesetLinkControlProvider(server);
        versionedItemLinkProvider = new VersionedItemLinkProvider(server);
        testResultLinkControlProvider = new TestResultLinkControlProvider();
        resultAttachmentLinkControlProvider = new ResultAttachmentLinkControlProvider();
        storyboardLinkControlProvider = new StoryboardLinkControlProvider(server);
        defaultLinkControlProvider = new DefaultLinkControlProvider();

        mapLinkControlProviders.put(RegisteredLinkTypeNames.WORKITEM, relatedWorkitemControlProvider);
        mapLinkControlProviders.put(RegisteredLinkTypeNames.HYPERLINK, hyperlinkControlProvider);
        mapLinkControlProviders.put(RegisteredLinkTypeNames.CHANGESET, changesetLinkControlProvider);
        mapLinkControlProviders.put(RegisteredLinkTypeNames.VERSIONED_ITEM, versionedItemLinkProvider);
        mapLinkControlProviders.put(RegisteredLinkTypeNames.TEST_RESULT, testResultLinkControlProvider);
        mapLinkControlProviders.put(RegisteredLinkTypeNames.RESULT_ATTACHMENT, resultAttachmentLinkControlProvider);
        mapLinkControlProviders.put(RegisteredLinkTypeNames.STORYBOARD, storyboardLinkControlProvider);
    }

    public LinkControlProvider getLinkControlProvider(final String linkTypeName) {
        if (mapLinkControlProviders.containsKey(linkTypeName)) {
            return mapLinkControlProviders.get(linkTypeName);
        } else {
            return defaultLinkControlProvider;
        }
    }

    public static String getDisplayName(final String linkTypeName) {
        if (mapDisplayNames.containsKey(linkTypeName)) {
            return mapDisplayNames.get(linkTypeName);
        } else {
            return linkTypeName;
        }
    }
}
