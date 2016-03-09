// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.form;

/**
 * WIFormSplitter - represents the "SplitterType" complex type.
 *
 * @since TEE-SDK-10.1
 */
public interface WIFormSplitter extends WIFormElement {

    /**
     * Corresponds to the "Dock" attribute in XML use: required
     */
    public WIFormDockEnum getDock();

}