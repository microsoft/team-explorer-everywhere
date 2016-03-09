// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.framework.location.exceptions;

import java.text.MessageFormat;

import com.microsoft.tfs.core.Messages;

/**
 * Thrown when the URL defined for a mapping's access point is malformed.
 */
public class AccessPointIsMalformedURLException extends LocationException {
    public AccessPointIsMalformedURLException(final String accessPoint) {
        super(
            MessageFormat.format(
                Messages.getString("AccessPointIsMalformedUrlException.AccessPointIsMalformedURLFormat"), //$NON-NLS-1$
                accessPoint));
    }
}
