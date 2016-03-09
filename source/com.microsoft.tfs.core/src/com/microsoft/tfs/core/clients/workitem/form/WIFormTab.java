// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.form;

/**
 * WIFormTab - represents the "TabType" complex type.
 *
 * Possible children: WIFormGroup WIFormControl WIFormTabGroup WIFormSplitter
 *
 * @since TEE-SDK-10.1
 */
public interface WIFormTab extends WIFormElement {

    /**
     * Corresponds to the "Label" attribute in XML use: required
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

}