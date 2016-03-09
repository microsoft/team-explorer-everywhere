// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse.ui.propertypages;

import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ChangeType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PropertyValue;

public class StatusItem {
    private final String userName;
    private final ChangeType changeType;
    private final String workspaceName;
    private final PropertyValue[] properties;

    public StatusItem(
        final String userName,
        final ChangeType changeType,
        final String workspaceName,
        final PropertyValue[] properties) {
        this.userName = userName;
        this.changeType = changeType;
        this.workspaceName = workspaceName;
        this.properties = properties;
    }

    public String getUserName() {
        return userName;
    }

    public ChangeType getChangeType() {
        return changeType;
    }

    public String getWorkspaceName() {
        return workspaceName;
    }

    public PropertyValue[] getPropertyValues() {
        return properties;
    }
}
