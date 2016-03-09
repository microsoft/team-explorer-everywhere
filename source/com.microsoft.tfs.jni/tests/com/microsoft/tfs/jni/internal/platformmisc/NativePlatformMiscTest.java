// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.jni.internal.platformmisc;

import java.io.File;
import java.io.IOException;

import com.microsoft.tfs.jni.FileSystemUtils;
import com.microsoft.tfs.jni.WellKnownSID;
import com.microsoft.tfs.util.Platform;

import junit.framework.TestCase;

public class NativePlatformMiscTest extends TestCase {
    private NativePlatformMisc nativeImpl;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        this.nativeImpl = new NativePlatformMisc();
    }

    public void testGetHomeDirectory() throws Exception {
        if (Platform.isCurrentPlatform(Platform.GENERIC_UNIX)) {
            final String userToFind = System.getProperty("user.name"); //$NON-NLS-1$
            final ExecPlatformMisc epm = new ExecPlatformMisc();
            final String homeOutput = epm.getHomeDirectory(userToFind);

            assertEquals(homeOutput, nativeImpl.getHomeDirectory(userToFind));

            // Should not exist.
            assertNull(nativeImpl.getHomeDirectory("fdljsafklsdjlkfdsafds")); //$NON-NLS-1$
            assertNull(nativeImpl.getHomeDirectory("XYZ")); //$NON-NLS-1$
        }

        if (Platform.isCurrentPlatform(Platform.SOLARIS) || Platform.isCurrentPlatform(Platform.HPUX)) {
            assertEquals("/usr/bin", nativeImpl.getHomeDirectory("bin")); //$NON-NLS-1$ //$NON-NLS-2$
        }

        if (Platform.isCurrentPlatform(Platform.LINUX)) {
            assertEquals("/bin", nativeImpl.getHomeDirectory("bin")); //$NON-NLS-1$ //$NON-NLS-2$
        }

        if (Platform.isCurrentPlatform(Platform.MAC_OS_X)) {
            assertEquals("/var/root", nativeImpl.getHomeDirectory("root")); //$NON-NLS-1$ //$NON-NLS-2$
        }

        if (Platform.isCurrentPlatform(Platform.Z_OS)) {
            /*
             * "ibmuser" is the IBM-configured superuser for our remote
             * development z/OS box.
             */
            assertEquals("/u/ibmuser", nativeImpl.getHomeDirectory("IBMUSER")); //$NON-NLS-1$ //$NON-NLS-2$
            assertEquals("/u/ibmuser", nativeImpl.getHomeDirectory("ibmuser")); //$NON-NLS-1$ //$NON-NLS-2$
        }

        if (Platform.isCurrentPlatform(Platform.WINDOWS)) {
            // Should always return null since Windows doesn't have a "home
            // directory" notational convention.
            assertNull(nativeImpl.getHomeDirectory(System.getProperty("user.name"))); //$NON-NLS-1$
            assertNull(nativeImpl.getHomeDirectory("fun")); //$NON-NLS-1$
            assertNull(nativeImpl.getHomeDirectory("Administrator")); //$NON-NLS-1$
        }
    }

    public void testGetEnvironmentVariable() throws Exception {
        // Should be set.
        if (Platform.isCurrentPlatform(Platform.GENERIC_UNIX)) {
            assertEquals(System.getProperty("user.name"), nativeImpl.getEnvironmentVariable("USER")); //$NON-NLS-1$ //$NON-NLS-2$
        } else {
            assertEquals(System.getProperty("user.name"), nativeImpl.getEnvironmentVariable("USERNAME")); //$NON-NLS-1$ //$NON-NLS-2$
        }

        // Should not be set.
        assertNull(nativeImpl.getEnvironmentVariable("flkdasjflidsjlaifjdsa")); //$NON-NLS-1$
        assertNull(nativeImpl.getEnvironmentVariable("j82fj3")); //$NON-NLS-1$

        // A huge variable name.
        final StringBuffer sb = new StringBuffer();
        for (int i = 0; i < 12000; i++) {
            sb.append("x"); //$NON-NLS-1$
        }
        assertNull(nativeImpl.getEnvironmentVariable(sb.toString()));
    }

    public void testExpandEnvironmentString() {
        if (Platform.isCurrentPlatform(Platform.WINDOWS)) {
            final String user = nativeImpl.getEnvironmentVariable("USERNAME"); //$NON-NLS-1$
            String expanded = nativeImpl.expandEnvironmentString("%USERNAME%"); //$NON-NLS-1$
            assertEquals(user, expanded);

            final String domain = nativeImpl.getEnvironmentVariable("USERDOMAIN"); //$NON-NLS-1$
            expanded = nativeImpl.expandEnvironmentString("%USERDOMAIN%"); //$NON-NLS-1$
            assertEquals(domain, expanded);

            expanded = nativeImpl.expandEnvironmentString("%USERNAME%%USERDOMAIN%"); //$NON-NLS-1$
            assertEquals(user + domain, expanded);

            String value = "%ThisVariableDoesNotExist%"; //$NON-NLS-1$
            expanded = nativeImpl.expandEnvironmentString(value);
            assertEquals(value, expanded);

            value = ""; //$NON-NLS-1$
            expanded = nativeImpl.expandEnvironmentString(value);
            assertEquals(value, expanded);
        } else {
            // Non-Windows platorms merely return the passed in value.
            final String original = "%USERNAME%"; //$NON-NLS-1$
            final String expanded = nativeImpl.expandEnvironmentString(original);
            assertEquals(original, expanded);
        }
    }

    public void testGetDefaultCodePage() throws Exception {
        if (Platform.isCurrentPlatform(Platform.WINDOWS)) {
            assertTrue(nativeImpl.getDefaultCodePage() > 0);
        } else {
            assertTrue(nativeImpl.getDefaultCodePage() == -1);
        }
    }

    public void testGetComputerName() throws Exception {
        assertNotNull(nativeImpl.getComputerName());
        assertTrue(nativeImpl.getComputerName().length() > 0);

        final ExecPlatformMisc epm = new ExecPlatformMisc();
        final String computerName = epm.getComputerName();

        assertEquals(computerName, nativeImpl.getComputerName());
    }

    public void testChangeCurrentDirectory() throws Exception {
        final String originalDirectory = System.getProperty("user.dir"); //$NON-NLS-1$

        final File newTempFile = File.createTempFile("chdir", "test"); //$NON-NLS-1$ //$NON-NLS-2$
        final File newDirectory = new File(newTempFile.getParent() + File.separator + newTempFile.getName());
        newTempFile.delete();
        newDirectory.mkdir();

        /*
         * Try to set it back even on failure, for the sanity of the outer VM.
         */
        try {
            nativeImpl.changeCurrentDirectory(newDirectory.getCanonicalPath());
            assertEquals(System.getProperty("user.dir"), newDirectory.getCanonicalPath()); //$NON-NLS-1$
        } finally {
            nativeImpl.changeCurrentDirectory(originalDirectory);
            newDirectory.delete();
        }
    }

    public void testGetCurrentIdentityUser() throws IOException {
        // Not supported on Unix
        if (Platform.isCurrentPlatform(Platform.GENERIC_UNIX)) {
            return;
        }

        final File testFile = File.createTempFile("testGetCurrentIdentityUser", ".txt"); //$NON-NLS-1$ //$NON-NLS-2$

        assertEquals(
            "file we just created isn't owned by process owner", //$NON-NLS-1$
            FileSystemUtils.getInstance().getOwner(testFile.getAbsolutePath()),
            nativeImpl.getCurrentIdentityUser());

        testFile.delete();
    }

    public void testGetWellKnownSID() {
        // Not supported on Unix
        if (Platform.isCurrentPlatform(Platform.GENERIC_UNIX)) {
            return;
        }

        // See http://support.microsoft.com/kb/243330 for some well known SIDs
        // and their formats

        // Test some well known SIDs that don't require a domain

        assertEquals(
            "can't resolve Creator Owner", //$NON-NLS-1$
            "S-1-3-0", //$NON-NLS-1$
            nativeImpl.getWellKnownSID(WellKnownSID.WinCreatorOwnerSid, null));

        assertEquals(
            "can't resolve Build-in Administrators Group", //$NON-NLS-1$
            "S-1-5-32-544", //$NON-NLS-1$
            nativeImpl.getWellKnownSID(WellKnownSID.WinBuiltinAdministratorsSid, null));

        // Test some that do require a domain

        // Format: S-1-5-21<domain>-512
        assertEquals(
            "can't resolve Domain Admins", //$NON-NLS-1$
            "S-1-5-21" + "-3623811015-3361044348-30300820-1013" + "-512", //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$
            nativeImpl.getWellKnownSID(
                WellKnownSID.WinAccountDomainAdminsSid,
                "S-1-5-21-3623811015-3361044348-30300820-1013")); //$NON-NLS-1$
    }
}