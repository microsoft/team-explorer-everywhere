// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.form;

/**
 * WIFormDescription - represents the "Form" complex type.
 *
 * Possible children: WIFormLayout
 *
 * @since TEE-SDK-10.1
 */
public interface WIFormDescription extends WIFormElement {

    /**
     * Corresponds to "Layout" child elements in XML minOccurs: 1 maxOccurs:
     * unbounded
     */
    public WIFormLayout[] getLayoutChildren();
}