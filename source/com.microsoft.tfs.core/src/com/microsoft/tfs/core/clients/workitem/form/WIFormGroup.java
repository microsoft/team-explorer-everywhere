// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.form;

/**
 * WIFormGroup - represents the "GroupType" complex type.
 *
 * Possible children: WIFormColumn
 *
 * @since TEE-SDK-10.1
 */
public interface WIFormGroup extends WIFormElement {

    /**
     * Corresponds to the "Label" attribute in XML use: optional
     */
    public String getLabel();

    /**
     * Corresponds to the "Padding" attribute in XML use: optional
     */
    public WIFormPaddingAttribute getPadding();

    /**
     * Corresponds to the "Margin" attribute in XML use: optional
     */
    public WIFormPaddingAttribute getMargin();

    /**
     * Corresponds to "Column" child elements in XML minOccurs: 1 maxOccurs:
     * unbounded
     */
    public WIFormColumn[] getColumnChildren();
}