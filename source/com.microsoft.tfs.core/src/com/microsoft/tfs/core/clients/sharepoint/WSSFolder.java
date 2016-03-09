// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.sharepoint;

import org.w3c.dom.Element;

/**
 * A folder (which contains folders and documents) in Sharepoint.
 *
 * @since TEE-SDK-10.1
 * @threadsafety thread-compatible
 */
public class WSSFolder extends WSSNode {
    public WSSFolder() {
        super();
    }

    public WSSFolder(final Element element) {
        super(element);
    }
}
