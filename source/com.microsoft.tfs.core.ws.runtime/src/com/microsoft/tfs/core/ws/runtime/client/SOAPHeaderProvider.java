// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.ws.runtime.client;

import org.w3c.dom.Element;

public interface SOAPHeaderProvider {
    public Element getSOAPHeader();
}
