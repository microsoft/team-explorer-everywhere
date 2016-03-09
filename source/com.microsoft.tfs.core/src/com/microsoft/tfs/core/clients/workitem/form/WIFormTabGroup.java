// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.form;

/**
 * WIFormTabGroup - represents the "TabGroupType" complex type.
 *
 * Possible children: WIFormTab
 *
 * @since TEE-SDK-10.1
 */
public interface WIFormTabGroup extends WIFormElement {

    /**
     * Corresponds to the "Padding" attribute in XML use: optional
     */
    public WIFormPaddingAttribute getPadding();

    /**
     * Corresponds to the "Margin" attribute in XML use: optional
     */
    public WIFormPaddingAttribute getMargin();

    /**
     * Corresponds to "Tab" child elements in XML minOccurs: 1 maxOccurs:
     * unbounded
     */
    public WIFormTab[] getTabChildren();
}