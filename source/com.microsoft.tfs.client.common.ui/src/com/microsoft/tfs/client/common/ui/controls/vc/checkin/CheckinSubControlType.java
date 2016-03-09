// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.controls.vc.checkin;

public class CheckinSubControlType {
    public static final CheckinSubControlType SOURCE_FILES = new CheckinSubControlType("sourcefiles"); //$NON-NLS-1$
    public static final CheckinSubControlType WORK_ITEMS = new CheckinSubControlType("workitems"); //$NON-NLS-1$
    public static final CheckinSubControlType CHECKIN_NOTES = new CheckinSubControlType("checkinnotes"); //$NON-NLS-1$
    public static final CheckinSubControlType POLICY_WARNINGS = new CheckinSubControlType("policywarnings"); //$NON-NLS-1$

    public static final CheckinSubControlType OFFLINE = new CheckinSubControlType("offline"); //$NON-NLS-1$
    public static final CheckinSubControlType CONNECTION_FAILURE = new CheckinSubControlType("connectionfailure"); //$NON-NLS-1$

    private final String id;

    public CheckinSubControlType(final String type) {
        id = type;
    }

    public String getID() {
        return id;
    }

    @Override
    public String toString() {
        return id;
    }
}
