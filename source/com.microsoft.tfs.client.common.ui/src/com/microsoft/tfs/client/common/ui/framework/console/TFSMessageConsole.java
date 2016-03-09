// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.framework.console;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.console.MessageConsole;

public class TFSMessageConsole extends MessageConsole {
    public TFSMessageConsole(final String name, final ImageDescriptor imageDescriptor) {
        super(name, imageDescriptor);
    }
}
