// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.exceptions;

import java.text.DateFormat;
import java.text.MessageFormat;
import java.util.Date;

import com.microsoft.tfs.core.Messages;

/**
 * Thrown when a preview release of the core libraries has expired.
 *
 * @since TEE-SDK-10.1
 */
public class TEPreviewExpiredException extends RuntimeException {
    private final Date expirationDate;

    public TEPreviewExpiredException(final Date expirationDate) {
        super(MessageFormat.format(
            Messages.getString("TEPreviewExpiredException.PreviewExpiredPleaseUpgradeFormat"), //$NON-NLS-1$
            DateFormat.getDateInstance(DateFormat.LONG).format(expirationDate)));

        this.expirationDate = expirationDate;
    }

    public Date getExpirationDate() {
        return expirationDate;
    }
}
