// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.jni.internal.platformmisc;

import java.text.MessageFormat;

import com.microsoft.tfs.jni.PlatformMisc;
import com.microsoft.tfs.util.Platform;

/**
 * A {@link PlatformMisc} implemented with external processes.
 */
public class ExecPlatformMisc implements PlatformMisc {
    private final PlatformMisc delegate;

    public ExecPlatformMisc() {
        if (Platform.isCurrentPlatform(Platform.WINDOWS)) {
            delegate = new WindowsExecPlatformMisc();
        } else if (Platform.isCurrentPlatform(Platform.GENERIC_UNIX)) {
            delegate = new UnixExecPlatformMisc();
        } else {
            throw new RuntimeException(
                MessageFormat.format(
                    "There is no ExecPlatformUtils functionality available for this platform ({0})", //$NON-NLS-1$
                    Platform.getCurrentPlatformString()));
        }
    }

    @Override
    public boolean changeCurrentDirectory(final String directory) {
        return delegate.changeCurrentDirectory(directory);
    }

    @Override
    public String getComputerName() {
        return delegate.getComputerName();
    }

    @Override
    public int getDefaultCodePage() {
        return delegate.getDefaultCodePage();
    }

    @Override
    public String getEnvironmentVariable(final String name) {
        return delegate.getEnvironmentVariable(name);
    }

    @Override
    public String getHomeDirectory(final String username) {
        return delegate.getHomeDirectory(username);
    }

    @Override
    public String expandEnvironmentString(final String value) {
        return delegate.expandEnvironmentString(value);
    }

    @Override
    public String getCurrentIdentityUser() {
        return delegate.getCurrentIdentityUser();
    }

    @Override
    public String getWellKnownSID(final int wellKnownSIDType, final String domainSIDString) {
        return delegate.getWellKnownSID(wellKnownSIDType, domainSIDString);
    }
}
