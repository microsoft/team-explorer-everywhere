// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teamexplorer.actions.wit;

import java.net.URI;

import com.microsoft.tfs.client.common.ui.teamexplorer.helpers.WebAccessHelper;
import com.microsoft.tfs.core.clients.workitem.queryhierarchy.QueryItem;
import com.microsoft.tfs.core.util.TSWAHyperlinkBuilder;

public class EditQueryInWebAccessAction extends BaseQueryWebAccessAction {
    @Override
    protected URI getWebAccessURI(final TSWAHyperlinkBuilder tswaBuilder, final QueryItem queryItem) {
        return WebAccessHelper.getWebAccessQueryEditorURI(tswaBuilder, queryItem);
    }
}
