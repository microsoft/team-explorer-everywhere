// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal;

import java.text.SimpleDateFormat;
import java.util.TimeZone;

/**
 * This class is the internal equivalent of WorkItemUtils.
 *
 * Methods defined here are not to be exposed as public API.
 */
public class InternalWorkItemUtils {
    public static final String METADATA_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.S"; //$NON-NLS-1$

    public static SimpleDateFormat newMetadataDateFormat() {
        final SimpleDateFormat format = new SimpleDateFormat(METADATA_DATE_FORMAT);

        /*
         * metadata dates come from the server without any explicit timezone
         * specified, and are implicitly GMT (UTC)
         */
        format.setTimeZone(TimeZone.getTimeZone("UTC")); //$NON-NLS-1$

        return format;
    }
}
