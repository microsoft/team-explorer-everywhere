// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core;

import java.util.Arrays;
import java.util.List;

import com.microsoft.tfs.util.MessagesTestCase;

public class MessagesTest extends MessagesTestCase {
    public void testMessages() {
        validate();
    }

    @Override
    protected List<String> getSourceDirectoryNames() {
        return Arrays.asList("src", "rest_core"); //$NON-NLS-1$ //$NON-NLS-2$
    }
}
