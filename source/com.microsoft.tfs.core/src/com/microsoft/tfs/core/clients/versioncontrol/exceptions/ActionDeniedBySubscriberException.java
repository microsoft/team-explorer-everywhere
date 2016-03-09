// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.exceptions;

/**
 * Models a server {@link ActionDeniedBySubscriberException} exception and its
 * associated property bag.
 *
 * @since TEE-SDK-10.1
 */
public class ActionDeniedBySubscriberException extends VersionControlException {
    private static final String SUBSCRIBER_TYPE_PROPERTY = "Microsoft.TeamFoundation.SubscriberType"; //$NON-NLS-1$
    private static final String SUBSCRIBER_NAME_PROPERTY = "Microsoft.TeamFoundation.SubscriberName"; //$NON-NLS-1$
    private static final String STATUS_CODE_PROPERTY = "Microsoft.TeamFoundation.StatusCode"; //$NON-NLS-1$

    public ActionDeniedBySubscriberException(final String message) {
        super(message);
    }

    public int getStatusCode() {
        return getProperties().getIntProperty(STATUS_CODE_PROPERTY);
    }

    public String getSubscriberName() {
        return getProperties().getStringProperty(SUBSCRIBER_NAME_PROPERTY);
    }

    public String getSubscriberType() {
        return getProperties().getStringProperty(SUBSCRIBER_TYPE_PROPERTY);
    }
}
