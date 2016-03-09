// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.link;

/**
 * A LinkDescriptionUpdater is an object that knows how to populate link
 * descriptions. Usually this population is expensive since it often involves
 * hitting the server to obtain additional information about a link. Hence
 * LinkDescriptionUpdater extends runnable and can easily by run in a background
 * thread.
 *
 * An instance of LinkDescriptionUpdater will only be used to update links for a
 * single work item.
 */
public interface LinkDescriptionUpdater extends Runnable {
    public void addLinkToBeUpdated(LinkImpl link);
}
