package com.microsoft.jni.internal.wincredential;

import com.microsoft.tfs.jni.WinCredential;
import com.microsoft.tfs.jni.internal.wincredential.NativeWinCredential;
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

    public void testStoreFindCredential() {
        WinCredential testCredential = createRandomCredential();
        String username = testCredential.getAccountName();
        nativeWinCredential.storeCredential(testCredential);

        WinCredential storedCredential = nativeWinCredential.findCredential(testCredential);
        Assert.assertEquals(TEST_SERVER_URI, storedCredential.getServerUri());
        Assert.assertEquals(username, storedCredential.getAccountName());
        Assert.assertEquals(TEST_PASSWORD, storedCredential.getPassword());
    }

    public void testEraseCredential() {
        WinCredential testCredential = createRandomCredential();
        nativeWinCredential.storeCredential(testCredential);

        nativeWinCredential.eraseCredential(testCredential);

        WinCredential storedCredential = nativeWinCredential.findCredential(testCredential);
        Assert.assertNull(storedCredential);
    }
}
