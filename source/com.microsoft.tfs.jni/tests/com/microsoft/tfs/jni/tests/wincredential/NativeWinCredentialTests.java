package com.microsoft.tfs.jni.tests.wincredential;

import com.microsoft.tfs.jni.WinCredential;
import com.microsoft.tfs.jni.internal.wincredential.NativeWinCredential;
import com.sun.jna.platform.win32.Kernel32Util;
import junit.framework.TestCase;
import org.junit.Assert;

import java.util.UUID;

public class NativeWinCredentialTests extends TestCase {
    private static final String TEST_SERVER_URI = "teamExplorerEverywhereTest.org";
    private static final String TEST_PASSWORD = "testpassword";

    private final NativeWinCredential nativeWinCredential = new NativeWinCredential();

    private static WinCredential createRandomCredential() {
        return new WinCredential(TEST_SERVER_URI, UUID.randomUUID().toString(), TEST_PASSWORD);
    }

    private static void assertCallResultTrue(boolean callResult) {
        if (!callResult) {
            throw new AssertionError(Kernel32Util.getLastErrorMessage());
        }
    }

    public void testStoreFindCredential() {
        WinCredential testCredential = createRandomCredential();
        String username = testCredential.getAccountName();
        assertCallResultTrue(nativeWinCredential.storeCredential(testCredential));

        WinCredential storedCredential = nativeWinCredential.findCredential(testCredential);
        Assert.assertNotNull(storedCredential);
        Assert.assertEquals(TEST_SERVER_URI, storedCredential.getServerUri());
        Assert.assertEquals(username, storedCredential.getAccountName());
        Assert.assertEquals(TEST_PASSWORD, storedCredential.getPassword());
    }

    public void testEraseCredential() {
        WinCredential testCredential = createRandomCredential();
        assertCallResultTrue(nativeWinCredential.storeCredential(testCredential));

        assertCallResultTrue(nativeWinCredential.eraseCredential(testCredential));

        WinCredential storedCredential = nativeWinCredential.findCredential(testCredential);
        Assert.assertNull(storedCredential);
    }
}
