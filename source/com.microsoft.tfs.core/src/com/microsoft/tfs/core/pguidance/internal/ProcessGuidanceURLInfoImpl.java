// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.pguidance.internal;

import com.microsoft.tfs.core.pguidance.ProcessGuidanceURLInfo;

public class ProcessGuidanceURLInfoImpl implements ProcessGuidanceURLInfo {
    private final String url;
    private final boolean valid;
    private final String invalidMessage;

    public ProcessGuidanceURLInfoImpl(final String url, final boolean valid, final String invalidMessage) {
        this.url = url;
        this.valid = valid;
        this.invalidMessage = invalidMessage;
    }

    @Override
    public boolean isValid() {
        return valid;
    }

    @Override
    public String getURL() {
        return url;
    }

    @Override
    public String getInvalidMessage() {
        return invalidMessage;
    }
}
