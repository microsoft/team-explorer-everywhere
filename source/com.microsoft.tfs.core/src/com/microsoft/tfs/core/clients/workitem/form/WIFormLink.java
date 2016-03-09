// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.form;

import com.microsoft.tfs.core.clients.workitem.WorkItem;

/**
 * @since TEE-SDK-10.1
 */
public interface WIFormLink extends WIFormElement {
    /*
     * Computes the URL for this link. The computed URL will have parameter and
     * macro substitutions applied.
     */
    public String getURL(WorkItem workItem);
}
