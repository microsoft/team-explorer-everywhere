// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.build.internal.soapextensions;

import java.util.Calendar;
import java.util.Comparator;

import com.microsoft.tfs.core.clients.build.IBuildInformationNode;
import com.microsoft.tfs.core.clients.build.flags.InformationFields;

public class BuildInformationNodeComparer implements Comparator<IBuildInformationNode> {
    private static BuildInformationNodeComparer INSTANCE;

    public static Comparator<IBuildInformationNode> getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new BuildInformationNodeComparer();
        }
        return INSTANCE;
    }

    @Override
    public int compare(final IBuildInformationNode x, final IBuildInformationNode y) {
        if (x == null && y == null) {
            return 0;
        }

        if (x == null && y != null) {
            return -1;
        }

        if (x != null && y == null) {
            return 1;
        }

        Calendar leftTimestamp = null;
        Calendar rightTimestamp = null;

        if (x.getFields().containsKey(InformationFields.START_TIME)) {
            leftTimestamp = CommonInformationHelper.getDateTime(x.getFields(), InformationFields.START_TIME);
        } else if (x.getFields().containsKey(InformationFields.TIMESTAMP)) {
            leftTimestamp = CommonInformationHelper.getDateTime(x.getFields(), InformationFields.TIMESTAMP);
        }

        if (y.getFields().containsKey(InformationFields.START_TIME)) {
            rightTimestamp = CommonInformationHelper.getDateTime(y.getFields(), InformationFields.START_TIME);
        } else if (y.getFields().containsKey(InformationFields.TIMESTAMP)) {
            rightTimestamp = CommonInformationHelper.getDateTime(y.getFields(), InformationFields.TIMESTAMP);
        }

        // If both the left and right node have a timestamp value then we
        // compare using this value, otherwise we fall back to the ID which
        // isn't perfect but it's better than nothing.
        if (leftTimestamp != null && rightTimestamp != null) {
            return leftTimestamp.compareTo(rightTimestamp);
        } else {
            return new Integer(x.getID()).compareTo(y.getID());
        }
    }
}
