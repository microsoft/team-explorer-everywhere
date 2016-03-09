// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.jni;

import com.microsoft.tfs.jni.MessageWindow.MessageListener;
import com.microsoft.tfs.util.Platform;

import junit.framework.TestCase;

public class MessageWindowTest extends TestCase {
    public void testCreateSendDestroy() {
        // TODO Return rearly if AllNativeTests.interactiveTestsDisabled() if
        // this test starts causing problems building on a remote machine or
        // headless. Until then, it's nice to run it every time we build the
        // native libraries.
        if (!Platform.isCurrentPlatform(Platform.WINDOWS)) {
            return;
        }

        /*
         * This test is super simple because we don't have SWT available in the
         * JNI plug-in, so we can't receive messages. So we're just testing the
         * basic happy path through the native methods.
         *
         * See the other MessageWindowTest class.
         */

        final MessageWindow mw = new MessageWindow(0L, "TestClass", "title", 0L, new MessageListener() //$NON-NLS-1$ //$NON-NLS-2$
        {
            @Override
            public void messageReceived(final int msg, final long wParam, final long lParam) {
                // Won't happen in this test
            }
        });

        mw.sendMessage("this window class probably won't exist", new long[] { //$NON-NLS-1$
            99
        }, 0x400, 1234, 5678);

        mw.destroyWindow();
    }
}
