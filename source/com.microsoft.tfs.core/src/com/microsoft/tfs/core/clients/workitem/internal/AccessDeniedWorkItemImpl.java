// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal;

import com.microsoft.tfs.core.Messages;
import com.microsoft.tfs.core.clients.workitem.CoreFieldReferenceNames;

public class AccessDeniedWorkItemImpl extends WorkItemImpl {
    public AccessDeniedWorkItemImpl(final WITContext witContext, final int id) {
        super(witContext);

        getFieldsInternal().addOriginalFieldValueFromServer(CoreFieldReferenceNames.ID, String.valueOf(id), true);

        getFieldsInternal().addOriginalFieldValueFromServer(
            CoreFieldReferenceNames.TITLE,
            Messages.getString("AccessDeniedWorkItemImpl.Message"), //$NON-NLS-1$
            true);
    }
}
