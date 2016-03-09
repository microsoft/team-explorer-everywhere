// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.form;

/**
 * @since TEE-SDK-10.1
 */
public interface WIFormElement {
    public WIFormElement[] getChildElements();

    public WIFormElement getParentElement();

    public String[] getAttributeNames();

    public String getAttribute(String name);
}
