// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.link;

import java.text.MessageFormat;

import com.microsoft.tfs.core.clients.workitem.internal.WITContext;
import com.microsoft.tfs.core.clients.workitem.link.DescriptionUpdateErrorCallback;

public class LinkDescriptionUpdaterFactory {
    public static LinkDescriptionUpdater getDescriptionUpdater(
        final Class linkType,
        final String[] fieldReferenceNames,
        final DescriptionUpdateErrorCallback errorCallback,
        final WITContext witContext) {
        if (ExternalLinkImpl.class.equals(linkType)) {
            return new ExternalLinkDescriptionUpdater(errorCallback, witContext);
        } else if (RelatedLinkImpl.class.equals(linkType)) {
            return new RelatedLinkDescriptionUpdater(fieldReferenceNames, errorCallback, witContext);
        } else {
            throw new IllegalArgumentException(MessageFormat.format("unknown link type [{0}]", linkType.getName())); //$NON-NLS-1$
        }
    }
}
