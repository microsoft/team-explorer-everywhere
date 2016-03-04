// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.vss.client.core.model;

import java.net.URI;
import java.text.MessageFormat;
import java.util.UUID;

import com.microsoft.tfs.core.Messages;
import com.microsoft.tfs.util.StringUtil;

public class VssResourceNotFoundException extends VssServiceException {

    public VssResourceNotFoundException(final UUID locationId) {
        super(MessageFormat.format(Messages.getString("VssResourceNotFoundException.NotRegisteredFormat"), locationId)); //$NON-NLS-1$
    }

    public VssResourceNotFoundException(final UUID locationId, final Exception innerException) {
        super(MessageFormat.format(Messages.getString("VssResourceNotFoundException.NotRegisteredFormat"), locationId) //$NON-NLS-1$
            + " " //$NON-NLS-1$
            + (innerException != null ? innerException.getMessage() : StringUtil.EMPTY), innerException);
    }

    public VssResourceNotFoundException(final UUID locationId, final URI serverBaseUri) {
        super(MessageFormat.format(
            Messages.getString("VssResourceNotFoundException.NotRegisteredOnFormat"), //$NON-NLS-1$
            locationId,
            serverBaseUri));
    }

    public VssResourceNotFoundException(
        final UUID locationId,
        final URI serverBaseUri,
        final Exception innerException) {
        super(MessageFormat.format(
            Messages.getString("VssResourceNotFoundException.NotRegisteredOnFormat"), //$NON-NLS-1$
            locationId,
            serverBaseUri)
            + " " //$NON-NLS-1$
            + (innerException != null ? innerException.getMessage() : StringUtil.EMPTY), innerException);
    }
}
