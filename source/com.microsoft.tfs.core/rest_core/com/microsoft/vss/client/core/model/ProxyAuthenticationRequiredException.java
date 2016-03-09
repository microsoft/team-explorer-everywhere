// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.vss.client.core.model;

import com.microsoft.tfs.core.Messages;

public class ProxyAuthenticationRequiredException extends VssException {

    public ProxyAuthenticationRequiredException() {
        super(Messages.getString("ProxyAuthenticationRequiredException.ErrorMessage")); //$NON-NLS-1$
        setHelpLink("http://go.microsoft.com/fwlink/?LinkID=324097"); //$NON-NLS-1$
    }
}
