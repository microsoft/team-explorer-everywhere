// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.internal.localworkspace;

import java.util.Arrays;
import java.util.Comparator;

public class BaselineFileGUIDComparer implements Comparator<byte[]> {
    @Override
    public int compare(final byte[] o1, final byte[] o2) {
        if (Arrays.equals(o1, o2)) {
            return 0;
        }

        // This choice of computing the difference may make this comparator
        // useless for sorted sets/trees
        return o1.hashCode() - o2.hashCode();
    }
}
