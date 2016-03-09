// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.form;

import com.microsoft.tfs.core.clients.workitem.WorkItem;

/**
 * @since TEE-SDK-10.1
 */
public interface WIFormParam extends WIFormElement {
    public String getSubstitutionToken();

    public String getSubstitutionValue(WorkItem workItem);
}
