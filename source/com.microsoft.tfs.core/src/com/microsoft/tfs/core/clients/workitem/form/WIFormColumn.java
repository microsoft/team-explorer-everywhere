// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.form;

/**
 * WIFormColumn - represents the "ColumnType" complex type.
 *
 * Possible children: WIFormGroup WIFormControl WIFormTabGroup WIFormSplitter
 *
 * @since TEE-SDK-10.1
 */
public interface WIFormColumn extends WIFormElement {

    /**
     * Corresponds to the "PercentWidth" attribute in XML use: optional
     */
    public Integer getPercentWidth();

    /**
     * Corresponds to the "FixedWidth" attribute in XML use: optional
     */
    public Integer getFixedWidth();

}