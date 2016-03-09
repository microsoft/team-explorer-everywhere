// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.util;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;

public final class StatusHelper {
    public static IStatus combine(
        final String pluginId,
        final int code,
        final String message,
        final IStatus one,
        final IStatus two) {
        if (one == null && two == null) {
            return null;
        }

        final MultiStatus newStatus = new MultiStatus(pluginId, code, message, null);

        if (one != null && one.isMultiStatus()) {
            newStatus.addAll(one);
        } else if (one != null) {
            newStatus.add(one);
        }

        if (two != null && two.isMultiStatus()) {
            newStatus.addAll(two);
        } else if (two != null) {
            newStatus.add(two);
        }

        return newStatus;
    }
}
