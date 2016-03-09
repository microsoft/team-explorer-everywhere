// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.form;

import com.microsoft.tfs.core.clients.workitem.form.WIFormPaddingAttribute;

public class WIFormPaddingAttributeImpl implements WIFormPaddingAttribute {

    private final String value;

    public WIFormPaddingAttributeImpl(final String value) {
        this.value = value;
    }

    @Override
    public String getValue() {
        return value;
    }

}
