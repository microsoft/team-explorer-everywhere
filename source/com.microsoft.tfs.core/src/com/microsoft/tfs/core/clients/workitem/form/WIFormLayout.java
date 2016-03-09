// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.form;

/**
 * WIFormLayout - represents the "LayoutType" complex type.
 *
 * Possible children: WIFormGroup WIFormControl WIFormTabGroup WIFormSplitter
 *
 * @since TEE-SDK-10.1
 */
public interface WIFormLayout extends WIFormElement {

    /**
     * Corresponds to the "Target" attribute in XML use: optional
     */
    public String getTarget();

    /**
     * Corresponds to the "MinimumSize" attribute in XML use: optional
     */
    public WIFormSizeAttribute getMinimumSize();

    /**
     * Corresponds to the "Padding" attribute in XML use: optional
     */
    public WIFormPaddingAttribute getPadding();

    /**
     * Corresponds to the "Margin" attribute in XML use: optional
     */
    public WIFormPaddingAttribute getMargin();

}